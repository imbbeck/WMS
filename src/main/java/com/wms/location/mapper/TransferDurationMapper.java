package com.wms.location.mapper;

import com.wms.location.domain.model.Location;
import com.wms.location.domain.model.TransferDuration;
import com.wms.location.dto.TransferDurationRequest;
import com.wms.location.dto.TransferDurationResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TransferDurationMapper {

    TransferDurationResponse toResponse(TransferDuration transferDuration);
} 