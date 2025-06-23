package com.wms.stock.application;

import com.wms.applicationInfra.domain.FieldEnum;
import com.wms.applicationInfra.idnameMapCashing.concrete.LocationCacheManager;
import com.wms.applicationInfra.idnameMapCashing.concrete.WareCacheManager;
import com.wms.location.domain.exception.LocationException;
import com.wms.location.domain.model.Location;
import com.wms.location.domain.model.LocationType;
import com.wms.location.domain.repository.LocationRepository;
import com.wms.stock.domain.event.StockCreatedEvent;
import com.wms.stock.domain.exception.StockException;
import com.wms.stock.domain.model.Stock;
import com.wms.stock.domain.repository.StockRepository;
import com.wms.stock.dto.StockDTO;
import com.wms.ware.domain.exception.WareException;
import com.wms.ware.domain.repository.WareRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class StockCtrlService {

	private static final String CACHE_KEY_PREFIX = "currentStock:";
	private static final String WAREHOUSE_STATUS_PREFIX = "warehouseStatus:";
	private static final long[] RETRY_DELAYS = {100, 300, 700, 1500, 3000};
	private static final int MAX_RETRIES = 5;
	private static final long CACHE_TTL = 30; // 30분

	private final RedisTemplate<String, Object> redisTemplate;

	private final StockRepository stockRepository;

	private final LocationRepository locationRepository;
	private final WareRepository wareRepository;

	private final ApplicationEventPublisher eventPublisher;

	@Transactional
	public Stock create(StockDTO.CreateReq req) {
		// 물품 존재 확인
		if (!wareRepository.existsById(req.getWareId())) {
			throw WareException.notFound(req.getWareId());
		}

		// 창고 존재 확인 및 타입 검증
		Location warehouse = locationRepository.findById(req.getWarehouseId())
				.orElseThrow(() -> LocationException.notFound(req.getWarehouseId()));

		if (!warehouse.getType().equals(LocationType.WAREHOUSE)) {
			throw LocationException.notWarehouseEx(req.getWarehouseId());
		}

		// 중복 재고 확인
		Optional<Stock> existingStock = stockRepository.findByWarehouseIdAndWareId(req.getWareId(), req.getWarehouseId());
		if (existingStock.isPresent()) {
			throw StockException.duplicate(FieldEnum.WARE_WAREHOUSE_PAIR);
		}

		// 창고 용량 확인
		int currentPaletteCount = stockRepository.getTotalPaletteCountByWarehouseId(req.getWarehouseId());
		if (currentPaletteCount + req.getQuantity() > warehouse.getCapacity()) {
			throw LocationException.warehouseCapacityExceeded(
					req.getWarehouseId(), warehouse.getCapacity(), currentPaletteCount, req.getQuantity());
		}

		Stock stock = req.toEntity();

		eventPublisher.publishEvent(new StockCreatedEvent(stock));


		return stockRepository.save(stock);
	}

	@Transactional
	public Stock update(Long id, StockDTO.UpdateReq req) {
		Stock stock = stockRepository.findById(id)
				.orElseThrow(() -> StockException.notFound(id));

		// 창고 용량 확인 (기존 수량 제외하고 새 수량으로 계산)
		Location warehouse = locationRepository.findById(stock.getWarehouseId())
				.orElseThrow(() -> LocationException.notFound(stock.getWarehouseId()));

		int currentPaletteCount = stockRepository.getTotalPaletteCountByWarehouseId(stock.getWarehouseId());
		int adjustedCurrentCount = currentPaletteCount - stock.getQuantity(); // 기존 수량 제외

		if (adjustedCurrentCount + req.getQuantity() > warehouse.getCapacity()) {
			throw LocationException.warehouseCapacityExceeded(
					stock.getWarehouseId(), warehouse.getCapacity(), adjustedCurrentCount, req.getQuantity());
		}

		stock.updateQuantity(req.getQuantity());
		return stock;
	}

	@Transactional
	public void delete(Long id) {
		Stock stock = stockRepository.findById(id)
				.orElseThrow(() -> StockException.notFound(id));



		stockRepository.delete(stock);
	}

	// TODO: LogisticInitiateEvent 발생 후 subtractQuantityWithCapacityCheck 로직 추가 필요, 캐쉬 업데이트도 잊지말자
	// TODO: LogisticCompleteEvent 발생 후 addQuantityWithCapacityCheck 로직 추가 필요, 캐쉬 업데이트도 잊지말자





}