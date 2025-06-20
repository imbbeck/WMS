package com.wms.z.domain.event;

import com.wms.applicationInfra.idnameMapCashing.ReferenceEvent;

public class EntityDeletedEvent extends ReferenceEvent {
	public EntityDeletedEvent(Long id) {
		super(id);
	}
}
