package com.wms.applicationInfra.domain;

import lombok.Getter;

@Getter
public enum FieldEnum {
	NAME("이름"),
	TYPE("타입"),
	USERID("사용자ID"),
	EMAIL("이메일"),



	LOCATION("장소"),
	LOCATION_CONNETCTION("장소간 연결"),
	WARE_WAREHOUSE_PAIR("물품_창고_쌍"),






	Z("Z");


	private final String label;

	FieldEnum(String label) {
		this.label = label;
	}
}
