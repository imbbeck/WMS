package com.wms.infra.idnameMapCashing;

import java.util.Map;

public interface DomainCacheManager<ID, NAME> {
	void initialize();  // 캐시 초기 로딩
	void handleCreated(ID id, NAME name);
	void handleUpdated(ID id, NAME name);
	void handleDeleted(ID id);
	NAME getName(ID id);
	boolean exists(ID id);
	Map<ID, NAME> getIdNamePair();
}
