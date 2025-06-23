package com.wms.location.domain.event;

import com.wms.applicationInfra.idnameMapCashing.ReferenceEvent;
import com.wms.location.domain.model.LocationType;
import lombok.Getter;

@Getter
public class LocationDeletedEvent extends ReferenceEvent {
	private final LocationType locationType;

	public LocationDeletedEvent(Long id, LocationType locationType) {
		super(id);
		this.locationType = locationType;

	}
}
