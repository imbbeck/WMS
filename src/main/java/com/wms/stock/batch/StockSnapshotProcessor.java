package com.wms.stock.batch;

import com.wms.stock.domain.model.Stock;
import com.wms.stock.domain.model.StockDailySnapshot;
import com.wms.stock.domain.model.StockSnapshotKey;
import com.wms.stock.domain.repository.StockDailySnapshotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class StockSnapshotProcessor implements ItemProcessor<Stock, StockDailySnapshot> {

	private final StockDailySnapshotRepository snapshotRepository;

	@Override
	public StockDailySnapshot process(Stock stock) {
		LocalDate today = LocalDate.now();
		LocalDate yesterday = today.minusDays(1);

		// 어제 스냅샷을 찾기 위해 그저께 날짜로 조회
		StockSnapshotKey yesterdayKey = StockSnapshotKey.of(stock.getKey(), today.minusDays(2)
		);

		Optional<StockDailySnapshot> ySnapshot = snapshotRepository.findByKey(yesterdayKey);
		int yQty = ySnapshot.map(StockDailySnapshot::getQuantity).orElse(0);

		return StockDailySnapshot.builder()
				.key(StockSnapshotKey.of(stock.getKey(), yesterday))
				.quantity(stock.getQuantity())
				.changeFromYesterday(stock.getQuantity() - yQty)
				.build();
	}
}
