package com.wms.location.domain.event;

import com.wms.infra.idnameMapCashing.ReferenceEvent;

public class LocationDeletedEvent extends ReferenceEvent {
	public LocationDeletedEvent(Long id) {
		super(id);
	}
}
