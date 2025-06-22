package com.wms.stock.batch;

import com.wms.stock.domain.model.Stock;
import com.wms.stock.domain.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.Iterator;

@StepScope
@Component("stockSnapshotReader")
@RequiredArgsConstructor
public class StockSnapshotReader implements ItemReader<Stock> {

	private final StockRepository stockRepository;

	@Value("#{stepExecutionContext['minId']}")
	private Long minId;

	@Value("#{stepExecutionContext['maxId']}")
	private Long maxId;

	private Iterator<Stock> stockIterator;
	private boolean initialized = false;

	@Override
	public Stock read() throws Exception {
		if (!initialized) {
			initialize();
			initialized = true;
		}

		if (stockIterator != null && stockIterator.hasNext()) {
			return stockIterator.next();
		}

		return null;
	}

	private void initialize() {
		if (minId != null && maxId != null) {
			Pageable pageable = PageRequest.of(0, Integer.MAX_VALUE, Sort.by("id").ascending());
			Page<Stock> stockPage = stockRepository.findByIdBetween(minId, maxId, pageable);
			stockIterator = stockPage.getContent().iterator();
		}
	}
}