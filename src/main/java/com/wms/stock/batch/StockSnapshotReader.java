package com.wms.stock.batch;

import com.wms.stock.domain.model.Stock;
import com.wms.stock.domain.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.data.RepositoryItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@StepScope
@Component("stockSnapshotReader")
@RequiredArgsConstructor
public class StockSnapshotReader extends RepositoryItemReader<Stock> {

	public StockSnapshotReader(
			StockRepository stockRepository,
			@Value("#{stepExecutionContext['minId']}") Long minId,
			@Value("#{stepExecutionContext['maxId']}") Long maxId) {

		this.setRepository(stockRepository);
		this.setMethodName("findByIdBetween");

		// 파라미터를 List로 설정 (순서대로)
		this.setArguments(List.of(minId, maxId));

		// 정렬 설정
		Map<String, Sort.Direction> sorts = new HashMap<>();
		sorts.put("id", Sort.Direction.ASC);
		this.setSort(sorts);

		this.setPageSize(1000);
	}
}