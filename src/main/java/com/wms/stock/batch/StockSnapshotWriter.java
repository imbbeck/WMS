package com.wms.stock.batch;

import com.wms.stock.domain.model.StockDailySnapshot;
import com.wms.stock.domain.repository.StockDailySnapshotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.data.RepositoryItemWriter;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StockSnapshotWriter extends RepositoryItemWriter<StockDailySnapshot> {

	public StockSnapshotWriter(StockDailySnapshotRepository snapshotRepository) {
		this.setRepository(snapshotRepository);
		this.setMethodName("save");
	}
}
