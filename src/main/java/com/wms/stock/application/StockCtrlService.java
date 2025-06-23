package com.wms.stock.application;

import javax.sound.midi.VoiceStatus;

import com.wms.applicationInfra.domain.FieldEnum;
import com.wms.location.application.LocationCacheService;
import com.wms.location.domain.exception.LocationException;
import com.wms.location.domain.model.Location;
import com.wms.location.domain.model.LocationType;
import com.wms.location.domain.repository.LocationRepository;
import com.wms.stock.domain.event.StockCreatedEvent;
import com.wms.stock.domain.event.StockDeletedEvent;
import com.wms.stock.domain.event.StockUpdatedEvent;
import com.wms.stock.domain.exception.StockException;
import com.wms.stock.domain.model.Stock;
import com.wms.stock.domain.repository.StockRepository;
import com.wms.stock.dto.StockDTO;
import com.wms.ware.domain.exception.WareException;
import com.wms.ware.domain.repository.WareRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.MessageFormat;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class StockCtrlService {

	private final StockRepository stockRepository;

	private final LocationRepository locationRepository;
	private final WareRepository wareRepository;

	private final ApplicationEventPublisher eventPublisher;
	private final LocationCacheService locationCacheService;
	private final StockCacheService stockCacheService;

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

		int oldQuantity = stock.getQuantity();

		// 창고 용량 확인 (기존 수량 제외하고 새 수량으로 계산)
		Location warehouse = locationRepository.findById(stock.getWarehouseId())
				.orElseThrow(() -> LocationException.notFound(stock.getWarehouseId()));

		int currentPaletteCount = stockRepository.getTotalPaletteCountByWarehouseId(stock.getWarehouseId());
		int adjustedCurrentCount = currentPaletteCount - stock.getQuantity(); // 기존 수량 제외

		if (adjustedCurrentCount + req.getQuantity() > warehouse.getCapacity()) {
			throw LocationException.warehouseCapacityExceeded(
					stock.getWarehouseId(), warehouse.getCapacity(), adjustedCurrentCount, req.getQuantity());
		}

		// 재고 수량 업데이트 및 삭제 여부(재고가 0인 경우) 확인
		boolean shouldDelete = stock.updateQuantityAndCheckDeletion(req.getQuantity());

		if (shouldDelete) {
			stockRepository.delete(stock);
			// 삭제 이벤트 발행
			eventPublisher.publishEvent(new StockDeletedEvent(stock)
			);
		} else {
			// 수정 이벤트 발행
			eventPublisher.publishEvent(new StockUpdatedEvent(stock, oldQuantity));
		}

		return stock;
	}

	@Transactional
	public void delete(Long id) {
		Stock stock = stockRepository.findById(id)
				.orElseThrow(() -> StockException.notFound(id));

		// 삭제 이벤트 발행
		eventPublisher.publishEvent(new StockDeletedEvent(stock));

		stockRepository.delete(stock);
	}

	// TODO: LogisticInitiateEvent 구독으로
	/**
	 * 재고 감소 메소드
	 * @param warehouseId 창고 ID
	 * @param wareId 물품 ID
	 * @param quantity 감소할 수량
	 */
	private void decreaseStock(Long warehouseId, Long wareId, Integer quantity) {
		Stock stock = stockRepository.findByWarehouseIdAndWareId(wareId, warehouseId)
				.orElseThrow(() -> StockException.notFound(
						MessageFormat.format("wareId({0}), warehouseId({1})", wareId, warehouseId)
				));

		Integer oldQuantity = stock.getQuantity();

		// 엔티티에서 한방에 처리
		boolean shouldDelete = stock.minusQuantityWithStockCheck(oldQuantity, quantity);

		if (shouldDelete) {
			stockRepository.delete(stock);
			eventPublisher.publishEvent(new StockDeletedEvent(stock));
		} else {
			eventPublisher.publishEvent(new StockUpdatedEvent(stock, oldQuantity));
		}
	}

	// TODO: LogisticCompleteEvent 구독으로
	/**
	 * 재고 감소 메소드
	 * @param warehouseId 창고 ID
	 * @param wareId 물품 ID
	 * @param quantity 증가할 수량
	 */
	private void increaseStock(Long warehouseId, Long wareId, Integer quantity) {
		Stock stock = stockRepository.findByWarehouseIdAndWareId(wareId, warehouseId)
				.orElseThrow(() -> StockException.notFound(
						MessageFormat.format("wareId({0}), warehouseId({1})", wareId, warehouseId)
				));

		Integer oldQuantity = stock.getQuantity();

		Integer warehouseCapacity = locationCacheService.getWarehouseCapacity(warehouseId); // 캐시에서 창고 용량 조회
		if (warehouseCapacity == null) {
			throw LocationException.notFound(warehouseId);
		}

		Integer currentSum  = stockCacheService.getWarehouseCurrentSum(warehouseId);

		// 엔티티에서 한방에 처리
		stock.plusQuantityWithCapacityCheck(warehouseCapacity, currentSum != null ? currentSum : 0 , quantity);  // null 이면 재고가 없는 상태니 0으로 초기화

		if (currentSum == null) { // 현재 재고가 없던 상태라면 새로 생성된 것으로 간주
			eventPublisher.publishEvent(new StockCreatedEvent(stock));
		} else {                  // 기존 재고가 있었던 상태라면 업데이트 이벤트 발행
			eventPublisher.publishEvent(new StockUpdatedEvent(stock, oldQuantity));
		}
	}
}