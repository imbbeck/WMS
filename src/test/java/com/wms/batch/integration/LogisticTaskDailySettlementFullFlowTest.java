package com.wms.batch.integration;

import com.wms.location.domain.model.Location;
import com.wms.location.domain.model.LocationType;
import com.wms.location.domain.repository.LocationRepository;
import com.wms.logisticTask.domain.model.LogisticTask;
import com.wms.logisticTask.domain.model.LogisticTaskHistory;
import com.wms.logisticTask.domain.model.LogisticTaskStatus;
import com.wms.logisticTask.domain.repository.LogisticTaskHistoryRepository;
import com.wms.logisticTask.domain.repository.LogisticTaskRepository;
import com.wms.logisticTemplate.domain.model.LogisticType;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 물류작업 일일 결산 배치 전체 플로우 테스트
 * 실제 데이터를 생성하여 배치 시스템의 동작을 검증
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("물류작업 일일 결산 배치 전체 플로우 테스트")
class LogisticTaskDailySettlementFullFlowTest {
    
    @Autowired
    private LogisticTaskRepository logisticTaskRepository;
    
    @Autowired
    private LogisticTaskHistoryRepository logisticTaskHistoryRepository;
    
    @Autowired
    private UserInfoRepository userInfoRepository;
    
    @Autowired
    private WareRepository wareRepository;
    
    @Autowired
    private LocationRepository locationRepository;
    
    private UserInfo testWorker;
    private Ware testWare;
    private Location fromLocation;
    private Location toLocation;
    private LocalDate targetDate;
    
    @BeforeEach
    void setUp() {
        targetDate = LocalDate.now().minusDays(1);
        
        // 테스트 데이터 초기화
        logisticTaskHistoryRepository.deleteAll();
        logisticTaskRepository.deleteAll();
        
        // 기본 엔티티 생성
        createTestEntities();
    }
    
    @Test
    @DisplayName("전체 배치 플로우 시뮬레이션 - 모든 상태의 작업 처리")
    void fullBatchFlowSimulation() {
        // Given: 다양한 상태의 물류작업 생성
        createDiverseLogisticTasks();
        
        long initialTaskCount = logisticTaskRepository.countByScheduledDate(targetDate);
        assertThat(initialTaskCount).isEqualTo(7); // 모든 상태별로 1개씩
        
        // When: LogisticTaskHistory 변환 시뮬레이션 (실제 배치 실행 대신)
        List<LogisticTask> tasks = logisticTaskRepository.findByScheduledDate(targetDate);
        LocalDate settlementDate = LocalDate.now();
        
        for (LogisticTask task : tasks) {
            LogisticTaskHistory history = LogisticTaskHistory.fromLogisticTask(task, settlementDate);
            logisticTaskHistoryRepository.save(history);
        }
        
        // 원본 작업들 삭제 (배치에서 수행될 작업)
        logisticTaskRepository.deleteAll();
        
        // Then: 결과 검증
        long finalTaskCount = logisticTaskRepository.countByScheduledDate(targetDate);
        List<LogisticTaskHistory> histories = logisticTaskHistoryRepository.findBySettlementDate(settlementDate);
        
        assertThat(finalTaskCount).isEqualTo(0);
        assertThat(histories).hasSize(7);
        
        // 상태별 변환 검증
        verifyStatusConversions(histories);
        
        // 통계 검증
        verifyHistoryStatistics(histories, settlementDate);
    }
    
    @Test
    @DisplayName("대량 데이터 처리 성능 테스트")
    void largeBatchPerformanceTest() {
        // Given: 대량 테스트 데이터 생성
        createLargeDataSet(1000);
        
        long startTime = System.currentTimeMillis();
        
        // When: 배치 처리 시뮬레이션
        List<LogisticTask> tasks = logisticTaskRepository.findByScheduledDate(targetDate);
        LocalDate settlementDate = LocalDate.now();
        
        for (LogisticTask task : tasks) {
            LogisticTaskHistory history = LogisticTaskHistory.fromLogisticTask(task, settlementDate);
            logisticTaskHistoryRepository.save(history);
        }
        
        long endTime = System.currentTimeMillis();
        long processingTime = endTime - startTime;
        
        // Then: 성능 검증
        List<LogisticTaskHistory> histories = logisticTaskHistoryRepository.findBySettlementDate(settlementDate);
        
        assertThat(histories).hasSize(1000);
        assertThat(processingTime).isLessThan(10000); // 10초 이내 처리
        
        System.out.println("1000개 작업 처리 시간: " + processingTime + "ms");
    }
    
    @Test
    @DisplayName("실제 시간 데이터 보존 테스트")
    void actualTimeDataPreservationTest() {
        // Given: 실제 시간 데이터가 있는 작업 생성
        LogisticTask completedTask = createTaskWithActualTimes();
        logisticTaskRepository.save(completedTask);
        
        // When: 히스토리 변환
        LogisticTaskHistory history = LogisticTaskHistory.fromLogisticTask(completedTask, LocalDate.now());
        logisticTaskHistoryRepository.save(history);
        
        // Then: 실제 시간 데이터 보존 확인
        assertThat(history.getAtd()).isNotNull();
        assertThat(history.getAta()).isNotNull();
        assertThat(history.getAtd()).isEqualTo(LocalTime.of(9, 15));
        assertThat(history.getAta()).isEqualTo(LocalTime.of(10, 30));
    }
    
    @Test
    @DisplayName("빈 데이터 처리 테스트")
    void emptyDataHandlingTest() {
        // Given: 데이터 없음
        logisticTaskRepository.deleteAll();
        
        // When: 빈 상태에서 처리
        List<LogisticTask> tasks = logisticTaskRepository.findByScheduledDate(targetDate);
        
        // Then: 안전한 처리 확인
        assertThat(tasks).isEmpty();
        
        List<LogisticTaskHistory> histories = logisticTaskHistoryRepository.findBySettlementDate(LocalDate.now());
        assertThat(histories).isEmpty();
    }
    
    private void createTestEntities() {
        testWorker = UserInfo.builder()
                .username("testworker")
                .name("테스트 작업자")
                .email("testworker@test.com")
                .type(UserType.WORKER)
		        .password(Password.builder().value("password").build())
                .build();
        testWorker = userInfoRepository.save(testWorker);
        
        testWare = Ware.builder()
                .name("테스트 물품")
                .type("전자제품")
                .paletteUnit(10)
                .build();
        testWare = wareRepository.save(testWare);
        
        fromLocation = Location.builder()
                .name("출발지")
                .type(LocationType.WAREHOUSE)
                .capacity(1000)
                .coordinateX(100)
                .coordinateY(200)
                .build();
        fromLocation = locationRepository.save(fromLocation);
        
        toLocation = Location.builder()
                .name("도착지")
                .type(LocationType.WAREHOUSE)
                .capacity(800)
                .coordinateX(300)
                .coordinateY(400)
                .build();
        toLocation = locationRepository.save(toLocation);
    }
    
    private void createDiverseLogisticTasks() {
        LogisticTaskStatus[] statuses = LogisticTaskStatus.values();
	    // LogisticTaskStatus.EXPIRED 상태는 생성 시점에서 의미가 없으므로 제외
	    statuses = java.util.Arrays.copyOfRange(statuses, 1, statuses.length);

        
        for (int i = 0; i < statuses.length; i++) {
            LogisticTask task = LogisticTask.builder()
                    .name("테스트 작업 " + statuses[i])
                    .type(LogisticType.INNER)
                    .worker(testWorker)
                    .ware(testWare)
                    .fromLocation(fromLocation)
                    .toLocation(toLocation)
                    .quantity(5 + i)
                    .scheduledDate(targetDate)
                    .etd(LocalTime.of(9 + i, 0))
                    .eta(LocalTime.of(10 + i, 0))
                    .status(statuses[i])
                    .templateIdSnapshot((int) (100 + i))
                    .build();
            
            logisticTaskRepository.save(task);
        }
    }
    
    private void createLargeDataSet(int count) {
        for (int i = 0; i < count; i++) {
            LogisticTaskStatus status = LogisticTaskStatus.values()[i % LogisticTaskStatus.values().length];
            
            LogisticTask task = LogisticTask.builder()
                    .name("대량 테스트 작업 " + (i + 1))
                    .type(LogisticType.values()[i % LogisticType.values().length])
                    .worker(testWorker)
                    .ware(testWare)
                    .fromLocation(fromLocation)
                    .toLocation(toLocation)
                    .quantity(1 + (i % 10))
                    .scheduledDate(targetDate)
                    .etd(LocalTime.of(9, 0).plusMinutes(i % 480))
                    .eta(LocalTime.of(10, 0).plusMinutes(i % 480))
                    .status(status)
                    .templateIdSnapshot((int) (1000 + i))
                    .build();
            
            logisticTaskRepository.save(task);
        }
    }
    
    private LogisticTask createTaskWithActualTimes() {
        LogisticTask task = LogisticTask.builder()
                .name("실제 시간 테스트 작업")
                .type(LogisticType.INNER)
                .worker(testWorker)
                .ware(testWare)
                .fromLocation(fromLocation)
                .toLocation(toLocation)
                .quantity(10)
                .scheduledDate(targetDate)
                .etd(LocalTime.of(9, 0))
                .eta(LocalTime.of(10, 0))
                .status(LogisticTaskStatus.PENDING)
                .templateIdSnapshot(999)
                .build();
        
        task.initiateTask(LocalTime.of(9, 15));
        task.completeTask(LocalTime.of(10, 30));
        
        return task;
    }
    
    private void verifyStatusConversions(List<LogisticTaskHistory> histories) {
        for (LogisticTaskHistory history : histories) {
            LogisticTaskStatus originalStatus = history.getOriginalStatus();
            LogisticTaskStatus finalStatus = history.getFinalStatus();
            
            switch (originalStatus) {
                case PENDING, INITIATE_DELAYED -> 
                    assertThat(finalStatus).isEqualTo(LogisticTaskStatus.EXPIRED);
                    
                case INITIATED, COMPLETE_DELAYED, FAILED -> 
                    assertThat(finalStatus).isEqualTo(LogisticTaskStatus.FAILED);
                    
                case COMPLETED, CANCELLED -> 
                    assertThat(finalStatus).isEqualTo(originalStatus);
                    
                case EXPIRED -> 
                    assertThat(finalStatus).isEqualTo(LogisticTaskStatus.EXPIRED);
            }
        }
    }
    
    private void verifyHistoryStatistics(List<LogisticTaskHistory> histories, LocalDate settlementDate) {
        var statusStats = logisticTaskHistoryRepository.findStatusStatsBySettlementDate(settlementDate);
        
        assertThat(statusStats).isNotEmpty();
        
        long totalFromStats = statusStats.stream()
                .mapToLong(stat -> (Long) stat[1])
                .sum();
        
        assertThat(totalFromStats).isEqualTo(histories.size());
    }
}
