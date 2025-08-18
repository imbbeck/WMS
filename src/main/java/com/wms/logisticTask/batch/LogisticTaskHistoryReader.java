package com.wms.logisticTask.batch;

import com.wms.logisticTask.domain.model.LogisticTask;
import com.wms.logisticTask.domain.repository.LogisticTaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Iterator;

@StepScope
@Component("logisticTaskHistoryReader")
@RequiredArgsConstructor
public class LogisticTaskHistoryReader implements ItemReader<LogisticTask> {

    private final LogisticTaskRepository logisticTaskRepository;

    @Value("#{stepExecutionContext['minId']}")
    private Long minId;

    @Value("#{stepExecutionContext['maxId']}")
    private Long maxId;

    @Value("#{stepExecutionContext['targetDate']}")
    private String targetDateStr;

    private Iterator<LogisticTask> taskIterator;
    private boolean initialized = false;

    @Override
    public LogisticTask read() throws Exception {
        if (!initialized) {
            initialize();
            initialized = true;
        }

        if (taskIterator != null && taskIterator.hasNext()) {
            return taskIterator.next();
        }

        return null;
    }

    private void initialize() {
        LocalDate targetDate = targetDateStr != null ? LocalDate.parse(targetDateStr) : LocalDate.now().minusDays(1);

        if (minId != null && maxId != null) {
            Pageable pageable = PageRequest.of(0, Integer.MAX_VALUE, Sort.by("id").ascending());
            Page<LogisticTask> taskPage = logisticTaskRepository.findByScheduledDateAndIdBetween(
                    targetDate, minId, maxId, pageable);
            taskIterator = taskPage.getContent().iterator();
        }
    }
}
