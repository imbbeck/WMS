package com.wms.logisticTask.batch;

import com.wms.logisticTask.domain.model.LogisticTask;
import com.wms.logisticTask.domain.model.LogisticTaskHistory;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class LogisticTaskHistoryProcessor implements ItemProcessor<LogisticTask, LogisticTaskHistory> {

    @Override
    public LogisticTaskHistory process(LogisticTask task) {
        LocalDate settlementDate = LocalDate.now();
        
        return LogisticTaskHistory.fromLogisticTask(task, settlementDate);
    }
}
