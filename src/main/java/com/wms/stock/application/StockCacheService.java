package com.wms.stock.application;

import static org.springframework.transaction.event.TransactionPhase.AFTER_COMMIT;
import com.wms.stock.domain.event.StockCreatedEvent;
import com.wms.stock.domain.event.StockDeletedEvent;
import com.wms.stock.domain.event.StockUpdatedEvent;
import com.wms.stock.domain.model.Stock;
import com.wms.stock.domain.model.StockKey;
import com.wms.stock.domain.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionalEventListener;

@Service
@RequiredArgsConstructor
@Slf4j
public class StockCacheService {

	private final StockRepository stockRepository;
	private final RedisTemplate<String, Object> redisTemplate;

	/**
	 * 개별 재고 조회 (캐시 조회 후 miss 시 DB 조회)
	 */
	@Cacheable(value = "stockInventory", keyGenerator = "stockCacheKeyGenerator", unless = "#result == 0") // 0은 캐싱 안함
	public Integer getInventoryQuantity(StockKey stockKey) {
		log.debug("Cache miss - querying DB for {}", stockKey.toString());
		return stockRepository.findByKey(stockKey)
				.map(Stock::getQuantity)
				.orElse(0); // 재고가 없으면 null 반환
	}

	/**
	 * 개별 재고 갱신
	 */
	@CachePut(value = "stockInventory", keyGenerator = "stockCacheKeyGenerator")
	public Integer updateInventoryQuantity(StockKey stockKey, Integer quantity) {
		log.debug("Updated inventory cache - {}, quantity: {}",	stockKey.toString(), quantity);
		return quantity;
	}

	/**
	 * 개별 재고 캐시 increment (Redis 직접 사용)
	 */
	public void incrementStockCache(StockKey stockKey, Integer increment) {
		try {
			String key = stockKey.toCacheKey();
			redisTemplate.opsForValue().increment(key, increment);
			log.debug("Incremented stock cache - key: {}, increment: {}", key, increment);
		} catch (Exception e) {
			log.error("Failed to increment stock cache - {}, increment: {}", stockKey.toString(), increment, e);
		}
	}

	/**
	 * 개별 재고 캐시 decrement (Redis 직접 사용)
	 */
	public void decrementStockCache(StockKey stockKey, Integer decrement) {
		try {
			String key = stockKey.toCacheKey();
			redisTemplate.opsForValue().increment(key, -decrement);
			log.debug("Decremented stock cache - key: {}, decrement: {}", key, decrement);
		} catch (Exception e) {
			log.error("Failed to decrement stock cache - {}, increment: {}", stockKey.toString(), decrement, e);
		}
	}

	/**
	 * 개별 재고 캐시 삭제
	 */
	@CacheEvict(value = "stockInventory", keyGenerator = "stockCacheKeyGenerator")
	public void invalidateInventoryQuantity(StockKey stockKey) {
		log.debug("Invalidated inventory cache - {}", stockKey.toString());
	}

	/**
	 * 창고 총 사용량 조회 (캐시 조회 후 miss 시 DB 조회)
	 */
	@Cacheable(value = "warehouseCurrentSum", keyGenerator = "stockCacheKeyGenerator", unless = "#result == 0") // 0은 캐싱 안함
	public Integer getWarehouseCurrentSum(Long warehouseId) {
		log.debug("Cache miss - querying DB for warehouseId: {}", warehouseId);
		return stockRepository.getTotalPaletteCountByWarehouseId(warehouseId);
	}

	/**
	 * 창고 총 사용량 갱신
	 */
	@CachePut(value = "warehouseCurrentSum", keyGenerator = "stockCacheKeyGenerator")
	public Integer updateWarehouseCurrentSum(Long warehouseId, Integer currentSum) {
		log.debug("Updated warehouse current sum cache - warehouseId: {}, currentSum: {}",
				warehouseId, currentSum);
		return currentSum;
	}

	/**
	 * 창고 총 사용량 캐시 increment (Redis 직접 사용)
	 */
	public void incrementWarehouseCurrentSumCache(Long warehouseId, Integer increment) {
		try {
			String key = String.format("warehouse:%d:currentSum", warehouseId);
			redisTemplate.opsForValue().increment(key, increment);
			log.debug("Incremented warehouse current sum cache - warehouseId: {}, increment: {}",
					warehouseId, increment);
		} catch (Exception e) {
			log.error("Failed to increment warehouse current sum cache - warehouseId: {}, increment: {}",
					warehouseId, increment, e);
		}
	}

	/**
	 * 창고 총 사용량 캐시 decrement (Redis 직접 사용)
	 */
	public void decrementWarehouseCurrentSumCache(Long warehouseId, Integer decrement) {
		try {
			String key = String.format("warehouse:%d:currentSum", warehouseId);
			redisTemplate.opsForValue().increment(key, -decrement);
			log.debug("Decremented warehouse current sum cache - warehouseId: {}, decrement: {}",
					warehouseId, decrement);
		} catch (Exception e) {
			log.error("Failed to decrement warehouse current sum cache - warehouseId: {}, decrement: {}",
					warehouseId, decrement, e);
		}
	}

	/**
	 * 창고 총 사용량 캐시 삭제 (재계산 필요)
	 */
	@CacheEvict(value = "warehouseCurrentSum", keyGenerator = "stockCacheKeyGenerator")
	public void invalidateWarehouseCurrentSum(Long warehouseId) {
		log.debug("Invalidated warehouse current sum cache - warehouseId: {}", warehouseId);
	}



	// 이벤트 리스너 - 재고 생성 이벤트 처리
	@TransactionalEventListener(phase = AFTER_COMMIT)
	public void onStockCreated(StockCreatedEvent event) {
		log.debug("StockCreatedEvent - updating cache for {}", event.getKey().toString());

		// 개별 재고 캐시 갱신
		updateInventoryQuantity(event.getKey(), event.getQuantity());

		// 테스트용 보완: Redis 직접 설정
		try {
			String key = event.getKey().toCacheKey();
			redisTemplate.opsForValue().set(key, event.getQuantity());
			log.debug("Backup: Manually set stock cache - key: {}, quantity: {}", key, event.getQuantity());
		} catch (Exception e) {
			log.warn("Failed to manually set stock cache", e);
		}

		// 창고 총 사용량 캐시 증가
		incrementWarehouseCurrentSumCache(event.getKey().getWarehouseId(), event.getQuantity());
	}

	// 이벤트 리스너 - 재고 수정 이벤트 처리
	@TransactionalEventListener(phase = AFTER_COMMIT)
	public void onStockUpdated(StockUpdatedEvent event) {
		log.debug("StockUpdatedEvent - updating cache for {}, oldQuantity: {}, newQuantity: {}",
				event.getKey(), event.getOldQuantity(), event.getNewQuantity());

		// 개별 재고 캐시 갱신
		updateInventoryQuantity(event.getKey(), event.getNewQuantity());

		// 테스트용 보완: Redis 직접 갱신 (확실한 캐시 업데이트 보장)
		try {
			String key = event.getKey().toCacheKey();
			redisTemplate.opsForValue().set(key, event.getNewQuantity());
			log.debug("Backup: Manually updated stock cache - key: {}, quantity: {}", key, event.getNewQuantity());
		} catch (Exception e) {
			log.warn("Failed to manually update stock cache", e);
		}

		// 창고 총 사용량 캐시 증감 계산
		int quantityDifference = event.getNewQuantity() - event.getOldQuantity();

		if (quantityDifference > 0) {
			// 재고 증가
			incrementWarehouseCurrentSumCache(event.getKey().getWarehouseId(), quantityDifference);
		} else if (quantityDifference < 0) {
			// 재고 감소
			decrementWarehouseCurrentSumCache(event.getKey().getWarehouseId(), Math.abs(quantityDifference));
		}
		// quantityDifference == 0 이면 아무것도 안함
	}

	// 이벤트 리스너 - 재고 삭제 이벤트 처리
	@TransactionalEventListener(phase = AFTER_COMMIT)
	public void onStockDeleted(StockDeletedEvent event) {
		log.debug("StockDeletedEvent - invalidating cache for {}, quantity: {}",
				event.getKey(), event.getQuantity());

		// 1. 캐시 완전 삭제 (0을 캐시하는 대신)
		invalidateInventoryQuantity(event.getKey());
		try {
			String key = event.getKey().toCacheKey();
			redisTemplate.delete(key);  // Redis에서 완전 삭제
			log.debug("Completely removed stock cache - key: {}", key);
		} catch (Exception e) {
			log.warn("Failed to remove stock cache", e);
		}

		// 2. 창고 총량 처리 (개선된 로직)
		String warehouseKey = String.format("warehouse:%d:currentSum", event.getKey().getWarehouseId());
		Integer currentWarehouseSum = (Integer) redisTemplate.opsForValue().get(warehouseKey);

		if (currentWarehouseSum != null && currentWarehouseSum >= event.getQuantity()) {
			decrementWarehouseCurrentSumCache(event.getKey().getWarehouseId(), event.getQuantity());
		} else {
			// 이미 차감되었거나 불일치 상황
			log.warn("Warehouse sum inconsistency detected - resetting to DB value");
			// 창고 총량 캐시 무효화 -> 다음 조회 시 DB에서 재계산
			try {
				redisTemplate.delete(warehouseKey);
			} catch (Exception e) {
				log.error("Failed to invalidate warehouse sum cache", e);
			}
		}
	}


}