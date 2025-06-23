package com.wms.location.domain.event;

import com.wms.applicationInfra.idnameMapCashing.ReferenceEvent;
import com.wms.location.domain.model.LocationType;
import lombok.Getter;

@Getter
public class LocationCreatedEvent extends ReferenceEvent {
	private final Integer capacity;
	private final LocationType locationType;

	public LocationCreatedEvent(Long id, String name, Integer capacity, LocationType locationType) {
		super(id, name);
		this.capacity = capacity;
		this.locationType = locationType;
	}
}
