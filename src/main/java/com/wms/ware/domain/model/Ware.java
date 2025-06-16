package com.wms.ware.domain.model;

import com.wms.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Ware extends BaseEntity {

    @Column(nullable = false)
    private String name;  // 물품명

    @Column(nullable = false)
    private String type;  // 물품 타입 (예: 가전제품, 나사 등)

    @Column(nullable = false)
    private Integer paletteUnit;  // 파레트 당 물품 개수

    @Builder
    public Ware(String name, String type, Integer paletteUnit) {
        this.name = name;
        this.type = type;
        this.paletteUnit = paletteUnit;
    }

    public void update(String name, String type, Integer paletteUnit) {
        this.name = name;
        this.type = type;
        this.paletteUnit = paletteUnit;
    }
} 