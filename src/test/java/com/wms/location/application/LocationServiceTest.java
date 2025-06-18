package com.wms.location.application;

import com.wms.location.domain.model.Location;
import com.wms.location.domain.model.LocationConnection;
import com.wms.location.domain.model.LocationType;
import com.wms.location.domain.repository.LocationConnectionRepository;
import com.wms.location.domain.repository.LocationRepository;
import com.wms.location.dto.LocationDTO;
import com.wms.location.dto.LocationWithConnectionsDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class LocationServiceTest {

	@Autowired
	private LocationService locationService;

	@Autowired
	private LocationRepository locationRepository;

	@Autowired
	private LocationConnectionRepository locationConnectionRepository;

	@Test
	@DisplayName("창고 타입의 위치를 성공적으로 생성한다.")
	void createWarehouseLocation_Success() {
		// given
		var request = LocationDTO.createReq.builder()
				.name("메인 창고")
				.type(LocationType.WAREHOUSE)
				.capacity(1000)
				.build();

		// when
		Location savedLocation = locationService.createLocation(request);

		// then
		assertThat(savedLocation.getId()).isNotNull();
		assertThat(savedLocation.getName()).isEqualTo("메인 창고");
		assertThat(savedLocation.getType()).isEqualTo(LocationType.WAREHOUSE);
		assertThat(savedLocation.getCapacity()).isEqualTo(1000);
	}

	@Test
	@DisplayName("이미 존재하는 이름으로 위치를 생성하면 예외가 발생한다.")
	void createLocation_WithDuplicateName_ThrowsException() {
		// given
		var request1 = LocationDTO.createReq.builder()
				.name("중복 이름 창고")
				.type(LocationType.WAREHOUSE)
				.capacity(100)
				.build();
		locationService.createLocation(request1);

		var request2 = LocationDTO.createReq.builder()
				.name("중복 이름 창고")
				.type(LocationType.INBOUND)
				.build();

		// when & then
		assertThatThrownBy(() -> locationService.createLocation(request2))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("이미 존재하는 장소 이름입니다.");
	}

	@Test
	@DisplayName("위치 정보를 성공적으로 수정한다.")
	void updateLocation_Success() {
		// given
		Location savedLocation = locationRepository.save(Location.builder()
				.name("수정 전 이름")
				.type(LocationType.WAREHOUSE)
				.capacity(50)
				.build());
		var updateRequest = LocationDTO.updateReq.builder()
				.name("수정 후 이름")
				.type(LocationType.OUTBOUND)
				.capacity(null) // OUTPUT_ZONE은 capacity가 null
				.build();

		// when
		Location updatedLocation = locationService.updateLocation(savedLocation.getId(), updateRequest);

		// then
		assertThat(updatedLocation.getName()).isEqualTo("수정 후 이름");
		assertThat(updatedLocation.getType()).isEqualTo(LocationType.OUTBOUND);
		assertThat(updatedLocation.getCapacity()).isNull();
	}

	@Test
	@DisplayName("위치를 삭제하면 해당 위치와 관련된 연결 정보도 모두 삭제된다 (이벤트 리스너 동작 검증)")
	void deleteLocation_WithConnections_AlsoDeletesConnections() {
		// given
		Location locationA = locationRepository.save(Location.builder().name("A 창고").type(LocationType.WAREHOUSE).capacity(100).build());
		Location locationB = locationRepository.save(Location.builder().name("B 창고").type(LocationType.WAREHOUSE).capacity(100).build());
		Location locationC = locationRepository.save(Location.builder().name("C 창고").type(LocationType.WAREHOUSE).capacity(100).build());

		locationConnectionRepository.save(LocationConnection.builder().locationId1(locationA.getId()).locationId2(locationB.getId()).trt(60).build());
		locationConnectionRepository.save(LocationConnection.builder().locationId1(locationA.getId()).locationId2(locationC.getId()).trt(30).build());


		assertThat(locationConnectionRepository.count()).isEqualTo(2);

		// when
		locationService.deleteLocation(locationA.getId());

		// then
		assertThat(locationRepository.findById(locationA.getId())).isEmpty();
		assertThat(locationConnectionRepository.findAllByLocationId(locationA.getId())).isEmpty();
		assertThat(locationConnectionRepository.count()).isZero();
	}

	@Test
	@DisplayName("특정 위치와 연결 정보를 함께 조회한다.")
	void getLocationWithConnections_Success() {
		// given
		Location locA = locationRepository.save(Location.builder().name("A").type(LocationType.WAREHOUSE).capacity(100).build());
		Location locB = locationRepository.save(Location.builder().name("B").type(LocationType.WAREHOUSE).capacity(100).build());
		Location locC = locationRepository.save(Location.builder().name("C").type(LocationType.OUTBOUND).capacity(null).build());
		locationConnectionRepository.save(LocationConnection.builder().locationId1(locA.getId()).locationId2(locB.getId()).trt(50).build());
		locationConnectionRepository.save(LocationConnection.builder().locationId1(locA.getId()).locationId2(locC.getId()).trt(20).build());

		// when
		LocationWithConnectionsDTO result = locationService.getLocationWithConnections(locA.getId());

		// then
		assertThat(result.id()).isEqualTo(locA.getId());
		assertThat(result.name()).isEqualTo("A");
		assertThat(result.connections()).hasSize(2);
		assertThat(result.connections())
				.extracting("trt")
				.containsExactlyInAnyOrder(50, 20);
	}
}