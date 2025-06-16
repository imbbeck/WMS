package com.wms.location.mapper;

import com.wms.location.domain.model.Location;
import com.wms.location.dto.LocationRequest;
import com.wms.location.dto.LocationResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface LocationMapper {

    LocationResponse toResponse(Location location);
} 