package com.wms.stock.batch;

import com.wms.stock.domain.model.Stock;
import com.wms.stock.domain.model.StockDailySnapshot;
import com.wms.stock.domain.model.StockSnapshotKey;
import com.wms.stock.domain.repository.StockDailySnapshotRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockSnapshotProcessorTest {

	@Mock
	private StockDailySnapshotRepository snapshotRepository;

	@InjectMocks
	private StockSnapshotProcessor processor;

	@Test
	@DisplayName("어제 데이터가 있는 경우 변화량 계산 테스트")
	void testProcessWithYesterdayData() {
		// Given
		Stock stock = Stock.builder()
				.wareId(1L)
				.warehouseId(100L)
				.quantity(150)
				.build();

		LocalDate yesterday = LocalDate.now().minusDays(1);
		StockSnapshotKey yesterdayKey = new StockSnapshotKey(1L, 100L, yesterday.minusDays(1));
		StockDailySnapshot yesterdaySnapshot = StockDailySnapshot.builder()
				.key(yesterdayKey)
				.quantity(100)
				.changeFromYesterday(0)
				.build();

		when(snapshotRepository.findByKey(any(StockSnapshotKey.class)))
				.thenReturn(Optional.of(yesterdaySnapshot));

		// When
		StockDailySnapshot result = processor.process(stock);

		// Then
		assertThat(result).isNotNull();
		assertThat(result.getKey().getWareId()).isEqualTo(1L);
		assertThat(result.getKey().getWarehouseId()).isEqualTo(100L);
		assertThat(result.getKey().getSnapshotDate()).isEqualTo(yesterday);
		assertThat(result.getQuantity()).isEqualTo(150);
		assertThat(result.getChangeFromYesterday()).isEqualTo(50); // 150 - 100
	}

	@Test
	@DisplayName("어제 데이터가 없는 경우 변화량 계산 테스트")
	void testProcessWithoutYesterdayData() {
		// Given
		Stock stock = Stock.builder()
				.wareId(2L)
				.warehouseId(200L)
				.quantity(80)
				.build();

		when(snapshotRepository.findByKey(any(StockSnapshotKey.class)))
				.thenReturn(Optional.empty());

		// When
		StockDailySnapshot result = processor.process(stock);

		// Then
		assertThat(result).isNotNull();
		assertThat(result.getKey().getWareId()).isEqualTo(2L);
		assertThat(result.getKey().getWarehouseId()).isEqualTo(200L);
		assertThat(result.getQuantity()).isEqualTo(80);
		assertThat(result.getChangeFromYesterday()).isEqualTo(80); // 80 - 0 (기본값)
	}

	@Test
	@DisplayName("수량이 감소한 경우 변화량 계산 테스트")
	void testProcessWithDecreasedQuantity() {
		// Given
		Stock stock = Stock.builder()
				.wareId(3L)
				.warehouseId(300L)
				.quantity(50)
				.build();

		LocalDate yesterday = LocalDate.now().minusDays(1);
		StockSnapshotKey yesterdayKey = new StockSnapshotKey(3L, 300L, yesterday.minusDays(1));
		StockDailySnapshot yesterdaySnapshot = StockDailySnapshot.builder()
				.key(yesterdayKey)
				.quantity(120)
				.changeFromYesterday(20)
				.build();

		when(snapshotRepository.findByKey(any(StockSnapshotKey.class)))
				.thenReturn(Optional.of(yesterdaySnapshot));

		// When
		StockDailySnapshot result = processor.process(stock);

		// Then
		assertThat(result).isNotNull();
		assertThat(result.getQuantity()).isEqualTo(50);
		assertThat(result.getChangeFromYesterday()).isEqualTo(-70); // 50 - 120 = -70
	}

	@Test
	@DisplayName("수량이 동일한 경우 변화량 계산 테스트")
	void testProcessWithSameQuantity() {
		// Given
		Stock stock = Stock.builder()
				.wareId(4L)
				.warehouseId(400L)
				.quantity(100)
				.build();

		LocalDate yesterday = LocalDate.now().minusDays(1);
		StockSnapshotKey yesterdayKey = new StockSnapshotKey(4L, 400L, yesterday.minusDays(1));
		StockDailySnapshot yesterdaySnapshot = StockDailySnapshot.builder()
				.key(yesterdayKey)
				.quantity(100)
				.changeFromYesterday(10)
				.build();

		when(snapshotRepository.findByKey(any(StockSnapshotKey.class)))
				.thenReturn(Optional.of(yesterdaySnapshot));

		// When
		StockDailySnapshot result = processor.process(stock);

		// Then
		assertThat(result).isNotNull();
		assertThat(result.getQuantity()).isEqualTo(100);
		assertThat(result.getChangeFromYesterday()).isEqualTo(0); // 100 - 100 = 0
	}

	@Test
	@DisplayName("수량이 0인 경우 변화량 계산 테스트")
	void testProcessWithZeroQuantity() {
		// Given
		Stock stock = Stock.builder()
				.wareId(5L)
				.warehouseId(500L)
				.quantity(0)
				.build();

		LocalDate yesterday = LocalDate.now().minusDays(1);
		StockSnapshotKey yesterdayKey = new StockSnapshotKey(5L, 500L, yesterday.minusDays(1));
		StockDailySnapshot yesterdaySnapshot = StockDailySnapshot.builder()
				.key(yesterdayKey)
				.quantity(30)
				.changeFromYesterday(-10)
				.build();

		when(snapshotRepository.findByKey(any(StockSnapshotKey.class)))
				.thenReturn(Optional.of(yesterdaySnapshot));

		// When
		StockDailySnapshot result = processor.process(stock);

		// Then
		assertThat(result).isNotNull();
		assertThat(result.getQuantity()).isEqualTo(0);
		assertThat(result.getChangeFromYesterday()).isEqualTo(-30); // 0 - 30 = -30
	}

	@Test
	@DisplayName("날짜 키 생성 테스트")
	void testSnapshotKeyGeneration() {
		// Given
		Stock stock = Stock.builder()
				.wareId(6L)
				.warehouseId(600L)
				.quantity(200)
				.build();

		when(snapshotRepository.findByKey(any(StockSnapshotKey.class)))
				.thenReturn(Optional.empty());

		// When
		StockDailySnapshot result = processor.process(stock);

		// Then
		LocalDate expectedDate = LocalDate.now().minusDays(1);
		assertThat(result.getKey().getSnapshotDate()).isEqualTo(expectedDate);
		assertThat(result.getKey().getWareId()).isEqualTo(6L);
		assertThat(result.getKey().getWarehouseId()).isEqualTo(600L);
	}
}