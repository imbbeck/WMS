package com.wms.logisticTask.application;

import com.wms.location.domain.model.Location;
import com.wms.location.domain.model.LocationConnection;
import com.wms.location.domain.model.LocationType;
import com.wms.location.domain.repository.LocationConnectionRepository;
import com.wms.location.domain.repository.LocationRepository;
import com.wms.location.application.LocationCacheService;
import com.wms.logisticTask.domain.exception.LogisticTaskException;
import com.wms.logisticTask.domain.model.LogisticTask;
import com.wms.logisticTask.domain.model.LogisticTaskStatus;
import com.wms.logisticTask.domain.repository.LogisticTaskRepository;
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
 * LogisticTaskValidationService 상수 기반 리팩토링된 테스트
 * 초기 데이터를 상수로 관리하고 Given 절을 명확하게 표현
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("LogisticTaskValidationService 통합 테스트")
class LogisticTaskValidationServiceIntegrationTest {

    // ===== 테스트 상수 정의 =====

    // 창고 용량
    private static final int WAREHOUSE_A_CAPACITY = 100;
    private static final int WAREHOUSE_B_CAPACITY = 55;

    // 초기 재고량
    private static final int WAREHOUSE_A_LAPTOP_STOCK = 30;
    private static final int WAREHOUSE_B_LAPTOP_STOCK = 30;

    // 창고 사용량 (초기 재고 합계)
    private static final int WAREHOUSE_A_USAGE = WAREHOUSE_A_LAPTOP_STOCK;
    private static final int WAREHOUSE_B_USAGE = WAREHOUSE_B_LAPTOP_STOCK;

    // 장소 간 이동 시간
    private static final int TRAVEL_TIME_MINUTES = 30;

    // 테스트 날짜 및 시간
    private static final LocalDate TOMORROW = LocalDate.now().plusDays(1);
    private static final LocalDate YESTERDAY = LocalDate.now().minusDays(1);
    private static final LocalTime MORNING_START = LocalTime.of(9, 0);
    private static final LocalTime MORNING_END = LocalTime.of(10, 0);
    private static final LocalTime AFTERNOON_START = LocalTime.of(14, 0);
    private static final LocalTime AFTERNOON_END = LocalTime.of(15, 0);

    @Autowired
    private LogisticTaskValidationService validationService;

    @Autowired
    private LogisticTaskRepository logisticTaskRepository;

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private LocationRepository locationRepository;

    @Autowired
    private LocationConnectionRepository locationConnectionRepository;

    @Autowired
    private UserInfoRepository userInfoRepository;

    @Autowired
    private WareRepository wareRepository;

    @Autowired
    private StockCacheService stockCacheService;

    @Autowired
    private LocationCacheService locationCacheService;

    private UserInfo worker1;
    private UserInfo worker2;
    private Ware laptop;
    private Location warehouseA;
    private Location warehouseB;
    private Location inboundLocation;
    private Location outboundLocation;

    @BeforeEach
    void setUp() {
        // 작업자 생성
        worker1 = userInfoRepository.save(UserInfo.builder()
                .username("worker1")
                .name("김작업")
                .email("worker1@test.com")
                .password(new Password("password"))
                .type(UserType.WORKER)
                .build());

        worker2 = userInfoRepository.save(UserInfo.builder()
                .username("worker2")
                .name("박작업")
                .email("worker2@test.com")
                .password(new Password("password"))
                .type(UserType.WORKER)
                .build());

        // 물품 생성
        laptop = wareRepository.save(Ware.builder()
                .name("노트북")
                .type("전자제품")
                .paletteUnit(20)
                .build());

        // 장소 생성
        warehouseA = locationRepository.save(Location.builder()
                .name("창고A")
                .type(LocationType.WAREHOUSE)
                .capacity(WAREHOUSE_A_CAPACITY)
                .coordinateX(100)
                .coordinateY(100)
                .build());

        warehouseB = locationRepository.save(Location.builder()
                .name("창고B")
                .type(LocationType.WAREHOUSE)
                .capacity(WAREHOUSE_B_CAPACITY)
                .coordinateX(200)
                .coordinateY(100)
                .build());

        inboundLocation = locationRepository.save(Location.builder()
                .name("입고장")
                .type(LocationType.INBOUND)
                .coordinateX(50)
                .coordinateY(50)
                .build());

        outboundLocation = locationRepository.save(Location.builder()
                .name("출고장")
                .type(LocationType.OUTBOUND)
                .coordinateX(250)
                .coordinateY(50)
                .build());

        // 장소 간 연결 생성
        locationConnectionRepository.save(LocationConnection.builder()
                .locationId1(Math.min(warehouseA.getId(), warehouseB.getId()))
                .locationId2(Math.max(warehouseA.getId(), warehouseB.getId()))
                .trt(TRAVEL_TIME_MINUTES)
                .build());

        // 재고 생성
        Stock stockWarehouseA = stockRepository.save(Stock.builder()
                .key(StockKey.of(laptop.getId(), warehouseA.getId()))
                .quantity(WAREHOUSE_A_LAPTOP_STOCK)
                .build());

        Stock stockWarehouseB = stockRepository.save(Stock.builder()
                .key(StockKey.of(laptop.getId(), warehouseB.getId()))
                .quantity(WAREHOUSE_B_LAPTOP_STOCK)
                .build());

        // 캐시 강제 동기화
        stockCacheService.updateInventoryQuantity(stockWarehouseA.getKey(), stockWarehouseA.getQuantity());
        stockCacheService.updateInventoryQuantity(stockWarehouseB.getKey(), stockWarehouseB.getQuantity());

        locationCacheService.updateWarehouseCapacity(warehouseA.getId(), warehouseA.getCapacity());
        locationCacheService.updateWarehouseCapacity(warehouseB.getId(), warehouseB.getCapacity());

        stockCacheService.updateWarehouseCurrentSum(warehouseA.getId(), WAREHOUSE_A_USAGE);
        stockCacheService.updateWarehouseCurrentSum(warehouseB.getId(), WAREHOUSE_B_USAGE);
    }

    // ===== 기본 필드 검증 테스트 =====

    @Test
    @DisplayName("수량이 음수인 경우 예외 발생")
    void validateBasicFields_NegativeQuantity_ThrowsException() {
        assertThatThrownBy(() -> LogisticTask.builder()
                .name("잘못된 작업")
                .type(LogisticType.INNER)
                .worker(worker1)
                .ware(laptop)
                .fromLocation(warehouseA)
                .toLocation(warehouseB)
                .quantity(-1) // 음수 수량
                .scheduledDate(TOMORROW)
                .etd(MORNING_START)
                .eta(MORNING_END)
                .build())
                .isInstanceOf(LogisticTaskException.ValidationEx.class)
                .hasMessageContaining("수량은 0보다 커야 합니다");
    }

    @Test
    @DisplayName("ETD가 ETA보다 늦은 경우 예외 발생")
    void validateBasicFields_ETDAfterETA_ThrowsException() {
        assertThatThrownBy(() -> LogisticTask.builder()
                .name("잘못된 시간 작업")
                .type(LogisticType.INNER)
                .worker(worker1)
                .ware(laptop)
                .fromLocation(warehouseA)
                .toLocation(warehouseB)
                .quantity(10)
                .scheduledDate(TOMORROW)
                .etd(MORNING_END) // ETD가 ETA보다 늦음
                .eta(MORNING_START)
                .build())
                .isInstanceOf(LogisticTaskException.ValidationEx.class)
                .hasMessageContaining("출발 예정시간은 도착 예정시간보다 빨라야 합니다");
    }

    @Test
    @DisplayName("출발지와 도착지가 같은 경우 예외 발생")
    void validateBasicFields_SameFromAndTo_ThrowsException() {
        // Given: 출발지와 도착지가 같은 작업
        LogisticTask task = LogisticTask.builder()
                .name("같은 장소 작업")
                .type(LogisticType.INNER)
                .worker(worker1)
                .ware(laptop)
                .fromLocation(warehouseA)
                .toLocation(warehouseA) // 같은 장소
                .quantity(10)
                .scheduledDate(TOMORROW)
                .etd(MORNING_START)
                .eta(MORNING_END)
                .build();

        // When & Then
        assertThatThrownBy(() -> validationService.validateTaskCreation(task))
                .isInstanceOf(LogisticTaskException.ValidationEx.class)
                .hasMessageContaining("출발지와 도착지가 같을 수 없습니다");
    }

    @Test
    @DisplayName("과거 시간으로 작업 생성 시 예외 발생")
    void validateBasicFields_PastTime_ThrowsException() {
        // Given: 과거 날짜로 스케줄된 작업
        LogisticTask task = LogisticTask.builder()
                .name("과거 시간 작업")
                .type(LogisticType.INNER)
                .worker(worker1)
                .ware(laptop)
                .fromLocation(warehouseA)
                .toLocation(warehouseB)
                .quantity(10)
                .scheduledDate(YESTERDAY) // 과거 날짜
                .etd(MORNING_START)
                .eta(MORNING_END)
                .build();

        // When & Then
        assertThatThrownBy(() -> validationService.validateTaskCreation(task))
                .isInstanceOf(LogisticTaskException.ValidationEx.class)
                .hasMessageContaining("출발 예정시간 및 도착 예정시간은 현재 시각 이후여야 합니다");
    }

    // ===== 성공 케이스 테스트 =====

    @Test
    @DisplayName("기본 검증 통과 - 올바른 작업 데이터")
    void validateBasicFields_ValidTask_Success() {
        // Given: 창고A 재고(30개) 내에서의 정상 작업
        int requestQuantity = WAREHOUSE_A_LAPTOP_STOCK - 25; // 5개
        LogisticTask validTask = LogisticTask.builder()
                .name("정상 작업")
                .type(LogisticType.INNER)
                .worker(worker1)
                .ware(laptop)
                .fromLocation(warehouseA)
                .toLocation(warehouseB)
                .quantity(requestQuantity)
                .scheduledDate(TOMORROW)
                .etd(MORNING_START)
                .eta(MORNING_END)
                .build();

        // When & Then: 예외 없이 성공
        assertThatCode(() -> validationService.validateTaskCreation(validTask))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("입고 작업은 재고 제약 없이 성공")
    void validateInboundTask_NoStockConstraints_Success() {
        // Given: 창고A 용량(100) 내에서의 대량 입고
        int largeQuantity = WAREHOUSE_A_CAPACITY - WAREHOUSE_A_USAGE; // 80개
        LogisticTask inboundTask = LogisticTask.builder()
                .name("입고 작업")
                .type(LogisticType.INBOUND)
                .worker(worker1)
                .ware(laptop)
                .fromLocation(inboundLocation)
                .toLocation(warehouseA)
                .quantity(largeQuantity)
                .scheduledDate(TOMORROW)
                .etd(LocalTime.of(8, 0))
                .eta(MORNING_START)
                .build();

        // When & Then: 재고 제약 없이 성공
        assertThatCode(() -> validationService.validateTaskCreation(inboundTask))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("출고 작업은 재고 체크 후 성공")
    void validateOutboundTask_StockCheckThenSuccess() {
        // Given: 창고A 재고(20개) 내에서의 출고
        int requestQuantity = WAREHOUSE_A_LAPTOP_STOCK / 2; // 10개
        LogisticTask outboundTask = LogisticTask.builder()
                .name("출고 작업")
                .type(LogisticType.OUTBOUND)
                .worker(worker1)
                .ware(laptop)
                .fromLocation(warehouseA)
                .toLocation(outboundLocation)
                .quantity(requestQuantity)
                .scheduledDate(TOMORROW)
                .etd(MORNING_START)
                .eta(MORNING_END)
                .build();

        // When & Then: 재고 체크 후 성공
        assertThatCode(() -> validationService.validateTaskCreation(outboundTask))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("경계값 테스트 - 정확히 재고량과 같은 수량")
    void validateBoundaryValue_ExactStockQuantity_Success() {
        // Given: 창고A의 전체 재고와 동일한 수량
        warehouseB.changeCapacity(70); // 창고B 용량 업데이트
        locationRepository.save(warehouseB); // 변경된 용량 저장
        locationCacheService.updateWarehouseCapacity(warehouseB.getId(), 70); // 캐시도 업데이트
        System.out.println("Updated Warehouse B Capacity: " + warehouseB.getCapacity());

        int exactStockQuantity = WAREHOUSE_A_LAPTOP_STOCK; // 30개
        LogisticTask exactQuantityTask = LogisticTask.builder()
                .name("전체 재고 이동")
                .type(LogisticType.INNER)
                .worker(worker1)
                .ware(laptop)
                .fromLocation(warehouseA)
                .toLocation(warehouseB)
                .quantity(exactStockQuantity)
                .scheduledDate(TOMORROW)
                .etd(MORNING_START)
                .eta(MORNING_END)
                .build();

        // When & Then: 경계값에서 성공
        assertThatCode(() -> validationService.validateTaskCreation(exactQuantityTask))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("경계값 테스트 - 정확히 창고 용량만큼 적재")
    void validateBoundaryValue_ExactWarehouseCapacity_Success() {
        // Given: 창고B를 정확히 용량만큼 채우는 작업
        int remainingCapacity = WAREHOUSE_B_CAPACITY - WAREHOUSE_B_USAGE; // 50 - 30 = 20개
        LogisticTask exactCapacityTask = LogisticTask.builder()
                .name("용량 정확히 채우기")
                .type(LogisticType.INNER)
                .worker(worker1)
                .ware(laptop)
                .fromLocation(warehouseA)
                .toLocation(warehouseB)
                .quantity(remainingCapacity)
                .scheduledDate(TOMORROW)
                .etd(MORNING_START)
                .eta(MORNING_END)
                .build();

        // When & Then: 경계값에서 성공
        assertThatCode(() -> validationService.validateTaskCreation(exactCapacityTask))
                .doesNotThrowAnyException();
    }

    // ===== 검증 실패 케이스 테스트 =====

    @Test
    @DisplayName("재고 부족 시 작업 생성 실패")
    void validateStockSimulation_InsufficientStock_ThrowsException() {
        // Given: 창고A 재고(30개)보다 많은 수량 요청
        int excessiveQuantity = WAREHOUSE_A_LAPTOP_STOCK + 5; // 35개
        LogisticTask task = LogisticTask.builder()
                .name("재고 부족 작업")
                .type(LogisticType.INNER)
                .worker(worker1)
                .ware(laptop)
                .fromLocation(warehouseA)
                .toLocation(warehouseB)
                .quantity(excessiveQuantity)
                .scheduledDate(TOMORROW)
                .etd(MORNING_START)
                .eta(MORNING_END)
                .build();

        // When & Then: 재고 부족으로 실패
        assertThatThrownBy(() -> validationService.validateTaskCreation(task))
                .isInstanceOf(LogisticTaskException.ValidationEx.class)
                .hasMessageContaining("재고 부족");
    }

    @Test
    @DisplayName("창고 용량 초과 시 작업 생성 실패")
    void validateCapacitySimulation_ExceedsWarehouseCapacity_ThrowsException() {
        // Given: 창고B 용량(55)을 초과하는 작업
        int excessiveQuantity = WAREHOUSE_B_CAPACITY - WAREHOUSE_B_USAGE + 5; // 70 - 55 + 5 = 20개
        LogisticTask task = LogisticTask.builder()
                .name("용량 초과 작업")
                .type(LogisticType.INNER)
                .worker(worker1)
                .ware(laptop)
                .fromLocation(warehouseA)
                .toLocation(warehouseB)
                .quantity(excessiveQuantity)
                .scheduledDate(TOMORROW)
                .etd(MORNING_START)
                .eta(MORNING_END)
                .build();

        // When & Then: 용량 초과로 실패
        assertThatThrownBy(() -> validationService.validateTaskCreation(task))
                .isInstanceOf(LogisticTaskException.ValidationEx.class)
                .hasMessageContaining("창고 용량");
    }

    // ===== 상태 관리 테스트 =====

    @Test
    @DisplayName("진행 중인 작업 삭제 시 예외 발생")
    void validateTaskDeletion_TaskInProgress_ThrowsException() {
        // Given: 시작된 작업 (삭제 불가 상태)
        LogisticTask task = logisticTaskRepository.save(LogisticTask.builder()
                .name("진행 중 작업")
                .type(LogisticType.INNER)
                .worker(worker1)
                .ware(laptop)
                .fromLocation(warehouseA)
                .toLocation(warehouseB)
                .quantity(5)
                .scheduledDate(TOMORROW)
                .etd(MORNING_START)
                .eta(MORNING_END)
                .build());

        task.initiateTask(MORNING_START);
        logisticTaskRepository.save(task);

        // When & Then: 진행 중 작업 삭제 시 예외
        assertThatThrownBy(() -> validationService.validateTaskDeletion(task))
                .isInstanceOf(LogisticTaskException.TaskCancellationNotAllowedEx.class);
    }

    @Test
    @DisplayName("수정 불가능한 상태의 작업 수정 시 예외 발생")
    void validateTaskModification_NotModifiable_ThrowsException() {
        // Given: 시작된 작업 (수정 불가 상태)
        LogisticTask originalTask = logisticTaskRepository.save(LogisticTask.builder()
                .name("원본 작업")
                .type(LogisticType.INNER)
                .worker(worker1)
                .ware(laptop)
                .fromLocation(warehouseA)
                .toLocation(warehouseB)
                .quantity(5)
                .scheduledDate(TOMORROW)
                .etd(MORNING_START)
                .eta(MORNING_END)
                .build());

        originalTask.initiateTask(MORNING_START);
        logisticTaskRepository.save(originalTask);

        LogisticTask modifiedTask = LogisticTask.builder()
                .name("수정된 작업명")
                .type(LogisticType.INNER)
                .worker(worker1)
                .ware(laptop)
                .fromLocation(warehouseA)
                .toLocation(warehouseB)
                .quantity(10)
                .scheduledDate(TOMORROW)
                .etd(AFTERNOON_START)
                .eta(AFTERNOON_END)
                .build();

        // When & Then: 수정 불가 상태에서 수정 시 예외
        assertThatThrownBy(() -> validationService.validateTaskModification(originalTask, modifiedTask))
                .isInstanceOf(LogisticTaskException.TaskNotModifiableEx.class);
    }

    // ===== 작업자 스케줄 충돌 테스트 =====

    @Test
    @DisplayName("같은 작업자의 시간대 겹치는 작업 생성 시 예외 발생")
    void validateWorkerSchedule_OverlappingTasks_ThrowsException() {
        // Given: 기존 작업이 있는 상태
        LogisticTask existingTask = LogisticTask.builder()
                .name("기존 작업")
                .type(LogisticType.INNER)
                .worker(worker1)
                .ware(laptop)
                .fromLocation(warehouseA)
                .toLocation(warehouseB)
                .quantity(5)
                .scheduledDate(TOMORROW)
                .etd(MORNING_START)
                .eta(MORNING_END)
                .build();
        logisticTaskRepository.save(existingTask);

        // When: 같은 작업자의 겹치는 시간대 작업 생성
        LogisticTask newTask = LogisticTask.builder()
                .name("겹치는 작업")
                .type(LogisticType.INNER)
                .worker(worker1) // 같은 작업자
                .ware(laptop)
                .fromLocation(warehouseB)
                .toLocation(warehouseA)
                .quantity(5)
                .scheduledDate(TOMORROW) // 같은 날짜
                .etd(LocalTime.of(9, 30)) // 겹치는 시간
                .eta(LocalTime.of(10, 30))
                .build();

        // Then: 스케줄 충돌로 예외
        assertThatThrownBy(() -> validationService.validateTaskCreation(newTask))
                .isInstanceOf(LogisticTaskException.ValidationEx.class)
                .hasMessageContaining("작업자")
                .hasMessageContaining("스케줄 충돌");
    }

    // ===== 상태 변경 검증 테스트 =====

    @Test
    @DisplayName("작업 시작 시 재고 부족 검증")
    void validateTaskStatusChange_InitiateWithInsufficientStock_ThrowsException() {
        // Given: 재고보다 많은 수량을 요구하는 작업
        int excessiveQuantity = WAREHOUSE_A_LAPTOP_STOCK + 5; // 25개
        LogisticTask task = logisticTaskRepository.save(LogisticTask.builder()
                .name("재고 부족 시작 작업")
                .type(LogisticType.INNER)
                .worker(worker1)
                .ware(laptop)
                .fromLocation(warehouseA)
                .toLocation(warehouseB)
                .quantity(excessiveQuantity)
                .scheduledDate(TOMORROW)
                .etd(MORNING_START)
                .eta(MORNING_END)
                .build());

        // When & Then: 작업 시작 시 재고 부족으로 예외
        assertThatThrownBy(() ->
                validationService.validateTaskStatusChange(task, LogisticTaskStatus.INITIATED))
                .isInstanceOf(LogisticTaskException.ValidationEx.class)
                .hasMessageContaining("재고 부족");
    }

    @Test
    @DisplayName("작업 완료 시 창고 용량 초과 검증")
    void validateTaskStatusChange_CompleteWithCapacityExceeded_ThrowsException() {
        // Given: 용량을 초과하는 작업
        int excessiveQuantity = WAREHOUSE_B_CAPACITY - WAREHOUSE_B_USAGE + 5; // 25개
        LogisticTask exceedingTask = logisticTaskRepository.save(LogisticTask.builder()
                .name("용량 초과 작업")
                .type(LogisticType.INNER)
                .worker(worker1)
                .ware(laptop)
                .fromLocation(warehouseA)
                .toLocation(warehouseB)
                .quantity(excessiveQuantity)
                .scheduledDate(TOMORROW)
                .etd(AFTERNOON_START)
                .eta(AFTERNOON_END)
                .build());

        exceedingTask.initiateTask(AFTERNOON_START);
        logisticTaskRepository.save(exceedingTask);

        // When & Then: 작업 완료 시 용량 초과로 예외
        assertThatThrownBy(() ->
                validationService.validateTaskStatusChange(exceedingTask, LogisticTaskStatus.COMPLETED))
                .isInstanceOf(LogisticTaskException.ValidationEx.class)
                .hasMessageContaining("창고 용량");
    }

    @Test
    @DisplayName("정상적인 작업 상태 변경은 성공")
    void validateTaskStatusChange_ValidTransitions_Success() {
        // Given: 정상 범위 내의 작업
        int validQuantity = WAREHOUSE_A_LAPTOP_STOCK / 4; // 5개
        LogisticTask task = logisticTaskRepository.save(LogisticTask.builder()
                .name("정상 작업")
                .type(LogisticType.INNER)
                .worker(worker1)
                .ware(laptop)
                .fromLocation(warehouseA)
                .toLocation(warehouseB)
                .quantity(validQuantity)
                .scheduledDate(TOMORROW)
                .etd(MORNING_START)
                .eta(MORNING_END)
                .build());

        // When & Then: 정상적인 상태 변경 플로우
        assertThatCode(() -> {
            validationService.validateTaskStatusChange(task, LogisticTaskStatus.INITIATED);
            task.initiateTask(MORNING_START);
            logisticTaskRepository.save(task);

            validationService.validateTaskStatusChange(task, LogisticTaskStatus.COMPLETED);
            task.completeTask(MORNING_END);
            logisticTaskRepository.save(task);
        }).doesNotThrowAnyException();
    }

    // ===== 복합 시나리오 테스트 =====

    @Test
    @DisplayName("복수 작업 간 재고 충돌 시뮬레이션")
    void validateMultipleTasksSimulation_StockConflict_ThrowsException() {
        // Given: 기존 작업이 재고 사용
        int firstTaskQuantity = WAREHOUSE_A_LAPTOP_STOCK - 15; // 15개
        LogisticTask existingTask = LogisticTask.builder()
                .name("기존 작업")
                .type(LogisticType.INNER)
                .worker(worker1)
                .ware(laptop)
                .fromLocation(warehouseA)
                .toLocation(warehouseB)
                .quantity(firstTaskQuantity)
                .scheduledDate(TOMORROW)
                .etd(MORNING_START)
                .eta(MORNING_END)
                .build();
        logisticTaskRepository.save(existingTask);

        // When: 나머지 재고보다 많은 양을 요구하는 새 작업
        int remainingStock = WAREHOUSE_A_LAPTOP_STOCK - firstTaskQuantity; // 15개 남음
        int newTaskQuantity = remainingStock + 5; // 10개 (초과)
        LogisticTask newTask = LogisticTask.builder()
                .name("충돌 작업")
                .type(LogisticType.INNER)
                .worker(worker2)
                .ware(laptop)
                .fromLocation(warehouseA)
                .toLocation(warehouseB)
                .quantity(newTaskQuantity)
                .scheduledDate(TOMORROW)
                .etd(LocalTime.of(11, 0))
                .eta(LocalTime.of(12, 0))
                .build();

        // Then: 재고 충돌로 예외
        assertThatThrownBy(() -> validationService.validateTaskCreation(newTask))
                .isInstanceOf(LogisticTaskException.ValidationEx.class)
                .hasMessageContaining("재고 부족");
    }

    @Test
    @DisplayName("정상적인 작업 수정은 성공")
    void validateTaskModification_ValidModification_Success() {
        // Given: 기존 작업 (재고의 절반 사용)
        int originalQuantity = WAREHOUSE_A_LAPTOP_STOCK / 2; // 10개
        LogisticTask originalTask = logisticTaskRepository.save(LogisticTask.builder()
                .name("원본 작업")
                .type(LogisticType.INNER)
                .worker(worker1)
                .ware(laptop)
                .fromLocation(warehouseA)
                .toLocation(warehouseB)
                .quantity(originalQuantity)
                .scheduledDate(TOMORROW)
                .etd(MORNING_START)
                .eta(MORNING_END)
                .build());

        // When: 수량 감소 및 작업자 변경
        int modifiedQuantity = originalQuantity - 2; // 8개
        LogisticTask modifiedTask = LogisticTask.builder()
                .name("수정된 작업")
                .type(LogisticType.INNER)
                .worker(worker2) // 작업자 변경
                .ware(laptop)
                .fromLocation(warehouseA)
                .toLocation(warehouseB)
                .quantity(modifiedQuantity) // 수량 감소
                .scheduledDate(TOMORROW)
                .etd(AFTERNOON_START) // 시간 변경
                .eta(AFTERNOON_END)
                .build();

        // Then: 정상적인 수정 성공
        assertThatCode(() ->
                validationService.validateTaskModification(originalTask, modifiedTask))
                .doesNotThrowAnyException();
    }

    /*@Test
    @DisplayName("작업 삭제가 후속 작업에 미치는 긍정적 영향 검증")
    void validateTaskDeletion_PositiveImpactOnSubsequentTasks_Success() {
        // Given: 대량의 재고를 사용하는 작업
        int heavyTaskQuantity = WAREHOUSE_A_LAPTOP_STOCK * 3 / 4; // 15개
        LogisticTask resourceHeavyTask = logisticTaskRepository.save(LogisticTask.builder()
                .name("리소스 많이 쓰는 작업")
                .type(LogisticType.INNER)
                .worker(worker1)
                .ware(laptop)
                .fromLocation(warehouseA)
                .toLocation(warehouseB)
                .quantity(heavyTaskQuantity)
                .scheduledDate(TOMORROW)
                .etd(MORNING_START)
                .eta(MORNING_END)
                .build());

        // And: 후속 작업
        int subsequentTaskQuantity = WAREHOUSE_A_LAPTOP_STOCK / 2; // 10개
        LogisticTask subsequentTask = LogisticTask.builder()
                .name("후속 작업")
                .type(LogisticType.INNER)
                .worker(worker2)
                .ware(laptop)
                .fromLocation(warehouseA)
                .toLocation(warehouseB)
                .quantity(subsequentTaskQuantity)
                .scheduledDate(TOMORROW)
                .etd(LocalTime.of(11, 0))
                .eta(LocalTime.of(12, 0))
                .build();
        logisticTaskRepository.save(subsequentTask);

        // When & Then: 첫 번째 작업 삭제는 성공 (PENDING 상태)
        assertThatCode(() -> validationService.validateTaskDeletion(resourceHeavyTask))
                .doesNotThrowAnyException();
    }
    */

    @Test
    @DisplayName("모든 검증 단계 통과 - 전체 플로우 테스트")
    void validateCompleteFlow_AllValidationSteps_Success() {
        // Given: 정상 범위 내의 작업
        int validQuantity = WAREHOUSE_A_LAPTOP_STOCK / 4; // 5개
        LogisticTask task = logisticTaskRepository.save(LogisticTask.builder()
                .name("전체 플로우 테스트")
                .type(LogisticType.INNER)
                .worker(worker1)
                .ware(laptop)
                .fromLocation(warehouseA)
                .toLocation(warehouseB)
                .quantity(validQuantity)
                .scheduledDate(TOMORROW)
                .etd(MORNING_START)
                .eta(MORNING_END)
                .build());

        // When & Then: 전체 작업 라이프사이클 검증
        assertThatCode(() -> {
            // 1. 작업 생성 검증
            validationService.validateTaskCreation(task);

            // 2. 작업 시작 검증
            validationService.validateTaskStatusChange(task, LogisticTaskStatus.INITIATED);
            task.initiateTask(MORNING_START);
            logisticTaskRepository.save(task);

            // 3. 작업 완료 검증
            validationService.validateTaskStatusChange(task, LogisticTaskStatus.COMPLETED);
            task.completeTask(MORNING_END);
            logisticTaskRepository.save(task);

        }).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("동시 시간대 다른 작업자 작업 허용")
    void validateConcurrentTasksDifferentWorkers_Success() {
        // Given: 같은 시간대, 다른 작업자의 작업들
        int quantityPerWorker = WAREHOUSE_A_LAPTOP_STOCK / 4; // 각 5개씩

        LogisticTask worker1Task = LogisticTask.builder()
                .name("작업자1 작업")
                .type(LogisticType.INNER)
                .worker(worker1)
                .ware(laptop)
                .fromLocation(warehouseA)
                .toLocation(warehouseB)
                .quantity(quantityPerWorker)
                .scheduledDate(TOMORROW)
                .etd(MORNING_START)
                .eta(MORNING_END)
                .build();

        LogisticTask worker2Task = LogisticTask.builder()
                .name("작업자2 작업")
                .type(LogisticType.INNER)
                .worker(worker2) // 다른 작업자
                .ware(laptop)
                .fromLocation(warehouseB)
                .toLocation(warehouseA)
                .quantity(quantityPerWorker)
                .scheduledDate(TOMORROW)
                .etd(MORNING_START) // 같은 시간
                .eta(MORNING_END)
                .build();

        // When & Then: 다른 작업자의 동시 작업 허용
        assertThatCode(() -> {
            validationService.validateTaskCreation(worker1Task);
            logisticTaskRepository.save(worker1Task);
            validationService.validateTaskCreation(worker2Task);
        }).doesNotThrowAnyException();
    }

    // ===== 헬퍼 메서드 (테스트 가독성 향상) =====

    /**
     * 테스트용 작업 빌더 - 기본값으로 정상 작업 생성
     */
    private LogisticTask.LogisticTaskBuilder createBasicTaskBuilder() {
        return LogisticTask.builder()
                .type(LogisticType.INNER)
                .worker(worker1)
                .ware(laptop)
                .fromLocation(warehouseA)
                .toLocation(warehouseB)
                .scheduledDate(TOMORROW)
                .etd(MORNING_START)
                .eta(MORNING_END);
    }

    /**
     * 재고 여유량 계산
     */
    private int getAvailableStock(Location warehouse) {
        if (warehouse.equals(warehouseA)) {
            return WAREHOUSE_A_LAPTOP_STOCK;
        } else if (warehouse.equals(warehouseB)) {
            return WAREHOUSE_B_LAPTOP_STOCK;
        }
        return 0;
    }

    /**
     * 창고 여유 용량 계산
     */
    private int getAvailableCapacity(Location warehouse) {
        if (warehouse.equals(warehouseA)) {
            return WAREHOUSE_A_CAPACITY - WAREHOUSE_A_USAGE;
        } else if (warehouse.equals(warehouseB)) {
            return WAREHOUSE_B_CAPACITY - WAREHOUSE_B_USAGE;
        }
        return 0;
    }
}