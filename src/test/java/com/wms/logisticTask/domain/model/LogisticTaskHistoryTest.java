package com.wms.logisticTask.domain.model;

import com.wms.location.domain.model.Location;
import com.wms.location.domain.model.LocationType;
import com.wms.logisticTemplate.domain.model.LogisticType;
import com.wms.userInfo.domain.model.UserInfo;
import com.wms.userInfo.domain.model.UserType;
import com.wms.ware.domain.model.Ware;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("LogisticTaskHistory 도메인 테스트")
class LogisticTaskHistoryTest {
    
    @Test
    @DisplayName("LogisticTask에서 LogisticTaskHistory로 변환 - 기본 케이스")
    void fromLogisticTask_BasicConversion() {
        // Given
        LogisticTask task = createSampleLogisticTask(LogisticTaskStatus.COMPLETED);
        LocalDate settlementDate = LocalDate.of(2024, 1, 15);
        
        // When
        LogisticTaskHistory history = LogisticTaskHistory.fromLogisticTask(task, settlementDate);
        
        // Then
        assertThat(history.getOriginalTaskId()).isEqualTo(task.getId());
        assertThat(history.getName()).isEqualTo(task.getName());
        assertThat(history.getType()).isEqualTo(task.getType());
        assertThat(history.getWorkerId()).isEqualTo(task.getWorker().getId());
        assertThat(history.getWorkerName()).isEqualTo(task.getWorker().getName());
        assertThat(history.getWareId()).isEqualTo(task.getWare().getId());
        assertThat(history.getWareName()).isEqualTo(task.getWare().getName());
        assertThat(history.getFromLocationId()).isEqualTo(task.getFromLocation().getId());
        assertThat(history.getFromLocationName()).isEqualTo(task.getFromLocation().getName());
        assertThat(history.getToLocationId()).isEqualTo(task.getToLocation().getId());
        assertThat(history.getToLocationName()).isEqualTo(task.getToLocation().getName());
        assertThat(history.getQuantity()).isEqualTo(task.getQuantity());
        assertThat(history.getScheduledDate()).isEqualTo(task.getScheduledDate());
        assertThat(history.getEtd()).isEqualTo(task.getEtd());
        assertThat(history.getEta()).isEqualTo(task.getEta());
        assertThat(history.getAtd()).isEqualTo(task.getAtd());
        assertThat(history.getAta()).isEqualTo(task.getAta());
        assertThat(history.getOriginalStatus()).isEqualTo(LogisticTaskStatus.COMPLETED);
        assertThat(history.getFinalStatus()).isEqualTo(LogisticTaskStatus.COMPLETED);
        assertThat(history.getSettlementDate()).isEqualTo(settlementDate);
    }
    
    @ParameterizedTest
    @EnumSource(LogisticTaskStatus.class)
    @DisplayName("모든 상태에 대한 최종 상태 변환 테스트")
    void determineFinalStatus_AllStatuses(LogisticTaskStatus originalStatus) {
        // Given
        LogisticTask task = createSampleLogisticTask(originalStatus);
        LocalDate settlementDate = LocalDate.now();
        
        // When
        LogisticTaskHistory history = LogisticTaskHistory.fromLogisticTask(task, settlementDate);
        LogisticTaskStatus finalStatus = history.getFinalStatus();
        
        // Then
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
        
        // 원본 상태는 항상 보존되어야 함
        assertThat(history.getOriginalStatus()).isEqualTo(originalStatus);
    }
    
    @Test
    @DisplayName("미완료 작업들의 최종 상태 변환")
    void determineFinalStatus_IncompleteTasksToExpiredOrFailed() {
        // Given
        LocalDate settlementDate = LocalDate.now();
        
        // When & Then
        // PENDING → EXPIRED
        LogisticTask pendingTask = createSampleLogisticTask(LogisticTaskStatus.PENDING);
        LogisticTaskHistory pendingHistory = LogisticTaskHistory.fromLogisticTask(pendingTask, settlementDate);
        assertThat(pendingHistory.getFinalStatus()).isEqualTo(LogisticTaskStatus.EXPIRED);
        assertThat(pendingHistory.getOriginalStatus()).isEqualTo(LogisticTaskStatus.PENDING);
        
        // INITIATE_DELAYED → EXPIRED
        LogisticTask delayedTask = createSampleLogisticTask(LogisticTaskStatus.INITIATE_DELAYED);
        LogisticTaskHistory delayedHistory = LogisticTaskHistory.fromLogisticTask(delayedTask, settlementDate);
        assertThat(delayedHistory.getFinalStatus()).isEqualTo(LogisticTaskStatus.EXPIRED);
        assertThat(delayedHistory.getOriginalStatus()).isEqualTo(LogisticTaskStatus.INITIATE_DELAYED);
        
        // INITIATED → FAILED
        LogisticTask initiatedTask = createSampleLogisticTask(LogisticTaskStatus.INITIATED);
        LogisticTaskHistory initiatedHistory = LogisticTaskHistory.fromLogisticTask(initiatedTask, settlementDate);
        assertThat(initiatedHistory.getFinalStatus()).isEqualTo(LogisticTaskStatus.FAILED);
        assertThat(initiatedHistory.getOriginalStatus()).isEqualTo(LogisticTaskStatus.INITIATED);
        
        // COMPLETE_DELAYED → FAILED
        LogisticTask completeDelayedTask = createSampleLogisticTask(LogisticTaskStatus.COMPLETE_DELAYED);
        LogisticTaskHistory completeDelayedHistory = LogisticTaskHistory.fromLogisticTask(completeDelayedTask, settlementDate);
        assertThat(completeDelayedHistory.getFinalStatus()).isEqualTo(LogisticTaskStatus.FAILED);
        assertThat(completeDelayedHistory.getOriginalStatus()).isEqualTo(LogisticTaskStatus.COMPLETE_DELAYED);
    }
    
    @Test
    @DisplayName("완료된 작업들의 상태 보존")
    void determineFinalStatus_CompletedTasksKeepStatus() {
        // Given
        LocalDate settlementDate = LocalDate.now();
        
        // When & Then
        // COMPLETED → COMPLETED
        LogisticTask completedTask = createSampleLogisticTask(LogisticTaskStatus.COMPLETED);
        LogisticTaskHistory completedHistory = LogisticTaskHistory.fromLogisticTask(completedTask, settlementDate);
        assertThat(completedHistory.getFinalStatus()).isEqualTo(LogisticTaskStatus.COMPLETED);
        assertThat(completedHistory.getOriginalStatus()).isEqualTo(LogisticTaskStatus.COMPLETED);
        
        // CANCELLED → CANCELLED
        LogisticTask cancelledTask = createSampleLogisticTask(LogisticTaskStatus.CANCELLED);
        LogisticTaskHistory cancelledHistory = LogisticTaskHistory.fromLogisticTask(cancelledTask, settlementDate);
        assertThat(cancelledHistory.getFinalStatus()).isEqualTo(LogisticTaskStatus.CANCELLED);
        assertThat(cancelledHistory.getOriginalStatus()).isEqualTo(LogisticTaskStatus.CANCELLED);
        
        // FAILED → FAILED
        LogisticTask failedTask = createSampleLogisticTask(LogisticTaskStatus.FAILED);
        LogisticTaskHistory failedHistory = LogisticTaskHistory.fromLogisticTask(failedTask, settlementDate);
        assertThat(failedHistory.getFinalStatus()).isEqualTo(LogisticTaskStatus.FAILED);
        assertThat(failedHistory.getOriginalStatus()).isEqualTo(LogisticTaskStatus.FAILED);
    }
    
    @Test
    @DisplayName("실제 시간 데이터가 포함된 LogisticTask 변환")
    void fromLogisticTask_WithActualTimes() {
        // Given
        LogisticTask task = createSampleLogisticTask(LogisticTaskStatus.PENDING);
        LocalTime actualDeparture = LocalTime.of(9, 15);
        LocalTime actualArrival = LocalTime.of(10, 30);
        task.initiateTask(actualDeparture);
        task.completeTask(actualArrival);
        
        LocalDate settlementDate = LocalDate.of(2024, 1, 15);
        
        // When
        LogisticTaskHistory history = LogisticTaskHistory.fromLogisticTask(task, settlementDate);
        
        // Then
        assertThat(history.getAtd()).isEqualTo(actualDeparture);
        assertThat(history.getAta()).isEqualTo(actualArrival);
        assertThat(history.getFinalStatus()).isEqualTo(LogisticTaskStatus.COMPLETED);
    }
    
    private LogisticTask createSampleLogisticTask(LogisticTaskStatus status) {
        // 사용자 생성
        UserInfo worker = UserInfo.builder()
                .username("worker01")
                .name("김작업")
                .email("worker@test.com")
                .type(UserType.WORKER)
                .build();
        
        // 물품 생성
        Ware ware = Ware.builder()
                .name("테스트 물품")
                .type("전자제품")
                .paletteUnit(10)
                .build();
        
        // 장소 생성
        Location fromLocation = Location.builder()
                .name("창고A")
                .type(LocationType.WAREHOUSE)
                .capacity(1000)
                .coordinateX(100)
                .coordinateY(200)
                .build();
        
        Location toLocation = Location.builder()
                .name("창고B")
                .type(LocationType.WAREHOUSE)
                .capacity(800)
                .coordinateX(300)
                .coordinateY(400)
                .build();
        
        // LogisticTask 생성
        LogisticTask task = LogisticTask.builder()
                .name("테스트 물류 작업")
                .type(LogisticType.INNER)
                .worker(worker)
                .ware(ware)
                .fromLocation(fromLocation)
                .toLocation(toLocation)
                .quantity(5)
                .scheduledDate(LocalDate.of(2024, 1, 14))
                .etd(LocalTime.of(9, 0))
                .eta(LocalTime.of(10, 0))
                .status(status)
                .templateIdSnapshot(123)
                .build();
        
        return task;
    }
}
