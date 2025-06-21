package com.wms.ware.domain.model;

import com.wms.applicationInfra.domain.BaseEntity;
import com.wms.applicationInfra.domain.FieldEnum;
import com.wms.ware.domain.exception.WareException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "ware")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Ware extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String name;  // 물품명

    @Column(nullable = false)
    private String type;  // 물품 타입 (예: 가전제품, 나사 등)

    @Column(nullable = false)
    private Integer paletteUnit;  // 팔레트 당 물품 개수

    @Builder
    public Ware(String name, String type, Integer paletteUnit) {
        validateWareData(name, type, paletteUnit);
        this.name = name;
        this.type = type;
        this.paletteUnit = paletteUnit;
    }

    public void update(String name, String type, Integer paletteUnit) {
        validateWareData(name, type, paletteUnit);
        this.name = name;
        this.type = type;
        this.paletteUnit = paletteUnit;
    }

    private static void validateWareData(String name, String type, Integer paletteUnit) {
        if (name == null || name.trim().isEmpty()) {
            throw WareException.validation(FieldEnum.NAME);
        }

        if (type == null) {
            throw WareException.validation(FieldEnum.TYPE);
        }

        if (paletteUnit == null || paletteUnit <= 0) {
            throw WareException.validation("파레트당 물품 개수은 0보다 커야 합니다.");
        }
    }
} 