package com.wms.location.application;

import static org.springframework.transaction.event.TransactionPhase.AFTER_COMMIT;
import com.wms.location.domain.event.LocationCreatedEvent;
import com.wms.location.domain.event.LocationDeletedEvent;
import com.wms.location.domain.event.LocationUpdatedEvent;
import com.wms.location.domain.model.LocationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionalEventListener;

@Service
@RequiredArgsConstructor
@Slf4j
public class LocationCacheService {

	private final LocationService locationService;
	/**
	 * 창고 용량 조회 (캐시 조회 후 miss 시 DB 조회)
	 */
//	@Cacheable(value = "warehouseCapacity", keyGenerator = "locationCacheKeyGenerator", unless = "#result == null")
	@Cacheable(value = "warehouseCapacity", keyGenerator = "locationCacheKeyGenerator") // RedisConfig 에서 .disableCachingNullValues() 설정으로 null 값 캐싱 방지
	public Integer getWarehouseCapacity(Long warehouseId) {
		log.debug("Cache miss - querying DB for warehouseId: {}", warehouseId);
		return locationService.getCapacityByWarehouseId(warehouseId);
	}

	/**
	 * 창고 용량 갱신
	 */
	@CachePut(value = "warehouseCapacity", keyGenerator = "locationCacheKeyGenerator")
	public Integer updateWarehouseCapacity(Long warehouseId, Integer capacity) {
		log.debug("Updated warehouse capacity - warehouseId: {}, capacity: {}", warehouseId, capacity);
		return capacity;
	}

	/**
	 * 창고 용량 캐시 삭제
	 */
	@CacheEvict(value = "warehouseCapacity", keyGenerator = "locationCacheKeyGenerator")
	public void invalidateWarehouseCapacity(Long warehouseId) {
		log.debug("Invalidated warehouse capacity cache - warehouseId: {}", warehouseId);
	}

	// 이벤트 리스너 - 생성 이벤트 처리 (캐시 갱신 or 무효화)
	@TransactionalEventListener(phase = AFTER_COMMIT)
	public void onLocationCreated(LocationCreatedEvent event) {
		if (event.getLocationType() == LocationType.WAREHOUSE) {
			log.debug("LocationCreatedEvent - WAREHOUSE → updating cache for warehouseId: {}", event.getId());
			updateWarehouseCapacity(event.getId(), event.getCapacity());
		}
	}

	// 이벤트 리스너 - 수정 이벤트 처리 (캐시 갱신)
	@TransactionalEventListener(phase = AFTER_COMMIT)
	public void onLocationUpdated(LocationUpdatedEvent event) {
		if (event.getLocationType() == LocationType.WAREHOUSE) {
			log.debug("LocationUpdatedEvent received - updating cache for warehouseId: {}", event.getId());
			updateWarehouseCapacity(event.getId(), event.getCapacity());
		}
	}

	// 이벤트 리스너 - 삭제 이벤트 처리 (캐시 무효화)
	@TransactionalEventListener(phase = AFTER_COMMIT)
	public void onLocationDeleted(LocationDeletedEvent event) {
		if (event.getLocationType() == LocationType.WAREHOUSE) {
			log.debug("LocationDeletedEvent received - invalidating cache for warehouseId: {}", event.getId());
			invalidateWarehouseCapacity(event.getId());
		}
	}

}
