package com.example.wms.ware.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.util.Assert;

@Entity
@Table(name = "ware")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Ware {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ware_id")
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "type", nullable = false)
    private String type;

    @Column(name = "palette_unit", nullable = false)
    private Long paletteUnit;

    public Ware(String name, String type, Long paletteUnit) {
        validate(name, type, paletteUnit);
        this.name = name;
        this.type = type;
        this.paletteUnit = paletteUnit;
    }

    public void update(String name, String type, Long paletteUnit) {
        validate(name, type, paletteUnit);
        this.name = name;
        this.type = type;
        this.paletteUnit = paletteUnit;
    }

    private void validate(String name, String type, Long paletteUnit) {
        Assert.hasText(name, "물품 이름은 필수입니다.");
        Assert.hasText(type, "물품 종류는 필수입니다.");
        Assert.notNull(paletteUnit, "파레트 당 물품 개수는 필수입니다.");
        Assert.isTrue(paletteUnit > 0, "파레트 당 물품 개수는 0보다 커야 합니다.");
    }
} 