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
import org.springframework.batch.core.job.builder.JobBuilderHelper;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.builder.SimpleStepBuilder;
import org.springframework.batch.core.step.builder.StepBuilderHelper;
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

	@Bean
	public Step slaveStep() {
		StepBuilder stepBuilder = new StepBuilder("slaveStep", jobRepository);

		SimpleStepBuilder<Stock, StockDailySnapshot> step = stepBuilder
				.<Stock, StockDailySnapshot>chunk(1000, transactionManager)
				.reader(stockSnapshotReader)
				.processor(stockSnapshotProcessor)
				.writer(stockSnapshotWriter)
				.faultTolerant()
				.retryLimit(3)
				.retry(Exception.class)
				.skipLimit(10)
				.skip(Exception.class);

		return step.build();
	}

	@Bean
	public Step masterStep() {
		StepBuilder stepBuilder = new StepBuilder("masterStep", jobRepository);

		return stepBuilder.partitioner("slaveStep", partitioner)
				.step(slaveStep())
				.gridSize(10)
				.taskExecutor(taskExecutor())
				.build();
	}

	@Bean
	public Job stockSnapshotJob() {
		JobBuilder jobBuilder = new JobBuilder("stockSnapshotJob", jobRepository);

		return jobBuilder
				.incrementer(new RunIdIncrementer())
				.start(masterStep())
				.build();
	}

	@Bean
	public TaskExecutor taskExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(10);
		executor.setMaxPoolSize(20);
		executor.setQueueCapacity(25);
		executor.setThreadNamePrefix("stock-partition-thread-");
		executor.initialize();
		return executor;
	}
}
