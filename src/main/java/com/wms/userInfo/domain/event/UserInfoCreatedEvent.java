package com.wms.userInfo.domain.event;

import com.wms.applicationInfra.idnameMapCashing.ReferenceEvent;

public class UserInfoCreatedEvent extends ReferenceEvent {
	public UserInfoCreatedEvent(Long id, String name) {
		super(id, name);
	}
}
