package com.wms.stock.cashing;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class StockCacheService {

	private final RedisTemplate<String, Object> redisTemplate;

	// 캐시 키 패턴
	//	current_stock:{warehouseId}:{wareId} = quantity (개별 재고)
	//	warehouse:{warehouseId}:currentSum = 현재총사용량
	private static final long CACHE_TTL_HOURS = 24;

	private String getCacheKey(Long wareId, Long warehouseId) {
		return String.format("current_stock:%d:%d", warehouseId, wareId);
	}

	private String getWarehouseCacheKey(Long warehouseId) {
		return String.format("warehouse:%d", warehouseId) + ":currentSum";
	}

	/**
	 * 개별 재고 캐시: stock-service:inventory:{warehouseId}:{wareId} = quantity
	 */
	public void updateInventoryCache(Long wareId, Long warehouseId, Integer quantity) {
		try {
			String key = getCacheKey(wareId, warehouseId);
			redisTemplate.opsForValue().set(key, quantity, Duration.ofHours(CACHE_TTL_HOURS));
			log.debug("Updated inventory cache - key: {}, quantity: {}", key, quantity);
		} catch (Exception e) {
			log.error("Failed to update inventory cache", e);
		}
	}

	/**
	 * 창고 총 사용량 캐시: warehouse:{warehouseId}:currentSum = 현재총사용량
	 */
	public void updateWarehouseCurrentSumCache(Long warehouseId, Integer currentSum) {
		try {
			String key = getWarehouseCacheKey(warehouseId);
			redisTemplate.opsForValue().set(key, currentSum, Duration.ofHours(CACHE_TTL_HOURS));
			log.debug("Updated warehouse current sum cache - warehouseId: {}, currentSum: {}",
					warehouseId, currentSum);
		} catch (Exception e) {
			log.error("Failed to update warehouse current sum cache - warehouseId: {}",
					warehouseId, e);
		}
	}

	/**
	 * 창고별 총 재고 캐시 무효화 (재계산 필요)
	 */
	public void invalidateWarehouseCache(Long warehouseId) {
		try {
			String key = getWarehouseCacheKey(warehouseId);
			redisTemplate.delete(key);
			log.debug("Invalidated warehouse cache - warehouseId: {}", warehouseId);
		} catch (Exception e) {
			log.error("Failed to invalidate warehouse cache - warehouseId: {}", warehouseId, e);
		}
	}

	/**
	 * 개별 재고 캐시 조회
	 */
	public Integer getStockFromCache(Long wareId, Long warehouseId) {
		try {
			String key = getCacheKey(wareId, warehouseId);
			Object value = redisTemplate.opsForValue().get(key);
			return value != null ? (Integer) value : null;
		} catch (Exception e) {
			log.error("Failed to get stock from cache - warehouseId: {}, wareId: {}",
					warehouseId, wareId, e);
			return null;
		}
	}

	/**
	 * 창고별 총 재고 캐시 조회
	 */
	public Integer getWarehouseTotalFromCache(Long warehouseId) {
		try {
			String key = getWarehouseCacheKey(warehouseId);
			Object value = redisTemplate.opsForValue().get(key);
			return value != null ? (Integer) value : null;
		} catch (Exception e) {
			log.error("Failed to get warehouse total from cache - warehouseId: {}",
					warehouseId, e);
			return null;
		}
	}


}