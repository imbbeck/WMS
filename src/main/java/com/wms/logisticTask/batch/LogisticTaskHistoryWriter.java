package com.wms.logisticTask.batch;

import com.wms.logisticTask.domain.model.LogisticTaskHistory;
import com.wms.logisticTask.domain.repository.LogisticTaskHistoryRepository;
import com.wms.logisticTask.domain.repository.LogisticTaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class LogisticTaskHistoryWriter implements ItemWriter<LogisticTaskHistory> {

    private final LogisticTaskHistoryRepository logisticTaskHistoryRepository;
    private final LogisticTaskRepository logisticTaskRepository;

    @Override
    public void write(Chunk<? extends LogisticTaskHistory> chunk) throws Exception {
        List<LogisticTaskHistory> historyList = chunk.getItems().stream()
                .map(LogisticTaskHistory.class::cast)
                .collect(Collectors.toList());

        // 히스토리 저장
        logisticTaskHistoryRepository.saveAll(historyList);

        // 원본 삭제
        List<Long> taskIdsToDelete = historyList.stream()
                .map(LogisticTaskHistory::getOriginalTaskId)
                .collect(Collectors.toList());
        
        logisticTaskRepository.deleteAllByIdInBatch(taskIdsToDelete);
    }
}
