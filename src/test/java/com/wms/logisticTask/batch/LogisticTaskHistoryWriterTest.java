package com.wms.logisticTask.batch;

import com.wms.logisticTask.domain.model.LogisticTaskHistory;
import com.wms.logisticTask.domain.model.LogisticTaskStatus;
import com.wms.logisticTask.domain.repository.LogisticTaskHistoryRepository;
import com.wms.logisticTask.domain.repository.LogisticTaskRepository;
import com.wms.logisticTemplate.domain.model.LogisticType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.item.Chunk;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LogisticTaskHistoryWriterTest {

    @Mock
    private LogisticTaskHistoryRepository historyRepository;

    @Mock
    private LogisticTaskRepository taskRepository;

    @InjectMocks
    private LogisticTaskHistoryWriter writer;

    @Test
    void write_정상저장_및_삭제() throws Exception {
        // Given
        LogisticTaskHistory history1 = createTestHistory(1L, "작업1");
        LogisticTaskHistory history2 = createTestHistory(2L, "작업2");
        
        Chunk<LogisticTaskHistory> chunk = new Chunk<>(Arrays.asList(history1, history2));
        
        when(historyRepository.saveAll(anyList())).thenReturn(Arrays.asList(history1, history2));

        // When
        writer.write(chunk);

        // Then
        verify(historyRepository).saveAll(anyList());
        verify(taskRepository).deleteAllByIdInBatch(Arrays.asList(1L, 2L));
    }

    @Test
    void write_빈청크_처리() throws Exception {
        // Given
        Chunk<LogisticTaskHistory> emptyChunk = new Chunk<>();

        // When
        writer.write(emptyChunk);

        // Then
        // 예외 없이 정상 처리되어야 함
    }

    @Test
    void write_단일항목_처리() throws Exception {
        // Given
        LogisticTaskHistory history = createTestHistory(1L, "단일작업");
        Chunk<LogisticTaskHistory> chunk = new Chunk<>(List.of(history));
        
        when(historyRepository.saveAll(anyList())).thenReturn(List.of(history));

        // When
        writer.write(chunk);

        // Then
        verify(historyRepository).saveAll(List.of(history));
        verify(taskRepository).deleteAllByIdInBatch(List.of(1L));
    }

    @Test
    void write_대량데이터_처리() throws Exception {
        // Given
        List<LogisticTaskHistory> histories = createBulkHistories(100);
        Chunk<LogisticTaskHistory> chunk = new Chunk<>(histories);
        
        when(historyRepository.saveAll(anyList())).thenReturn(histories);

        // When
        writer.write(chunk);

        // Then
        verify(historyRepository).saveAll(histories);
        
        List<Long> expectedIds = histories.stream()
                .map(LogisticTaskHistory::getOriginalTaskId)
                .toList();
        verify(taskRepository).deleteAllByIdInBatch(expectedIds);
    }

    private LogisticTaskHistory createTestHistory(Long originalTaskId, String name) {
        return LogisticTaskHistory.builder()
                .originalTaskId(originalTaskId)
                .name(name)
                .type(LogisticType.INNER)
                .workerId(1L)
                .workerName("테스트 작업자")
                .wareId(1L)
                .wareName("테스트 물품")
                .fromLocationId(1L)
                .fromLocationName("출발지")
                .toLocationId(2L)
                .toLocationName("도착지")
                .quantity(10)
                .scheduledDate(LocalDate.now().minusDays(1))
                .etd(LocalTime.of(9, 0))
                .eta(LocalTime.of(10, 0))
                .originalStatus(LogisticTaskStatus.PENDING)
                .finalStatus(LogisticTaskStatus.EXPIRED)
                .templateIdSnapshot(100L)
                .settlementDate(LocalDate.now())
                .build();
    }

    private List<LogisticTaskHistory> createBulkHistories(int count) {
        return java.util.stream.IntStream.range(0, count)
                .mapToObj(i -> createTestHistory((long) (i + 1), "대량작업" + (i + 1)))
                .toList();
    }
}
