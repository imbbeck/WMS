package com.wms.ware.domain.model;

import com.wms.applicationInfra.domain.BaseEntity;
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
    private String name;  // Product name

    @Column(nullable = false)
    private String type;  // Product type (e.g., electronics, screws, etc.)

    @Column(nullable = false)
    private Integer paletteUnit;  // Number of items per palette

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
            throw new IllegalArgumentException("Product name is required.");
        }

        if (type == null) {
            throw new IllegalArgumentException("Product type is required.");
        }

        if (paletteUnit == null || paletteUnit <= 0) {
            throw new IllegalArgumentException("The number of items per palette must be greater than 0.");
        }
    }
} 