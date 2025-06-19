package com.wms.infra.idnameMapCashing;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class AbstractDomainCacheManager<ID, NAME> implements DomainCacheManager<ID, NAME> {
	protected final Map<ID, NAME> cache = new ConcurrentHashMap<>();

	@Override
	public NAME getName(ID id) {
		return cache.get(id);
	}

	@Override
	public boolean exists(ID id) {
		return cache.containsKey(id);
	}

	@Override
	public void handleCreated(ID id, NAME name) {
		cache.put(id, name);
	}

	@Override
	public void handleUpdated(ID id, NAME name) {
		cache.put(id, name);
	}

	@Override
	public void handleDeleted(ID id) {
		cache.remove(id);
	}
}

