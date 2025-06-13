package com.example.wms.location.domain;

public enum LocationType {
    INBOUND("입고처"),
    OUTBOUND("출고처"),
    YARD("야적장"),
    WAREHOUSE("창고");

    private final String description;

    LocationType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public boolean isStorageType() {
        return this == YARD || this == WAREHOUSE;
    }
} 