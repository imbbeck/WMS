package com.wms.location.domain.repository;

import com.wms.location.domain.model.Location;
import com.wms.location.domain.model.LocationConnection;
import com.wms.location.domain.model.LocationType;
import com.wms.location.dto.LocationWithConnectionsDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import com.wms.applicationInfra.config.QuerydslConfig; // QueryDslConfig를 사용한다면 Import

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(QuerydslConfig.class) // JPAQueryFactory Bean을 위해 추가
class LocationRepositoryTest {

	@Autowired
	private TestEntityManager entityManager; // 테스트용 EntityManager

	@Autowired
	private LocationRepository locationRepository;


	@Test
	@DisplayName("findLocationWithConnections는 장소와 모든 연결 정보를 함께 조회한다.")
	void findLocationWithConnections_Success() {
		// given
		Location locA = entityManager.persist(Location.builder().name("A").type(LocationType.WAREHOUSE).capacity(100).coordinateX(100).coordinateY(100).build());
		Location locB = entityManager.persist(Location.builder().name("B").type(LocationType.WAREHOUSE).capacity(100).coordinateX(100).coordinateY(100).build());
		entityManager.persist(LocationConnection.builder().locationId1(locA.getId()).locationId2(locB.getId()).trt(50).build());

		// when
		Optional<LocationWithConnectionsDTO> resultOpt = locationRepository.findLocationWithConnections(locA.getId());

		// then
		assertThat(resultOpt).isPresent();
		LocationWithConnectionsDTO result = resultOpt.get();
		assertThat(result.name()).isEqualTo("A");
		assertThat(result.connections()).hasSize(1);
		assertThat(result.connections().get(0).getTrt()).isEqualTo(50);
	}
}