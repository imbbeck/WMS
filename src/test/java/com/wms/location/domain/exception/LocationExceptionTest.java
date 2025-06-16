package com.wms.location.domain.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LocationExceptionTest {

    @Test
    @DisplayName("NotFoundException 메시지 테스트")
    void notFoundExceptionMessage() {
        // given
        Long locationId = 1L;

        // when
        LocationException.NotFoundException exception = 
            new LocationException.NotFoundException(locationId);

        // then
        assertThat(exception.getMessage())
            .contains("존재하지 않는 위치")
            .contains(locationId.toString());
    }

    @Test
    @DisplayName("InvalidTypeException 메시지 테스트")
    void invalidTypeExceptionMessage() {
        // given
        String locationType = "INVALID";

        // when
        LocationException.InvalidTypeException exception = 
            new LocationException.InvalidTypeException(locationType);

        // then
        assertThat(exception.getMessage())
            .contains("잘못된 위치 유형")
            .contains(locationType);
    }

    @Test
    @DisplayName("CapacityExceededException 메시지 테스트")
    void capacityExceededExceptionMessage() {
        // given
        Long locationId = 1L;
        int requested = 100;
        int capacity = 50;

        // when
        LocationException.CapacityExceededException exception = 
            new LocationException.CapacityExceededException(locationId, requested, capacity);

        // then
        assertThat(exception.getMessage())
            .contains("위치 용량 초과")
            .contains(locationId.toString())
            .contains(String.valueOf(requested))
            .contains(String.valueOf(capacity));
    }
} 