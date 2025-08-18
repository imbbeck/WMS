package com.wms.batch.interfaces;

import com.wms.batch.runner.DailySettlementScheduler;
import com.wms.logisticTask.domain.repository.LogisticTaskHistoryRepository;
import com.wms.logisticTask.domain.repository.LogisticTaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/batch")
@RequiredArgsConstructor
@Slf4j
public class BatchTestController {
    
    private final DailySettlementScheduler dailySettlementScheduler;
    private final LogisticTaskRepository logisticTaskRepository;
    private final LogisticTaskHistoryRepository logisticTaskHistoryRepository;
    
    /**
     * 수동으로 일일 결산 배치 실행
     */
    @PostMapping("/daily-settlement")
    public ResponseEntity<Map<String, Object>> runDailySettlement(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate targetDate) {
        
        LocalDate processDate = targetDate != null ? targetDate : LocalDate.now().minusDays(1);
        
        try {
            log.info("수동 일일 결산 배치 실행 요청 - 대상 날짜: {}", processDate);
            
            // 비동기로 배치 실행
            new Thread(() -> {
                dailySettlementScheduler.runManualSettlement(processDate);
            }).start();
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "started");
            response.put("targetDate", processDate);
            response.put("message", "배치 실행이 시작되었습니다. 로그를 확인해주세요.");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("배치 실행 요청 중 오류 발생", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 배치 실행 전 현재 상태 조회
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getBatchStatus(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate targetDate) {
        
        LocalDate checkDate = targetDate != null ? targetDate : LocalDate.now().minusDays(1);
        
        Map<String, Object> response = new HashMap<>();
        
        // 대상 날짜의 LogisticTask 개수
        Long taskCount = logisticTaskRepository.countByScheduledDate(checkDate);
        
        // 해당 날짜의 히스토리 개수
        Long historyCount = (long) logisticTaskHistoryRepository.findBySettlementDate(checkDate).size();
        
        response.put("targetDate", checkDate);
        response.put("pendingTaskCount", taskCount);
        response.put("historyCount", historyCount);
        response.put("isProcessed", taskCount == 0 && historyCount > 0);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 특정 날짜의 히스토리 통계 조회
     */
    @GetMapping("/history-stats/{date}")
    public ResponseEntity<Map<String, Object>> getHistoryStats(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        
        var historyList = logisticTaskHistoryRepository.findBySettlementDate(date);
        var statusStats = logisticTaskHistoryRepository.findStatusStatsBySettlementDate(date);
        
        Map<String, Object> response = new HashMap<>();
        response.put("settlementDate", date);
        response.put("totalTasks", historyList.size());
        
        Map<String, Long> statusCounts = new HashMap<>();
        for (Object[] stat : statusStats) {
            statusCounts.put(stat[0].toString(), (Long) stat[1]);
        }
        response.put("statusCounts", statusCounts);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 배치 실행 가능 여부 체크
     */
    @GetMapping("/can-run/{date}")
    public ResponseEntity<Map<String, Object>> canRunBatch(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        
        Long taskCount = logisticTaskRepository.countByScheduledDate(date);
        boolean canRun = taskCount > 0;
        
        Map<String, Object> response = new HashMap<>();
        response.put("targetDate", date);
        response.put("canRun", canRun);
        response.put("taskCount", taskCount);
        response.put("message", canRun ? "배치 실행 가능" : "처리할 작업이 없습니다");
        
        return ResponseEntity.ok(response);
    }
}
