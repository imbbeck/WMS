package com.wms.batch.partitioner;

import com.wms.stock.domain.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
@Component
public class DynamicStockPartitioner implements Partitioner {

	private final StockRepository stockRepository;

	@Value("${batch.stock-partition.target-size:1000}")
	private int targetSize;

	@Value("${batch.stock-partition.max-partition-size:50}")
	private int maxPartitionSize;

	/**
	 * targetSize 에 맞춰서 partitions 늘리다가 maxPartitionSize 에 다다르면 로깅하고 partitions 를 maxPartitionSize 에 고정.
	 */
	@Override
	public Map<String, ExecutionContext> partition(int gridSize) {
		long totalCount = stockRepository.count();

		// 데이터가 없는 경우 조기 반환
		if (totalCount == 0) {
			log.info("[DynamicStockPartitioner] 처리할 데이터가 없습니다. 빈 파티션 반환");
			return new HashMap<>();
		}

		int calculatedPartitions = (int) Math.ceil((double) totalCount / targetSize);
		// 데이터가 있지만 계산된 파티션 수가 0인 경우(예: totalCount < targetSize), 최소 1개로 설정
		if (calculatedPartitions == 0) {
			calculatedPartitions = 1;
		}

		int partitions = Math.min(calculatedPartitions, maxPartitionSize);

		if (calculatedPartitions > maxPartitionSize) {
			log.warn("[DynamicStockPartitioner] 파티션 개수 제한 초과: 계산된 파티션 수 = {}, 최대 = {}",
					calculatedPartitions, maxPartitionSize);
		}

		Long minId = stockRepository.findMinId();
		Long maxId = stockRepository.findMaxId();

		Map<String, ExecutionContext> result = new HashMap<>();

		// ID가 null인 경우 체크
		if (minId == null || maxId == null) {
			log.warn("[DynamicStockPartitioner] minId 또는 maxId가 null입니다. minId: {}, maxId: {}", minId, maxId);
			return result;
		}

		log.info("[DynamicStockPartitioner] 파티션 설정 - 총 데이터: {}, 목표 크기: {}, 파티션 수: {}",
				totalCount, targetSize, partitions);

		long range = (maxId - minId) / partitions;
		long start = minId;

		for (int i = 0; i < partitions; i++) {
			ExecutionContext context = new ExecutionContext();
			context.putLong("minId", start);

			long end;
			if (i == partitions - 1) {
				// 마지막 파티션은 maxId까지 모두 포함하도록 설정
				end = maxId;
			} else {
				end = start + range;
			}

			context.putLong("maxId", end);
			result.put("partition" + i, context);

			start = end + 1;
		}

		return result;
	}
}
