package com.wms.batch.config;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.JobRepositoryTestUtils;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;

/**
 * 배치 테스트용 설정 클래스
 * 테스트 시에는 동기 실행으로 변경하여 테스트 안정성 확보
 */
@TestConfiguration
@EnableBatchProcessing
public class BatchTestConfiguration {

	/**
	 * 테스트용 JobLauncherTestUtils Bean
	 */
	@Bean
	public JobLauncherTestUtils jobLauncherTestUtils() {
		return new JobLauncherTestUtils();
	}

	/**
	 * 테스트용 JobRepositoryTestUtils Bean
	 */
	@Bean
	public JobRepositoryTestUtils jobRepositoryTestUtils() {
		return new JobRepositoryTestUtils();
	}

	/**
	 * 테스트용 TaskExecutor - 동기 실행
	 * 운영 환경의 ThreadPoolTaskExecutor 대신 사용
	 */
	@Bean
	@Primary
	public TaskExecutor testTaskExecutor() {
		return new SyncTaskExecutor();
	}
}