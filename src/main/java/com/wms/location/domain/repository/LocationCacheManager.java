package com.wms.location.domain.repository;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.wms.location.domain.model.LocationType;
import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class LocationCacheManager {

	private final LocationRepository locationRepository;
	private final Map<Long, LocationInfo> locationCache = new ConcurrentHashMap<>();

	@Data
	@AllArgsConstructor
	public static class LocationInfo {
		private String name;
		private LocationType type;
	}

	@PostConstruct
	public void initializeCache() {
		log.info("Location 캐시 초기화 시작...");
		loadAllLocations();
		log.info("Location 캐시 초기화 완료. 총 {}개 항목", locationCache.size());
	}

	private void loadAllLocations() {
		locationRepository.findAll().forEach(location -> {
			locationCache.put(location.getId(),
					new LocationInfo(location.getName(), location.getType()));
		});
	}

	// Walk-through: CUD 작업 시 즉시 캐시 갱신
	public void updateCache(Long id, String name, LocationType type) {
		locationCache.put(id, new LocationInfo(name, type));
		log.debug("Location 캐시 업데이트: ID={}, Name={}", id, name);
	}

	public void removeFromCache(Long id) {
		locationCache.remove(id);
		log.debug("Location 캐시에서 제거: ID={}", id);
	}

	public String getName(Long id) {
		LocationInfo info = locationCache.get(id);
		return info != null ? info.getName() : "Unknown";
	}

	public LocationType getType(Long id) {
		LocationInfo info = locationCache.get(id);
		return info != null ? info.getType() : null;
	}

	public boolean exists(Long id) {
		return locationCache.containsKey(id);
	}
}
