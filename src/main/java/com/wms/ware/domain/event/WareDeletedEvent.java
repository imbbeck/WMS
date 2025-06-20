package com.wms.ware.domain.event;

import com.wms.applicationInfra.idnameMapCashing.ReferenceEvent;

public class WareDeletedEvent extends ReferenceEvent {
	public WareDeletedEvent(Long id) {
		super(id);
	}
}
