package com.wms.batch.partitioner;

import com.wms.logisticTask.domain.repository.LogisticTaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class DynamicLogisticTaskPartitioner implements Partitioner {
    
    private final LogisticTaskRepository logisticTaskRepository;
    
    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {
        LocalDate targetDate = LocalDate.now().minusDays(1);
        
        // 어제 날짜의 LogisticTask 개수와 ID 범위 조회
        Long totalCount = logisticTaskRepository.countByScheduledDate(targetDate);
        
        if (totalCount == 0) {
            log.info("어제 날짜({})의 LogisticTask가 없습니다.", targetDate);
            return Collections.emptyMap();
        }
        
        Long minId = logisticTaskRepository.findMinIdByScheduledDate(targetDate);
        Long maxId = logisticTaskRepository.findMaxIdByScheduledDate(targetDate);
        
        if (minId == null || maxId == null) {
            log.warn("어제 날짜({})의 LogisticTask ID 범위를 찾을 수 없습니다.", targetDate);
            return Collections.emptyMap();
        }
        
        log.info("파티셔닝 대상: 총 {}개, ID 범위: {} ~ {}", totalCount, minId, maxId);
        
        Map<String, ExecutionContext> partitions = new HashMap<>();
        
        // ID 범위를 gridSize만큼 분할
        long range = (maxId - minId + 1);
        long partitionSize = Math.max(1, range / gridSize);
        
        for (int i = 0; i < gridSize; i++) {
            long partitionMinId = minId + (i * partitionSize);
            long partitionMaxId = (i == gridSize - 1) ? maxId : partitionMinId + partitionSize - 1;
            
            if (partitionMinId <= maxId) {
                ExecutionContext context = new ExecutionContext();
                context.putLong("minId", partitionMinId);
                context.putLong("maxId", partitionMaxId);
                context.putString("targetDate", targetDate.toString());
                
                partitions.put("partition" + i, context);
                log.info("파티션 {}: ID {} ~ {} (날짜: {})", i, partitionMinId, partitionMaxId, targetDate);
            }
        }
        
        log.info("총 {} 개의 파티션 생성 완료", partitions.size());
        return partitions;
    }
}
