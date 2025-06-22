package com.wms.batch.partitioner;

import com.wms.stock.domain.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
@Component
public class DynamicStockPartitioner implements Partitioner {

	private final StockRepository stockRepository;

	private static final int TARGET_SIZE = 2;
	private static final int MAX_PARTITION_SIZE = 50;

	/**
	 * TARGET_SIZE 에 맞춰서 partitions 늘리다가 MAX_PARTITION_SIZE 에 다다르면 로깅하고 partitions 를 MAX_PARTITION_SIZE 에 고정.
	 */
	@Override
	public Map<String, ExecutionContext> partition(int gridSize) {
		long totalCount = stockRepository.count();
		int calculatedPartitions = (int) ((totalCount / TARGET_SIZE) + 1);
		int partitions = Math.min(calculatedPartitions, MAX_PARTITION_SIZE);

		if (calculatedPartitions > MAX_PARTITION_SIZE) {
			log.warn("[DynamicStockPartitioner] 파티션 개수 제한 초과: 계산된 파티션 수 = {}, 최대 = {}", calculatedPartitions, MAX_PARTITION_SIZE);
		}

		Long minId = stockRepository.findMinId();
		Long maxId = stockRepository.findMaxId();

		Map<String, ExecutionContext> result = new HashMap<>();

		if (minId == null || maxId == null) return result;

		long range = (maxId - minId) / partitions + 1;
		long start = minId;
		long end = start + range - 1;

		for (int i = 0; i < partitions; i++) {
			ExecutionContext context = new ExecutionContext();
			context.putLong("minId", start);
			context.putLong("maxId", Math.min(end, maxId));
			result.put("partition" + i, context);

			start += range;
			end += range;
		}
		return result;
	}
}
