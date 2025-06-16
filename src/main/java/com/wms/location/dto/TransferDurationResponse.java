package com.wms.location.dto;

import com.wms.location.domain.model.TransferDuration;
import lombok.Getter;

@Getter
public class TransferDurationResponse {
    private final Long id;
    private final Long fromLocationId;
    private final String fromLocationName;
    private final Long toLocationId;
    private final String toLocationName;
    private final Integer estimatedDuration;

    public TransferDurationResponse(TransferDuration transferDuration) {
        this.id = transferDuration.getId();
        this.fromLocationId = transferDuration.getFromLocation().getId();
        this.fromLocationName = transferDuration.getFromLocation().getName();
        this.toLocationId = transferDuration.getToLocation().getId();
        this.toLocationName = transferDuration.getToLocation().getName();
        this.estimatedDuration = transferDuration.getEstimatedDuration();
    }
} 