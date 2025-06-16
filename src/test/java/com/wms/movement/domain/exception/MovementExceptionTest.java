package com.wms.movement.domain.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MovementExceptionTest {

    @Test
    @DisplayName("InvalidStatusException 메시지 테스트")
    void invalidStatusExceptionMessage() {
        // given
        String currentStatus = "COMPLETED";
        String requiredStatus = "PENDING";

        // when
        MovementException.InvalidStatusException exception = 
            new MovementException.InvalidStatusException(currentStatus, requiredStatus);

        // then
        assertThat(exception.getMessage())
            .contains("잘못된 상태 변경")
            .contains(currentStatus)
            .contains(requiredStatus);
    }

    @Test
    @DisplayName("InvalidPathException 메시지 테스트")
    void invalidPathExceptionMessage() {
        // given
        String movementType = "INBOUND";
        String fromLocationType = "WAREHOUSE";
        String toLocationType = "OUTBOUND";

        // when
        MovementException.InvalidPathException exception = 
            new MovementException.InvalidPathException(movementType, fromLocationType, toLocationType);

        // then
        assertThat(exception.getMessage())
            .contains("잘못된 이동 경로")
            .contains(movementType)
            .contains(fromLocationType)
            .contains(toLocationType);
    }

    @Test
    @DisplayName("NotFoundException 메시지 테스트")
    void notFoundExceptionMessage() {
        // given
        Long movementId = 1L;

        // when
        MovementException.NotFoundException exception = 
            new MovementException.NotFoundException(movementId);

        // then
        assertThat(exception.getMessage())
            .contains("존재하지 않는 물류이동")
            .contains(movementId.toString());
    }
} 