package com.wms.Infra.domain;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.wms.location.domain.event.LocationCreatedEvent;
import com.wms.location.domain.event.LocationDeletedEvent;
import com.wms.location.domain.event.LocationUpdatedEvent;
import com.wms.location.domain.repository.LocationRepository;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class ReferenceDataCacheManager {
	@Getter
	private final Map<Long, String> locationCache = new ConcurrentHashMap<>();
	private final LocationRepository locationRepository;

	public ReferenceDataCacheManager(LocationRepository locationRepository) {
		this.locationRepository = locationRepository;
	}

	@PostConstruct
	public void initializeCache() {
		loadAllLocations();
	}

	private void loadAllLocations() {
		locationRepository.findAll().forEach(location -> locationCache.put(location.getId(), location.getName()));
	}

	@EventListener
	public void handleLocationCreated(LocationCreatedEvent event) {
		locationCache.put(event.getId(), event.getName());
	}

	@EventListener
	public void handleLocationUpdated(LocationUpdatedEvent event) {
		locationCache.put(event.getId(), event.getName());
	}

	@EventListener
	public void handleLocationDeleted(LocationDeletedEvent event) {
		locationCache.remove(event.getId());
	}

	public String getLocationName(Long id) {
		return locationCache.get(id);
	}

	public boolean existsLocation (Long id) {
		return locationCache.containsKey(id);
	}

}
