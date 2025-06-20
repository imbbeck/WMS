package com.wms.z.domain.event;

import com.wms.applicationInfra.idnameMapCashing.ReferenceEvent;

public class EntityUpdatedEvent extends ReferenceEvent {
	public EntityUpdatedEvent(Long id, String name) {
		super(id, name);
	}
}
