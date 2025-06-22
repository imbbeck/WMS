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
		StockSnapshotKey key = new StockSnapshotKey(stock.getWareId(), stock.getWarehouseId(), today.minusDays(1));
		Optional<StockDailySnapshot> ySnapshot = snapshotRepository
				.findByKey(key);
		int yQty = ySnapshot.map(StockDailySnapshot::getQuantity).orElse(0);

		return StockDailySnapshot.builder()
				.key(key)
				.quantity(stock.getQuantity())
				.changeFromYesterday(stock.getQuantity() - yQty)
				.build();
	}
}
