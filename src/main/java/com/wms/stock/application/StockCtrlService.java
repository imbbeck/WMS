package com.wms.stock.application;

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
import com.wms.stock.domain.model.StockKey;
import com.wms.stock.domain.repository.StockRepository;
import com.wms.stock.dto.StockDTO;
import com.wms.ware.domain.exception.WareException;
import com.wms.ware.domain.repository.WareRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
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
		StockKey stockKey = StockKey.of(req.getWareId(), req.getWarehouseId());
		stockRepository.findByKey(stockKey)
				.ifPresent(s -> { throw StockException.duplicate(FieldEnum.WARE_WAREHOUSE_PAIR); });

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
	public Stock update(Long wareId, Long warehouseId, StockDTO.UpdateReq req) {
		StockKey stockKey = StockKey.of(wareId, warehouseId);
		Stock stock = stockRepository.findByKey(stockKey)
				.orElseThrow(() -> StockException.notFound(stockKey));

		Integer oldQuantity = stock.getQuantity(); // 원래 수량 보존

		// 창고 용량 확인
		Location warehouse = locationRepository.findById(stock.getKey().getWarehouseId())
				.orElseThrow(() -> LocationException.notFound(stock.getKey().getWarehouseId()));

		int currentPaletteCount = stockRepository.getTotalPaletteCountByWarehouseId(stock.getKey().getWarehouseId());
		int adjustedCurrentCount = currentPaletteCount - stock.getQuantity();

		if (adjustedCurrentCount + req.getQuantity() > warehouse.getCapacity()) {
			throw LocationException.warehouseCapacityExceeded(
					stock.getKey().getWarehouseId(), warehouse.getCapacity(), adjustedCurrentCount, req.getQuantity());
		}

		// 재고 수량 업데이트 및 삭제 여부 확인
		boolean shouldDelete = stock.updateQuantityAndCheckDeletion(req.getQuantity());

		if (shouldDelete) {
			stockRepository.delete(stock);
			// 삭제 이벤트 발행 - 원래 수량으로 생성
			Stock deletedStock = Stock.builder()
					.key(stock.getKey())
					.quantity(oldQuantity) // 원래 수량 사용
					.build();
			eventPublisher.publishEvent(new StockDeletedEvent(deletedStock));
		} else {
			// 수정 이벤트 발행
			eventPublisher.publishEvent(new StockUpdatedEvent(stock, oldQuantity));
		}

		return stock;
	}

	@Transactional
	public void delete(Long wareId, Long warehouseId) {
		StockKey stockKey = StockKey.of(wareId, warehouseId);
		Stock stock = stockRepository.findByKey(stockKey)
				.orElseThrow(() -> StockException.notFound(stockKey));

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
	@Transactional
	public void decreaseStock(Long warehouseId, Long wareId, Integer quantity) {
		StockKey stockKey = StockKey.of(wareId, warehouseId);
		Stock stock = stockRepository.findByKey(stockKey)
				.orElseThrow(() -> StockException.notFound(stockKey));

		Integer oldQuantity = stock.getQuantity();

		// 재고 부족 검증 추가
		if (oldQuantity < quantity) {
			throw StockException.insufficientStock(warehouseId, wareId, quantity, oldQuantity);
		}

		// 엔티티에서 한방에 처리
		boolean shouldDelete = stock.minusQuantityWithStockCheck(oldQuantity, quantity);

		if (shouldDelete) {
			stockRepository.delete(stock);
			// 원래 수량으로 삭제 이벤트 발행
			Stock deletedStock = Stock.builder()
					.key(stock.getKey())
					.quantity(oldQuantity)
					.build();
			eventPublisher.publishEvent(new StockDeletedEvent(deletedStock));
		} else {
			eventPublisher.publishEvent(new StockUpdatedEvent(stock, oldQuantity));
		}
	}

	// TODO: LogisticCompleteEvent 구독으로
	/**
	 * 재고 증가 메소드
	 * @param warehouseId 창고 ID
	 * @param wareId 물품 ID
	 * @param quantity 증가할 수량
	 */
	@Transactional
	public void increaseStock(Long warehouseId, Long wareId, Integer quantity) {
		StockKey stockKey = StockKey.of(wareId, warehouseId);
		Optional<Stock> stockOpt = stockRepository.findByKey(stockKey);

		Integer warehouseCapacity = locationCacheService.getWarehouseCapacity(warehouseId); // 캐시에서 창고 용량 조회
		if (warehouseCapacity == null) {
			throw LocationException.notFound(warehouseId);
		}

		Integer currentSum = stockCacheService.getWarehouseCurrentSum(warehouseId);

		if (stockOpt.isPresent()) {
			// 기존 재고가 있는 경우 - 증가
			Stock stock = stockOpt.get();
			Integer oldQuantity = stock.getQuantity();
			
			// 엔티티에서 한방에 처리
			stock.plusQuantityWithCapacityCheck(warehouseCapacity, currentSum != null ? currentSum : 0, quantity);
			
			eventPublisher.publishEvent(new StockUpdatedEvent(stock, oldQuantity));
		} else {
			// 재고가 없는 경우 - 새로 생성
			
			// 물품과 창고 존재 확인
			if (!wareRepository.existsById(wareId)) {
				throw WareException.notFound(wareId);
			}
			
			Location warehouse = locationRepository.findById(warehouseId)
					.orElseThrow(() -> LocationException.notFound(warehouseId));
			
			if (!warehouse.getType().equals(LocationType.WAREHOUSE)) {
				throw LocationException.notWarehouseEx(warehouseId);
			}
			
			// 창고 용량 확인
			int currentPaletteCount = currentSum != null ? currentSum : 0;
			if (currentPaletteCount + quantity > warehouse.getCapacity()) {
				throw LocationException.warehouseCapacityExceeded(
						warehouseId, warehouse.getCapacity(), currentPaletteCount, quantity);
			}
			
			// 새 재고 생성
			Stock newStock = Stock.builder()
					.key(stockKey)
					.quantity(quantity)
					.build();
			
			stockRepository.save(newStock);
			eventPublisher.publishEvent(new StockCreatedEvent(newStock));
		}
	}
}