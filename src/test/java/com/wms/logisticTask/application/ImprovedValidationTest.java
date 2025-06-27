package com.wms.logisticTask.application;

import com.wms.location.domain.model.Location;
import com.wms.location.domain.model.LocationConnection;
import com.wms.location.domain.model.LocationType;
import com.wms.location.domain.repository.LocationConnectionRepository;
import com.wms.location.domain.repository.LocationRepository;
import com.wms.location.application.LocationCacheService;
import com.wms.logisticTask.domain.exception.LogisticTaskException;
import com.wms.logisticTask.domain.model.LogisticTask;
import com.wms.logisticTemplate.domain.model.LogisticType;
import com.wms.stock.application.StockCacheService;
import com.wms.stock.domain.model.Stock;
import com.wms.stock.domain.model.StockKey;
import com.wms.stock.domain.repository.StockRepository;
import com.wms.userInfo.domain.model.Password;
import com.wms.userInfo.domain.model.UserInfo;
import com.wms.userInfo.domain.model.UserType;
import com.wms.userInfo.domain.repository.UserInfoRepository;
import com.wms.ware.domain.model.Ware;
import com.wms.ware.domain.repository.WareRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.*;

/**
 * 개선된 물류작업 검증 테스트
 * 
 * 개선사항:
 * - 상수 체계적 관리
 * - 픽스처 Factory 패턴  
 * - 캐시 관리 헬퍼
 * - 시나리오 기반 명명
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("개선된 물류작업 검증 테스트")
class ImprovedValidationTest {

    // ===== 테스트 상수 =====
    public static class Constants {
        public static final class Warehouse {
            public static final int A_CAPACITY = 100;
            public static final int B_CAPACITY = 55;
            public static final int A_STOCK = 30;
            public static final int B_STOCK = 30;
        }
        
        public static final class Time {
            public static final LocalDate TOMORROW = LocalDate.now().plusDays(1);
            public static final LocalTime MORNING_START = LocalTime.of(9, 0);
            public static final LocalTime MORNING_END = LocalTime.of(10, 0);
        }
        
        public static final class Quantities {
            public static final int SMALL = 5;
            public static final int MEDIUM = 10;
            public static final int LARGE = 20;
            public static final int EXCESS = 35; // 재고 초과
        }
    }

    // ===== 테스트 픽스처 =====
    public static class TaskFixture {
        
        public static LogisticTask.LogisticTaskBuilder validTask() {
            return LogisticTask.builder()
                    .name("기본 작업")
                    .type(LogisticType.INNER)
                    .quantity(Constants.Quantities.SMALL)
                    .scheduledDate(Constants.Time.TOMORROW)
                    .etd(Constants.Time.MORNING_START)
                    .eta(Constants.Time.MORNING_END);
        }
        
        public static LogisticTask.LogisticTaskBuilder stockShortageTask() {
            return validTask()
                    .name("재고 부족 작업")
                    .quantity(Constants.Quantities.EXCESS);
        }
        
        public static LogisticTask.LogisticTaskBuilder exactStockTask() {
            return validTask()
                    .name("전체 재고 이동")
                    .quantity(Constants.Warehouse.A_STOCK);
        }
    }
    
    // ===== 캐시 관리자 =====
    public class CacheManager {
        
        public void resetCaches() {
            stockCacheService.updateInventoryQuantity(
                StockKey.of(laptop.getId(), warehouseA.getId()), 
                Constants.Warehouse.A_STOCK);
            stockCacheService.updateInventoryQuantity(
                StockKey.of(laptop.getId(), warehouseB.getId()), 
                Constants.Warehouse.B_STOCK);
                
            locationCacheService.updateWarehouseCapacity(warehouseA.getId(), Constants.Warehouse.A_CAPACITY);
            locationCacheService.updateWarehouseCapacity(warehouseB.getId(), Constants.Warehouse.B_CAPACITY);
            
            stockCacheService.updateWarehouseCurrentSum(warehouseA.getId(), Constants.Warehouse.A_STOCK);
            stockCacheService.updateWarehouseCurrentSum(warehouseB.getId(), Constants.Warehouse.B_STOCK);
        }
        
        public void increaseWarehouseBCapacity(int newCapacity) {
            warehouseB.changeCapacity(newCapacity);
            locationRepository.save(warehouseB);
            locationCacheService.updateWarehouseCapacity(warehouseB.getId(), newCapacity);
        }
    }

    @Autowired private LogisticTaskValidationService validationService;
    @Autowired private LocationRepository locationRepository;
    @Autowired private StockRepository stockRepository;
    @Autowired private LocationConnectionRepository locationConnectionRepository;
    @Autowired private UserInfoRepository userInfoRepository;
    @Autowired private WareRepository wareRepository;
    @Autowired private StockCacheService stockCacheService;
    @Autowired private LocationCacheService locationCacheService;

    private CacheManager cacheManager;
    private UserInfo worker1, worker2;
    private Ware laptop;
    private Location warehouseA, warehouseB, inboundLocation;

    @BeforeEach
    void setUp() {
        this.cacheManager = new CacheManager();
        setupTestData();
        cacheManager.resetCaches();
    }

    private void setupTestData() {
        // 작업자
        worker1 = userInfoRepository.save(UserInfo.builder()
                .username("worker1").name("김작업").email("worker1@test.com")
                .password(new Password("password")).type(UserType.WORKER).build());
        worker2 = userInfoRepository.save(UserInfo.builder()
                .username("worker2").name("박작업").email("worker2@test.com")
                .password(new Password("password")).type(UserType.WORKER).build());

        // 물품
        laptop = wareRepository.save(Ware.builder()
                .name("노트북").type("전자제품").paletteUnit(20).build());

        // 장소
        warehouseA = locationRepository.save(Location.builder()
                .name("창고A").type(LocationType.WAREHOUSE)
                .capacity(Constants.Warehouse.A_CAPACITY)
                .coordinateX(100).coordinateY(100).build());
        warehouseB = locationRepository.save(Location.builder()
                .name("창고B").type(LocationType.WAREHOUSE)
                .capacity(Constants.Warehouse.B_CAPACITY)
                .coordinateX(200).coordinateY(100).build());
        inboundLocation = locationRepository.save(Location.builder()
                .name("입고장").type(LocationType.INBOUND)
                .coordinateX(50).coordinateY(50).build());

        // 재고
        stockRepository.save(Stock.builder()
                .key(StockKey.of(laptop.getId(), warehouseA.getId()))
                .quantity(Constants.Warehouse.A_STOCK).build());
        stockRepository.save(Stock.builder()
                .key(StockKey.of(laptop.getId(), warehouseB.getId()))
                .quantity(Constants.Warehouse.B_STOCK).build());

        // 연결
        locationConnectionRepository.save(LocationConnection.builder()
                .locationId1(Math.min(warehouseA.getId(), warehouseB.getId()))
                .locationId2(Math.max(warehouseA.getId(), warehouseB.getId()))
                .trt(30).build());
    }

    // ===== 테스트 케이스 =====

    @Test
    @DisplayName("시나리오: 기본 유효 작업 - 성공")
    void scenario_ValidTask_Success() {
        // Given
        LogisticTask task = TaskFixture.validTask()
                .worker(worker1).ware(laptop)
                .fromLocation(warehouseA).toLocation(warehouseB)
                .build();

        // When & Then
        assertThatCode(() -> validationService.validateTaskCreation(task))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("시나리오: 재고 부족 - 실패")
    void scenario_StockShortage_Fails() {
        // Given
        LogisticTask task = TaskFixture.stockShortageTask()
                .worker(worker1).ware(laptop)
                .fromLocation(warehouseA).toLocation(warehouseB)
                .build();

        // When & Then
        assertThatThrownBy(() -> validationService.validateTaskCreation(task))
                .isInstanceOf(LogisticTaskException.ValidationEx.class)
                .hasMessageContaining("재고 부족");
    }

    @Test
    @DisplayName("시나리오: 전체 재고 이동 - 용량 증가 후 성공")
    void scenario_FullStockMove_SucceedsAfterCapacityIncrease() {
        // Given: 창고B 용량 증가
        cacheManager.increaseWarehouseBCapacity(70);
        
        LogisticTask task = TaskFixture.exactStockTask()
                .worker(worker1).ware(laptop)
                .fromLocation(warehouseA).toLocation(warehouseB)
                .build();

        // When & Then
        assertThatCode(() -> validationService.validateTaskCreation(task))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("시나리오: 입고 작업 - 재고 제약 없이 성공")
    void scenario_InboundTask_NoStockConstraints() {
        // Given
        LogisticTask task = TaskFixture.validTask()
                .name("입고 작업").type(LogisticType.INBOUND)
                .quantity(Constants.Quantities.LARGE)
                .worker(worker1).ware(laptop)
                .fromLocation(inboundLocation).toLocation(warehouseA)
                .build();

        // When & Then
        assertThatCode(() -> validationService.validateTaskCreation(task))
                .doesNotThrowAnyException();
    }
}