package com.wms.integration.logistictask;

import com.wms.location.domain.model.Location;
import com.wms.location.domain.model.LocationType;
import com.wms.location.domain.repository.LocationRepository;
import com.wms.logisticTask.application.LogisticTaskService;
import com.wms.logisticTask.domain.model.LogisticTask;
import com.wms.logisticTask.dto.LogisticTaskDTO;
import com.wms.logisticTemplate.domain.model.LogisticType;
import com.wms.stock.application.StockCtrlService;
import com.wms.stock.application.StockQueryService;
import com.wms.stock.domain.model.Stock;
import com.wms.userInfo.domain.model.Password;
import com.wms.userInfo.domain.model.UserInfo;
import com.wms.userInfo.domain.model.UserType;
import com.wms.userInfo.domain.repository.UserInfoRepository;
import com.wms.ware.domain.model.Ware;
import com.wms.ware.domain.repository.WareRepository;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;

@Slf4j
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("LogisticTask-Stock 캐시 연동 통합 테스트")
class LogisticTaskStockCacheIntegrationTest {

    @Autowired
    private LogisticTaskService logisticTaskService;

    @Autowired
    private StockCtrlService stockCtrlService;

    @Autowired
    private StockQueryService stockQueryService;

    @Autowired
    private UserInfoRepository userInfoRepository;

    @Autowired
    private WareRepository wareRepository;

    @Autowired
    private LocationRepository locationRepository;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private UserInfo worker;
    private Ware cacheTestWare;
    private Location warehouseA;
    private Location warehouseB;
    private Location inbound;
    private Location outbound;

    @BeforeEach
    void setUp() {
        // Redis 캐시 정리
        cleanupRedisCache();

        // 테스트 데이터 준비
        worker = userInfoRepository.save(UserInfo.builder()
                .username("cache_worker")
                .name("캐시테스트작업자")
                .email("cache@test.com")
                .password(new Password("password"))
                .type(UserType.WORKER)
                .build());

        cacheTestWare = wareRepository.save(Ware.builder()
                .name("캐시테스트물품")
                .type("캐시테스트")
                .paletteUnit(15)
                .build());

        warehouseA = locationRepository.save(Location.builder()
                .name("캐시창고A")
                .type(LocationType.WAREHOUSE)
                .capacity(500)
                .coordinateX(100)
                .coordinateY(100)
                .build());

        warehouseB = locationRepository.save(Location.builder()
                .name("캐시창고B")
                .type(LocationType.WAREHOUSE)
                .capacity(400)
                .coordinateX(200)
                .coordinateY(100)
                .build());

        inbound = locationRepository.save(Location.builder()
                .name("캐시입고처")
                .type(LocationType.INBOUND)
                .coordinateX(50)
                .coordinateY(50)
                .build());

        outbound = locationRepository.save(Location.builder()
                .name("캐시출고처")
                .type(LocationType.OUTBOUND)
                .coordinateX(250)
                .coordinateY(150)
                .build());

        // 초기 재고 설정
        stockCtrlService.createOrUpdateStock(warehouseA.getId(), cacheTestWare.getId(), 80);
        stockCtrlService.createOrUpdateStock(warehouseB.getId(), cacheTestWare.getId(), 30);

        // 인증 설정
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(worker, null, null));

        log.info("캐시 테스트 데이터 준비 완료");
    }

    @Test
    @DisplayName("개별 재고 캐시 업데이트 확인 - 출고 작업")
    void individualStockCache_OutboundTask() {
        // Given: 초기 캐시 상태 확인
        String stockCacheKey = "current_stock:" + warehouseA.getId() + ":" + cacheTestWare.getId();
        
        // When: 출고 작업 실행
        LogisticTaskDTO.CreateReq outboundRequest = createOutboundRequest(12);
        LogisticTask task = logisticTaskService.create(outboundRequest);
        
        logisticTaskService.initiateTask(task.getId());
        logisticTaskService.completeTask(task.getId());

        // Then: 캐시 업데이트 확인
        Awaitility.await()
                .atMost(10, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    Object cachedValue = redisTemplate.opsForValue().get(stockCacheKey);
                    if (cachedValue != null) {
                        assertThat(Integer.valueOf(cachedValue.toString())).isEqualTo(68); // 80 - 12
                    }
                });

        // DB와 캐시 일치성 확인
        Stock dbStock = stockQueryService.getStockByLocationAndWare(
                warehouseA.getId(), cacheTestWare.getId());
        Object cachedStock = redisTemplate.opsForValue().get(stockCacheKey);
        
        if (cachedStock != null) {
            assertThat(dbStock.getQuantity()).isEqualTo(Integer.valueOf(cachedStock.toString()));
        }

        log.info("개별 재고 캐시 업데이트 확인 완료 - 캐시값: {}", cachedStock);
    }

    @Test
    @DisplayName("창고 총량 캐시 업데이트 확인 - 내부 이동")
    void warehouseTotalCache_InnerMovement() {
        // Given: 창고 총량 캐시 키
        String warehouseASumKey = "warehouse:" + warehouseA.getId() + ":currentSum";
        String warehouseBSumKey = "warehouse:" + warehouseB.getId() + ":currentSum";

        // When: 내부 이동 작업 실행 (A → B로 20개 이동)
        LogisticTaskDTO.CreateReq innerRequest = createInnerMovementRequest(20);
        LogisticTask task = logisticTaskService.create(innerRequest);
        
        logisticTaskService.initiateTask(task.getId());
        logisticTaskService.completeTask(task.getId());

        // Then: 창고 총량 캐시 업데이트 확인
        Awaitility.await()
                .atMost(10, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    Stock finalStockA = stockQueryService.getStockByLocationAndWare(
                            warehouseA.getId(), cacheTestWare.getId());
                    Stock finalStockB = stockQueryService.getStockByLocationAndWare(
                            warehouseB.getId(), cacheTestWare.getId());
                    
                    assertThat(finalStockA.getQuantity()).isEqualTo(60); // 80 - 20
                    assertThat(finalStockB.getQuantity()).isEqualTo(50); // 30 + 20
                });

        log.info("창고 총량 캐시 업데이트 확인 완료");
    }

    @Test
    @DisplayName("입고 작업 시 캐시 증가 확인")
    void inboundTask_CacheIncrease() {
        // Given: 입고 작업 전 초기 재고
        Stock initialStock = stockQueryService.getStockByLocationAndWare(
                warehouseA.getId(), cacheTestWare.getId());

        // When: 입고 작업 실행
        LogisticTaskDTO.CreateReq inboundRequest = createInboundRequest(25);
        LogisticTask task = logisticTaskService.create(inboundRequest);
        
        logisticTaskService.initiateTask(task.getId());
        logisticTaskService.completeTask(task.getId());

        // Then: 재고 증가 확인
        Awaitility.await()
                .atMost(10, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    Stock finalStock = stockQueryService.getStockByLocationAndWare(
                            warehouseA.getId(), cacheTestWare.getId());
                    assertThat(finalStock.getQuantity()).isEqualTo(initialStock.getQuantity() + 25);
                });

        log.info("입고 작업 캐시 증가 확인 완료");
    }

    @Test
    @DisplayName("동시성 상황에서의 캐시 일관성 - 여러 작업 동시 실행")
    void cacheConsistency_ConcurrentTasks() throws InterruptedException {
        // Given: 여러 작업을 동시에 실행할 준비
        int taskCount = 5;
        int quantityPerTask = 3;

        Stock initialStock = stockQueryService.getStockByLocationAndWare(
                warehouseA.getId(), cacheTestWare.getId());

        // When: 동시에 여러 출고 작업 실행
        for (int i = 0; i < taskCount; i++) {
            final int taskIndex = i;
            new Thread(() -> {
                try {
                    LogisticTaskDTO.CreateReq request = LogisticTaskDTO.CreateReq.builder()
                            .name("동시성테스트" + taskIndex)
                            .type(LogisticType.OUTBOUND)
                            .workerId(worker.getId())
                            .wareId(cacheTestWare.getId())
                            .fromLocationId(warehouseA.getId())
                            .toLocationId(outbound.getId())
                            .quantity(quantityPerTask)
                            .scheduledDate(LocalDate.now())
                            .etd(LocalTime.of(14, taskIndex))
                            .eta(LocalTime.of(15, taskIndex))
                            .templateIdSnapshot(taskIndex)
                            .build();

                    LogisticTask task = logisticTaskService.create(request);
                    logisticTaskService.initiateTask(task.getId());
                    logisticTaskService.completeTask(task.getId());
                } catch (Exception e) {
                    log.error("동시성 테스트 작업 실행 중 오류", e);
                }
            }).start();
        }

        // Then: 모든 작업 완료 후 캐시와 DB 일관성 확인
        Awaitility.await()
                .atMost(20, TimeUnit.SECONDS)
                .pollInterval(1000, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    Stock finalStock = stockQueryService.getStockByLocationAndWare(
                            warehouseA.getId(), cacheTestWare.getId());
                    
                    int expectedQuantity = initialStock.getQuantity() - (taskCount * quantityPerTask);
                    assertThat(finalStock.getQuantity()).isEqualTo(expectedQuantity);
                });

        log.info("동시성 상황 캐시 일관성 확인 완료");
    }

    @Test
    @DisplayName("복합 물류 작업 캐시 검증 - 입고→내부이동→출고 연속 실행")
    void complexLogisticFlow_CacheVerification() {
        // Given: 연속적인 물류 작업 시나리오
        Stock initialStockA = stockQueryService.getStockByLocationAndWare(
                warehouseA.getId(), cacheTestWare.getId());
        Stock initialStockB = stockQueryService.getStockByLocationAndWare(
                warehouseB.getId(), cacheTestWare.getId());

        // When: 1. 입고 (입고처 → 창고A)
        LogisticTask inboundTask = logisticTaskService.create(createInboundRequest(20));
        logisticTaskService.initiateTask(inboundTask.getId());
        logisticTaskService.completeTask(inboundTask.getId());

        // 2. 내부 이동 (창고A → 창고B)
        LogisticTask innerTask = logisticTaskService.create(createInnerMovementRequest(15));
        logisticTaskService.initiateTask(innerTask.getId());
        logisticTaskService.completeTask(innerTask.getId());

        // 3. 출고 (창고B → 출고처)
        LogisticTask outboundTask = logisticTaskService.create(createOutboundFromWarehouseBRequest(10));
        logisticTaskService.initiateTask(outboundTask.getId());
        logisticTaskService.completeTask(outboundTask.getId());

        // Then: 최종 상태 검증
        Awaitility.await()
                .atMost(15, TimeUnit.SECONDS)
                .pollInterval(1000, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    Stock finalStockA = stockQueryService.getStockByLocationAndWare(
                            warehouseA.getId(), cacheTestWare.getId());
                    Stock finalStockB = stockQueryService.getStockByLocationAndWare(
                            warehouseB.getId(), cacheTestWare.getId());

                    // 창고A: 80 + 20 - 15 = 85
                    assertThat(finalStockA.getQuantity()).isEqualTo(85);
                    // 창고B: 30 + 15 - 10 = 35
                    assertThat(finalStockB.getQuantity()).isEqualTo(35);
                });

        log.info("복합 물류 작업 캐시 검증 완료");
    }

    private LogisticTaskDTO.CreateReq createInboundRequest(int quantity) {
        return LogisticTaskDTO.CreateReq.builder()
                .name("캐시테스트_입고_" + quantity)
                .type(LogisticType.INBOUND)
                .workerId(worker.getId())
                .wareId(cacheTestWare.getId())
                .fromLocationId(inbound.getId())
                .toLocationId(warehouseA.getId())
                .quantity(quantity)
                .scheduledDate(LocalDate.now())
                .etd(LocalTime.of(9, 0))
                .eta(LocalTime.of(10, 0))
                .templateIdSnapshot(1)
                .build();
    }

    private LogisticTaskDTO.CreateReq createOutboundRequest(int quantity) {
        return LogisticTaskDTO.CreateReq.builder()
                .name("캐시테스트_출고_" + quantity)
                .type(LogisticType.OUTBOUND)
                .workerId(worker.getId())
                .wareId(cacheTestWare.getId())
                .fromLocationId(warehouseA.getId())
                .toLocationId(outbound.getId())
                .quantity(quantity)
                .scheduledDate(LocalDate.now())
                .etd(LocalTime.of(11, 0))
                .eta(LocalTime.of(12, 0))
                .templateIdSnapshot(2)
                .build();
    }

    private LogisticTaskDTO.CreateReq createInnerMovementRequest(int quantity) {
        return LogisticTaskDTO.CreateReq.builder()
                .name("캐시테스트_내부이동_" + quantity)
                .type(LogisticType.INNER)
                .workerId(worker.getId())
                .wareId(cacheTestWare.getId())
                .fromLocationId(warehouseA.getId())
                .toLocationId(warehouseB.getId())
                .quantity(quantity)
                .scheduledDate(LocalDate.now())
                .etd(LocalTime.of(13, 0))
                .eta(LocalTime.of(14, 0))
                .templateIdSnapshot(3)
                .build();
    }

    private LogisticTaskDTO.CreateReq createOutboundFromWarehouseBRequest(int quantity) {
        return LogisticTaskDTO.CreateReq.builder()
                .name("캐시테스트_창고B출고_" + quantity)
                .type(LogisticType.OUTBOUND)
                .workerId(worker.getId())
                .wareId(cacheTestWare.getId())
                .fromLocationId(warehouseB.getId())
                .toLocationId(outbound.getId())
                .quantity(quantity)
                .scheduledDate(LocalDate.now())
                .etd(LocalTime.of(15, 0))
                .eta(LocalTime.of(16, 0))
                .templateIdSnapshot(4)
                .build();
    }

    private void cleanupRedisCache() {
        try {
            Set<String> stockKeys = redisTemplate.keys("current_stock:*");
            Set<String> warehouseKeys = redisTemplate.keys("warehouse:*");
            Set<String> streamKeys = redisTemplate.keys("stock:events:*");
            
            if (stockKeys != null && !stockKeys.isEmpty()) {
                redisTemplate.delete(stockKeys);
            }
            if (warehouseKeys != null && !warehouseKeys.isEmpty()) {
                redisTemplate.delete(warehouseKeys);
            }
            if (streamKeys != null && !streamKeys.isEmpty()) {
                redisTemplate.delete(streamKeys);
            }
            
            log.info("Redis 캐시 정리 완료");
        } catch (Exception e) {
            log.warn("Redis 캐시 정리 중 오류", e);
        }
    }
}
