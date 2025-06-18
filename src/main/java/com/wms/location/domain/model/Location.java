package com.wms.location.domain.model;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.wms.common.domain.BaseEntity;
import com.wms.location.domain.exception.LocationException.*;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Entity
@Table(name = "location")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Location extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LocationType type;

    @Column
    private Integer capacity;  // WAREHOUSE 타입일 때만 유효

    // 🎯 해당 Location과 연결된 모든 Connection들 (A 또는 B로 참여하는 것들)
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "location_a_id")
    private List<LocationConnection> connectionsAsA = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "location_b_id")
    private List<LocationConnection> connectionsAsB = new ArrayList<>();

    @Builder
    public Location(String name, LocationType type, Integer capacity) {
        validateLocationData(name, type, capacity);
        this.name = name;
        this.type = type;
        this.capacity = capacity;
    }

    // 모든 연결 조회
    public List<LocationConnection> getAllConnections() {
        List<LocationConnection> allConnections = new ArrayList<>();
        allConnections.addAll(connectionsAsA);
        allConnections.addAll(connectionsAsB);
        return allConnections;
    }

    public void update(String name, LocationType type, Integer capacity) {
        validateLocationData(name, type, capacity);
        this.name = name;
        this.type = type;
        this.capacity = capacity;
    }

    private static void validateLocationData(String name, LocationType type, Integer capacity) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("위치 이름은 필수입니다.");
        }

        if (type == null) {
            throw new IllegalArgumentException("위치 타입은 필수입니다.");
        }

        if (type == LocationType.WAREHOUSE) {
            if (capacity == null || capacity <= 0) {
                throw new IllegalArgumentException("창고의 용량은 0보다 커야 합니다.");
            }
        } else {
            if (capacity != null) {
                throw new IllegalArgumentException("입고/출고처는 용량을 지정할 수 없습니다.");
            }
        }
    }

} 