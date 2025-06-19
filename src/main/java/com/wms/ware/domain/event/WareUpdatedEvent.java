package com.wms.ware.domain.event;

import com.wms.infra.idnameMapCashing.ReferenceEvent;

public class WareUpdatedEvent extends ReferenceEvent {
	public WareUpdatedEvent(Long id, String name) {
		super(id, name);
	}
}
