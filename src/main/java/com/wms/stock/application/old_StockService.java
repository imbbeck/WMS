//package com.wms.stock.application;
//
//import java.util.List;
//import java.util.Objects;
//import java.util.Set;
//import java.util.concurrent.TimeUnit;
//
//import com.wms.location.domain.model.Location;
//import com.wms.location.domain.repository.LocationRepository;
////import com.wms.logisticTemplate.domain.event.MovementCompletedEvent;
////import com.wms.logisticTemplate.domain.event.MovementStartedEvent;
//import com.wms.stock.domain.exception.StockExceptions;
//import com.wms.stock.domain.model.Stock;
//import com.wms.stock.domain.repository.StockRepository;
//import com.wms.ware.domain.model.Ware;
//import com.wms.ware.domain.repository.WareRepository;
//import jakarta.annotation.PostConstruct;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.cache.CacheManager;
//import org.springframework.data.redis.core.RedisTemplate;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//@Slf4j
//@Service
//@RequiredArgsConstructor
//@Transactional(readOnly = true)
//public class StockService {
//
//	private static final String CACHE_KEY_PREFIX = "currentStock:";
//	private static final String WAREHOUSE_STATUS_PREFIX = "warehouseStatus:";
//	private static final long[] RETRY_DELAYS = {100, 300, 700, 1500, 3000};
//	private static final int MAX_RETRIES = 5;
//	private static final long CACHE_TTL = 30; // 30분
//
//	private final RedisTemplate<String, Object> redisTemplate;
//	private final StockRepository stockRepository;
//	private final WareRepository wareRepository;
//	private final LocationRepository locationRepository;
//	private final CacheManager cacheManager;
//
//	@PostConstruct
//	@Transactional(readOnly = true)
//	public void initializeCache() {
//		List<Stock> allStocks = stockRepository.findAll();
//		var cache = cacheManager.getCache("currentStock");
//
//		if (cache != null) {
//			allStocks.forEach(stock ->
//					cache.put(
//							stock.getLocation().getId() + ":" + stock.getWare().getId(),
//							stock.getQuantity()
//					)
//			);
//		}
//	}
//
//	private String getCacheKey(Long locationId, Long wareId) {
//		return CACHE_KEY_PREFIX + locationId + ":" + wareId;
//	}
//
//	private String getWarehouseStatusKey(Long locationId) {
//		return WAREHOUSE_STATUS_PREFIX + locationId;
//	}
//
//	public Stock getStock(Long id) {
//		return stockRepository.findById(id)
//				.orElseThrow(() -> new StockExceptions("존재하지 않는 재고입니다: " + id));
//	}
//
//	public List<Stock> getAllStocks() {
//		return stockRepository.findAll();
//	}
//
//	public Integer getStockQuantity(Long locationId, Long wareId) {
//		String cacheKey = getCacheKey(locationId, wareId);
//		Integer cachedQuantity = (Integer) redisTemplate.opsForValue().get(cacheKey);
//
//		if (cachedQuantity != null) {
//			log.debug("캐시에서 재고 조회: locationId={}, wareId={}, quantity={}", locationId, wareId, cachedQuantity);
//			return cachedQuantity;
//		}
//
//		Integer quantity = stockRepository.findByWareIdAndLocationId(wareId, locationId)
//				.map(Stock::getQuantity)
//				.orElse(0);
//
//		redisTemplate.opsForValue().set(cacheKey, quantity, CACHE_TTL, TimeUnit.MINUTES);
//		log.debug("DB에서 재고 조회 후 캐시 저장: locationId={}, wareId={}, quantity={}", locationId, wareId, quantity);
//
//		return quantity;
//	}
//
//	public Integer getTotalQuantityByWare(Long wareId) {
//		try {
//			// Redis에서 패턴으로 키 찾기
//			Set<String> keys = redisTemplate.keys("*:" + wareId);
//
//			if (keys.isEmpty()) {
//				return stockRepository.sumQuantityByWareId(wareId);
//			}
//
//			// 해당 키들의 값을 모두 가져와서 합산
//			List<Object> values = redisTemplate.opsForValue().multiGet(keys);
//
//			return Objects.requireNonNull(values).stream()
//					.filter(Objects::nonNull)
//					.mapToInt(value -> (Integer) value)
//					.sum();
//
//		}
//		catch (Exception e) {
//			return stockRepository.sumQuantityByWareId(wareId);
//		}
//	}
//
//	public Integer getTotalQuantityByLocation(Long locationId) {
//		try {
//			// Redis에서 패턴으로 키 찾기
//			Set<String> keys = redisTemplate.keys(locationId + ":*");
//
//			if (keys.isEmpty()) {
//				return stockRepository.sumQuantityByWareId(locationId);
//			}
//
//			// 해당 키들의 값을 모두 가져와서 합산
//			List<Object> values = redisTemplate.opsForValue().multiGet(keys);
//
//			return Objects.requireNonNull(values).stream()
//					.filter(Objects::nonNull)
//					.mapToInt(value -> (Integer) value)
//					.sum();
//
//		}
//		catch (Exception e) {
//			return stockRepository.sumQuantityByLocationId(locationId);
//		}
//	}
//
//	private void createStock(Long locationId, Long wareId, Integer quantity) {
//		Ware ware = wareRepository.findById(wareId)
//				.orElseThrow(() -> new StockExceptions("존재하지 않는 물품입니다: " + wareId));
//
//		Location location = locationRepository.findById(locationId)
//				.orElseThrow(() -> new StockExceptions("존재하지 않는 장소입니다: " + locationId));
//
//		if (stockRepository.existsByWareAndLocation(ware, location)) {
//			throw new StockExceptions("이미 해당 장소에 재고가 존재합니다.");
//		}
//
//		stockRepository.save(Stock.create(ware, location, quantity));
//
//		// 캐시에 직접 추가
//		var cache = cacheManager.getCache("currentStock");
//		if (cache != null) {
//			cache.put(
//					locationId + ":" + wareId, quantity
//			);
//		}
//	}
//
////	@EventListener
////	@Transactional
////	public void handleMovementStarted(MovementStartedEvent event) {
////		log.info("물류이동 시작 이벤트 처리: {}", event.getMovementId());
////
////		int retry = 0;
////		while (retry < MAX_RETRIES) {
////			try {
////				Stock fromStock = stockRepository.findByWareIdAndLocationId(event.getWareId(), event.getFromLocationId())
////						.orElseThrow(() -> new StockExceptions.InsufficientStockExceptions(
////								event.getWareId(), event.getFromLocationId(), event.getQuantity(), 0));
////
////				if (fromStock.getQuantity() < event.getQuantity()) {
////					throw new StockExceptions.InsufficientStockExceptions(
////							event.getWareId(), event.getFromLocationId(), event.getQuantity(), fromStock.getQuantity());
////				}
////
////				fromStock.subtractQuantity(event.getQuantity());
////				stockRepository.save(fromStock);
////
////				// 캐시 업데이트
////				String cacheKey = getCacheKey(event.getFromLocationId(), event.getWareId());
////				redisTemplate.opsForValue().set(cacheKey, fromStock.getQuantity(), CACHE_TTL, TimeUnit.MINUTES);
////
////				log.info("재고 차감 완료: locationId={}, wareId={}, quantity={}",
////						event.getFromLocationId(), event.getWareId(), fromStock.getQuantity());
////				return;
////			}
////			catch (OptimisticLockingFailureException e) {
////				log.warn("Optimistic locking failed for movement {} (attempt {}/{})",
////						event.getMovementId(), retry + 1, MAX_RETRIES);
////				if (retry == MAX_RETRIES - 1) {
////					throw new StockExceptions("재고 처리 중 충돌이 발생했습니다. 다시 시도해주세요.");
////				}
////				try {
////					Thread.sleep(RETRY_DELAYS[retry]);
////				}
////				catch (InterruptedException ie) {
////					Thread.currentThread().interrupt();
////					throw new StockExceptions("재고 처리 중 중단되었습니다.");
////				}
////				retry++;
////			}
////		}
////	}
////
////	@EventListener
////	@Transactional
////	public void handleMovementCompleted(MovementCompletedEvent event) {
////		log.info("물류이동 완료 이벤트 처리: {}", event.getMovementId());
////
////		int retry = 0;
////		while (retry < MAX_RETRIES) {
////			try {
////				Location toLocation = locationRepository.findById(event.getToLocationId())
////						.orElseThrow(() -> new IllegalArgumentException("존재하지 않는 장소입니다: " + event.getToLocationId()));
////
////				// 창고인 경우 용량 검증
////				if (toLocation.getType() == LocationType.WAREHOUSE) {
////					int currentQuantity = getTotalQuantityByLocation(event.getToLocationId());
////					if (currentQuantity + event.getQuantity() > toLocation.getCapacity()) {
////						throw new StockExceptions.WarehouseCapacityExceededExceptions(
////								event.getToLocationId(), event.getQuantity(), toLocation.getCapacity());
////					}
////				}
////
////				try {
////					Stock toStock = stockRepository.findByWareIdAndLocationId(event.getWareId(), event.getToLocationId())
////							.orElseThrow(() -> new IllegalArgumentException("도착 장소에 재고가 존재하지 않습니다."));
////					toStock.addQuantity(event.getQuantity());
////					stockRepository.save(toStock);
////
////					// 캐시 업데이트
////					String cacheKey = getCacheKey(event.getToLocationId(), event.getWareId());
////					redisTemplate.opsForValue().set(cacheKey, toStock.getQuantity(), CACHE_TTL, TimeUnit.MINUTES);
////
////					log.info("재고 증가 완료: locationId={}, wareId={}, quantity={}",
////							event.getToLocationId(), event.getWareId(), toStock.getQuantity());
////				}
////				catch (IllegalArgumentException e) {
////					// 도착 장소에 재고가 없는 경우 새로 생성
////					createStock(event.getToLocationId(), event.getWareId(), event.getQuantity());
////				}
////				return;
////			}
////			catch (OptimisticLockingFailureException e) {
////				log.warn("Optimistic locking failed for movement {} (attempt {}/{})",
////						event.getMovementId(), retry + 1, MAX_RETRIES);
////				if (retry == MAX_RETRIES - 1) {
////					throw new StockExceptions("재고 처리 중 충돌이 발생했습니다. 다시 시도해주세요.");
////				}
////				try {
////					Thread.sleep(RETRY_DELAYS[retry]);
////				}
////				catch (InterruptedException ie) {
////					Thread.currentThread().interrupt();
////					throw new StockExceptions("재고 처리 중 중단되었습니다.");
////				}
////				retry++;
////			}
////		}
////	}
//}