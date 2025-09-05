package com.wms.batch.runner;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.launch.JobLauncher;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DailySettlementScheduler 테스트")
class DailySettlementSchedulerTest {
    
    @Mock
    private JobLauncher jobLauncher;
    
    @Mock
    private Job stockSnapshotJob;
    
    @Mock
    private Job logisticTaskHistoryJob;
    
    @Mock
    private JobExecution stockJobExecution;
    
    @Mock
    private JobExecution historyJobExecution;

    private DailySettlementScheduler scheduler;
    
    @BeforeEach
    void setUp() throws Exception {
	    scheduler = new DailySettlementScheduler(jobLauncher, stockSnapshotJob, logisticTaskHistoryJob);

	    // 1. stockJobExecution Mock 설정
	    lenient().when(stockJobExecution.getStatus()).thenReturn(BatchStatus.COMPLETED);
	    lenient().when(stockJobExecution.getExitStatus()).thenReturn(ExitStatus.COMPLETED); // NPE 방지
	    lenient().when(jobLauncher.run(eq(stockSnapshotJob), any(JobParameters.class)))
			    .thenReturn(stockJobExecution);

	    // 2. historyJobExecution Mock 설정
	    lenient().when(historyJobExecution.getStatus()).thenReturn(BatchStatus.COMPLETED);
	    lenient().when(historyJobExecution.getExitStatus()).thenReturn(ExitStatus.COMPLETED); // NPE 방지
	    lenient().when(jobLauncher.run(eq(logisticTaskHistoryJob), any(JobParameters.class)))
			    .thenReturn(historyJobExecution);
    }

	@Test
	@DisplayName("정상적인 일일 결산 스케줄 실행")
	void runDailySettlement_SuccessfulExecution() throws Exception {
		// Given - setUp()에서 이미 설정됨

		// When
		scheduler.runDailySettlement();

		// Then
		verify(jobLauncher, times(1)).run(eq(stockSnapshotJob), any(JobParameters.class));
		verify(jobLauncher, times(1)).run(eq(logisticTaskHistoryJob), any(JobParameters.class));
	}

	@Test
	@DisplayName("JobLauncher 예외 발생 시 안전한 처리")
	void runDailySettlement_JobLauncherException() throws Exception {
		// Given
		// setUp 설정을 오버라이드하여 예외를 발생시킵니다.
		when(jobLauncher.run(eq(stockSnapshotJob), any(JobParameters.class)))
				.thenThrow(new RuntimeException("Job execution failed"));

		// When & Then (예외가 발생하지 않아야 함)
		scheduler.runDailySettlement();

		verify(jobLauncher, times(1)).run(eq(stockSnapshotJob), any(JobParameters.class));
		verify(jobLauncher, never()).run(eq(logisticTaskHistoryJob), any(JobParameters.class));
	}

	@Test
	@DisplayName("수동 결산 실행")
	void runManualSettlement() throws Exception {
		// Given - setUp()에서 이미 설정됨
		LocalDate targetDate = LocalDate.of(2024, 1, 15);

		// When
		scheduler.runManualSettlement(targetDate);

		// Then
		verify(jobLauncher, times(1)).run(eq(stockSnapshotJob), any(JobParameters.class));
		verify(jobLauncher, times(1)).run(eq(logisticTaskHistoryJob), any(JobParameters.class));
	}

	@Test
	@DisplayName("물류작업 히스토리 배치 실패 시 로깅")
	void runDailySettlement_HistoryJobFails() throws Exception {
		// Given
		// historyJobExecution만 실패하도록 setUp 설정을 오버라이드합니다.
		when(historyJobExecution.getStatus()).thenReturn(BatchStatus.FAILED);
		when(historyJobExecution.getExitStatus()).thenReturn(ExitStatus.FAILED);

		// When
		scheduler.runDailySettlement();

		// Then
		verify(jobLauncher, times(1)).run(eq(stockSnapshotJob), any(JobParameters.class));
		verify(jobLauncher, times(1)).run(eq(logisticTaskHistoryJob), any(JobParameters.class));
	}

	@Test
	@DisplayName("배치 상태가 null인 경우 처리")
	void runDailySettlement_NullBatchStatus() throws Exception {
		// Given
		when(stockJobExecution.getStatus()).thenReturn(null);
		when(jobLauncher.run(eq(stockSnapshotJob), any(JobParameters.class)))
				.thenReturn(stockJobExecution);

		// When
		scheduler.runDailySettlement();

		// Then
		verify(jobLauncher, times(1)).run(eq(stockSnapshotJob), any(JobParameters.class));
		verify(jobLauncher, times(1)).run(eq(logisticTaskHistoryJob), any(JobParameters.class));
	}
}
