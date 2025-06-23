package com.wms.location.cashing;

import java.time.Duration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class LocationCacheService {

	private final RedisTemplate<String, Object> redisTemplate;

	private static final long CACHE_TTL_HOURS = 24;

	// 캐시 키 패턴 warehouse:{warehouseId}:capacity = capacity
	private String getWarehouseCapacityCacheKey(Long warehouseId) {
		return String.format("warehouse:%d", warehouseId) + ":capacity ";
	}
	// 캐시 키 패턴 location_id_name:{locationId} = capacity
	private String getLocationNameCacheKey(Long locationId) {
		return String.format("location_id_name:%d", locationId);
	}

	/**
	 * 창고 용량 캐시 업데이트
	 * @param warehouseId 창고 ID
	 * @param capacity 창고 용량
	 */
	public void updateWarehouseCapacityCache(Long warehouseId, Integer capacity) {
		try {
			String key = getWarehouseCapacityCacheKey(warehouseId);
			redisTemplate.opsForValue().set(key, capacity, Duration.ofHours(24));
			log.debug("Updated warehouse capacity cache - warehouseId: {}, capacity: {}",
					warehouseId, capacity);
		} catch (Exception e) {
			log.error("Failed to update warehouse capacity cache - warehouseId: {}",
					warehouseId, e);
		}
	}

	/**
	 * 창고 용량 캐시 조회
	 * @param warehouseId 창고 ID
	 * @return 창고 용량, 캐시가 없으면 null 반환
	 */
	public Integer getWarehouseCapacityFromCache(Long warehouseId) {
		try {
			String key = getWarehouseCapacityCacheKey(warehouseId);
			Object value = redisTemplate.opsForValue().get(key);
			return value != null ? (Integer) value : null;
		} catch (Exception e) {
			log.error("Failed to get warehouse capacity from cache - warehouseId: {}",
					warehouseId, e);
			return null;
		}
	}

	/**
	 * 장소명 캐시 업데이트
	 * @param locationId 장소 ID
	 * @param name 장소명
	 */
	public void updateLocationNameCache(Long locationId, String name) {
		try {
			String key = getLocationNameCacheKey(locationId);
			redisTemplate.opsForValue().set(key, name, Duration.ofHours(24));
			log.debug("Updated location name cache - locationId: {}, name: {}", locationId, name);
		} catch (Exception e) {
			log.error("Failed to update location name cache - locationId: {}", locationId, e);
		}
	}

	/**
	 * 장소명 캐시 조회
	 * @param locationId 장소 ID
	 * @return 장소명, 캐시가 없으면 null 반환
	 */
	public String getLocationNameFromCache(Long locationId) {
		try {
			String key = getLocationNameCacheKey(locationId);
			Object value = redisTemplate.opsForValue().get(key);
			return value != null ? (String) value : null;
		} catch (Exception e) {
			log.error("Failed to get location name from cache - locationId: {}", locationId, e);
			return null;
		}
	}

	/**
	 * 장소 캐시 무효화
	 * @param locationId 장소 ID
	 */
	public void invalidateLocationCaches(Long locationId) {
		try {
			// 장소명 캐시 무효화
			String nameKey = getLocationNameCacheKey(locationId);
			redisTemplate.delete(nameKey);

			// 창고인 경우 용량 캐시도 무효화
			String capacityKey = getLocationNameCacheKey(locationId);
			redisTemplate.delete(capacityKey);

			log.debug("Invalidated location caches - locationId: {}", locationId);
		} catch (Exception e) {
			log.error("Failed to invalidate location caches - locationId: {}", locationId, e);
		}
	}
}
