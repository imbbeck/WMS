package com.example.wms.ware;

import jakarta.persistence.*;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "ware")
public class Ware {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long wareId;

	private String name;

	private String specification;

	private String unit;

	private String description;


	@Builder
	public Ware(String name, String specification, String unit, String description) {
		this.name = name;
		this.specification = specification;
		this.unit = unit;
		this.description = description;
	}

	// 도메인 행위: 부품 정보 수정
	public void updateInfo(String name, String specification, String unit, String description) {
		this.name = name;
		this.specification = specification;
		this.unit = unit;
		this.description = description;
	}

}