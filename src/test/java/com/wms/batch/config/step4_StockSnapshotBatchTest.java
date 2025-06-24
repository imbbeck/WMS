package com.wms.batch.config;

import com.wms.applicationInfra.config.QuerydslConfig;
import com.wms.stock.domain.model.Stock;
import com.wms.stock.domain.model.StockDailySnapshot;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@SpringBatchTest
@Import(QuerydslConfig.class)
//@TestPropertySource(properties = {
//		"spring.batch.job.enabled=false",
//		"spring.jpa.hibernate.ddl-auto=create-drop",
//		"spring.datasource.url=jdbc:h2:mem:testdb;MODE=MySQL;DATABASE_TO_LOWER=TRUE",
//		"spring.datasource.driver-class-name=org.h2.Driver",
//		"spring.datasource.username=sa",
//		"spring.datasource.password=",
//		"spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
//		"spring.cache.type=none",
//		"spring.data.redis.repositories.enabled=false"
//})
class step4_StockSnapshotBatchTest {

	@Autowired
	private JobLauncherTestUtils jobLauncherTestUtils;

	@Autowired
	private JobRepositoryTestUtils jobRepositoryTestUtils;

	@Autowired
	private StockRepository stockRepository;

	@Autowired
	private StockDailySnapshotRepository snapshotRepository;

	// 프로퍼티 값들을 테스트에서 참조
	@Value("${batch.stock-partition.target-size:3}")
	private int targetSize;

	@Value("${batch.stock-partition.max-partition-size:5}")
	private int maxPartitionSize;

	@Value("${batch.chunk-size:10}")
	private int chunkSize;

	@BeforeEach
	@Transactional
	void setUp() {
		jobRepositoryTestUtils.removeJobExecutions();
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
		JobParameters jobParameters = new JobParametersBuilder()
				.addLong("time", System.currentTimeMillis())
				.toJobParameters();

		JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

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
		int testDataSize = targetSize * 3; // target-size의 3배 = 예상 파티션 4개
		createTestStockData(testDataSize);

		// When
		JobExecution jobExecution = jobLauncherTestUtils.launchStep("masterStep");

		// Then
		assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

		// 예상 파티션 수 계산: (testDataSize / targetSize) + 1
		int expectedPartitions = (testDataSize / targetSize) + 1;
		expectedPartitions = Math.min(expectedPartitions, maxPartitionSize);

		// Master Step 검증
		StepExecution masterStep = jobExecution.getStepExecutions().stream()
				.filter(step -> "masterStep".equals(step.getStepName()))
				.findFirst()
				.orElse(null);

		assertThat(masterStep).isNotNull();
		assertThat(masterStep.getStatus()).isEqualTo(BatchStatus.COMPLETED);
		assertThat(masterStep.getReadCount()).isEqualTo(testDataSize);

		// Slave Steps 검증
		long actualSlaveStepCount = jobExecution.getStepExecutions().stream()
				.filter(step -> step.getStepName().startsWith("slaveStep:partition"))
				.count();

		assertThat(actualSlaveStepCount).isEqualTo(expectedPartitions);

		System.out.println("데이터: " + testDataSize + "건, 예상 파티션: " + expectedPartitions + "개, 실제 파티션: " + actualSlaveStepCount + "개");
		System.out.println("TARGET_SIZE: " + targetSize + ", MAX_PARTITION_SIZE: " + maxPartitionSize);
	}

	@Test
	@DisplayName("파티션 개수 한계 테스트")
	void testMaxPartitionLimit() throws Exception {
		// Given: MAX_PARTITION_SIZE를 초과하는 데이터 생성
		int testDataSize = targetSize * (maxPartitionSize + 2); // 최대 파티션 수를 초과하는 데이터
		createTestStockData(testDataSize);

		// When
		JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters());

		// Then
		assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

		List<StockDailySnapshot> snapshots = snapshotRepository.findAll();
		assertThat(snapshots).hasSize(testDataSize);

		System.out.println("대용량 데이터 테스트 - 데이터: " + testDataSize + "건, 최대 파티션 제한: " + maxPartitionSize + "개");
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
			Stock stock = Stock.builder()
					.wareId((long) i)
					.warehouseId((long) (i % 3 + 1))
					.quantity(i * 10)
					.build();
			stockRepository.save(stock);
		}
		System.out.println("테스트 데이터 생성 완료: " + count + "건");
	}
}