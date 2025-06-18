package com.wms.location.domain.event;

public class LocationDeletedEvent {
	private final Long locationId;

	public LocationDeletedEvent(Long locationId) {
		this.locationId = locationId;
	}

	public Long getLocationId() {
		return locationId;
	}
}
