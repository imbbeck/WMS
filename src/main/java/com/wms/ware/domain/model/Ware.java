package com.wms.ware.domain.model;

import com.wms.infra.domain.BaseEntity;
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
    private Integer paletteUnit;  // 파레트 당 물품 개수

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
            throw new IllegalArgumentException("물품 이름은 필수입니다.");
        }

        if (type == null) {
            throw new IllegalArgumentException("물품 타입은 필수입니다.");
        }

        if (paletteUnit == null || paletteUnit <= 0) {
            throw new IllegalArgumentException("파레트 당 물품 개수는 0보다 커야 합니다.");
        }


    }
} 