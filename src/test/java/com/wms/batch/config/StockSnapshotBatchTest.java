package com.wms.batch.config;

import com.wms.applicationInfra.config.QuerydslConfig;
import com.wms.stock.domain.model.Stock;
import com.wms.stock.domain.model.StockDailySnapshot;
import com.wms.stock.domain.model.StockKey;
import com.wms.stock.domain.repository.StockDailySnapshotRepository;
import com.wms.stock.domain.repository.StockRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.*;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.JobRepositoryTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier; // 수정: Qualifier 임포트
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
// import org.springframework.transaction.annotation.Transactional; // 수정: Transactional 임포트 제거

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@SpringBatchTest
@ActiveProfiles("test")
@Import(QuerydslConfig.class)
class StockSnapshotBatchTest {

	@Autowired
	private JobLauncherTestUtils jobLauncherTestUtils;

	@Autowired
	private JobRepositoryTestUtils jobRepositoryTestUtils;

	// 수정: 테스트할 Job을 명시적으로 주입받음
	// Batch 설정 파일의 Job Bean 이름과 일치해야 함 (예: @Bean public Job stockSnapshotJob(...))
	@Autowired
	@Qualifier("stockSnapshotJob")
	private Job stockSnapshotJob;

	@Autowired
	private StockRepository stockRepository;

	@Autowired
	private StockDailySnapshotRepository snapshotRepository;

	// 프로퍼티 값들을 테스트에서 참조 (이 부분은 그대로 유지)
	@Value("${batch.stock-partition.target-size:1000}")
	private int targetSize;

	@Value("${batch.stock-partition.max-partition-size:10}")
	private int maxPartitionSize;

	@Value("${batch.chunk-size:100}")
	private int chunkSize;

	// 수정: @Transactional 어노테이션 제거
	@BeforeEach
	void setUp() {
		jobRepositoryTestUtils.removeJobExecutions();
		// 수정: 주입받은 Job을 JobLauncherTestUtils에 설정
		jobLauncherTestUtils.setJob(stockSnapshotJob);

		// 데이터 정리 로직은 그대로 유지 (트랜잭션 롤백 대신 수동 정리)
		stockRepository.deleteAllInBatch();
		snapshotRepository.deleteAllInBatch();
	}

	@Test
	@DisplayName("재고 스냅샷 배치 Job 전체 실행 테스트")
	void testStockSnapshotJob() throws Exception {
		// Given: 프로퍼티 기반 테스트 데이터 생성
		int testDataSize = targetSize * 2; // target-size의 2배 데이터 생성
		createTestStockData(testDataSize);

		// When
		// 수정: JobParameters를 고유하게 만드는 것이 좋으므로 jobParameters() 헬퍼 메소드 사용
		JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters());

		// Then
		assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
		assertThat(jobExecution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);

		List<StockDailySnapshot> snapshots = snapshotRepository.findAll();
		assertThat(snapshots).hasSize(testDataSize); // 실제 생성된 데이터 수와 동일

		LocalDate yesterday = LocalDate.now().minusDays(1);
		StockDailySnapshot firstSnapshot = snapshots.get(0);
		assertThat(firstSnapshot.getKey().getSnapshotDate()).isEqualTo(yesterday);
		assertThat(firstSnapshot.getQuantity()).isGreaterThan(0);

		System.out.println("처리된 데이터: " + testDataSize + "건, 생성된 스냅샷: " + snapshots.size() + "개");
	}

	@Test
	@DisplayName("Master Step 실행 테스트 - 파티션 수 검증")
	void testMasterStep() throws Exception {
		// Given: 프로퍼티 기반 데이터 생성
		int testDataSize = targetSize * 5;
		createTestStockData(testDataSize);

		// When
		JobExecution jobExecution = jobLauncherTestUtils.launchStep("masterStep");

		// Then
		assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

		// 수정: 예상 파티션 수를 Math.ceil을 사용하여 정확하게 계산
		int expectedPartitions = (int) Math.ceil((double) testDataSize / targetSize);
		// 데이터가 0개일 경우, 파티션이 0개 또는 1개가 될 수 있으므로, 데이터가 있을 때만 계산
		if (testDataSize == 0) {
			expectedPartitions = 1; // 혹은 파티셔너 구현에 따라 0
		}
		expectedPartitions = Math.min(expectedPartitions, maxPartitionSize);

		// Slave Steps 검증
		long actualSlaveStepCount = jobExecution.getStepExecutions().stream()
				.filter(step -> step.getStepName().startsWith("slaveStep:partition"))
				.count();

		// 검증 실패한 부분
		assertThat(actualSlaveStepCount).isEqualTo(expectedPartitions);

		System.out.println("데이터: " + testDataSize + "건, 예상 파티션: " + expectedPartitions + "개, 실제 파티션: " + actualSlaveStepCount + "개");
		System.out.println("TARGET_SIZE: " + targetSize + ", MAX_PARTITION_SIZE: " + maxPartitionSize);
	}


	@Test
	@DisplayName("청크 단위 처리 검증 테스트")
	void testChunkProcessing() throws Exception {
		// Given: 청크 사이즈의 배수로 데이터 생성
		int testDataSize = chunkSize * 3; // 청크 3개 분량
		createTestStockData(testDataSize);

		// When
		JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters());

		// Then
		assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

		// 각 파티션의 commit count 확인
		jobExecution.getStepExecutions().stream()
				.filter(step -> step.getStepName().startsWith("slaveStep:partition"))
				.forEach(step -> System.out.println("Step: " + step.getStepName() +
						", ReadCount: " + step.getReadCount() +
						", CommitCount: " + step.getCommitCount() +
						", 예상 CommitCount: " + Math.ceil((double)step.getReadCount() / chunkSize)));

		List<StockDailySnapshot> snapshots = snapshotRepository.findAll();
		assertThat(snapshots).hasSize(testDataSize);

		System.out.println("청크 처리 테스트 - 총 데이터: " + testDataSize + "건, 청크 사이즈: " + chunkSize + "건");
	}

	@Test
	@DisplayName("소량 데이터 처리 테스트")
	void testSmallDataProcessing() throws Exception {
		// Given: target-size보다 작은 데이터
		int testDataSize = Math.max(1, targetSize / 2);
		createTestStockData(testDataSize);

		// When
		JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters());

		// Then
		assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

		List<StockDailySnapshot> snapshots = snapshotRepository.findAll();
		assertThat(snapshots).hasSize(testDataSize);

		// 최소 1개 파티션은 생성되어야 함
		long partitionCount = jobExecution.getStepExecutions().stream()
				.filter(step -> step.getStepName().startsWith("slaveStep:partition"))
				.count();

		assertThat(partitionCount).isGreaterThanOrEqualTo(1);

		System.out.println("소량 데이터 테스트 - 데이터: " + testDataSize + "건, 파티션: " + partitionCount + "개");
	}

	private JobParameters jobParameters() {
		return new JobParametersBuilder()
				.addLong("time", System.currentTimeMillis())
				.toJobParameters();
	}

	private void createTestStockData(int count) {
		for (int i = 1; i <= count; i++) {
			StockKey key = StockKey.of((long) i, (long) (i % 3 + 1));  // 3개의 창고로 분배
			Stock stock = Stock.builder()
					.key(key)
					.quantity(i * 10)
					.build();
			stockRepository.save(stock);
		}
		System.out.println("테스트 데이터 생성 완료: " + count + "건");
	}
}