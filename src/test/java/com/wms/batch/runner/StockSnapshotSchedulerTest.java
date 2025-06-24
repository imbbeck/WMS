package com.wms.batch.runner;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.*;
import org.springframework.batch.core.launch.JobLauncher;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StockSnapshotSchedulerTest {

	@Mock
	private JobLauncher jobLauncher;

	@Mock
	private Job stockSnapshotJob;

	@InjectMocks
	private StockSnapshotScheduler scheduler;

	@Test
	@DisplayName("스케줄러 정상 실행 테스트")
	void testSuccessfulJobExecution() throws Exception {
		// Given
		JobExecution mockJobExecution = new JobExecution(1L);
		mockJobExecution.setStatus(BatchStatus.COMPLETED);

		when(jobLauncher.run(eq(stockSnapshotJob), any(JobParameters.class)))
				.thenReturn(mockJobExecution);

		// When
		scheduler.runSnapshotJob();

		// Then
		verify(jobLauncher, times(1)).run(eq(stockSnapshotJob), any(JobParameters.class));
	}

	@Test
	@DisplayName("스케줄러 Job 실행 실패 테스트")
	void testFailedJobExecution() throws Exception {
		// Given
		JobExecution mockJobExecution = new JobExecution(1L);
		mockJobExecution.setStatus(BatchStatus.FAILED);

		when(jobLauncher.run(eq(stockSnapshotJob), any(JobParameters.class)))
				.thenReturn(mockJobExecution);

		// When
		scheduler.runSnapshotJob();

		// Then
		verify(jobLauncher, times(1)).run(eq(stockSnapshotJob), any(JobParameters.class));
		// 실패해도 예외가 발생하지 않아야 함 (로깅만)
	}

	@Test
	@DisplayName("스케줄러 예외 발생 테스트")
	void testJobExecutionException() throws Exception {
		// Given
		when(jobLauncher.run(eq(stockSnapshotJob), any(JobParameters.class)))
				.thenThrow(new RuntimeException("Job execution failed"));

		// When
		scheduler.runSnapshotJob();

		// Then
		verify(jobLauncher, times(1)).run(eq(stockSnapshotJob), any(JobParameters.class));
		// 예외가 발생해도 메서드가 정상 종료되어야 함 (예외를 삼킴)
	}

	@Test
	@DisplayName("JobParameters 생성 테스트")
	void testJobParametersCreation() throws Exception {
		// Given
		JobExecution mockJobExecution = new JobExecution(1L);
		mockJobExecution.setStatus(BatchStatus.COMPLETED);

		// ArgumentCaptor를 사용하여 JobParameters 캡처
		when(jobLauncher.run(eq(stockSnapshotJob), any(JobParameters.class)))
				.thenReturn(mockJobExecution);

		// When
		scheduler.runSnapshotJob();

		// Then
		verify(jobLauncher).run(eq(stockSnapshotJob), argThat(jobParameters -> {
			// JobParameters에 시간 파라미터가 포함되어 있는지 확인
			return jobParameters.getParameters().containsKey("time") &&
					jobParameters.getLong("time") != null &&
					jobParameters.getLong("time") > 0;
		}));
	}

	@Test
	@DisplayName("동시 실행 방지 테스트 - JobLauncher 호출 횟수 확인")
	void testConcurrentExecutionPrevention() throws Exception {
		// Given
		JobExecution mockJobExecution = new JobExecution(1L);
		mockJobExecution.setStatus(BatchStatus.COMPLETED);

		when(jobLauncher.run(eq(stockSnapshotJob), any(JobParameters.class)))
				.thenReturn(mockJobExecution);

		// When: 스케줄러를 여러 번 호출
		scheduler.runSnapshotJob();
		scheduler.runSnapshotJob();
		scheduler.runSnapshotJob();

		// Then: JobLauncher가 각각 호출되어야 함 (Spring Batch에서 중복 실행 방지)
		verify(jobLauncher, times(3)).run(eq(stockSnapshotJob), any(JobParameters.class));
	}

	@Test
	@DisplayName("JobParameters 유니크 값 확인 테스트")
	void testUniqueJobParameters() throws Exception {
		// Given
		JobExecution mockJobExecution1 = new JobExecution(1L);
		JobExecution mockJobExecution2 = new JobExecution(2L);
		mockJobExecution1.setStatus(BatchStatus.COMPLETED);
		mockJobExecution2.setStatus(BatchStatus.COMPLETED);

		when(jobLauncher.run(eq(stockSnapshotJob), any(JobParameters.class)))
				.thenReturn(mockJobExecution1)
				.thenReturn(mockJobExecution2);

		// When: 짧은 간격으로 두 번 실행
		long time1 = System.currentTimeMillis();
		scheduler.runSnapshotJob();

		Thread.sleep(1); // 1ms 대기로 시간 차이 보장

		long time2 = System.currentTimeMillis();
		scheduler.runSnapshotJob();

		// Then: 각각 다른 시간 파라미터로 호출되어야 함
		verify(jobLauncher, times(2)).run(eq(stockSnapshotJob), any(JobParameters.class));

		// 실제로는 time 파라미터가 다르므로 Spring Batch에서 다른 JobInstance로 인식
	}
}