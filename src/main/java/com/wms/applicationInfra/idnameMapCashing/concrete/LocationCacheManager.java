package com.wms.infra.idnameMapCashing.concrete;

import java.util.Map;

import static org.springframework.transaction.event.TransactionPhase.AFTER_COMMIT;
import com.wms.infra.idnameMapCashing.AbstractDomainCacheManager;
import com.wms.location.domain.event.LocationCreatedEvent;
import com.wms.location.domain.event.LocationDeletedEvent;
import com.wms.location.domain.event.LocationUpdatedEvent;
import com.wms.location.domain.repository.LocationRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class LocationCacheManager extends AbstractDomainCacheManager<Long, String> {
	private final LocationRepository locationRepository;

	public LocationCacheManager(LocationRepository locationRepository) {
		this.locationRepository = locationRepository;
	}

	@PostConstruct
	public void initialize() {
		locationRepository.findAll()
				.forEach(loc -> cache.put(loc.getId(), loc.getName()));
	}

	@Async
	@TransactionalEventListener(phase = AFTER_COMMIT)
	public void onCreated(LocationCreatedEvent event) {
		handleCreated(event.getId(), event.getName());
	}

	@Async
	@TransactionalEventListener(phase = AFTER_COMMIT)
	public void onUpdated(LocationUpdatedEvent event) {
		handleUpdated(event.getId(), event.getName());
	}

	@Async
	@TransactionalEventListener(phase = AFTER_COMMIT)
	public void onDeleted(LocationDeletedEvent event) {
		handleDeleted(event.getId());
	}

	public Map<Long, String> getIdNamePair() {
		return this.cache;
	}
}
