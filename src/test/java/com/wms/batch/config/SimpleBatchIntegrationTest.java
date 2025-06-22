package com.wms.batch.config;

import com.wms.batch.partitioner.DynamicStockPartitioner;
import com.wms.applicationInfra.config.QuerydslConfig;
import com.wms.stock.batch.StockSnapshotProcessor;
import com.wms.stock.domain.model.Stock;
import com.wms.stock.domain.model.StockDailySnapshot;
import com.wms.stock.domain.model.StockSnapshotKey;
import com.wms.stock.domain.repository.StockDailySnapshotRepository;
import com.wms.stock.domain.repository.StockRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JPA와 기본 비즈니스 로직을 테스트하는 통합 테스트
 * Spring Batch 없이 Repository와 도메인 로직만 검증
 */
@DataJpaTest
@Import(QuerydslConfig.class)
@TestPropertySource(properties = {
		"spring.cache.type=none",
		"spring.data.redis.repositories.enabled=false",
		"logging.level.org.hibernate.SQL=DEBUG"
})
class SimpleBatchIntegrationTest {

	@Autowired
	private StockRepository stockRepository;

	@Autowired
	private StockDailySnapshotRepository snapshotRepository;

	@BeforeEach
	void setUp() {
		stockRepository.deleteAll();
		snapshotRepository.deleteAll();
	}

	@Test
	@DisplayName("재고 데이터 기본 CRUD 테스트")
	void testStockBasicOperations() {
		// Given
		Stock stock = Stock.builder()
				.wareId(1L)
				.warehouseId(100L)
				.quantity(50)
				.build();

		// When
		Stock saved = stockRepository.save(stock);

		// Then
		assertThat(saved.getId()).isNotNull();
		assertThat(saved.getWareId()).isEqualTo(1L);
		assertThat(saved.getWarehouseId()).isEqualTo(100L);
		assertThat(saved.getQuantity()).isEqualTo(50);

		// 조회 테스트
		Stock found = stockRepository.findById(saved.getId()).orElse(null);
		assertThat(found).isNotNull();
		assertThat(found.getWareId()).isEqualTo(1L);
	}

	@Test
	@DisplayName("재고 Repository 집계 메서드 테스트")
	void testStockRepositoryAggregations() {
		// Given: 여러 재고 데이터 생성
		for (int i = 1; i <= 5; i++) {
			Stock stock = Stock.builder()
					.wareId((long) i)
					.warehouseId((long) (i % 2 + 1)) // 1 또는 2
					.quantity(i * 10)
					.build();
			stockRepository.save(stock);
		}

		// When
		long totalCount = stockRepository.count();
		Long minId = stockRepository.findMinId();
		Long maxId = stockRepository.findMaxId();
		List<Stock> allStocks = stockRepository.findAll();

		// Then
		assertThat(totalCount).isEqualTo(5);
		assertThat(minId).isNotNull();
		assertThat(maxId).isNotNull();
		assertThat(maxId).isGreaterThanOrEqualTo(minId);
		assertThat(allStocks).hasSize(5);
	}

	@Test
	@DisplayName("스냅샷 데이터 기본 CRUD 테스트")
	void testSnapshotBasicOperations() {
		// Given
		LocalDate testDate = LocalDate.of(2024, 6, 21);
		StockSnapshotKey key = new StockSnapshotKey(1L, 100L, testDate);

		StockDailySnapshot snapshot = StockDailySnapshot.builder()
				.key(key)
				.quantity(150)
				.changeFromYesterday(20)
				.build();

		// When
		StockDailySnapshot saved = snapshotRepository.save(snapshot);

		// Then
		assertThat(saved.getId()).isNotNull();
		assertThat(saved.getKey().getWareId()).isEqualTo(1L);
		assertThat(saved.getKey().getWarehouseId()).isEqualTo(100L);
		assertThat(saved.getKey().getSnapshotDate()).isEqualTo(testDate);
		assertThat(saved.getQuantity()).isEqualTo(150);
		assertThat(saved.getChangeFromYesterday()).isEqualTo(20);
	}

	@Test
	@DisplayName("스냅샷 키를 이용한 조회 테스트")
	void testSnapshotFindByKey() {
		// Given
		LocalDate testDate = LocalDate.of(2024, 6, 21);
		StockSnapshotKey key = new StockSnapshotKey(1L, 100L, testDate);

		StockDailySnapshot snapshot = StockDailySnapshot.builder()
				.key(key)
				.quantity(100)
				.changeFromYesterday(10)
				.build();
		snapshotRepository.save(snapshot);

		// When
		var found = snapshotRepository.findByKey(key);

		// Then
		assertThat(found).isPresent();
		assertThat(found.get().getQuantity()).isEqualTo(100);
		assertThat(found.get().getChangeFromYesterday()).isEqualTo(10);
	}

	@Test
	@DisplayName("스냅샷 프로세서 단독 테스트 - 어제 데이터 없음")
	void testProcessorWithoutYesterdayData() {
		// Given
		Stock stock = Stock.builder()
				.wareId(1L)
				.warehouseId(100L)
				.quantity(150)
				.build();
		stockRepository.save(stock);

		StockSnapshotProcessor processor = new StockSnapshotProcessor(snapshotRepository);

		// When
		StockDailySnapshot result = processor.process(stock);

		// Then
		assertThat(result).isNotNull();
		assertThat(result.getKey().getWareId()).isEqualTo(1L);
		assertThat(result.getKey().getWarehouseId()).isEqualTo(100L);
		assertThat(result.getKey().getSnapshotDate()).isEqualTo(LocalDate.now().minusDays(1));
		assertThat(result.getQuantity()).isEqualTo(150);
		assertThat(result.getChangeFromYesterday()).isEqualTo(150); // 어제 데이터가 없으므로 150 - 0
	}

	@Test
	@DisplayName("스냅샷 프로세서 단독 테스트 - 어제 데이터 있음")
	void testProcessorWithYesterdayData() {
		// Given: 어제 스냅샷 데이터 생성
		LocalDate yesterday = LocalDate.now().minusDays(1);
		LocalDate dayBeforeYesterday = yesterday.minusDays(1);

		StockSnapshotKey yesterdayKey = new StockSnapshotKey(1L, 100L, dayBeforeYesterday);
		StockDailySnapshot yesterdaySnapshot = StockDailySnapshot.builder()
				.key(yesterdayKey)
				.quantity(100)
				.changeFromYesterday(0)
				.build();
		snapshotRepository.save(yesterdaySnapshot);

		// 오늘 재고 데이터
		Stock stock = Stock.builder()
				.wareId(1L)
				.warehouseId(100L)
				.quantity(130)
				.build();
		stockRepository.save(stock);

		StockSnapshotProcessor processor = new StockSnapshotProcessor(snapshotRepository);

		// When
		StockDailySnapshot result = processor.process(stock);

		// Then
		assertThat(result).isNotNull();
		assertThat(result.getQuantity()).isEqualTo(130);
		assertThat(result.getChangeFromYesterday()).isEqualTo(30); // 130 - 100
		assertThat(result.getKey().getSnapshotDate()).isEqualTo(yesterday);
	}

	@Test
	@DisplayName("파티셔너 동작 테스트")
	void testDynamicStockPartitioner() {
		// Given: 테스트 데이터 생성
		createTestStockData(20);
		DynamicStockPartitioner partitioner = new DynamicStockPartitioner(stockRepository);

		// When
		var partitions = partitioner.partition(10);

		// Then
		assertThat(partitions).isNotEmpty();

		// 파티션 개수 검증 (20/5 + 1 = 11개 파티션)
		assertThat(partitions.size()).isEqualTo(5);

		// 첫 번째와 마지막 파티션 검증
		if (!partitions.isEmpty()) {
			var firstPartition = partitions.get("partition0");
			var lastPartition = partitions.get("partition10");

			assertThat(firstPartition.getLong("minId")).isNotNull();
			assertThat(firstPartition.getLong("maxId")).isNotNull();
			assertThat(lastPartition.getLong("minId")).isNotNull();
			assertThat(lastPartition.getLong("maxId")).isNotNull();

			// minId <= maxId 검증
			assertThat(firstPartition.getLong("maxId"))
					.isGreaterThanOrEqualTo(firstPartition.getLong("minId"));
		}
	}

	@Test
	@DisplayName("ID 범위 조회 테스트")
	void testFindByIdBetween() {
		// Given: ID가 연속되지 않은 데이터 생성
		Stock stock1 = stockRepository.save(Stock.builder().wareId(1L).warehouseId(1L).quantity(10).build());
		Stock stock2 = stockRepository.save(Stock.builder().wareId(2L).warehouseId(1L).quantity(20).build());
		Stock stock3 = stockRepository.save(Stock.builder().wareId(3L).warehouseId(1L).quantity(30).build());

		Long minId = stock1.getId();
		Long maxId = stock3.getId();

		// When
		var result = stockRepository.findByIdBetween(minId, maxId,
				org.springframework.data.domain.PageRequest.of(0, 10));

		// Then
		assertThat(result.getContent()).hasSize(3);
		assertThat(result.getContent()).extracting("quantity").containsExactly(10, 20, 30);
	}

	@Test
	@DisplayName("복합 시나리오 테스트 - 전체 플로우")
	void testCompleteScenario() {
		// Given: 재고 데이터 생성
		Stock stock = Stock.builder()
				.wareId(1L)
				.warehouseId(100L)
				.quantity(200)
				.build();
		stockRepository.save(stock);

		StockSnapshotProcessor processor = new StockSnapshotProcessor(snapshotRepository);

		// When: 첫 번째 스냅샷 생성 (어제 데이터 없음)
		StockDailySnapshot firstSnapshot = processor.process(stock);
		snapshotRepository.save(firstSnapshot);

		// 재고 수량 변경
		stock.updateQuantity(250);
		stockRepository.save(stock);

		// 두 번째 스냅샷 생성 (어제 데이터 있음)
		// 하지만 실제로는 같은 날짜로 처리되므로,
		// 어제 데이터를 찾으려면 firstSnapshot의 날짜를 어제로 설정해야 함

		// 실제 시나리오를 시뮬레이션하기 위해 첫 번째 스냅샷을 그저께 날짜로 수정
		LocalDate today = LocalDate.now();
		LocalDate dayBeforeYesterday = today.minusDays(2);

		StockDailySnapshot adjustedFirstSnapshot = StockDailySnapshot.builder()
				.key(new StockSnapshotKey(1L, 100L, dayBeforeYesterday))
				.quantity(200)
				.changeFromYesterday(200)
				.build();
		snapshotRepository.save(adjustedFirstSnapshot);

		// 이제 두 번째 스냅샷 생성 (어제 데이터 있음)
		StockDailySnapshot secondSnapshot = processor.process(stock);

		// Then
		assertThat(adjustedFirstSnapshot.getQuantity()).isEqualTo(200);
		assertThat(adjustedFirstSnapshot.getChangeFromYesterday()).isEqualTo(200);

		assertThat(secondSnapshot.getQuantity()).isEqualTo(250);
		assertThat(secondSnapshot.getChangeFromYesterday()).isEqualTo(50); // 250 - 200
		assertThat(secondSnapshot.getKey().getSnapshotDate()).isEqualTo(today.minusDays(1));
	}

	private void createTestStockData(int count) {
		for (int i = 1; i <= count; i++) {
			Stock stock = Stock.builder()
					.wareId((long) i)
					.warehouseId((long) (i % 3 + 1)) // 1, 2, 3 순환
					.quantity(i * 10)
					.build();
			stockRepository.save(stock);
		}
	}
}