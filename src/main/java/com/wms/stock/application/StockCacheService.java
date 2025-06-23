package com.wms.stock.application;

import static org.springframework.transaction.event.TransactionPhase.AFTER_COMMIT;
import com.wms.stock.domain.event.StockCreatedEvent;
import com.wms.stock.domain.event.StockDeletedEvent;
import com.wms.stock.domain.event.StockUpdatedEvent;
import com.wms.stock.domain.model.Stock;
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
	public Integer getInventoryQuantity(Long wareId, Long warehouseId) {
		log.debug("Cache miss - querying DB for wareId: {}, warehouseId: {}", wareId, warehouseId);
		return stockRepository.findByWarehouseIdAndWareId(warehouseId, wareId)
				.map(Stock::getQuantity)
				.orElse(0);
	}

	/**
	 * 개별 재고 갱신
	 */
	@CachePut(value = "stockInventory", keyGenerator = "stockCacheKeyGenerator")
	public Integer updateInventoryQuantity(Long wareId, Long warehouseId, Integer quantity) {
		log.debug("Updated inventory cache - wareId: {}, warehouseId: {}, quantity: {}",
				wareId, warehouseId, quantity);
		return quantity;
	}

	/**
	 * 개별 재고 캐시 increment (Redis 직접 사용)
	 */
	public void incrementStockCache(Long wareId, Long warehouseId, Integer increment) {
		try {
			String key = String.format("current_stock:%d:%d", warehouseId, wareId);
			redisTemplate.opsForValue().increment(key, increment);
			log.debug("Incremented stock cache - key: {}, increment: {}", key, increment);
		} catch (Exception e) {
			log.error("Failed to increment stock cache - warehouseId: {}, wareId: {}, increment: {}",
					warehouseId, wareId, increment, e);
		}
	}

	/**
	 * 개별 재고 캐시 decrement (Redis 직접 사용)
	 */
	public void decrementStockCache(Long wareId, Long warehouseId, Integer decrement) {
		try {
			String key = String.format("current_stock:%d:%d", warehouseId, wareId);
			redisTemplate.opsForValue().increment(key, -decrement);
			log.debug("Decremented stock cache - key: {}, decrement: {}", key, decrement);
		} catch (Exception e) {
			log.error("Failed to decrement stock cache - warehouseId: {}, wareId: {}, decrement: {}",
					warehouseId, wareId, decrement, e);
		}
	}

	/**
	 * 개별 재고 캐시 삭제
	 */
	@CacheEvict(value = "stockInventory", keyGenerator = "stockCacheKeyGenerator")
	public void invalidateInventoryQuantity(Long wareId, Long warehouseId) {
		log.debug("Invalidated inventory cache - wareId: {}, warehouseId: {}", wareId, warehouseId);
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
		log.debug("StockCreatedEvent - updating cache for wareId: {}, warehouseId: {}",
				event.getWareId(), event.getWarehouseId());

		// 개별 재고 캐시 갱신
		updateInventoryQuantity(event.getWareId(), event.getWarehouseId(), event.getQuantity());

		// 창고 총 사용량 캐시 증가
		incrementWarehouseCurrentSumCache(event.getWarehouseId(), event.getQuantity());
	}

	// 이벤트 리스너 - 재고 수정 이벤트 처리
	@TransactionalEventListener(phase = AFTER_COMMIT)
	public void onStockUpdated(StockUpdatedEvent event) {
		log.debug("StockUpdatedEvent - updating cache for wareId: {}, warehouseId: {}, oldQuantity: {}, newQuantity: {}",
				event.getWareId(), event.getWarehouseId(), event.getOldQuantity(), event.getNewQuantity());

		// 개별 재고 캐시 갱신
		updateInventoryQuantity(event.getWareId(), event.getWarehouseId(), event.getNewQuantity());

		// 창고 총 사용량 캐시 증감 계산
		int quantityDifference = event.getNewQuantity() - event.getOldQuantity();

		if (quantityDifference > 0) {
			// 재고 증가
			incrementWarehouseCurrentSumCache(event.getWarehouseId(), quantityDifference);
		} else if (quantityDifference < 0) {
			// 재고 감소
			decrementWarehouseCurrentSumCache(event.getWarehouseId(), Math.abs(quantityDifference));
		}
		// quantityDifference == 0 이면 아무것도 안함
	}

	// 이벤트 리스너 - 재고 삭제 이벤트 처리
	@TransactionalEventListener(phase = AFTER_COMMIT)
	public void onStockDeleted(StockDeletedEvent event) {
		log.debug("StockDeletedEvent - invalidating cache for wareId: {}, warehouseId: {}, quantity: {}",
				event.getWareId(), event.getWarehouseId(), event.getQuantity());

		// 개별 재고 캐시 무효화
		invalidateInventoryQuantity(event.getWareId(), event.getWarehouseId());

		// 창고 총 사용량 캐시 감소 (무효화 X, 감소 O)
		decrementWarehouseCurrentSumCache(event.getWarehouseId(), event.getQuantity());
	}


}