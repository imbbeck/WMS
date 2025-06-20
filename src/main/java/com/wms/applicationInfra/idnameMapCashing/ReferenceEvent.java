package com.wms.applicationInfra.idnameMapCashing;

import lombok.Getter;

@Getter
public abstract class ReferenceEvent {
	private final Long id;
	private final String name;

	protected ReferenceEvent(Long id, String name) {
		this.id = id;
		this.name = name;
	}

	protected ReferenceEvent(Long id) {
		this.id = id;
		this.name = null;
	}
}
