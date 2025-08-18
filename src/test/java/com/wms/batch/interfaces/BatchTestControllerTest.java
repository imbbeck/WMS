package com.wms.batch.interfaces;

import com.wms.batch.runner.DailySettlementScheduler;
import com.wms.logisticTask.domain.repository.LogisticTaskHistoryRepository;
import com.wms.logisticTask.domain.repository.LogisticTaskRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BatchTestController.class)
@DisplayName("BatchTestController API 테스트")
class BatchTestControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private DailySettlementScheduler dailySettlementScheduler;
    
    @MockBean
    private LogisticTaskRepository logisticTaskRepository;
    
    @MockBean
    private LogisticTaskHistoryRepository logisticTaskHistoryRepository;
    
    @Test
    @DisplayName("일일 결산 배치 수동 실행 API")
    void runDailySettlement() throws Exception {
        // Given
        doNothing().when(dailySettlementScheduler).runManualSettlement(any(LocalDate.class));
        
        // When & Then
        mockMvc.perform(post("/api/batch/daily-settlement")
                        .param("targetDate", "2024-01-15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("started"))
                .andExpect(jsonPath("$.targetDate").value("2024-01-15"))
                .andExpect(jsonPath("$.message").exists());
    }
    
    @Test
    @DisplayName("일일 결산 배치 수동 실행 - 기본 날짜")
    void runDailySettlement_DefaultDate() throws Exception {
        // Given
        doNothing().when(dailySettlementScheduler).runManualSettlement(any(LocalDate.class));
        
        // When & Then
        mockMvc.perform(post("/api/batch/daily-settlement"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("started"))
                .andExpect(jsonPath("$.targetDate").exists())
                .andExpect(jsonPath("$.message").exists());
        
        verify(dailySettlementScheduler, times(1)).runManualSettlement(any(LocalDate.class));
    }
    
    @Test
    @DisplayName("배치 상태 조회 API")
    void getBatchStatus() throws Exception {
        // Given
        LocalDate testDate = LocalDate.of(2024, 1, 15);
        when(logisticTaskRepository.countByScheduledDate(testDate)).thenReturn(10L);
        when(logisticTaskHistoryRepository.findBySettlementDate(testDate)).thenReturn(Collections.emptyList());
        
        // When & Then
        mockMvc.perform(get("/api/batch/status")
                        .param("targetDate", "2024-01-15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.targetDate").value("2024-01-15"))
                .andExpect(jsonPath("$.pendingTaskCount").value(10))
                .andExpect(jsonPath("$.historyCount").value(0))
                .andExpect(jsonPath("$.isProcessed").value(false));
    }
    
    @Test
    @DisplayName("배치 상태 조회 - 기본 날짜")
    void getBatchStatus_DefaultDate() throws Exception {
        // Given
        when(logisticTaskRepository.countByScheduledDate(any(LocalDate.class))).thenReturn(5L);
        when(logisticTaskHistoryRepository.findBySettlementDate(any(LocalDate.class))).thenReturn(Collections.emptyList());
        
        // When & Then
        mockMvc.perform(get("/api/batch/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.targetDate").exists())
                .andExpect(jsonPath("$.pendingTaskCount").value(5))
                .andExpect(jsonPath("$.historyCount").value(0))
                .andExpect(jsonPath("$.isProcessed").value(false));
    }
    
    @Test
    @DisplayName("히스토리 통계 조회 API")
    void getHistoryStats() throws Exception {
        // Given
        LocalDate testDate = LocalDate.of(2024, 1, 15);
        when(logisticTaskHistoryRepository.findBySettlementDate(testDate))
                .thenReturn(Collections.emptyList());
        when(logisticTaskHistoryRepository.findStatusStatsBySettlementDate(testDate))
                .thenReturn(Arrays.asList(
                        new Object[]{"COMPLETED", 10L},
                        new Object[]{"EXPIRED", 5L},
                        new Object[]{"FAILED", 2L}
                ));
        
        // When & Then
        mockMvc.perform(get("/api/batch/history-stats/2024-01-15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.settlementDate").value("2024-01-15"))
                .andExpect(jsonPath("$.totalTasks").value(0))
                .andExpect(jsonPath("$.statusCounts").exists())
                .andExpect(jsonPath("$.statusCounts.COMPLETED").value(10))
                .andExpect(jsonPath("$.statusCounts.EXPIRED").value(5))
                .andExpect(jsonPath("$.statusCounts.FAILED").value(2));
    }
    
    @Test
    @DisplayName("배치 실행 가능 여부 체크 API - 실행 가능")
    void canRunBatch_CanRun() throws Exception {
        // Given
        LocalDate testDate = LocalDate.of(2024, 1, 15);
        when(logisticTaskRepository.countByScheduledDate(testDate)).thenReturn(20L);
        
        // When & Then
        mockMvc.perform(get("/api/batch/can-run/2024-01-15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.targetDate").value("2024-01-15"))
                .andExpect(jsonPath("$.canRun").value(true))
                .andExpect(jsonPath("$.taskCount").value(20))
                .andExpect(jsonPath("$.message").value("배치 실행 가능"));
    }
    
    @Test
    @DisplayName("배치 실행 가능 여부 체크 API - 실행 불가능")
    void canRunBatch_CannotRun() throws Exception {
        // Given
        LocalDate testDate = LocalDate.of(2024, 1, 15);
        when(logisticTaskRepository.countByScheduledDate(testDate)).thenReturn(0L);
        
        // When & Then
        mockMvc.perform(get("/api/batch/can-run/2024-01-15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.targetDate").value("2024-01-15"))
                .andExpect(jsonPath("$.canRun").value(false))
                .andExpect(jsonPath("$.taskCount").value(0))
                .andExpect(jsonPath("$.message").value("처리할 작업이 없습니다"));
    }
    
    @Test
    @DisplayName("예외 발생 시 에러 응답")
    void runDailySettlement_Exception() throws Exception {
        // Given
        doThrow(new RuntimeException("테스트 예외"))
                .when(dailySettlementScheduler).runManualSettlement(any(LocalDate.class));
        
        // When & Then
        mockMvc.perform(post("/api/batch/daily-settlement"))
                .andExpect(status().isOk()) // 비동기 실행이므로 성공 응답
                .andExpect(jsonPath("$.status").value("started"));
    }
}
