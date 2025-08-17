package com.wms.logisticTemplate.domain.model;

import java.util.HashMap;
import java.util.Map;

import com.wms.location.domain.model.LocationType;

public enum LogisticType {
    INBOUND(LocationType.INBOUND, LocationType.WAREHOUSE),    // 입고
    OUTBOUND(LocationType.WAREHOUSE, LocationType.OUTBOUND),   // 출고
    INNER(LocationType.WAREHOUSE, LocationType.WAREHOUSE);       // 창고간 이동

    private final LocationType requiredFromType;
    private final LocationType requiredToType;

    LogisticType(LocationType requiredFromType, LocationType requiredToType) {
        this.requiredFromType = requiredFromType;
        this.requiredToType = requiredToType;
    }

    public boolean isValidLocationTypes(LocationType fromType, LocationType toType) {
        return this.requiredFromType.equals(fromType) && this.requiredToType.equals(toType);
    }

    public String getValidationMessage() {
        return String.format("%s 작업은 %s에서 %s로만 가능합니다.", 
                this.name(), this.requiredFromType, this.requiredToType);
    }

	private static final Map<LogisticType, String> COLOR_MAP = 	Map.of(
			LogisticType.INBOUND, "bg-green-500",
			LogisticType.OUTBOUND, "bg-purple-500",
			LogisticType.INNER, "bg-blue-500"
	);


	public String getColor() {
		return COLOR_MAP.get(this);
	}
}