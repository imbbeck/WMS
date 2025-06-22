package com.wms.stock.batch;

import com.wms.stock.domain.model.StockDailySnapshot;
import com.wms.stock.domain.repository.StockDailySnapshotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StockSnapshotWriter implements ItemWriter<StockDailySnapshot> {

	private final StockDailySnapshotRepository snapshotRepository;

	@Override
	public void write(Chunk<? extends StockDailySnapshot> chunk) throws Exception {
		snapshotRepository.saveAll(chunk.getItems());
	}
}