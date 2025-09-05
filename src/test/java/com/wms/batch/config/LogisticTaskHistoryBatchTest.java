package com.wms.batch.config;

import com.wms.location.domain.model.Location;
import com.wms.location.domain.model.LocationType;
import com.wms.location.domain.repository.LocationRepository;
import com.wms.logisticTask.domain.model.LogisticTask;
import com.wms.logisticTask.domain.model.LogisticTaskHistory;
import com.wms.logisticTask.domain.model.LogisticTaskStatus;
import com.wms.logisticTask.domain.repository.LogisticTaskHistoryRepository;
import com.wms.logisticTask.domain.repository.LogisticTaskRepository;
import com.wms.logisticTemplate.domain.model.LogisticType;
import com.wms.userInfo.domain.model.UserInfo;
import com.wms.userInfo.domain.model.UserType;
import com.wms.userInfo.domain.model.Password;
import com.wms.userInfo.domain.repository.UserInfoRepository;
import com.wms.ware.domain.model.Ware;
import com.wms.ware.domain.repository.WareRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.JobRepositoryTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@SpringBatchTest
@ActiveProfiles("test")
class LogisticTaskHistoryBatchTest {
    
    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;
    
    @Autowired
    private JobRepositoryTestUtils jobRepositoryTestUtils;
    
    @Autowired
    private Job logisticTaskHistoryJob;
    
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
    
    private LocalDate targetDate;
    private UserInfo testWorker;
    private Ware testWare;
    private Location fromLocation;
    private Location toLocation;
    
    @BeforeEach
    void setUp() {
        jobRepositoryTestUtils.removeJobExecutions();
        jobLauncherTestUtils.setJob(logisticTaskHistoryJob);
        
        targetDate = LocalDate.now().minusDays(1);
        
        // 데이터 초기화
        logisticTaskHistoryRepository.deleteAll();
        logisticTaskRepository.deleteAll();
	    userInfoRepository.deleteAll();
	    wareRepository.deleteAll();
	    locationRepository.deleteAll();
        
        // 테스트 엔티티 생성
        setupTestEntities();
	    createTestTasks();
    }
    
    @Test
    void 배치_정상실행() throws Exception {
        // Given
	    createTestTasks();
        JobParameters jobParameters = createJobParameters();
        long initialTaskCount = logisticTaskRepository.countByScheduledDate(targetDate);
        
        // When
        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);
        
        // Then
        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(logisticTaskRepository.countByScheduledDate(targetDate)).isZero();
        assertThat(logisticTaskHistoryRepository.findBySettlementDate(LocalDate.now()))
                .hasSize((int) initialTaskCount);
    }
    
    @Test
    void 빈데이터_배치실행() throws Exception {
        // Given
        logisticTaskRepository.deleteAll();
        JobParameters jobParameters = createJobParameters();
        
        // When
        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);
        
        // Then
        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(logisticTaskHistoryRepository.findBySettlementDate(LocalDate.now())).isEmpty();
    }
    
    @Test
    void 대량데이터_배치실행() throws Exception {
        // Given
        logisticTaskRepository.deleteAll();
        createBulkTestTasks(1000);
        JobParameters jobParameters = createJobParameters();
        
        // When
        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);
        
        // Then
        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(logisticTaskRepository.countByScheduledDate(targetDate)).isZero();
        assertThat(logisticTaskHistoryRepository.findBySettlementDate(LocalDate.now()))
                .hasSize(1000);
    }
    
    @Test
    void 상태변환_검증() throws Exception {
        // Given
        logisticTaskRepository.deleteAll();
        createTasksWithDifferentStatuses();
        JobParameters jobParameters = createJobParameters();
        
        // When
        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);
        
        // Then
        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        
        List<LogisticTaskHistory> histories = logisticTaskHistoryRepository.findBySettlementDate(LocalDate.now());
        verifyStatusConversions(histories);
    }
    
    @Test
    void 완료작업_실제시간_이관검증() throws Exception {
        // Given
        logisticTaskRepository.deleteAll();
        createCompletedTaskWithActualTimes();
        JobParameters jobParameters = createJobParameters();
        
        // When
        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);
        
        // Then
        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        
        List<LogisticTaskHistory> histories = logisticTaskHistoryRepository.findBySettlementDate(LocalDate.now());
        LogisticTaskHistory completedHistory = histories.stream()
                .filter(h -> h.getFinalStatus() == LogisticTaskStatus.COMPLETED)
                .findFirst()
                .orElseThrow();
        
        assertThat(completedHistory.getAtd()).isNotNull();
        assertThat(completedHistory.getAta()).isNotNull();
    }
    
    private JobParameters createJobParameters() {
        return new JobParametersBuilder()
                .addLocalDateTime("timestamp", LocalDateTime.now())
                .addLocalDate("targetDate", targetDate)
                .toJobParameters();
    }
    
    private void setupTestEntities() {
        testWorker = userInfoRepository.save(UserInfo.builder()
                .username("testworker")
                .name("테스트 작업자")
                .email("test@test.com")
                .password(Password.builder().value("password").build())
                .type(UserType.WORKER)
                .build());
        
        testWare = wareRepository.save(Ware.builder()
                .name("테스트 물품")
                .type("전자제품")
                .paletteUnit(10)
                .build());
        
        fromLocation = locationRepository.save(Location.builder()
                .name("출발지")
                .type(LocationType.WAREHOUSE)
                .capacity(1000)
                .coordinateX(100)
                .coordinateY(200)
                .build());
        
        toLocation = locationRepository.save(Location.builder()
                .name("도착지")
                .type(LocationType.WAREHOUSE)
                .capacity(800)
                .coordinateX(300)
                .coordinateY(400)
                .build());
    }
    
    private void createTestTasks() {
        for (int i = 0; i < 5; i++) {
            LogisticTask task = createBaseTask("테스트 작업 " + (i + 1), LogisticTaskStatus.PENDING);
            logisticTaskRepository.save(task);
        }
    }
    
    private void createBulkTestTasks(int count) {
        for (int i = 0; i < count; i++) {
            LogisticTaskStatus status = LogisticTaskStatus.values()[i % LogisticTaskStatus.values().length];
            LogisticTask task = createBaseTask("대량 작업 " + (i + 1), status);
            logisticTaskRepository.save(task);
        }
    }
    
    private void createTasksWithDifferentStatuses() {
        for (LogisticTaskStatus status : LogisticTaskStatus.values()) {
            LogisticTask task = createBaseTask("상태테스트 " + status.name(), status);
            logisticTaskRepository.save(task);
        }
    }
    
    private void createCompletedTaskWithActualTimes() {
        LogisticTask task = createBaseTask("완료된 작업", LogisticTaskStatus.PENDING);
        task.initiateTask(LocalTime.of(9, 5));
        task.completeTask(LocalTime.of(10, 15));
        logisticTaskRepository.save(task);
    }
    
    private LogisticTask createBaseTask(String name, LogisticTaskStatus status) {
        return LogisticTask.builder()
                .name(name)
                .type(LogisticType.INNER)
                .worker(testWorker)
                .ware(testWare)
                .fromLocation(fromLocation)
                .toLocation(toLocation)
                .quantity(10)
                .scheduledDate(targetDate)
                .etd(LocalTime.of(9, 0))
                .eta(LocalTime.of(10, 0))
                .status(status)
                .templateIdSnapshot(100)
                .build();
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
                case COMPLETED, CANCELLED, EXPIRED -> 
                    assertThat(finalStatus).isEqualTo(originalStatus);
            }
            
            assertThat(history.getOriginalTaskId()).isNotNull();
            assertThat(history.getSettlementDate()).isEqualTo(LocalDate.now());
            assertThat(history.getScheduledDate()).isEqualTo(targetDate);
        }
    }
}
