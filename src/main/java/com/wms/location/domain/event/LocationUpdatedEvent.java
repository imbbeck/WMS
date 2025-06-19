package com.wms.location.domain.event;

import com.wms.infra.idnameMapCashing.ReferenceEvent;

public class LocationUpdatedEvent extends ReferenceEvent {
	public LocationUpdatedEvent(Long id, String name) {
		super(id, name);
	}
}
