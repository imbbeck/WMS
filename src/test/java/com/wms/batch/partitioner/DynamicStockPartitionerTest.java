package com.wms.batch.partitioner;

import com.wms.stock.domain.repository.StockRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DynamicStockPartitionerTest {

	@Mock
	private StockRepository stockRepository;

	private DynamicStockPartitioner partitioner;

	// application.yml 설정값과 일치
	private final int targetSize = 20;
	private final int maxPartitionSize = 5;

	// 테스트용 기본값들 (동적 계산)
	private final long defaultMinId = 1L;
	private final long defaultMaxId = 100L;
	private final long nonConsecutiveMinId = 1000L;
	private final long nonConsecutiveMaxId = 2000L;

	@BeforeEach
	void setUp() {
		partitioner = new DynamicStockPartitioner(stockRepository);
		ReflectionTestUtils.setField(partitioner, "targetSize", targetSize);
		ReflectionTestUtils.setField(partitioner, "maxPartitionSize", maxPartitionSize);

		// 기본 mock 설정 (대부분의 테스트에서 사용) - lenient로 설정하여 사용되지 않아도 오류 없음
		lenient().when(stockRepository.findMinId()).thenReturn(defaultMinId);
		lenient().when(stockRepository.findMaxId()).thenReturn(defaultMaxId);

		System.out.println("테스트 설정 - TARGET_SIZE: " + targetSize + ", MAX_PARTITION_SIZE: " + maxPartitionSize);
	}

	@Test
	@DisplayName("기본 파티션 생성 테스트")
	void testBasicPartitioning() {
		// Given
		long dataCount = targetSize * 2L; // 20 * 2 = 40건
		when(stockRepository.count()).thenReturn(dataCount);

		// When
		Map<String, ExecutionContext> partitions = partitioner.partition(10);

		// Then
		int expectedPartitions = (int)((dataCount / targetSize) + 1); // (40/20) + 1 = 3개 파티션
		assertThat(partitions).hasSize(expectedPartitions);

		ExecutionContext partition0 = partitions.get("partition0");
		assertThat(partition0.getLong("minId")).isEqualTo(defaultMinId);

		// 마지막 파티션 확인
		String lastPartitionKey = "partition" + (expectedPartitions - 1);
		ExecutionContext lastPartition = partitions.get(lastPartitionKey);
		assertThat(lastPartition.getLong("maxId")).isEqualTo(defaultMaxId);

		System.out.println("데이터: " + dataCount + "건 → 예상 파티션: " + expectedPartitions + "개, 실제: " + partitions.size() + "개");
	}

	@Test
	@DisplayName("최대 파티션 수 제한 테스트")
	void testMaxPartitionLimit() {
		// Given: MAX_PARTITION_SIZE를 초과하는 데이터
		long dataCount = targetSize * (maxPartitionSize + 2L); // 20 * (5 + 2) = 140건
		when(stockRepository.count()).thenReturn(dataCount);

		// When
		Map<String, ExecutionContext> partitions = partitioner.partition(10);

		// Then: 최대 파티션 수로 제한
		assertThat(partitions).hasSize(maxPartitionSize);

		// 각 파티션의 범위 확인
		ExecutionContext firstPartition = partitions.get("partition0");
		String lastPartitionKey = "partition" + (maxPartitionSize - 1);
		ExecutionContext lastPartition = partitions.get(lastPartitionKey);

		assertThat(firstPartition.getLong("minId")).isEqualTo(defaultMinId);
		assertThat(lastPartition.getLong("maxId")).isEqualTo(defaultMaxId);

		System.out.println("대용량 데이터: " + dataCount + "건 → 최대 파티션 제한: " + maxPartitionSize + "개");
	}

	@Test
	@DisplayName("데이터가 적은 경우 파티션 생성 테스트")
	void testSmallDataPartitioning() {
		// Given
		long dataCount = Math.max(1, targetSize / 2); // 20/2 = 10건
		when(stockRepository.count()).thenReturn(dataCount);

		// When
		Map<String, ExecutionContext> partitions = partitioner.partition(10);

		// Then
		int expectedPartitions = (int)((dataCount / targetSize) + 1); // (10/20) + 1 = 1개
		assertThat(partitions).hasSize(expectedPartitions);

		ExecutionContext partition0 = partitions.get("partition0");
		assertThat(partition0.getLong("minId")).isEqualTo(defaultMinId);

		if (expectedPartitions > 1) {
			String lastPartitionKey = "partition" + (expectedPartitions - 1);
			ExecutionContext lastPartition = partitions.get(lastPartitionKey);
			assertThat(lastPartition.getLong("maxId")).isEqualTo(defaultMaxId);
		}

		System.out.println("소량 데이터: " + dataCount + "건 → 파티션: " + expectedPartitions + "개");
	}

	@Test
	@DisplayName("데이터가 없는 경우 테스트")
	void testNoDataPartitioning() {
		// Given
		when(stockRepository.count()).thenReturn(0L);
		// findMinId, findMaxId는 호출되지 않으므로 mock 설정 불필요

		// When
		Map<String, ExecutionContext> partitions = partitioner.partition(10);

		// Then: 빈 파티션 맵 반환
		assertThat(partitions).isEmpty();

		System.out.println("데이터 없음 → 파티션: 0개");
	}

	@Test
	@DisplayName("minId가 null인 경우 테스트")
	void testNullMinIdPartitioning() {
		// Given
		when(stockRepository.count()).thenReturn(10L);
		when(stockRepository.findMinId()).thenReturn(null);  // 재정의
		when(stockRepository.findMaxId()).thenReturn(defaultMaxId);

		// When
		Map<String, ExecutionContext> partitions = partitioner.partition(10);

		// Then
		assertThat(partitions).isEmpty();

		System.out.println("minId가 null → 파티션: 0개");
	}

	@Test
	@DisplayName("연속된 ID가 아닌 경우 파티션 범위 테스트")
	void testNonConsecutiveIdPartitioning() {
		// Given: ID가 1000~2000 범위
		long dataCount = targetSize * 4L; // 20 * 4 = 80건
		when(stockRepository.count()).thenReturn(dataCount);
		when(stockRepository.findMinId()).thenReturn(nonConsecutiveMinId);  // 재정의
		when(stockRepository.findMaxId()).thenReturn(nonConsecutiveMaxId);  // 재정의

		// When
		Map<String, ExecutionContext> partitions = partitioner.partition(10);

		// Then
		int expectedPartitions = (int)((dataCount / targetSize) + 1); // (80/20) + 1 = 5개
		assertThat(partitions).hasSize(expectedPartitions);

		ExecutionContext partition0 = partitions.get("partition0");
		String lastPartitionKey = "partition" + (expectedPartitions - 1);
		ExecutionContext lastPartition = partitions.get(lastPartitionKey);

		assertThat(partition0.getLong("minId")).isEqualTo(nonConsecutiveMinId);
		assertThat(lastPartition.getLong("maxId")).isEqualTo(nonConsecutiveMaxId);

		// 범위 계산 확인
		long totalRange = nonConsecutiveMaxId - nonConsecutiveMinId;
		long expectedRange = totalRange / expectedPartitions + 1;
		assertThat(partition0.getLong("maxId")).isEqualTo(nonConsecutiveMinId + expectedRange - 1);

		System.out.println("비연속 ID 테스트 - 데이터: " + dataCount + "건, ID 범위: " + nonConsecutiveMinId + "~" + nonConsecutiveMaxId + ", 파티션: " + expectedPartitions + "개");
	}

	@Test
	@DisplayName("정확한 TARGET_SIZE 배수 데이터 테스트")
	void testExactMultipleData() {
		// Given: target-size의 정확한 배수
		long dataCount = targetSize * 3L; // 20 * 3 = 60건
		when(stockRepository.count()).thenReturn(dataCount);

		// When
		Map<String, ExecutionContext> partitions = partitioner.partition(10);

		// Then
		int expectedPartitions = (int)((dataCount / targetSize) + 1); // (60/20) + 1 = 4개
		assertThat(partitions).hasSize(expectedPartitions);

		System.out.println("정확한 배수 테스트 - 데이터: " + dataCount + "건 (TARGET_SIZE * 3), 파티션: " + expectedPartitions + "개");
	}

	@Test
	@DisplayName("데이터가 1건인 경우 테스트")
	void testSingleDataPartitioning() {
		// Given
		when(stockRepository.count()).thenReturn(1L);

		// When
		Map<String, ExecutionContext> partitions = partitioner.partition(10);

		// Then
		int expectedPartitions = (int)((1L / targetSize) + 1); // (1/20) + 1 = 1개
		assertThat(partitions).hasSize(expectedPartitions);

		ExecutionContext partition0 = partitions.get("partition0");
		assertThat(partition0.getLong("minId")).isEqualTo(defaultMinId);
		assertThat(partition0.getLong("maxId")).isEqualTo(defaultMaxId);

		System.out.println("단일 데이터: 1건 → 파티션: " + expectedPartitions + "개");
	}

	@Test
	@DisplayName("TARGET_SIZE와 동일한 데이터 테스트")
	void testTargetSizeExactData() {
		// Given
		long dataCount = targetSize; // 20건
		when(stockRepository.count()).thenReturn(dataCount);

		// When
		Map<String, ExecutionContext> partitions = partitioner.partition(10);

		// Then
		int expectedPartitions = (int)((dataCount / targetSize) + 1); // (20/20) + 1 = 2개
		assertThat(partitions).hasSize(expectedPartitions);

		System.out.println("TARGET_SIZE 정확한 데이터: " + dataCount + "건 → 파티션: " + expectedPartitions + "개");
	}

	@Test
	@DisplayName("최대 파티션 수와 동일한 데이터 테스트")
	void testMaxPartitionSizeExactData() {
		// Given: 정확히 maxPartitionSize만큼 파티션이 생성되는 데이터
		long dataCount = targetSize * maxPartitionSize; // 20 * 5 = 100건
		when(stockRepository.count()).thenReturn(dataCount);

		// When
		Map<String, ExecutionContext> partitions = partitioner.partition(10);

		// Then
		int expectedPartitions = Math.min((int)((dataCount / targetSize) + 1), maxPartitionSize); // min(6, 5) = 5개
		assertThat(partitions).hasSize(expectedPartitions);

		System.out.println("최대 파티션 경계값 테스트 - 데이터: " + dataCount + "건 → 파티션: " + expectedPartitions + "개");
	}
}