//package com.wms.stock.application;
//
//import java.util.List;
//import java.util.Optional;
//
//import com.wms.applicationInfra.idnameMapCashing.concrete.LocationCacheManager;
//import com.wms.applicationInfra.idnameMapCashing.concrete.WareCacheManager;
//import com.wms.location.domain.exception.LocationException;
//import com.wms.location.domain.model.Location;
//import com.wms.location.domain.repository.LocationRepository;
//import com.wms.stock.domain.exception.StockException;
//import com.wms.stock.domain.model.Stock;
//import com.wms.stock.domain.repository.StockRepository;
//import com.wms.stock.dto.StockDTO;
//import com.wms.ware.domain.exception.WareException;
//import com.wms.ware.domain.repository.WareRepository;
//import lombok.RequiredArgsConstructor;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.Pageable;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//@Service
//@RequiredArgsConstructor
//public class StockViewService {
//
//	private final StockRepository stockRepository;
//
//	private final LocationRepository locationRepository;
//	private final WareRepository wareRepository;
//
//	private final LocationCacheManager locationCacheManager;
//	private final WareCacheManager wareCacheManager;
//
//	public Stock findById(Long id) {
//		return stockRepository.findById(id)
//				.orElseThrow(() -> StockException.notFound(id));
//	}
//
//	public Optional<Stock> findByWarehouseIdAndWareId(Long wareId, Long warehouseId) {
//		return stockRepository.findByWarehouseIdAndWareId(wareId, warehouseId)
//	}
//
//	public List<Stock> getStocks() {
//		return stockRepository.findAll();
//	}
//
//	public List<Stock> getStocksByWarehouse(Long warehouseId) {
//		// 창고 존재 확인
//		if (!locationRepository.existsById(warehouseId)) {
//			throw LocationException.notFound(warehouseId);
//		}
//
//		return stockRepository.findAllByWarehouseId(warehouseId));
//	}
//
//	public List<Stock> getStocksByWare(Long wareId) {
//		// 물품 존재 확인
//		if (!wareRepository.existsById(wareId)) {
//			throw WareException.notFound(wareId);
//		}
//
//		return stockRepository.findAllByWareId(wareId);
//	}
//
//	public Page<StockDTO.Res> stocksWithConditions(StockDTO.SearchReq searchReq, Pageable pageable) {
//		return stockRepository.findBySearchCriteria(
//				searchReq.getWareId(),
//				searchReq.getWarehouseId(),
//				searchReq.getMinQuantity(),
//				searchReq.getMaxQuantity(),
//				pageable
//		).map(this::convertToRes);
//	}
//
//	@Transactional(readOnly = true)
//	public StockDTO.WarehouseSummaryRes getWarehouseSummary(Long warehouseId) {
//		// 창고 존재 확인
//		Location warehouse = locationRepository.findById(warehouseId)
//				.orElseThrow(() -> LocationException.notFound(warehouseId));
//
//		Optional<StockRepository.WarehouseStockSummary> summaryOpt =
//				stockRepository.findWarehouseStockSummary(warehouseId);
//
//		String warehouseName = locationCacheManager.getName(warehouseId);
//
//		if (summaryOpt.isPresent()) {
//			StockRepository.WarehouseStockSummary summary = summaryOpt.get();
//			Integer totalPaletteCount = summary.getTotalQuantity().intValue();
//			Double utilizationRate = warehouse.getCapacity() > 0 ?
//					(totalPaletteCount.doubleValue() / warehouse.getCapacity() * 100) : 0.0;
//
//			return StockDTO.WarehouseSummaryRes.builder()
//					.warehouseId(warehouseId)
//					.warehouseName(warehouseName)
//					.totalPaletteCount(totalPaletteCount)
//					.capacity(warehouse.getCapacity())
//					.utilizationRate(Math.round(utilizationRate * 100.0) / 100.0) // 소수점 2자리
//					.wareTypeCount(summary.getWareTypeCount().intValue())
//					.build();
//		} else {
//			// 재고가 없는 경우
//			return StockDTO.WarehouseSummaryRes.builder()
//					.warehouseId(warehouseId)
//					.warehouseName(warehouseName)
//					.totalPaletteCount(0)
//					.capacity(warehouse.getCapacity())
//					.utilizationRate(0.0)
//					.wareTypeCount(0)
//					.build();
//		}
//	}
//
//	@Transactional(readOnly = true)
//	public List<StockDTO.WarehouseSummaryRes> getAllWarehouseSummaries() {
//		List<StockRepository.WarehouseStockSummary> summaries =
//				stockRepository.findAllWarehouseStockSummaries();
//
//		return summaries.stream()
//				.map(summary -> {
//					Long warehouseId = summary.getWarehouseId();
//					String warehouseName = locationCacheManager.getName(warehouseId);
//
//					// 창고 정보 조회 (용량 확인용)
//					Location warehouse = locationRepository.findById(warehouseId)
//							.orElse(null);
//
//					Integer capacity = warehouse != null ? warehouse.getCapacity() : 0;
//					Integer totalPaletteCount = summary.getTotalQuantity().intValue();
//					Double utilizationRate = capacity > 0 ?
//							(totalPaletteCount.doubleValue() / capacity * 100) : 0.0;
//
//					return StockDTO.WarehouseSummaryRes.builder()
//							.warehouseId(warehouseId)
//							.warehouseName(warehouseName)
//							.totalPaletteCount(totalPaletteCount)
//							.capacity(capacity)
//							.utilizationRate(Math.round(utilizationRate * 100.0) / 100.0)
//							.wareTypeCount(summary.getWareTypeCount().intValue())
//							.build();
//				})
//				.toList();
//	}
//}
