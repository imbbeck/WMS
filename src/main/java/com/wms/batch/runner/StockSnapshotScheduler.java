package com.wms.batch.runner;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.*;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class StockSnapshotScheduler {

	private final JobLauncher jobLauncher;
	private final Job stockSnapshotJob;

	@Scheduled(cron = "${batch.schedule.stock-snapshot:0 0 0 * * *}")
	public void runSnapshotJob() {
		try {
			log.info(">>> [BATCH] 재고 스냅샷 배치 실행 시작 at {}", LocalDateTime.now());

			JobParameters jobParameters = new JobParametersBuilder()
					.addLong("time", System.currentTimeMillis())
					.toJobParameters();

			JobExecution execution = jobLauncher.run(stockSnapshotJob, jobParameters);
			log.info(">>> [BATCH] Job 실행 상태: {}", execution.getStatus());

		} catch (Exception e) {
			log.error(">>> [BATCH] 재고 스냅샷 Job 실행 중 예외 발생", e);
		}
	}
}
