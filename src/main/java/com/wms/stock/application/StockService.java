//package com.wms.stock.application.service;
//
//import com.wms.location.domain.model.Location;
//import com.wms.location.domain.repository.LocationRepository;
//import com.wms.stock.domain.exception.StockExceptions;
//import com.wms.stock.domain.model.Stock;
//import com.wms.stock.domain.repository.StockRepository;
//import com.wms.ware.domain.model.Ware;
//import com.wms.ware.domain.repository.WareRepository;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.util.List;
//import java.util.Optional;
//
///**
// * 재고 관리 서비스
// * 물류작업과 연동하여 재고를 관리하고 조회 기능을 제공
// */
//@Service
//@RequiredArgsConstructor
//@Slf4j
//@Transactional
//public class StockService {
//
//	private final StockRepository stockRepository;
//	private final LocationRepository locationRepository;
//	private final WareRepository wareRepository;
//
//	/**
//	 * 새로운 재고 생성
//	 */
//	public Stock createStock(Long wareId, Long warehouseId, Integer quantity) {
//		log.info("새 재고 생성: 물품ID={}, 창고ID={}, 수량={}", wareId, warehouseId, quantity);
//
//		// 1. 엔티티 조회
//		Ware ware = wareRepository.findById(wareId)
//				.orElseThrow(() -> new StockExceptions.WareNotFoundExceptions(wareId));
//
//		Location warehouse = locationRepository.findById(warehouseId)
//				.orElseThrow(() -> new StockExceptions.WarehouseNotFoundExceptions(warehouseId));
//
//		// 2. 기존 재고 존재 확인
//		Optional<Stock> existingStock = stockRepository.findByWarehouseIdAndWareId(warehouseId, wareId);
//		if (existingStock.isPresent()) {
//			throw new StockExceptions.DuplicateStockExceptions(wareId, warehouseId);
//		}
//
//		// 3. 재고 생성
//		Stock stock = Stock.create(ware, warehouse, quantity);
//		Stock savedStock = stockRepository.save(stock);
//
//		log.info("재고 생성 완료: ID={}", savedStock.getId());
//		return savedStock;
//	}
//
//	/**
//	 * 재고 수량 업데이트
//	 */
//	public Stock updateStockQuantity(Long stockId, Integer quantity) {
//		log.info("재고 수량 업데이트: ID={}, 수량={}", stockId, quantity);
//
//		Stock stock = stockRepository.findById(stockId)
//				.orElseThrow(() -> new StockExceptions.StockNotFoundExceptions(stockId));
//
//		stock.updateQuantity(quantity);
//
//		log.info("재고 수량 업데이트 완료: ID={}, 새 수량={}", stockId, quantity);
//		return stock;
//	}
//
//	/**
//	 * 재고 증가
//	 */
//	public Stock addStock(Long wareId, Long warehouseId, Integer quantity) {
//		log.info("재고 증가: 물품ID={}, 창고ID={}, 증가량={}", wareId, warehouseId, quantity);
//
//		Optional<Stock> stockOpt = stockRepository.findByWarehouseIdAndWareId(warehouseId, wareId);
//
//		Stock stock;
//		if (stockOpt.isEmpty()) {
//			// 재고가 없으면 새로 생성
//			Ware ware = wareRepository.findById(wareId)
//					.orElseThrow(() -> new StockExceptions.WareNotFoundExceptions(wareId));
//
//			Location warehouse = locationRepository.findById(warehouseId)
//					.orElseThrow(() -> new StockExceptions.WarehouseNotFoundExceptions(warehouseId));
//
//			stock = Stock.create(ware, warehouse, quantity);
//			stock = stockRepository.save(stock);
//		} else {
//			// 기존 재고에 추가
//			stock = stockOpt.get();
//			stock.addQuantity(quantity);
//		}
//
//		log.info("재고 증가 완료: 물품ID={}, 창고ID={}, 최종 수량={}",
//				wareId, warehouseId, stock.getQuantity());
//		return stock;
//	}
//
//	/**
//	 * 재고 감소
//	 */
//	public Stock removeStock(Long wareId, Long warehouseId, Integer quantity) {
//		log.info("재고 감소: 물품ID={}, 창고ID={}, 감소량={}", wareId, warehouseId, quantity);
//
//		Stock stock = stockRepository.findByWarehouseIdAndWareId(warehouseId, wareId)
//				.orElseThrow(() -> new StockExceptions.StockNotFoundForWareAndWarehouseExceptions(wareId, warehouseId));
//
//		stock.removeQuantity(quantity);
//
//		log.info("재고 감소 완료: 물품ID={}, 창고ID={}, 최종 수량={}",
//				wareId, warehouseId, stock.getQuantity());
//		return stock;
//	}
//
//	/**
//	 * 재고 삭제
//	 */
//	public void deleteStock(Long stockId) {
//		log.info("재고 삭제: ID={}", stockId);
//
//		Stock stock = stockRepository.findById(stockId)
//				.orElseThrow(() -> new StockExceptions.StockNotFoundExceptions(stockId));
//
//		stockRepository.delete(stock);
//
//		log.info("재고 삭제 완료: ID={}", stockId);
//	}
//
//	/**
//	 * 재고 이동 (한 번에 출발지 감소, 도착지 증가)
//	 */
//	public void moveStock(Long wareId, Long fromWarehouseId, Long toWarehouseId, Integer quantity) {
//		log.info("재고 이동: 물품ID={}, 출발창고ID={}, 도착창고ID={}, 수량={}",
//				wareId, fromWarehouseId, toWarehouseId, quantity);
//
//		// 1. 출발지 재고 감소
//		removeStock(wareId, fromWarehouseId, quantity);
//
//		// 2. 도착지 재고 증가
//		addStock(wareId, toWarehouseId, quantity);
//
//		log.info("재고 이동 완료: 물품ID={}, 출발창고ID={}, 도착창고ID={}, 수량={}",
//				wareId, fromWarehouseId, toWarehouseId, quantity);
//	}
//
//	// === 조회 메서드들 ===
//
//	/**
//	 * 재고 ID로 조회
//	 */
//	@Transactional(readOnly = true)
//	public Stock getStock(Long stockId) {
//		return stockRepository.findById(stockId)
//				.orElseThrow(() -> new StockExceptions.StockNotFoundExceptions(stockId));
//	}
//
//	/**
//	 * 특정 창고, 물품의 재고 조회
//	 */
//	@Transactional(readOnly = true)
//	public Optional<Stock> getStock(Long wareId, Long warehouseId) {
//		return stockRepository.findByWarehouseIdAndWareId(warehouseId, wareId);
//	}
//
//	/**
//	 * 특정 창고의 모든 재고 조회
//	 */
//	@Transactional(readOnly = true)
//	public List<Stock> getStocksByWarehouse(Long warehouseId) {
//		return stockRepository.findByWarehouseId(warehouseId);
//	}
//
//	/**
//	 * 특정 물품의 모든 창고별 재고 조회
//	 */
//	@Transactional(readOnly = true)
//	public List<Stock> getStocksByWare(Long wareId) {
//		return stockRepository.findByWareId(wareId);
//	}
//
//	/**
//	 * 재고가 있는 창고들의 특정 물품 재고 조회
//	 */
//	@Transactional(readOnly = true)
//	public List<Stock> getAvailableStocksByWare(Long wareId) {
//		return stockRepository.findByWareIdWithPositiveQuantity(wareId);
//	}
//
//	/**
//	 * 특정 창고들의 특정 물품 재고 조회
//	 */
//	@Transactional(readOnly = true)
//	public List<Stock> getStocksByWarehousesAndWare(List<Long> warehouseIds, Long wareId) {
//		return stockRepository.findByWarehouseIdsAndWareId(warehouseIds, wareId);
//	}
//
//	/**
//	 * 재고 부족 항목들 조회
//	 */
//	@Transactional(readOnly = true)
//	public List<Stock> getLowStockItems(Integer threshold) {
//		return stockRepository.findLowStockItems(threshold);
//	}
//
//	/**
//	 * 특정 창고의 총 재고 수량 조회
//	 */
//	@Transactional(readOnly = true)
//	public Long getTotalQuantityByWarehouse(Long warehouseId) {
//		return stockRepository.getTotalQuantityByWarehouse(warehouseId);
//	}
//
//	/**
//	 * 특정 물품의 전체 재고 수량 조회
//	 */
//	@Transactional(readOnly = true)
//	public Long getTotalQuantityByWare(Long wareId) {
//		return stockRepository.getTotalQuantityByWare(wareId);
//	}
//
//	/**
//	 * 재고가 0인 항목들 조회
//	 */
//	@Transactional(readOnly = true)
//	public List<Stock> getZeroQuantityStocks() {
//		return stockRepository.findZeroQuantityStocks();
//	}
//
//	/**
//	 * 특정 창고의 재고가 0인 항목들 조회
//	 */
//	@Transactional(readOnly = true)
//	public List<Stock> getZeroQuantityStocksByWarehouse(Long warehouseId) {
//		return stockRepository.findZeroQuantityStocksByWarehouse(warehouseId);
//	}
//
//	/**
//	 * 재고 가용성 체크
//	 */
//	@Transactional(readOnly = true)
//	public boolean isStockAvailable(Long wareId, Long warehouseId, Integer requiredQuantity) {
//		Optional<Stock> stockOpt = stockRepository.findByWarehouseIdAndWareId(warehouseId, wareId);
//
//		if (stockOpt.isEmpty()) {
//			return false;
//		}
//
//		return stockOpt.get().getQuantity() >= requiredQuantity;
//	}
//
//	/**
//	 * 재고 현황 요약 조회
//	 */
//	@Transactional(readOnly = true)
//	public StockSummary getStockSummary(Long warehouseId) {
//		List<Stock> stocks = stockRepository.findByWarehouseId(warehouseId);
//
//		long totalItems = stocks.size();
//		long totalQuantity = stocks.stream().mapToLong(Stock::getQuantity).sum();
//		long zeroQuantityItems = stocks.stream().mapToLong(s -> s.getQuantity() == 0 ? 1 : 0).sum();
//		long lowStockItems = stocks.stream().mapToLong(s -> s.getQuantity() <= 10 ? 1 : 0).sum(); // 10개 이하를 저재고로 가정
//
//		return StockSummary.builder()
//				.warehouseId(warehouseId)
//				.totalItems(totalItems)
//				.totalQuantity(totalQuantity)
//				.zeroQuantityItems(zeroQuantityItems)
//				.lowStockItems(lowStockItems)
//				.build();
//	}
//
//	/**
//	 * 재고 현황 요약 DTO
//	 */
//	public static class StockSummary {
//		private final Long warehouseId;
//		private final long totalItems;
//		private final long totalQuantity;
//		private final long zeroQuantityItems;
//		private final long lowStockItems;
//
//		public StockSummary(Long warehouseId, long totalItems, long totalQuantity,
//				long zeroQuantityItems, long lowStockItems) {
//			this.warehouseId = warehouseId;
//			this.totalItems = totalItems;
//			this.totalQuantity = totalQuantity;
//			this.zeroQuantityItems = zeroQuantityItems;
//			this.lowStockItems = lowStockItems;
//		}
//
//		public static StockSummaryBuilder builder() {
//			return new StockSummaryBuilder();
//		}
//
//		// Getters
//		public Long getWarehouseId() { return warehouseId; }
//		public long getTotalItems() { return totalItems; }
//		public long getTotalQuantity() { return totalQuantity; }
//		public long getZeroQuantityItems() { return zeroQuantityItems; }
//		public long getLowStockItems() { return lowStockItems; }
//
//		public static class StockSummaryBuilder {
//			private Long warehouseId;
//			private long totalItems;
//			private long totalQuantity;
//			private long zeroQuantityItems;
//			private long lowStockItems;
//
//			public StockSummaryBuilder warehouseId(Long warehouseId) {
//				this.warehouseId = warehouseId;
//				return this;
//			}
//
//			public StockSummaryBuilder totalItems(long totalItems) {
//				this.totalItems = totalItems;
//				return this;
//			}
//
//			public StockSummaryBuilder totalQuantity(long totalQuantity) {
//				this.totalQuantity = totalQuantity;
//				return this;
//			}
//
//			public StockSummaryBuilder zeroQuantityItems(long zeroQuantityItems) {
//				this.zeroQuantityItems = zeroQuantityItems;
//				return this;
//			}
//
//			public StockSummaryBuilder lowStockItems(long lowStockItems) {
//				this.lowStockItems = lowStockItems;
//				return this;
//			}
//
//			public StockSummary build() {
//				return new StockSummary(warehouseId, totalItems, totalQuantity,
//						zeroQuantityItems, lowStockItems);
//			}
//		}
//	}
//}