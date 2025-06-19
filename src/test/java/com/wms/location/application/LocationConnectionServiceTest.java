package com.wms.location.application;

import com.wms.location.domain.model.Location;
import com.wms.location.domain.model.LocationConnection;
import com.wms.location.domain.model.LocationType;
import com.wms.location.domain.repository.LocationConnectionRepository;
import com.wms.location.domain.repository.LocationRepository;
import com.wms.location.dto.LocationConnectionDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class LocationConnectionServiceTest {

	@Autowired
	private LocationConnectionService connectionService;

	@Autowired
	private LocationConnectionRepository connectionRepository;

	@Autowired
	private LocationRepository locationRepository;

	private Location locationA;
	private Location locationB;
	private Location locationC;

	@BeforeEach
	void setUp() {
		// 테스트에 필요한 위치 정보 미리 생성
		locationRepository.deleteAll(); // 이전 테스트 데이터 클리어
		locationA = locationRepository.save(Location.builder().name("테스트A").type(LocationType.WAREHOUSE).capacity(100).coordinateX(100).coordinateY(100).build());
		locationB = locationRepository.save(Location.builder().name("테스트B").type(LocationType.WAREHOUSE).capacity(200).coordinateX(100).coordinateY(100).build());
		locationC = locationRepository.save(Location.builder().name("테스트C").type(LocationType.INBOUND).coordinateX(100).coordinateY(100).build());
	}

	@Test
	@DisplayName("두 위치 간의 연결을 성공적으로 생성한다.")
	void createConnection_Success() {
		// given
		var request = LocationConnectionDTO.CreateReq.builder()
				.locationId1(locationA.getId())
				.locationId2(locationB.getId())
				.trt(120)
				.build();

		// when
		LocationConnection connection = connectionService.createConnection(request);

		// then
		assertThat(connection.getId()).isNotNull();
		assertThat(connection.getLocationAId()).isEqualTo(Math.min(locationA.getId(), locationB.getId()));
		assertThat(connection.getLocationBId()).isEqualTo(Math.max(locationA.getId(), locationB.getId()));
		assertThat(connection.getTrt()).isEqualTo(120);
	}

	@Test
	@DisplayName("이미 연결된 위치에 다시 연결을 생성하면 예외가 발생한다.")
	void createConnection_WithExistingConnection_ThrowsException() {
		// given
		var request1 = LocationConnectionDTO.CreateReq.builder()
				.locationId1(locationA.getId())
				.locationId2(locationB.getId())
				.trt(120)
				.build();
		connectionService.createConnection(request1);

		var request2 = LocationConnectionDTO.CreateReq.builder()
				.locationId1(locationB.getId())
				.locationId2(locationA.getId())
				.trt(100)
				.build();

		// when & then
		assertThatThrownBy(() -> connectionService.createConnection(request2))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("이미 연결된 위치입니다.");
	}

	@Test
	@DisplayName("존재하지 않는 위치 ID로 연결을 생성하면 예외가 발생한다.")
	void createConnection_WithNonexistentLocation_ThrowsException() {
		// given
		long nonExistentId = 9999L;
		var request = LocationConnectionDTO.CreateReq.builder()
				.locationId1(locationA.getId())
				.locationId2(nonExistentId)
				.trt(120)
				.build();

		// when & then
		assertThatThrownBy(() -> connectionService.createConnection(request))
				.isInstanceOf(RuntimeException.class)
				.hasMessageContaining("존재하지 않는 위치입니다: " + nonExistentId);
	}

	@Test
	@DisplayName("연결 정보를 성공적으로 삭제한다.")
	void deleteConnection_Success() {
		// given
		LocationConnection connection = connectionRepository.save(LocationConnection.builder().locationId1(locationA.getId()).locationId2(locationC.getId()).trt(70).build());
		long connectionId = connection.getId();
		assertThat(connectionRepository.findById(connectionId)).isPresent();

		// when
		connectionService.deleteConnection(connectionId);

		// then
		assertThat(connectionRepository.findById(connectionId)).isEmpty();
	}

	@Test
	@DisplayName("연결의 이동 시간(trt)을 성공적으로 수정한다.")
	void updateConnection_Success() {
		// given
		LocationConnection originalConnection = connectionRepository.save(
				LocationConnection.builder().locationId1(locationA.getId()).locationId2(locationB.getId()).trt(100).build()
		);
		Long connectionId = originalConnection.getId();
		int newTrt = 250;

		var request = LocationConnectionDTO.UpdateReq.builder()
				.trt(newTrt)
				.build();

		// when
		LocationConnection updatedConnection = connectionService.updateConnection(connectionId, request);

		// then
		assertThat(updatedConnection.getId()).isEqualTo(connectionId);
		assertThat(updatedConnection.getTrt()).isEqualTo(newTrt);

		LocationConnection foundConnection = connectionRepository.findById(connectionId).get();
		assertThat(foundConnection.getTrt()).isEqualTo(newTrt);
	}
}