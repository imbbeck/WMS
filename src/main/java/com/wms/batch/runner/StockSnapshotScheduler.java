//package com.wms.batch.runner;
//
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.batch.core.*;
//import org.springframework.batch.core.launch.JobLauncher;
//import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
//import org.springframework.stereotype.Component;
//
//import java.time.LocalDateTime;
//
//@Slf4j
//@Component
//@RequiredArgsConstructor
//@ConditionalOnProperty(name = "batch.stock-snapshot.standalone", havingValue = "true", matchIfMissing = false)
//public class StockSnapshotScheduler {
//
//	private final JobLauncher jobLauncher;
//	private final Job stockSnapshotJob;
//
//	/**
//	 * 독립적인 재고 스냅샷 배치 실행 (기본적으로 비활성화)
//	 * application.yml에서 batch.stock-snapshot.standalone=true로 설정해야 활성화
//	 * 일반적으로는 DailySettlementScheduler에서 통합 관리됨
//	 */
//	// @Scheduled(cron = "${batch.schedule.stock-snapshot:0 0 0 * * *}")
//	public void runSnapshotJob() {
//		try {
//			log.info(">>> [BATCH] 독립 재고 스냅샷 배치 실행 시작 at {}", LocalDateTime.now());
//
//			JobParameters jobParameters = new JobParametersBuilder()
//					.addLong("time", System.currentTimeMillis())
//					.toJobParameters();
//
//			JobExecution execution = jobLauncher.run(stockSnapshotJob, jobParameters);
//			log.info(">>> [BATCH] Job 실행 상태: {}", execution.getStatus());
//
//		} catch (Exception e) {
//			log.error(">>> [BATCH] 재고 스냅샷 Job 실행 중 예외 발생", e);
//		}
//	}
//
//	/**
//	 * 수동 실행용 메서드 (DailySettlementScheduler에서 호출)
//	 */
//	public JobExecution runStockSnapshotManually(JobParameters jobParameters) throws Exception {
//		log.info(">>> [BATCH] 수동 재고 스냅샷 배치 실행");
//		return jobLauncher.run(stockSnapshotJob, jobParameters);
//	}
//}
