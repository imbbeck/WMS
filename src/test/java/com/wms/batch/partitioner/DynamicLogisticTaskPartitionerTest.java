package com.wms.batch.partitioner;

import com.wms.logisticTask.domain.repository.LogisticTaskRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.item.ExecutionContext;

import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DynamicLogisticTaskPartitionerTest {

    @Mock
    private LogisticTaskRepository logisticTaskRepository;

    @InjectMocks
    private DynamicLogisticTaskPartitioner partitioner;

    @Test
    void partition_정상데이터_파티션생성() {
        // Given
        int gridSize = 4;
        when(logisticTaskRepository.countByScheduledDate(any(LocalDate.class))).thenReturn(1000L);
        when(logisticTaskRepository.findMinIdByScheduledDate(any(LocalDate.class))).thenReturn(1L);
        when(logisticTaskRepository.findMaxIdByScheduledDate(any(LocalDate.class))).thenReturn(1000L);

        // When
        Map<String, ExecutionContext> result = partitioner.partition(gridSize);

        // Then
        assertThat(result).hasSize(gridSize);
        
        // 각 파티션 검증
        for (int i = 0; i < gridSize; i++) {
            String partitionKey = "partition" + i;
            ExecutionContext context = result.get(partitionKey);
            
            assertThat(context).isNotNull();
            assertThat(context.get("minId")).isNotNull();
            assertThat(context.get("maxId")).isNotNull();
            assertThat(context.get("targetDate")).isNotNull();
        }
    }

    @Test
    void partition_빈데이터_빈맵반환() {
        // Given
        int gridSize = 4;
        when(logisticTaskRepository.countByScheduledDate(any(LocalDate.class))).thenReturn(0L);

        // When
        Map<String, ExecutionContext> result = partitioner.partition(gridSize);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void partition_소량데이터_단일파티션() {
        // Given
        int gridSize = 4;
        when(logisticTaskRepository.countByScheduledDate(any(LocalDate.class))).thenReturn(10L);
        when(logisticTaskRepository.findMinIdByScheduledDate(any(LocalDate.class))).thenReturn(1L);
        when(logisticTaskRepository.findMaxIdByScheduledDate(any(LocalDate.class))).thenReturn(10L);

        // When
        Map<String, ExecutionContext> result = partitioner.partition(gridSize);

        // Then
        assertThat(result).hasSize(1);
        
        ExecutionContext context = result.get("partition0");
        assertThat(context.getLong("minId")).isEqualTo(1L);
        assertThat(context.getLong("maxId")).isEqualTo(10L);
    }
}
