package com.wms.location.dto;

import com.wms.location.domain.model.Location;
import com.wms.location.domain.model.LocationType;
import lombok.Getter;

@Getter
public class LocationResponse {
    private final Long id;
    private final String name;
    private final LocationType type;
    private final Integer capacity;

    public LocationResponse(Location location) {
        this.id = location.getId();
        this.name = location.getName();
        this.type = location.getType();
        this.capacity = location.getCapacity();
    }
} 