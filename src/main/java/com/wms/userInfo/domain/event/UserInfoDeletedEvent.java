package com.wms.userInfo.domain.event;

import com.wms.applicationInfra.idnameMapCashing.ReferenceEvent;

public class UserInfoDeletedEvent extends ReferenceEvent {
	public UserInfoDeletedEvent(Long id) {
		super(id);
	}
}
