package com.wms.location.domain.event;

import com.wms.Infra.domain.ReferenceEvent;

public class LocationCreatedEvent extends ReferenceEvent {
	public LocationCreatedEvent(Long id, String name) {
		super(id, name);
	}
}
