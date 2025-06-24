package com.wms.location.domain.model;

import com.wms.applicationInfra.domain.BaseEntity;
import com.wms.location.domain.exception.LocationConnectException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "location_connection",
		uniqueConstraints = @UniqueConstraint(columnNames = {"location_a_id", "location_b_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LocationConnection extends BaseEntity {

	@Column(name = "location_a_id", nullable = false)
	private Long locationAId;  // 항상 더 작은 ID

	@Column(name = "location_b_id", nullable = false)
	private Long locationBId;  // 항상 더 큰 ID

	@Column(name = "trt", nullable = false)
	private Integer trt; // total required time

	@Builder
	public LocationConnection(Long locationId1, Long locationId2, Integer trt) {
		// 자기 자신과 연결 방지
		validateLocationIds(locationId1, locationId2);

		// 항상 작은 ID가 A, 큰 ID가 B가 되도록 정렬
		if (locationId1.compareTo(locationId2) < 0) {
			this.locationAId = locationId1;
			this.locationBId = locationId2;
		} else {
			this.locationAId = locationId2;
			this.locationBId = locationId1;
		}
		this.trt = trt;
	}

	// TransferDuration과 동일한 검증 로직
	private void validateLocationIds(Long locationId1, Long locationId2) {
		if (locationId1 == null || locationId2 == null) {
			throw LocationConnectException.validation("출발지ID, 도착지ID 는 필수입니다.");
		}
		if (locationId1.equals(locationId2)) {
			throw LocationConnectException.validation("출발지와 도착지가 같을 수 없습니다.");
		}
	}

	// 무방향이므로 어느 쪽이든 연결되어 있으면 true
	public boolean connectsLocations(Long id1, Long id2) {
		return (locationAId.equals(id1) && locationBId.equals(id2)) ||
				(locationAId.equals(id2) && locationBId.equals(id1));
	}

	// 상대방 Location ID 반환
	public Long getOtherLocationId(Long myLocationId) {
		if (locationAId.equals(myLocationId)) {
			return locationBId;
		} else if (locationBId.equals(myLocationId)) {
			return locationAId;
		} else {
			return null;
		}
	}

	// 연결 정보 업데이트
	public void updateDuration(Integer newDuration) {
		if (newDuration == null || newDuration <= 0) {
			throw LocationConnectException.validation("소요시간은 0보다 커야 합니다.");
		}
		this.trt = newDuration;
	}
}

