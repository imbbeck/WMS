package com.wms.stock.application;

import com.wms.applicationInfra.idnameMapCashing.concrete.LocationCacheManager;
import com.wms.applicationInfra.idnameMapCashing.concrete.WareCacheManager;
import com.wms.location.application.LocationCacheService;
import com.wms.location.domain.exception.LocationException;
import com.wms.location.domain.repository.LocationRepository;
import com.wms.stock.domain.model.Stock;
import com.wms.stock.domain.repository.StockRepository;
import com.wms.stock.dto.StockQueryDTO;
import com.wms.ware.domain.exception.WareException;
import com.wms.ware.domain.repository.WareRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class StockQueryService {

	private final StockRepository stockRepository;
	private final LocationRepository locationRepository;
	private final WareRepository wareRepository;
	private final StockCacheService stockCacheService;
	private final LocationCacheService locationCacheService;
	private final RedisTemplate<String, Object> redisTemplate;
	private final LocationCacheManager locationCacheManager;
	private final WareCacheManager wareCacheManager;

	// === 기본 조회 메서드들 ===

	/**
	 * 특정 창고-물품 조합의 재고 수량 조회 (캐시 우선)
	 */
	public Integer getStockQuantity(Long warehouseId, Long wareId) {
		log.debug("재고 수량 조회 - warehouseId: {}, wareId: {}", warehouseId, wareId);
		return stockCacheService.getInventoryQuantity(wareId, warehouseId);
	}

	/**
	 * 특정 창고-물품 조합의 재고 DTO 조회 (캐시 우선)
	 */
	public Optional<StockQueryDTO.Res> getStockResByWarehouseAndWare(Long warehouseId, Long wareId) {
		// 캐시에서 수량 먼저 확인
		Integer quantity = stockCacheService.getInventoryQuantity(wareId, warehouseId);

		if (quantity == null || quantity == 0) {
			log.debug("캐시에서 재고 없음 확인 - warehouseId: {}, wareId: {}", warehouseId, wareId);
			return getStockResByWarehouseAndWareFromDB(warehouseId, wareId);
		}

		// 캐시 데이터로 DTO 생성
		return Optional.of(createStockRes(wareId, warehouseId, quantity));
	}

	/**
	 * 특정 창고의 모든 재고 DTO 조회 (캐시 우선)
	 */
	public List<StockQueryDTO.Res> getStocksByWarehouse(Long warehouseId) {
		validateWarehouseExists(warehouseId);

		// 캐시에서 조회 시도
		Map<Long, Integer> cachedStocks = scanWarehouseStocksFromCache(warehouseId);
		if (!cachedStocks.isEmpty()) {
			log.debug("캐시에서 창고 재고 조회 완료 - warehouseId: {}, 재고 개수: {}", warehouseId, cachedStocks.size());
			return convertCachedWarehouseStocksToRes(warehouseId, cachedStocks);
		}

		// 캐시에 없으면 DB 조회
		log.debug("캐시 없음 - DB에서 창고 재고 조회 - warehouseId: {}", warehouseId);
		return stockRepository.findAllByWarehouseId(warehouseId).stream()
				.map(this::convertStockToRes)
				.collect(Collectors.toList());
	}

	/**
	 * 특정 물품의 모든 창고별 재고 DTO 조회 (DB 조회 - 캐시 스캔 비효율적)
	 */
	public List<StockQueryDTO.Res> getStocksByWare(Long wareId) {
		validateWareExists(wareId);

		// 물품별 조회는 키 패턴상 전체 스캔이 필요하므로 DB 조회가 더 효율적
		log.debug("DB에서 물품 재고 조회 - wareId: {}", wareId);
		return stockRepository.findAllByWareId(wareId).stream()
				.map(this::convertStockToRes)
				.collect(Collectors.toList());
	}

	// === 집계 조회 메서드들 ===

	/**
	 * 창고별 재고 집계 정보 조회 (캐시 우선)
	 */
	public StockQueryDTO.WarehouseAggregationRes getWarehouseAggregation(Long warehouseId) {
		validateWarehouseExists(warehouseId);

		String warehouseName = locationCacheManager.getName(warehouseId);
		Integer capacity = locationCacheService.getWarehouseCapacity(warehouseId);
		Integer totalQuantity = stockCacheService.getWarehouseCurrentSum(warehouseId);

		if (totalQuantity == 0) {
			return createEmptyWarehouseAggregation(warehouseId, warehouseName, capacity);
		}

		// 캐시에서 상세 조회 시도
		Map<Long, Integer> cachedStocks = scanWarehouseStocksFromCache(warehouseId);
		if (!cachedStocks.isEmpty()) {
			log.debug("캐시에서 창고 집계 조회 완료 - warehouseId: {}", warehouseId);
			return createWarehouseAggregationFromCache(warehouseId, warehouseName, capacity, totalQuantity, cachedStocks);
		}

		// 캐시에 없으면 DB 조회
		log.debug("캐시 없음 - DB에서 창고 집계 조회 - warehouseId: {}", warehouseId);
		return createWarehouseAggregationFromDB(warehouseId, warehouseName, capacity, totalQuantity);
	}

	/**
	 * 물품별 재고 집계 정보 조회 (DB 조회 - 캐시 스캔 비효율적)
	 */
	public StockQueryDTO.WareAggregationRes getWareAggregation(Long wareId) {
		validateWareExists(wareId);

		String wareName = wareCacheManager.getName(wareId);

		// 물품별 조회는 키 패턴상 전체 스캔이 필요하므로 DB 조회가 더 효율적
		log.debug("DB에서 물품 집계 조회 - wareId: {}", wareId);
		return createWareAggregationFromDB(wareId, wareName);
	}

	/**
	 * 모든 창고의 재고 집계 정보 조회 (캐시 우선)
	 */
	public List<StockQueryDTO.WarehouseAggregationRes> getAllWarehouseAggregations() {
		Map<Long, List<CachedStockInfo>> warehouseStocks = getStocksGroupedByWarehouse();

		if (!warehouseStocks.isEmpty()) {
			log.debug("캐시에서 전체 창고 집계 조회 완료 - 창고 개수: {}", warehouseStocks.size());
			return createAllWarehouseAggregationsFromCache(warehouseStocks);
		}

		// 캐시에 없으면 DB 조회
		log.debug("캐시 없음 - DB에서 전체 창고 집계 조회");
		return createAllWarehouseAggregationsFromDB();
	}

	/**
	 * 모든 물품의 재고 집계 정보 조회 (캐시 우선)
	 */
	public List<StockQueryDTO.WareAggregationRes> getAllWareAggregations() {
		Map<Long, List<CachedStockInfo>> wareStocks = getStocksGroupedByWare();

		if (!wareStocks.isEmpty()) {
			log.debug("캐시에서 전체 물품 집계 조회 완료 - 물품 개수: {}", wareStocks.size());
			return createAllWareAggregationsFromCache(wareStocks);
		}

		// 캐시에 없으면 DB 조회
		log.debug("캐시 없음 - DB에서 전체 물품 집계 조회");
		return createAllWareAggregationsFromDB();
	}

	// === 유틸리티 메서드들 ===

	/**
	 * 창고의 총 재고 수량 조회 (캐시 우선)
	 */
	public Integer getWarehouseTotalQuantity(Long warehouseId) {
		return stockCacheService.getWarehouseCurrentSum(warehouseId);
	}

	/**
	 * 특정 물품의 전체 재고 수량 조회 (DB 조회 - 캐시 스캔 비효율적)
	 */
	public Integer getWareTotalQuantity(Long wareId) {
		validateWareExists(wareId);

		// 물품별 조회는 키 패턴상 전체 스캔이 필요하므로 DB 조회가 더 효율적
		log.debug("DB에서 물품 전체 재고 합산 - wareId: {}", wareId);
		return stockRepository.findAllByWareId(wareId).stream()
				.mapToInt(Stock::getQuantity)
				.sum();
	}

	/**
	 * 창고 사용률 조회 (캐시 활용)
	 */
	public Double getWarehouseUtilizationRate(Long warehouseId) {
		Integer capacity = locationCacheService.getWarehouseCapacity(warehouseId);
		if (capacity == null || capacity == 0) {
			return 0.0;
		}

		Integer currentSum = stockCacheService.getWarehouseCurrentSum(warehouseId);
		return Math.round((double) currentSum / capacity * 100 * 100.0) / 100.0;
	}

	/**
	 * 창고 용량 확인 (캐시 활용)
	 */
	public boolean canAddStockToWarehouse(Long warehouseId, Integer additionalQuantity) {
		Integer capacity = locationCacheService.getWarehouseCapacity(warehouseId);
		if (capacity == null) {
			return false;
		}

		Integer currentSum = stockCacheService.getWarehouseCurrentSum(warehouseId);
		return currentSum + additionalQuantity <= capacity;
	}

	// === Private Helper Methods ===

	/**
	 * DB에서 창고-물품 조합 재고 조회
	 */
	private Optional<StockQueryDTO.Res> getStockResByWarehouseAndWareFromDB(Long warehouseId, Long wareId) {
		return stockRepository.findByWarehouseIdAndWareId(warehouseId, wareId)
				.map(this::convertStockToRes);
	}

	/**
	 * 검증 메서드들
	 */
	private void validateWarehouseExists(Long warehouseId) {
		if (!locationRepository.existsById(warehouseId)) {
			throw LocationException.notFound(warehouseId);
		}
	}

	private void validateWareExists(Long wareId) {
		if (!wareRepository.existsById(wareId)) {
			throw WareException.notFound(wareId);
		}
	}

	/**
	 * DTO 생성 헬퍼 메서드들
	 */
	private StockQueryDTO.Res createStockRes(Long wareId, Long warehouseId, Integer quantity) {
		String wareName = wareCacheManager.getName(wareId);
		String warehouseName = locationCacheManager.getName(warehouseId);
		return StockQueryDTO.Res.from(wareId, wareName, warehouseId, warehouseName, quantity);
	}

	private StockQueryDTO.Res convertStockToRes(Stock stock) {
		String wareName = wareCacheManager.getName(stock.getWareId());
		String warehouseName = locationCacheManager.getName(stock.getWarehouseId());
		return StockQueryDTO.Res.from(stock.getWareId(), wareName, stock.getWarehouseId(), warehouseName, stock.getQuantity());
	}

	/**
	 * 캐시 데이터를 DTO로 변환하는 메서드들
	 */
	private List<StockQueryDTO.Res> convertCachedWarehouseStocksToRes(Long warehouseId, Map<Long, Integer> cachedStocks) {
		String warehouseName = locationCacheManager.getName(warehouseId);
		return cachedStocks.entrySet().stream()
				.filter(entry -> entry.getValue() > 0)
				.map(entry -> {
					String wareName = wareCacheManager.getName(entry.getKey());
					return StockQueryDTO.Res.from(entry.getKey(), wareName, warehouseId, warehouseName, entry.getValue());
				})
				.collect(Collectors.toList());
	}

	/**
	 * 창고 집계 DTO 생성 메서드들
	 */
	private StockQueryDTO.WarehouseAggregationRes createEmptyWarehouseAggregation(Long warehouseId, String warehouseName, Integer capacity) {
		return StockQueryDTO.WarehouseAggregationRes.builder()
				.warehouseId(warehouseId)
				.warehouseName(warehouseName)
				.totalQuantity(0)
				.capacity(capacity)
				.wareTypeCount(0)
				.stockList(List.of())
				.build();
	}

	private StockQueryDTO.WarehouseAggregationRes createWarehouseAggregationFromCache(
			Long warehouseId, String warehouseName, Integer capacity, Integer totalQuantity, Map<Long, Integer> cachedStocks) {

		List<StockQueryDTO.WareUnit> wareUnits = cachedStocks.entrySet().stream()
				.map(entry -> StockQueryDTO.WareUnit.builder()
						.wareId(entry.getKey())
						.wareName(wareCacheManager.getName(entry.getKey()))
						.quantity(entry.getValue())
						.build())
				.collect(Collectors.toList());

		return StockQueryDTO.WarehouseAggregationRes.builder()
				.warehouseId(warehouseId)
				.warehouseName(warehouseName)
				.totalQuantity(totalQuantity)
				.capacity(capacity)
				.wareTypeCount(cachedStocks.size())
				.stockList(wareUnits)
				.build();
	}

	private StockQueryDTO.WarehouseAggregationRes createWarehouseAggregationFromDB(
			Long warehouseId, String warehouseName, Integer capacity, Integer totalQuantity) {

		Optional<StockRepository.WarehouseStockSummary> summary = stockRepository.findWarehouseStockSummary(warehouseId);
		List<Stock> stocks = stockRepository.findAllByWarehouseId(warehouseId);

		List<StockQueryDTO.WareUnit> wareUnits = stocks.stream()
				.map(stock -> StockQueryDTO.WareUnit.builder()
						.wareId(stock.getWareId())
						.wareName(wareCacheManager.getName(stock.getWareId()))
						.quantity(stock.getQuantity())
						.build())
				.collect(Collectors.toList());

		Integer wareTypeCount = summary.map(s -> s.getWareTypeCount().intValue()).orElse(0);

		return StockQueryDTO.WarehouseAggregationRes.builder()
				.warehouseId(warehouseId)
				.warehouseName(warehouseName)
				.totalQuantity(totalQuantity)
				.capacity(capacity)
				.wareTypeCount(wareTypeCount)
				.stockList(wareUnits)
				.build();
	}

	/**
	 * 물품 집계 DTO 생성 메서드
	 */
	private StockQueryDTO.WareAggregationRes createWareAggregationFromDB(Long wareId, String wareName) {
		Optional<StockRepository.WareStockSummary> summary = stockRepository.findWareStockSummary(wareId);
		List<Stock> stocks = stockRepository.findAllByWareId(wareId);

		Integer totalQuantity = summary.map(s -> s.getTotalQuantity().intValue()).orElse(0);
		Integer warehouseCount = summary.map(s -> s.getWarehouseCount().intValue()).orElse(0);

		List<StockQueryDTO.WarehouseUnit> warehouseUnits = stocks.stream()
				.map(stock -> StockQueryDTO.WarehouseUnit.builder()
						.warehouseId(stock.getWarehouseId())
						.warehouseName(locationCacheManager.getName(stock.getWarehouseId()))
						.quantity(stock.getQuantity())
						.build())
				.collect(Collectors.toList());

		return StockQueryDTO.WareAggregationRes.builder()
				.wareId(wareId)
				.wareName(wareName)
				.totalQuantity(totalQuantity)
				.warehouseCount(warehouseCount)
				.stockList(warehouseUnits)
				.build();
	}

	/**
	 * 전체 집계 DTO 생성 메서드들
	 */
	private List<StockQueryDTO.WarehouseAggregationRes> createAllWarehouseAggregationsFromCache(Map<Long, List<CachedStockInfo>> warehouseStocks) {
		return warehouseStocks.entrySet().stream()
				.map(entry -> {
					Long warehouseId = entry.getKey();
					List<CachedStockInfo> stocks = entry.getValue();

					String warehouseName = locationCacheManager.getName(warehouseId);
					Integer capacity = locationCacheService.getWarehouseCapacity(warehouseId);
					Integer totalQuantity = stocks.stream()
							.mapToInt(CachedStockInfo::quantity)
							.sum();

					return StockQueryDTO.WarehouseAggregationRes.builder()
							.warehouseId(warehouseId)
							.warehouseName(warehouseName)
							.totalQuantity(totalQuantity)
							.capacity(capacity)
							.wareTypeCount(stocks.size())
							.stockList(List.of()) // 목록은 개별 조회에서만 제공
							.build();
				})
				.collect(Collectors.toList());
	}

	private List<StockQueryDTO.WarehouseAggregationRes> createAllWarehouseAggregationsFromDB() {
		return stockRepository.findAllWarehouseStockSummaries().stream()
				.map(summary -> {
					Long warehouseId = summary.getWarehouseId();
					String warehouseName = locationCacheManager.getName(warehouseId);
					Integer capacity = locationCacheService.getWarehouseCapacity(warehouseId);

					return StockQueryDTO.WarehouseAggregationRes.builder()
							.warehouseId(warehouseId)
							.warehouseName(warehouseName)
							.totalQuantity(summary.getTotalQuantity().intValue())
							.capacity(capacity)
							.wareTypeCount(summary.getWareTypeCount().intValue())
							.stockList(List.of())
							.build();
				})
				.filter(Objects::nonNull)
				.collect(Collectors.toList());
	}

	private List<StockQueryDTO.WareAggregationRes> createAllWareAggregationsFromCache(Map<Long, List<CachedStockInfo>> wareStocks) {
		return wareStocks.entrySet().stream()
				.map(entry -> {
					Long wareId = entry.getKey();
					List<CachedStockInfo> stocks = entry.getValue();

					String wareName = wareCacheManager.getName(wareId);
					Integer totalQuantity = stocks.stream()
							.mapToInt(CachedStockInfo::quantity)
							.sum();

					return StockQueryDTO.WareAggregationRes.builder()
							.wareId(wareId)
							.wareName(wareName)
							.totalQuantity(totalQuantity)
							.warehouseCount(stocks.size())
							.stockList(List.of()) // 목록은 개별 조회에서만 제공
							.build();
				})
				.collect(Collectors.toList());
	}

	private List<StockQueryDTO.WareAggregationRes> createAllWareAggregationsFromDB() {
		return stockRepository.findAllWareStockSummaries().stream()
				.map(summary -> {
					Long wareId = summary.getWareId();
					String wareName = wareCacheManager.getName(wareId);

					return StockQueryDTO.WareAggregationRes.builder()
							.wareId(wareId)
							.wareName(wareName)
							.totalQuantity(summary.getTotalQuantity().intValue())
							.warehouseCount(summary.getWarehouseCount().intValue())
							.stockList(List.of())
							.build();
				})
				.collect(Collectors.toList());
	}

	// === Redis SCAN Helper Methods ===

	/**
	 * 전체 재고 현황 조회 - Redis SCAN으로 모든 재고 캐시 조회
	 */
	public List<CachedStockInfo> getAllStocksFromCache() {
		List<CachedStockInfo> result = new ArrayList<>();

		try {
			ScanOptions options = ScanOptions.scanOptions()
					.match("current_stock:*")  // 실제 키 패턴에 맞게 수정
					.count(1000)
					.build();

			try (Cursor<String> cursor = redisTemplate.scan(options)) {
				while (cursor.hasNext()) {
					String key = cursor.next();
					Integer quantity = (Integer) redisTemplate.opsForValue().get(key);

					if (quantity != null && quantity > 0) {
						parseCacheKey(key, quantity).ifPresent(result::add);
					}
				}
			}
		} catch (Exception e) {
			log.error("캐시에서 전체 재고 조회 실패", e);
		}

		return result;
	}

	public Map<Long, List<CachedStockInfo>> getStocksGroupedByWarehouse() {
		return getAllStocksFromCache().stream()
				.collect(Collectors.groupingBy(CachedStockInfo::warehouseId));
	}

	public Map<Long, List<CachedStockInfo>> getStocksGroupedByWare() {
		return getAllStocksFromCache().stream()
				.collect(Collectors.groupingBy(CachedStockInfo::wareId));
	}

	/**
	 * Redis SCAN으로 특정 창고의 모든 재고 조회
	 * 키 패턴: current_stock:warehouseId:wareId
	 */
	Map<Long, Integer> scanWarehouseStocksFromCache(Long warehouseId) {
		return scanStocksFromCache(String.format("current_stock:%d:*", warehouseId), 2);
	}

	/**
	 * Redis SCAN 공통 로직 (창고별 조회용)
	 * current_stock:warehouseId:wareId 패턴
	 */
	private Map<Long, Integer> scanStocksFromCache(String pattern, int targetIndex) {
		Map<Long, Integer> result = new HashMap<>();

		try {
			ScanOptions options = ScanOptions.scanOptions()
					.match(pattern)
					.count(1000)
					.build();

			try (Cursor<String> cursor = redisTemplate.scan(options)) {
				while (cursor.hasNext()) {
					String key = cursor.next();
					Integer quantity = (Integer) redisTemplate.opsForValue().get(key);

					if (quantity != null && quantity > 0) {
						String[] parts = key.split(":");
						if (parts.length == 3) {
							try {
								Long targetId = Long.parseLong(parts[targetIndex]);
								result.put(targetId, quantity);
							} catch (NumberFormatException e) {
								log.warn("Invalid cache key format: {}", key);
							}
						}
					}
				}
			}
		} catch (Exception e) {
			log.error("재고 캐시 SCAN 실패 - pattern: {}", pattern, e);
		}

		return result;
	}

	/**
	 * 캐시 키 파싱 헬퍼
	 * current_stock:warehouseId:wareId 패턴
	 */
	private Optional<CachedStockInfo> parseCacheKey(String key, Integer quantity) {
		String[] parts = key.split(":");
		if (parts.length == 3 && "current_stock".equals(parts[0])) {
			try {
				Long warehouseId = Long.parseLong(parts[1]);
				Long wareId = Long.parseLong(parts[2]);
				return Optional.of(new CachedStockInfo(wareId, warehouseId, quantity));
			} catch (NumberFormatException e) {
				log.warn("Invalid cache key format: {}", key);
			}
		}
		return Optional.empty();
	}

	// === Record Classes ===

	/**
	 * 캐시에서 조회한 재고 정보
	 */
	public record CachedStockInfo(
			Long wareId,
			Long warehouseId,
			Integer quantity
	) {}

}