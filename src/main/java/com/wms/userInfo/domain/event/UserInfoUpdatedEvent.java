package com.wms.userInfo.domain.event;

import com.wms.applicationInfra.idnameMapCashing.ReferenceEvent;

public class UserInfoUpdatedEvent extends ReferenceEvent {
	public UserInfoUpdatedEvent(Long id, String name) {
		super(id, name);
	}
}
