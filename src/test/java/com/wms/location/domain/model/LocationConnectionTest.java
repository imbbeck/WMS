package com.wms.location.domain.model;

import com.wms.applicationInfra.config.JpaAuditingConfig;
import com.wms.applicationInfra.config.QuerydslConfig;
import com.wms.location.domain.exception.LocationConnectException;
import com.wms.location.domain.exception.LocationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import({QuerydslConfig.class, JpaAuditingConfig.class})
class LocationConnectionTest {

    @Autowired
    private TestEntityManager entityManager;

    private Location locationA;
    private Location locationB;
    private Location locationC;

    @BeforeEach
    void setUp() {
        locationA = entityManager.persistAndFlush(Location.builder()
                .name("Location A")
                .type(LocationType.WAREHOUSE)
                .capacity(100)
                .coordinateX(100)
                .coordinateY(100)
                .build());

        locationB = entityManager.persistAndFlush(Location.builder()
                .name("Location B")
                .type(LocationType.WAREHOUSE)
                .capacity(200)
                .coordinateX(100)
                .coordinateY(100)
                .build());

        locationC = entityManager.persistAndFlush(Location.builder()
                .name("Location C")
                .type(LocationType.WAREHOUSE)
                .capacity(200)
                .coordinateX(100)
                .coordinateY(100)
                .build());
    }

    @Test
    @DisplayName("LocationConnection 생성 시 ID가 자동으로 정렬된다.")
    void createConnection_SortsLocationIds() {
        // given
        Long id1 = locationA.getId();
        Long id2 = locationB.getId();

        // when
        // ID 순서를 반대로 하여 생성
        LocationConnection connection = LocationConnection.builder()
                .locationId1(id2)
                .locationId2(id1)
                .trt(50)
                .build();

        LocationConnection savedConnection = entityManager.persistAndFlush(connection);

        // then
        assertThat(savedConnection.getLocationAId()).isEqualTo(Math.min(id1, id2));
        assertThat(savedConnection.getLocationBId()).isEqualTo(Math.max(id1, id2));
    }

    @Test
    @DisplayName("동일한 Location ID로 Connection을 생성하면 예외가 발생한다.")
    void createConnection_WithSameLocationIds_ThrowsException() {
        // when & then
        assertThatThrownBy(() -> {
            LocationConnection.builder()
                    .locationId1(locationA.getId())
                    .locationId2(locationA.getId())
                    .trt(100)
                    .build();
        }).isInstanceOf(LocationConnectException.ValidationEx.class)
                .hasMessage("출발지와 도착지가 같을 수 없습니다.");
    }

    @Test
    @DisplayName("소요시간을 0 이하의 값으로 수정하면 예외가 발생한다.")
    void updateDuration_WithInvalidValue_ThrowsException() {
        // given
        LocationConnection connection = LocationConnection.builder()
                .locationId1(locationA.getId())
                .locationId2(locationB.getId())
                .trt(100)
                .build();

        // when & then
        assertThatThrownBy(() -> connection.updateDuration(0))
                .isInstanceOf(LocationConnectException.ValidationEx.class)
                .hasMessage("소요시간은 0보다 커야 합니다.");

        assertThatThrownBy(() -> connection.updateDuration(-10))
                .isInstanceOf(LocationConnectException.ValidationEx.class)
                .hasMessage("소요시간은 0보다 커야 합니다.");
    }

    @Test
    @DisplayName("특정 Location ID가 주어졌을 때 상대방 Location ID를 정확히 반환한다.")
    void getOtherLocationId_Success() {
        // given
        LocationConnection connection = LocationConnection.builder()
                .locationId1(locationA.getId())
                .locationId2(locationB.getId())
                .trt(80)
                .build();

        // when
        Long otherIdFromA = connection.getOtherLocationId(locationA.getId());
        Long otherIdFromB = connection.getOtherLocationId(locationB.getId());

        // then
        assertThat(otherIdFromA).isEqualTo(locationB.getId());
        assertThat(otherIdFromB).isEqualTo(locationA.getId());
    }

    @Test
    @DisplayName("연결되지 않은 특정 Location ID가 주어졌을 때 null을 반환한다.")
    void getOtherLocationId_Null_Success() {
        // given
        LocationConnection connection = LocationConnection.builder()
                .locationId1(locationA.getId())
                .locationId2(locationB.getId())
                .trt(80)
                .build();

        // when
        Long otherIdFromC = connection.getOtherLocationId(locationC.getId());

        // then
        assertThat(otherIdFromC).isNull();
    }
}