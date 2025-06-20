package com.wms.location.domain.model;

import com.wms.applicationInfra.config.JpaAuditingConfig;
import com.wms.applicationInfra.config.QuerydslConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({QuerydslConfig.class, JpaAuditingConfig.class})
class LocationAuditingTest {

    @Autowired
    private TestEntityManager entityManager;

    @Test
    @DisplayName("Location 엔티티 저장 시 createdAt과 updatedAt이 자동으로 설정된다.")
    void locationEntity_Auditing_CreatesAndUpdatesTimestamps() {
        // given
        Location newLocation = Location.builder()
                .name("새로운 창고")
                .type(LocationType.WAREHOUSE)
                .capacity(500)
                .coordinateX(100)
                .coordinateY(100)
                .build();

        // when
        Location savedLocation = entityManager.persistAndFlush(newLocation);

        // then
        assertThat(savedLocation.getCreatedAt()).isNotNull();
        assertThat(savedLocation.getUpdatedAt()).isNotNull();
        assertThat(savedLocation.getCreatedAt()).isEqualTo(savedLocation.getUpdatedAt());

        // given
        LocalDateTime initialUpdatedAt = savedLocation.getUpdatedAt();
        // H2 DB는 timestamp 정밀도 문제로 미세한 시간 차이가 없을 수 있어 약간의 지연을 줌
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        Location foundLocation = entityManager.find(Location.class, savedLocation.getId());
        foundLocation.update("이름 변경", foundLocation.getCapacity(), foundLocation.getCoordinateX(), foundLocation.getCoordinateY());

        // when
        Location updatedLocation = entityManager.persistAndFlush(foundLocation);

        // then
        assertThat(updatedLocation.getCreatedAt()).isEqualTo(savedLocation.getCreatedAt()); // createdAt은 변경되지 않음
        assertThat(updatedLocation.getUpdatedAt()).isAfter(initialUpdatedAt); // updatedAt은 변경됨
    }
}