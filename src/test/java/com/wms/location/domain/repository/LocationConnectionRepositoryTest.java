package com.wms.location.domain.repository;

import com.wms.Infra.config.QuerydslConfig;
import com.wms.location.domain.model.Location;
import com.wms.location.domain.model.LocationConnection;
import com.wms.location.domain.model.LocationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(QuerydslConfig.class)
class LocationConnectionRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private LocationConnectionRepository connectionRepository;

    private Location locA, locB, locC;
    private LocationConnection connAB, connBC;

    @BeforeEach
    void setUp() {
        locA = entityManager.persist(Location.builder().name("A").type(LocationType.WAREHOUSE).capacity(100).coordinateX(100).coordinateY(100).build());
        locB = entityManager.persist(Location.builder().name("B").type(LocationType.WAREHOUSE).capacity(100).coordinateX(100).coordinateY(100).build());
        locC = entityManager.persist(Location.builder().name("C").type(LocationType.WAREHOUSE).capacity(100).coordinateX(100).coordinateY(100).build());

        connAB = entityManager.persist(LocationConnection.builder().locationId1(locA.getId()).locationId2(locB.getId()).trt(50).build());
        connBC = entityManager.persist(LocationConnection.builder().locationId1(locB.getId()).locationId2(locC.getId()).trt(30).build());
        entityManager.flush();
    }

    @Test
    @DisplayName("특정 Location ID로 모든 연결을 정확히 조회한다.")
    void findAllByLocationId_Success() {
        // when
        List<LocationConnection> connectionsForB = connectionRepository.findAllByLocationId(locB.getId());

        // then
        assertThat(connectionsForB).hasSize(2).containsExactlyInAnyOrder(connAB, connBC);
    }

    @Test
    @DisplayName("두 Location ID로 특정 연결을 정확히 조회한다.")
    void findByLocationIds_Success() {
        // when
        Optional<LocationConnection> foundConnection = connectionRepository.findByLocationIds(locA.getId(), locB.getId());
        Optional<LocationConnection> notFoundConnection = connectionRepository.findByLocationIds(locA.getId(), locC.getId());

        // then
        assertThat(foundConnection).isPresent().hasValue(connAB);
        assertThat(notFoundConnection).isNotPresent();
    }

    @Test
    @DisplayName("특정 Location ID에 연결된 개수를 정확히 반환한다.")
    void countByLocationId_Success() {
        // when
        long countForB = connectionRepository.countByLocationId(locB.getId());
        long countForA = connectionRepository.countByLocationId(locA.getId());

        // then
        assertThat(countForB).isEqualTo(2);
        assertThat(countForA).isEqualTo(1);
    }

    @Test
    @DisplayName("Location 삭제 시 관련 연결 정보가 모두 삭제된다.")
    void deleteByLocationId_Success() {
        // when
        connectionRepository.deleteByLocationId(locB.getId());
        entityManager.flush();
        entityManager.clear();

        // then
        List<LocationConnection> allConnections = connectionRepository.findAll();
        assertThat(allConnections).isEmpty();
        // locA와 locC는 남아있어야 함
        assertThat(entityManager.find(Location.class, locA.getId())).isNotNull();
        assertThat(entityManager.find(Location.class, locC.getId())).isNotNull();
    }
}
