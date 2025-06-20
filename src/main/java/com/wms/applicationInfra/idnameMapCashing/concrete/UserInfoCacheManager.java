package com.wms.applicationInfra.idnameMapCashing.concrete;

import java.util.Map;

import static org.springframework.transaction.event.TransactionPhase.AFTER_COMMIT;
import com.wms.applicationInfra.idnameMapCashing.AbstractDomainCacheManager;
import com.wms.userInfo.domain.event.UserInfoCreatedEvent;
import com.wms.userInfo.domain.event.UserInfoDeletedEvent;
import com.wms.userInfo.domain.event.UserInfoUpdatedEvent;
import com.wms.userInfo.domain.repository.UserInfoRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class UserInfoCacheManager extends AbstractDomainCacheManager<Long, String> {
	private final UserInfoRepository userInfoRepository;

	public UserInfoCacheManager(UserInfoRepository userInfoRepository) {
		this.userInfoRepository = userInfoRepository;
	}

	@PostConstruct
	public void initialize() {
		userInfoRepository.findAll()
				.forEach(loc -> cache.put(loc.getId(), loc.getName()));
	}

	@Async
	@TransactionalEventListener(phase = AFTER_COMMIT)
	public void onCreated(UserInfoCreatedEvent event) {
		handleCreated(event.getId(), event.getName());
	}

	@Async
	@TransactionalEventListener(phase = AFTER_COMMIT)
	public void onUpdated(UserInfoUpdatedEvent event) {
		handleUpdated(event.getId(), event.getName());
	}

	@Async
	@TransactionalEventListener(phase = AFTER_COMMIT)
	public void onDeleted(UserInfoDeletedEvent event) {
		handleDeleted(event.getId());
	}

	public Map<Long, String> getIdNamePair() {
		return this.cache;
	}
}
