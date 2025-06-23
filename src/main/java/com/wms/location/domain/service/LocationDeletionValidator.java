package com.wms.location.domain.service;

import java.util.ArrayList;
import java.util.List;

import com.wms.location.domain.exception.LocationException;
import com.wms.stock.application.StockCacheService;
import com.wms.stock.domain.repository.StockRepository;
import com.wms.logisticTask.domain.repository.LogisticTaskRepository;
import com.wms.logisticTemplate.domain.repository.LogisticTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LocationDeletionValidator {

	private final StockRepository stockRepository;
	private final LogisticTaskRepository logisticTaskRepository;
	private final LogisticTemplateRepository logisticTemplateRepository;
	private final StockCacheService stockCacheService;

	public void validateDeletionPossible(Long locationId) {
		List<Long> relatedWareList = new ArrayList<>();

		stockRepository.findAllByWarehouseId(locationId).forEach(stock -> {
			relatedWareList.add(stock.getWareId());
		});

		if (!relatedWareList.isEmpty()) { // 재고수량 0인 경우는 더이상 없으므로(stock 상태 변화 후 재고수량이 0이 되면 삭제됨) relatedWareList 비어있지 않으면 삭제 불가
			throw LocationException.cannotDeleteWithStock(locationId, relatedWareList);
		}

		List<Long> relatedTaskList = new ArrayList<>();

		logisticTaskRepository.findAllByFromLocationIdOrToLocationId(locationId, locationId).forEach(task -> {
			relatedTaskList.add(task.getId());
		});

		if (!relatedTaskList.isEmpty()) { // 물류 작업이 존재하는 경우 삭제 불가
			throw LocationException.cannotDeleteLocationWithTasks(locationId, relatedTaskList);
		}

		List<Long> relatedTemplateList = new ArrayList<>();
		logisticTemplateRepository.findAllByFromLocationIdOrToLocationId(locationId, locationId).forEach(template -> {
			relatedTemplateList.add(template.getId());
		});

		if (!relatedTemplateList.isEmpty()) { // 물류 템플릿이 존재하는 경우 삭제 불가
			throw LocationException.cannotDeleteLocationWithTemplates(locationId, relatedTemplateList);
		}

	}
}