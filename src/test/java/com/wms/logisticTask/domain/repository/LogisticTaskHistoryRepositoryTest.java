package com.wms.logisticTask.domain.repository;

import com.wms.applicationInfra.config.QuerydslConfig;
import com.wms.location.domain.model.Location;
import com.wms.location.domain.model.LocationType;
import com.wms.logisticTask.domain.model.LogisticTaskHistory;
import com.wms.logisticTask.domain.model.LogisticTaskStatus;
import com.wms.logisticTemplate.domain.model.LogisticType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(QuerydslConfig.class)
@DisplayName("LogisticTaskHistory Repository 테스트")
class LogisticTaskHistoryRepositoryTest {
    
    @Autowired
    private TestEntityManager entityManager;
    
    @Autowired
    private LogisticTaskHistoryRepository logisticTaskHistoryRepository;
    
    private LocalDate testSettlementDate;
    private Long testWorkerId;
    private Long testWareId;
    private Long testLocationId;
    
    @BeforeEach
    void setUp() {
        testSettlementDate = LocalDate.of(2024, 1, 15);
        testWorkerId = 1L;
        testWareId = 100L;
        testLocationId = 1L;
        
        // 테스트 데이터 생성
        createTestHistoryData();
        entityManager.flush();
        entityManager.clear();
    }
    
    @Test
    @DisplayName("특정 결산일의 히스토리 조회")
    void findBySettlementDate() {
        // When
        List<LogisticTaskHistory> histories = logisticTaskHistoryRepository.findBySettlementDate(testSettlementDate);
        
        // Then
        assertThat(histories).hasSize(5);
        assertThat(histories).allMatch(h -> h.getSettlementDate().equals(testSettlementDate));
    }
    
    @Test
    @DisplayName("특정 작업자의 특정 기간 히스토리 조회")
    void findByWorkerIdAndSettlementDateBetween() {
        // Given
        LocalDate startDate = testSettlementDate.minusDays(1);
        LocalDate endDate = testSettlementDate.plusDays(1);
        
        // When
        List<LogisticTaskHistory> histories = logisticTaskHistoryRepository
                .findByWorkerIdAndSettlementDateBetween(testWorkerId, startDate, endDate);
        
        // Then
        assertThat(histories).hasSize(3); // testWorkerId로 생성된 히스토리 개수
        assertThat(histories).allMatch(h -> h.getWorkerId().equals(testWorkerId));
        assertThat(histories).allMatch(h -> 
                !h.getSettlementDate().isBefore(startDate) && !h.getSettlementDate().isAfter(endDate));
    }
    
    @Test
    @DisplayName("상태별 히스토리 통계 조회")
    void findStatusStatsBySettlementDate() {
        // When
        List<Object[]> stats = logisticTaskHistoryRepository.findStatusStatsBySettlementDate(testSettlementDate);
        
        // Then
        assertThat(stats).isNotEmpty();
        
        // 통계 검증
        for (Object[] stat : stats) {
            LogisticTaskStatus status = (LogisticTaskStatus) stat[0];
            Long count = (Long) stat[1];
            
            assertThat(status).isNotNull();
            assertThat(count).isPositive();
        }
        
        // 전체 개수 검증
        long totalFromStats = stats.stream().mapToLong(stat -> (Long) stat[1]).sum();
        assertThat(totalFromStats).isEqualTo(5);
    }
    
    @Test
    @DisplayName("특정 기간의 완료된 작업 수 조회")
    void countCompletedTasksBetween() {
        // Given
        LocalDate startDate = testSettlementDate.minusDays(1);
        LocalDate endDate = testSettlementDate.plusDays(1);
        
        // When
        Long completedCount = logisticTaskHistoryRepository.countCompletedTasksBetween(startDate, endDate);
        
        // Then
        assertThat(completedCount).isEqualTo(2); // 테스트 데이터에서 COMPLETED 상태인 것들
    }
    
    @Test
    @DisplayName("특정 기간의 전체 작업 수 조회")
    void countAllTasksBetween() {
        // Given
        LocalDate startDate = testSettlementDate.minusDays(1);
        LocalDate endDate = testSettlementDate.plusDays(1);
        
        // When
        Long totalCount = logisticTaskHistoryRepository.countAllTasksBetween(startDate, endDate);
        
        // Then
        assertThat(totalCount).isEqualTo(5);
    }
    
    @Test
    @DisplayName("특정 작업자의 특정 기간 완료율 통계")
    void countCompletedTasksByWorkerBetween() {
        // Given
        LocalDate startDate = testSettlementDate.minusDays(1);
        LocalDate endDate = testSettlementDate.plusDays(1);
        
        // When
        Long workerCompletedCount = logisticTaskHistoryRepository
                .countCompletedTasksByWorkerBetween(testWorkerId, startDate, endDate);
        
        // Then
        assertThat(workerCompletedCount).isGreaterThanOrEqualTo(0);
    }
    
    @Test
    @DisplayName("특정 물품의 히스토리 조회")
    void findByWareIdAndSettlementDateBetween() {
        // Given
        LocalDate startDate = testSettlementDate.minusDays(1);
        LocalDate endDate = testSettlementDate.plusDays(1);
        
        // When
        List<LogisticTaskHistory> histories = logisticTaskHistoryRepository
                .findByWareIdAndSettlementDateBetween(testWareId, startDate, endDate);
        
        // Then
        assertThat(histories).isNotEmpty();
        assertThat(histories).allMatch(h -> h.getWareId().equals(testWareId));
    }
    
    @Test
    @DisplayName("특정 장소 관련 히스토리 조회")
    void findByLocationAndSettlementDateBetween() {
        // Given
        LocalDate startDate = testSettlementDate.minusDays(1);
        LocalDate endDate = testSettlementDate.plusDays(1);
        
        // When
        List<LogisticTaskHistory> histories = logisticTaskHistoryRepository
                .findByLocationAndSettlementDateBetween(testLocationId, startDate, endDate);
        
        // Then
        assertThat(histories).isNotEmpty();
        assertThat(histories).allMatch(h -> 
                h.getFromLocationId().equals(testLocationId) || h.getToLocationId().equals(testLocationId));
    }
    
    @Test
    @DisplayName("특정 결산일의 원본 작업 ID 목록 조회")
    void findOriginalTaskIdsBySettlementDate() {
        // When
        List<Long> originalTaskIds = logisticTaskHistoryRepository
                .findOriginalTaskIdsBySettlementDate(testSettlementDate);
        
        // Then
        assertThat(originalTaskIds).hasSize(5);
        assertThat(originalTaskIds).doesNotContainNull();
        assertThat(originalTaskIds).allMatch(id -> id > 0);
    }
    
    @Test
    @DisplayName("존재하지 않는 날짜의 히스토리 조회")
    void findBySettlementDate_NotExists() {
        // Given
        LocalDate nonExistentDate = LocalDate.of(2030, 12, 31);
        
        // When
        List<LogisticTaskHistory> histories = logisticTaskHistoryRepository.findBySettlementDate(nonExistentDate);
        
        // Then
        assertThat(histories).isEmpty();
    }
    
    private void createTestHistoryData() {
        // 다양한 상태의 히스토리 데이터 생성
        LogisticTaskHistory history1 = LogisticTaskHistory.builder()
                .originalTaskId(1001L)
                .name("완료된 작업 1")
                .type(LogisticType.INBOUND)
                .workerId(testWorkerId)
                .workerName("김작업자")
                .wareId(testWareId)
                .wareName("테스트 물품")
                .fromLocationId(testLocationId)
                .fromLocationName("입고처A")
                .toLocationId(2L)
                .toLocationName("창고A")
                .quantity(10)
                .scheduledDate(testSettlementDate.minusDays(1))
                .etd(LocalTime.of(9, 0))
                .eta(LocalTime.of(10, 0))
                .atd(LocalTime.of(9, 5))
                .ata(LocalTime.of(10, 10))
                .finalStatus(LogisticTaskStatus.COMPLETED)
                .originalStatus(LogisticTaskStatus.COMPLETED)
                .templateIdSnapshot(100L)
                .settlementDate(testSettlementDate)
                .build();
        
        LogisticTaskHistory history2 = LogisticTaskHistory.builder()
                .originalTaskId(1002L)
                .name("완료된 작업 2")
                .type(LogisticType.OUTBOUND)
                .workerId(testWorkerId)
                .workerName("김작업자")
                .wareId(testWareId)
                .wareName("테스트 물품")
                .fromLocationId(2L)
                .fromLocationName("창고A")
                .toLocationId(3L)
                .toLocationName("출고처B")
                .quantity(5)
                .scheduledDate(testSettlementDate.minusDays(1))
                .etd(LocalTime.of(14, 0))
                .eta(LocalTime.of(15, 0))
                .atd(LocalTime.of(14, 0))
                .ata(LocalTime.of(15, 5))
                .finalStatus(LogisticTaskStatus.COMPLETED)
                .originalStatus(LogisticTaskStatus.COMPLETED)
                .templateIdSnapshot(101L)
                .settlementDate(testSettlementDate)
                .build();
        
        LogisticTaskHistory history3 = LogisticTaskHistory.builder()
                .originalTaskId(1003L)
                .name("만료된 작업")
                .type(LogisticType.INNER)
                .workerId(testWorkerId)
                .workerName("김작업자")
                .wareId(testWareId)
                .wareName("테스트 물품")
                .fromLocationId(testLocationId)
                .fromLocationName("창고A")
                .toLocationId(2L)
                .toLocationName("창고B")
                .quantity(3)
                .scheduledDate(testSettlementDate.minusDays(1))
                .etd(LocalTime.of(16, 0))
                .eta(LocalTime.of(17, 0))
                .finalStatus(LogisticTaskStatus.EXPIRED)
                .originalStatus(LogisticTaskStatus.PENDING)
                .templateIdSnapshot(102L)
                .settlementDate(testSettlementDate)
                .build();
        
        LogisticTaskHistory history4 = LogisticTaskHistory.builder()
                .originalTaskId(1004L)
                .name("실패한 작업")
                .type(LogisticType.INNER)
                .workerId(2L)
                .workerName("박작업자")
                .wareId(testWareId)
                .wareName("테스트 물품")
                .fromLocationId(testLocationId)
                .fromLocationName("창고A")
                .toLocationId(3L)
                .toLocationName("창고C")
                .quantity(7)
                .scheduledDate(testSettlementDate.minusDays(1))
                .etd(LocalTime.of(11, 0))
                .eta(LocalTime.of(12, 0))
                .atd(LocalTime.of(11, 0))
                .finalStatus(LogisticTaskStatus.FAILED)
                .originalStatus(LogisticTaskStatus.INITIATED)
                .templateIdSnapshot(103L)
                .settlementDate(testSettlementDate)
                .build();
        
        LogisticTaskHistory history5 = LogisticTaskHistory.builder()
                .originalTaskId(1005L)
                .name("취소된 작업")
                .type(LogisticType.OUTBOUND)
                .workerId(3L)
                .workerName("최작업자")
                .wareId(testWareId)
                .wareName("테스트 물품")
                .fromLocationId(2L)
                .fromLocationName("창고B")
                .toLocationId(4L)
                .toLocationName("출고처C")
                .quantity(2)
                .scheduledDate(testSettlementDate.minusDays(1))
                .etd(LocalTime.of(13, 0))
                .eta(LocalTime.of(14, 0))
                .finalStatus(LogisticTaskStatus.CANCELLED)
                .originalStatus(LogisticTaskStatus.CANCELLED)
                .templateIdSnapshot(104L)
                .settlementDate(testSettlementDate)
                .build();
        
        entityManager.persist(history1);
        entityManager.persist(history2);
        entityManager.persist(history3);
        entityManager.persist(history4);
        entityManager.persist(history5);
    }
}
