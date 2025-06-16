
package com.wms.movement.domain.model;

import com.wms.location.domain.model.Location;
import com.wms.location.domain.model.LocationType;
import com.wms.ware.domain.model.Ware;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MovementTest {

    private Ware testWare;
    private Location sourceLocation;
    private Location targetLocation;

    @BeforeEach
    void setUp() {
        testWare = Ware.builder()
                .name("테스트물품")
                .type("TEST-001")
                .paletteUnit(10)
                .build();

        sourceLocation = Location.builder()
                .name("출발지")
                .type(LocationType.INBOUND)
                .build();

        targetLocation = Location.builder()
                .name("도착지")
                .type(LocationType.WAREHOUSE)
                .capacity(1000)
                .build();
    }

    @Test
    @DisplayName("물류이동 생성 테스트")
    void createMovement() {
        // given
        int quantity = 10;

        // when
        Movement movement = Movement.create(testWare, sourceLocation, targetLocation, quantity);

        // then
        assertThat(movement.getWare()).isEqualTo(testWare);
        assertThat(movement.getFromLocation()).isEqualTo(sourceLocation);
        assertThat(movement.getToLocation()).isEqualTo(targetLocation);
        assertThat(movement.getQuantity()).isEqualTo(quantity);
        assertThat(movement.getStatus()).isEqualTo(MovementStatus.PENDING);
        assertThat(movement.getStartedAt()).isNull();
        assertThat(movement.getCompletedAt()).isNull();
    }

    @Test
    @DisplayName("물류이동 시작 테스트")
    void startMovement() {
        // given
        Movement movement = Movement.create(testWare, sourceLocation, targetLocation, 10);

        // when
        movement.start();

        // then
        assertThat(movement.getStatus()).isEqualTo(MovementStatus.IN_PROGRESS);
        assertThat(movement.getStartedAt()).isNotNull();
    }

    @Test
    @DisplayName("이미 시작된 물류이동 시작 시도 시 예외 발생")
    void startMovementAlreadyStarted() {
        // given
        Movement movement = Movement.create(testWare, sourceLocation, targetLocation, 10);
        movement.start();

        // when & then
        assertThatThrownBy(() -> movement.start())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("대기 중인 이동 작업만 시작할 수 있습니다");
    }

    @Test
    @DisplayName("물류이동 완료 테스트")
    void completeMovement() {
        // given
        Movement movement = Movement.create(testWare, sourceLocation, targetLocation, 10);
        movement.start();

        // when
        movement.complete();

        // then
        assertThat(movement.getStatus()).isEqualTo(MovementStatus.COMPLETED);
        assertThat(movement.getCompletedAt()).isNotNull();
    }

    @Test
    @DisplayName("시작되지 않은 물류이동 완료 시도 시 예외 발생")
    void completeMovementNotStarted() {
        // given
        Movement movement = Movement.create(testWare, sourceLocation, targetLocation, 10);

        // when & then
        assertThatThrownBy(() -> movement.complete())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("진행 중인 이동 작업만 완료할 수 있습니다");
    }

    @Test
    @DisplayName("물류이동 취소 테스트")
    void cancelMovement() {
        // given
        Movement movement = Movement.create(testWare, sourceLocation, targetLocation, 10);

        // when
        movement.cancel();

        // then
        assertThat(movement.getStatus()).isEqualTo(MovementStatus.CANCELLED);
    }

    @Test
    @DisplayName("완료된 물류이동 취소 시도 시 예외 발생")
    void cancelCompletedMovement() {
        // given
        Movement movement = Movement.create(testWare, sourceLocation, targetLocation, 10);
        movement.start();
        movement.complete();

        // when & then
        assertThatThrownBy(() -> movement.cancel())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("완료된 이동 작업은 취소할 수 없습니다");
    }

    @Test
    @DisplayName("파레트 수량 계산 테스트")
    void calculatePaletteQuantity() {
        // given
        int quantity = 25; // 파레트 단위가 10이므로 3개의 파레트 필요

        // when
        Movement movement = Movement.create(testWare, sourceLocation, targetLocation, quantity);

        // then
        assertThat(movement.getPaletteQuantity()).isEqualTo(3);
    }
}