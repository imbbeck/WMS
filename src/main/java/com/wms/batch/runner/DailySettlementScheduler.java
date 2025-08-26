package com.wms.batch.runner;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
@Slf4j
@RequiredArgsConstructor
public class DailySettlementScheduler {
    
    private final JobLauncher jobLauncher;
    private final Job stockSnapshotJob;  // 기존 재고 스냅샷 Job
    private final Job logisticTaskHistoryJob;  // 새로운 물류작업 히스토리 Job
    
    /**
     * 매일 자정에 일일 결산 배치 실행
     * 1. 재고 스냅샷 배치 실행
     * 2. 재고 배치 성공 시에만 물류작업 히스토리 배치 실행
     */
    @Scheduled(cron = "0 0 0 * * *")  // 매일 자증
    public void runDailySettlement() {
        log.info("=== 일일 결산 배치 시작 ===");
        
        LocalDateTime startTime = LocalDateTime.now();
        LocalDate targetDate = LocalDate.now().minusDays(1);
        
        try {
            // 1단계: 재고 스냅샷 배치 실행
            JobExecution stockJobExecution = runStockSnapshotJob(targetDate);
            
            if (stockJobExecution.getStatus() == BatchStatus.COMPLETED) {
                log.info("재고 스냅샷 배치 성공 완료");
	            logBatchSummary(startTime, targetDate, true, null);
            } else {
	            String errorMsg = String.format("재고 스냅샷 배치 실패: %s", stockJobExecution.getExitStatus());
	            log.error(errorMsg);
	            logBatchSummary(startTime, targetDate, false, errorMsg);
            }
                
            // 2단계: 물류작업 히스토리 배치 실행
            JobExecution historyJobExecution = runLogisticTaskHistoryJob(targetDate);

            if (historyJobExecution.getStatus() == BatchStatus.COMPLETED) {
                log.info("=== 일일 결산 배치 전체 성공 완료 ===");
                logBatchSummary(startTime, targetDate, true, null);
            } else {
                String errorMsg = String.format("물류작업 히스토리 배치 실패: %s", historyJobExecution.getExitStatus());
                log.error(errorMsg);
                logBatchSummary(startTime, targetDate, false, errorMsg);
            }
            
        } catch (Exception e) {
            String errorMsg = "일일 결산 배치 실행 중 예외 발생: " + e.getMessage();
            log.error(errorMsg, e);
            logBatchSummary(startTime, targetDate, false, errorMsg);
        }
    }
    
    /**
     * 재고 스냅샷 배치 실행
     */
    private JobExecution runStockSnapshotJob(LocalDate targetDate) throws Exception {
        log.info("재고 스냅샷 배치 시작 - 대상 날짜: {}", targetDate);
        
        JobParameters stockJobParams = new JobParametersBuilder()
                .addLocalDateTime("timestamp", LocalDateTime.now())
                .addLocalDate("targetDate", targetDate)
                .addString("jobType", "stockSnapshot")
                .toJobParameters();
        
        JobExecution execution = jobLauncher.run(stockSnapshotJob, stockJobParams);
        
        log.info("재고 스냅샷 배치 실행 완료: Status={}, ExitCode={}", 
                execution.getStatus(), execution.getExitStatus().getExitCode());
        
        return execution;
    }
    
    /**
     * 물류작업 히스토리 배치 실행
     */
    private JobExecution runLogisticTaskHistoryJob(LocalDate targetDate) throws Exception {
        log.info("물류작업 히스토리 배치 시작 - 대상 날짜: {}", targetDate);
        
        JobParameters historyJobParams = new JobParametersBuilder()
                .addLocalDateTime("timestamp", LocalDateTime.now())
                .addLocalDate("targetDate", targetDate)
                .addString("jobType", "logisticTaskHistory")
                .toJobParameters();
        
        JobExecution execution = jobLauncher.run(logisticTaskHistoryJob, historyJobParams);
        
        log.info("물류작업 히스토리 배치 실행 완료: Status={}, ExitCode={}", 
                execution.getStatus(), execution.getExitStatus().getExitCode());
        
        return execution;
    }
    
    /**
     * 배치 실행 결과 요약 로깅
     */
    private void logBatchSummary(LocalDateTime startTime, LocalDate targetDate, 
                                boolean success, String errorMsg) {
        LocalDateTime endTime = LocalDateTime.now();
        long durationMinutes = java.time.Duration.between(startTime, endTime).toMinutes();
        
        log.info("========================================");
        log.info("일일 결산 배치 실행 요약");
        log.info("대상 날짜: {}", targetDate);
        log.info("시작 시간: {}", startTime);
        log.info("종료 시간: {}", endTime);
        log.info("소요 시간: {}분", durationMinutes);
        log.info("실행 결과: {}", success ? "성공" : "실패");
        if (!success && errorMsg != null) {
            log.info("실패 사유: {}", errorMsg);
        }
        log.info("========================================");
    }
    
    /**
     * 수동 배치 실행 (테스트 및 디버깅용)
     */
    public void runManualSettlement(LocalDate targetDate) {
        log.info("=== 수동 일일 결산 배치 시작 - 대상 날짜: {} ===", targetDate);
        
        LocalDateTime startTime = LocalDateTime.now();
        
        try {
            JobExecution stockJobExecution = runStockSnapshotJob(targetDate);
            
            if (stockJobExecution.getStatus() == BatchStatus.COMPLETED) {
                JobExecution historyJobExecution = runLogisticTaskHistoryJob(targetDate);
                
                boolean success = historyJobExecution.getStatus() == BatchStatus.COMPLETED;
                String errorMsg = success ? null : historyJobExecution.getExitStatus().toString();
                
                logBatchSummary(startTime, targetDate, success, errorMsg);
            } else {
                logBatchSummary(startTime, targetDate, false, stockJobExecution.getExitStatus().toString());
            }
            
        } catch (Exception e) {
            log.error("수동 배치 실행 중 오류 발생", e);
            logBatchSummary(startTime, targetDate, false, e.getMessage());
        }
    }
}
