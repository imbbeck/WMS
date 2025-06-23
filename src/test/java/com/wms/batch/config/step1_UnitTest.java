//package com.wms.batch;
//
//import com.wms.batch.partitioner.DynamicStockPartitioner;
//import com.wms.stock.batch.StockSnapshotProcessor;
//import com.wms.stock.domain.model.Stock;
//import com.wms.stock.domain.model.StockDailySnapshot;
//import com.wms.stock.domain.model.StockSnapshotKey;
//import com.wms.stock.domain.repository.StockDailySnapshotRepository;
//import com.wms.stock.domain.repository.StockRepository;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.batch.item.ExecutionContext;
//
//import java.time.LocalDate;
//import java.util.Map;
//import java.util.Optional;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.Mockito.when;
//
///**
// * 복잡한 Spring Context 없이 실행할 수 있는 순수 단위 테스트들
// */
//@ExtendWith(MockitoExtension.class)
//class UnitTest {
//
//	// ========== Partitioner Tests ==========
//
//	@Mock
//	private StockRepository stockRepository;
//
//	@InjectMocks
//	private DynamicStockPartitioner partitioner;
//
//	@Test
//	@DisplayName("[Partitioner] 기본 파티션 생성 테스트")
//	void testBasicPartitioning() {
//		// Given
//		when(stockRepository.count()).thenReturn(10L);
//		when(stockRepository.findMinId()).thenReturn(1L);
//		when(stockRepository.findMaxId()).thenReturn(100L);
//
//		// When
//		Map<String, ExecutionContext> partitions = partitioner.partition(10);
//
//		// Then
//		assertThat(partitions).hasSize(3); // (10/5) + 1 = 3개 파티션
//
//		ExecutionContext partition0 = partitions.get("partition0");
//		assertThat(partition0.getLong("minId")).isEqualTo(1L);
//		assertThat(partition0.getLong("maxId")).isEqualTo(17L);
//	}
//
//	@Test
//	@DisplayName("[Partitioner] 데이터가 없는 경우 테스트")
//	void testNoDataPartitioning() {
//		// Given
//		when(stockRepository.count()).thenReturn(0L);
//		when(stockRepository.findMinId()).thenReturn(null);
//		when(stockRepository.findMaxId()).thenReturn(null);
//
//		// When
//		Map<String, ExecutionContext> partitions = partitioner.partition(10);
//
//		// Then
//		assertThat(partitions).isEmpty();
//	}
//
//	// ========== Processor Tests ==========
//
//	@Mock
//	private StockDailySnapshotRepository snapshotRepository;
//
//	@InjectMocks
//	private StockSnapshotProcessor processor;
//
//	@Test
//	@DisplayName("[Processor] 어제 데이터가 있는 경우 변화량 계산")
//	void testProcessWithYesterdayData() {
//		// Given
//		Stock stock = Stock.builder()
//				.wareId(1L)
//				.warehouseId(100L)
//				.quantity(150)
//				.build();
//
//		StockDailySnapshot yesterdaySnapshot = StockDailySnapshot.builder()
//				.key(new StockSnapshotKey(1L, 100L, LocalDate.now().minusDays(2)))
//				.quantity(100)
//				.changeFromYesterday(0)
//				.build();
//
//		when(snapshotRepository.findByKey(any(StockSnapshotKey.class)))
//				.thenReturn(Optional.of(yesterdaySnapshot));
//
//		// When
//		StockDailySnapshot result = processor.process(stock);
//
//		// Then
//		assertThat(result).isNotNull();
//		assertThat(result.getQuantity()).isEqualTo(150);
//		assertThat(result.getChangeFromYesterday()).isEqualTo(50); // 150 - 100
//		assertThat(result.getKey().getSnapshotDate()).isEqualTo(LocalDate.now().minusDays(1));
//	}
//
//	@Test
//	@DisplayName("[Processor] 어제 데이터가 없는 경우")
//	void testProcessWithoutYesterdayData() {
//		// Given
//		Stock stock = Stock.builder()
//				.wareId(2L)
//				.warehouseId(200L)
//				.quantity(80)
//				.build();
//
//		when(snapshotRepository.findByKey(any(StockSnapshotKey.class)))
//				.thenReturn(Optional.empty());
//
//		// When
//		StockDailySnapshot result = processor.process(stock);
//
//		// Then
//		assertThat(result).isNotNull();
//		assertThat(result.getQuantity()).isEqualTo(80);
//		assertThat(result.getChangeFromYesterday()).isEqualTo(80); // 80 - 0 (기본값)
//	}
//
//	@Test
//	@DisplayName("[Processor] 수량이 감소한 경우")
//	void testProcessWithDecreasedQuantity() {
//		// Given
//		Stock stock = Stock.builder()
//				.wareId(3L)
//				.warehouseId(300L)
//				.quantity(50)
//				.build();
//
//		StockDailySnapshot yesterdaySnapshot = StockDailySnapshot.builder()
//				.key(new StockSnapshotKey(3L, 300L, LocalDate.now().minusDays(2)))
//				.quantity(120)
//				.changeFromYesterday(20)
//				.build();
//
//		when(snapshotRepository.findByKey(any(StockSnapshotKey.class)))
//				.thenReturn(Optional.of(yesterdaySnapshot));
//
//		// When
//		StockDailySnapshot result = processor.process(stock);
//
//		// Then
//		assertThat(result.getQuantity()).isEqualTo(50);
//		assertThat(result.getChangeFromYesterday()).isEqualTo(-70); // 50 - 120
//	}
//
//	// ========== Domain Model Tests ==========
//
//	@Test
//	@DisplayName("[Domain] Stock 엔티티 생성 및 수정 테스트")
//	void testStockEntity() {
//		// Given & When
//		Stock stock = Stock.builder()
//				.wareId(1L)
//				.warehouseId(100L)
//				.quantity(50)
//				.build();
//
//		// Then
//		assertThat(stock.getWareId()).isEqualTo(1L);
//		assertThat(stock.getWarehouseId()).isEqualTo(100L);
//		assertThat(stock.getQuantity()).isEqualTo(50);
//
//		// When - 수량 업데이트
//		stock.updateQuantity(80);
//
//		// Then
//		assertThat(stock.getQuantity()).isEqualTo(80);
//	}
//
//	@Test
//	@DisplayName("[Domain] StockDailySnapshot 엔티티 생성 테스트")
//	void testStockDailySnapshotEntity() {
//		// Given
//		LocalDate testDate = LocalDate.of(2024, 1, 15);
//		StockSnapshotKey key = new StockSnapshotKey(1L, 100L, testDate);
//
//		// When
//		StockDailySnapshot snapshot = StockDailySnapshot.builder()
//				.key(key)
//				.quantity(150)
//				.changeFromYesterday(20)
//				.build();
//
//		// Then
//		assertThat(snapshot.getKey()).isEqualTo(key);
//		assertThat(snapshot.getKey().getWareId()).isEqualTo(1L);
//		assertThat(snapshot.getKey().getWarehouseId()).isEqualTo(100L);
//		assertThat(snapshot.getKey().getSnapshotDate()).isEqualTo(testDate);
//		assertThat(snapshot.getQuantity()).isEqualTo(150);
//		assertThat(snapshot.getChangeFromYesterday()).isEqualTo(20);
//	}
//
//	@Test
//	@DisplayName("[Domain] StockSnapshotKey equals/hashCode 테스트")
//	void testStockSnapshotKeyEquality() {
//		// Given
//		LocalDate testDate = LocalDate.of(2024, 1, 15);
//		StockSnapshotKey key1 = new StockSnapshotKey(1L, 100L, testDate);
//		StockSnapshotKey key2 = new StockSnapshotKey(1L, 100L, testDate);
//		StockSnapshotKey key3 = new StockSnapshotKey(2L, 100L, testDate);
//
//		// Then
//		assertThat(key1).isEqualTo(key2);
//		assertThat(key1).isNotEqualTo(key3);
//		assertThat(key1.hashCode()).isEqualTo(key2.hashCode());
//	}
//}