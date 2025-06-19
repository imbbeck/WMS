//package com.wms.infra.domain;
//
//import java.util.Map;
//import java.util.concurrent.ConcurrentHashMap;
//
//import com.wms.location.domain.event.LocationCreatedEvent;
//import com.wms.location.domain.event.LocationDeletedEvent;
//import com.wms.location.domain.event.LocationUpdatedEvent;
//import com.wms.location.domain.repository.LocationRepository;
//import com.wms.ware.domain.event.WareCreatedEvent;
//import com.wms.ware.domain.event.WareDeletedEvent;
//import com.wms.ware.domain.event.WareUpdatedEvent;
//import com.wms.ware.domain.repository.WareRepository;
//import jakarta.annotation.PostConstruct;
//import lombok.Getter;
//import org.springframework.context.event.EventListener;
//import org.springframework.scheduling.annotation.Async;
//import org.springframework.stereotype.Component;
//
//@Component
//public class ReferenceDataCacheManager {
//	@Getter
//	private final Map<Long, String> locationCache = new ConcurrentHashMap<>();
//	@Getter
//	private final Map<Long, String> wareCache = new ConcurrentHashMap<>();
//
//	private final LocationRepository locationRepository;
//	private final WareRepository wareRepository;
//
//	public ReferenceDataCacheManager(LocationRepository locationRepository, WareRepository wareRepository) {
//		this.locationRepository = locationRepository;
//		this.wareRepository = wareRepository;
//	}
//
//	@PostConstruct
//	public void initializeCache() {
//		loadAllLocations();
//		loadAllWares();
//	}
//
//	private void loadAllLocations() {
//		locationRepository.findAll().forEach(location -> locationCache.put(location.getId(), location.getName()));
//	}
//
//	@Async
//	@EventListener
//	public void handleLocationCreated(LocationCreatedEvent event) {
//		locationCache.put(event.getId(), event.getName());
//	}
//
//	@Async
//	@EventListener
//	public void handleLocationUpdated(LocationUpdatedEvent event) {
//		locationCache.put(event.getId(), event.getName());
//	}
//
//	@Async
//	@EventListener
//	public void handleLocationDeleted(LocationDeletedEvent event) {
//		locationCache.remove(event.getId());
//	}
//
//	public String getLocationName(Long id) {
//		return locationCache.get(id);
//	}
//
//	public boolean existsLocation (Long id) {
//		return locationCache.containsKey(id);
//	}
//
//
//	private void loadAllWares() {
//		wareRepository.findAll().forEach(ware -> wareCache.put(ware.getId(), ware.getName()));
//	}
//
//	@Async
//	@EventListener
//	public void handleWareCreated(WareCreatedEvent event) {
//		wareCache.put(event.getId(), event.getName());
//	}
//
//	@Async
//	@EventListener
//	public void handleWareUpdated(WareUpdatedEvent event) {
//		wareCache.put(event.getId(), event.getName());
//	}
//
//	@Async
//	@EventListener
//	public void handleWareDeleted(WareDeletedEvent event) {
//		wareCache.remove(event.getId());
//	}
//
//	public String getWareName(Long id) {
//		return wareCache.get(id);
//	}
//
//	public boolean existsWare (Long id) {
//		return wareCache.containsKey(id);
//	}
//
//}
