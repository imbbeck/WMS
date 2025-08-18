package com.wms.batch.config;

import com.wms.batch.partitioner.DynamicLogisticTaskPartitioner;
import com.wms.logisticTask.batch.LogisticTaskHistoryProcessor;
import com.wms.logisticTask.batch.LogisticTaskHistoryReader;
import com.wms.logisticTask.batch.LogisticTaskHistoryWriter;
import com.wms.logisticTask.domain.model.LogisticTask;
import com.wms.logisticTask.domain.model.LogisticTaskHistory;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@RequiredArgsConstructor
public class LogisticTaskHistoryBatchConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    private final DynamicLogisticTaskPartitioner dynamicLogisticTaskPartitioner;
    private final LogisticTaskHistoryReader logisticTaskHistoryReader;
    private final LogisticTaskHistoryProcessor logisticTaskHistoryProcessor;
    private final LogisticTaskHistoryWriter logisticTaskHistoryWriter;

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
    public Job logisticTaskHistoryJob() {
       return new JobBuilder("logisticTaskHistoryJob", jobRepository)
             .incrementer(new RunIdIncrementer())
             .start(masterStep())
             .build();
    }

    @Bean
    public Step masterStep() {
       return new StepBuilder("masterStep", jobRepository)
             .partitioner("slaveStep", dynamicLogisticTaskPartitioner)
             .step(slaveStep())
             .gridSize(gridSize)
             .taskExecutor(taskExecutor())
             .build();
    }

    @Bean
    public Step slaveStep() {
       return new StepBuilder("slaveStep", jobRepository)
             .<LogisticTask, LogisticTaskHistory>chunk(chunkSize, transactionManager)
             .reader(logisticTaskHistoryReader)
             .processor(logisticTaskHistoryProcessor)
             .writer(logisticTaskHistoryWriter)
             .faultTolerant()
             .retryLimit(retryLimit)
             .retry(Exception.class)
             .skipLimit(skipLimit)
             .skip(Exception.class)
             .build();
    }

    @Bean
    public TaskExecutor taskExecutor() {
       ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
       executor.setCorePoolSize(corePoolSize);
       executor.setMaxPoolSize(maxPoolSize);
       executor.setQueueCapacity(queueCapacity);
       executor.setThreadNamePrefix("logistic-partition-thread-");
       executor.initialize();
       return executor;
    }
}
