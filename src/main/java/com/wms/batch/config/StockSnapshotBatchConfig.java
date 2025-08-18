package com.wms.batch.config;

import com.wms.batch.partitioner.DynamicStockPartitioner;
import com.wms.stock.batch.StockSnapshotProcessor;
import com.wms.stock.batch.StockSnapshotReader;
import com.wms.stock.batch.StockSnapshotWriter;
import com.wms.stock.domain.model.Stock;
import com.wms.stock.domain.model.StockDailySnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.builder.SimpleStepBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@RequiredArgsConstructor
public class StockSnapshotBatchConfig {

	private final JobRepository jobRepository;
	private final PlatformTransactionManager transactionManager;

	private final DynamicStockPartitioner partitioner;
	private final StockSnapshotReader stockSnapshotReader;
	private final StockSnapshotProcessor stockSnapshotProcessor;
	private final StockSnapshotWriter stockSnapshotWriter;

	@Value("${batch.chunk-size:500}")
	private int chunkSize;

	@Value("${batch.retry-limit:3}")
	private int retryLimit;

	@Value("${batch.skip-limit:10}")
	private int skipLimit;

	@Value("${batch.grid-size:10}")
	private int gridSize;

	@Value("${batch.thread-pool.core-size:5}")
	private int corePoolSize;

	@Value("${batch.thread-pool.max-size:10}")
	private int maxPoolSize;

	@Value("${batch.thread-pool.queue-capacity:100}")
	private int queueCapacity;

	@Bean
	public Job stockSnapshotJob() {
		JobBuilder jobBuilder = new JobBuilder("stockSnapshotJob", jobRepository);

		return jobBuilder
				.incrementer(new RunIdIncrementer())
				.start(stockSnapshotMasterStep())
				.build();
	}

	@Bean
	public Step stockSnapshotMasterStep() {
		StepBuilder stepBuilder = new StepBuilder("masterStep", jobRepository);

		return stepBuilder.partitioner("slaveStep", partitioner)
				.step(stockSnapshotSlaveStep())
				.gridSize(gridSize)
				.taskExecutor(stockSnapshotTaskExecutor())
				.build();
	}

	@Bean
	public Step stockSnapshotSlaveStep() {
		StepBuilder stepBuilder = new StepBuilder("slaveStep", jobRepository);

		SimpleStepBuilder<Stock, StockDailySnapshot> step = stepBuilder
				.<Stock, StockDailySnapshot>chunk(chunkSize, transactionManager)
				.reader(stockSnapshotReader)
				.processor(stockSnapshotProcessor)
				.writer(stockSnapshotWriter)
				.faultTolerant()
				.retryLimit(retryLimit)
				.retry(Exception.class)
				.skipLimit(skipLimit)
				.skip(Exception.class);

		return step.build();
	}

	@Bean
	public TaskExecutor stockSnapshotTaskExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(corePoolSize);
		executor.setMaxPoolSize(maxPoolSize);
		executor.setQueueCapacity(queueCapacity);
		executor.setThreadNamePrefix("stock-partition-thread-");
		executor.initialize();
		return executor;
	}
}
