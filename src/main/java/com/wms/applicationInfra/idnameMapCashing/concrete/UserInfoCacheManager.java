package com.wms.applicationInfra.idnameMapCashing.concrete;

import java.util.Map;

import static org.springframework.transaction.event.TransactionPhase.AFTER_COMMIT;
import com.wms.applicationInfra.idnameMapCashing.AbstractDomainCacheManager;
import com.wms.ware.domain.event.WareCreatedEvent;
import com.wms.ware.domain.event.WareDeletedEvent;
import com.wms.ware.domain.event.WareUpdatedEvent;
import com.wms.ware.domain.repository.WareRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class WareCacheManager extends AbstractDomainCacheManager<Long, String> {
	private final WareRepository wareRepository;

	public WareCacheManager(WareRepository wareRepository) {
		this.wareRepository = wareRepository;
	}

	@PostConstruct
	public void initialize() {
		wareRepository.findAll()
				.forEach(loc -> cache.put(loc.getId(), loc.getName()));
	}

	@Async
	@TransactionalEventListener(phase = AFTER_COMMIT)
	public void onCreated(WareCreatedEvent event) {
		handleCreated(event.getId(), event.getName());
	}

	@Async
	@TransactionalEventListener(phase = AFTER_COMMIT)
	public void onUpdated(WareUpdatedEvent event) {
		handleUpdated(event.getId(), event.getName());
	}

	@Async
	@TransactionalEventListener(phase = AFTER_COMMIT)
	public void onDeleted(WareDeletedEvent event) {
		handleDeleted(event.getId());
	}

	public Map<Long, String> getIdNamePair() {
		return this.cache;
	}
}
