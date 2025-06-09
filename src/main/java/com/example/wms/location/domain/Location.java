package com.example.wms.location.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.util.Assert;

@Entity
@Table(name = "location")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Location {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "location_id")
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private LocationType type;

    @Column(name = "capacity", nullable = false)
    private Long capacity;

    @Column(name = "remark")
    private String remark;

    public Location(String name, LocationType type, Long capacity, String remark) {
        validate(name, type, capacity);
        this.name = name;
        this.type = type;
        this.capacity = capacity;
        this.remark = remark;
    }

    public void update(String name, Long capacity, String remark) {
        validate(name, this.type, capacity);
        this.name = name;
        this.capacity = capacity;
        this.remark = remark;
    }

    private void validate(String name, LocationType type, Long capacity) {
        Assert.hasText(name, "장소 이름은 필수입니다.");
        Assert.notNull(type, "장소 유형은 필수입니다.");
        Assert.notNull(capacity, "용량은 필수입니다.");
        if (type == LocationType.YARD || type == LocationType.WAREHOUSE) {
            Assert.isTrue(capacity > 0, "야적장 또는 창고는 용량이 0보다 커야 합니다.");
        } else {
            Assert.isTrue(capacity == 0, "입고처 또는 출고처는 용량이 0이어야 합니다.");
        }
    }
} 