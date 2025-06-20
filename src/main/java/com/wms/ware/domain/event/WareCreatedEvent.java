package com.wms.ware.domain.event;

import com.wms.applicationInfra.idnameMapCashing.ReferenceEvent;

public class WareCreatedEvent extends ReferenceEvent {
	public WareCreatedEvent(Long id, String name) {
		super(id, name);
	}
}
