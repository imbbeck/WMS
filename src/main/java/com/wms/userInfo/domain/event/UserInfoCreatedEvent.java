package com.wms.z.domain.event;

import com.wms.applicationInfra.idnameMapCashing.ReferenceEvent;

public class EntityCreatedEvent extends ReferenceEvent {
	public EntityCreatedEvent(Long id, String name) {
		super(id, name);
	}
}
