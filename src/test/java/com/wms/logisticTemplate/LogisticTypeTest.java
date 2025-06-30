package com.wms.logisticTemplate;

import com.wms.location.domain.model.LocationType;
import com.wms.logisticTemplate.domain.model.LogisticType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("물류 타입 검증 테스트")
class LogisticTypeTest {

    @ParameterizedTest
    @MethodSource("validLocationTypeCombinations")
    @DisplayName("유효한 장소 타입 조합에 대해 검증이 성공한다")
    void testValidLocationTypeCombinations(LogisticType logisticType, LocationType fromType, LocationType toType) {
        // when & then
        assertThat(logisticType.isValidLocationTypes(fromType, toType)).isTrue();
    }

    @ParameterizedTest
    @MethodSource("invalidLocationTypeCombinations")
    @DisplayName("유효하지 않은 장소 타입 조합에 대해 검증이 실패한다")
    void testInvalidLocationTypeCombinations(LogisticType logisticType, LocationType fromType, LocationType toType) {
        // when & then
        assertThat(logisticType.isValidLocationTypes(fromType, toType)).isFalse();
    }

    @Test
    @DisplayName("INBOUND 타입의 검증 메시지가 올바르다")
    void testInboundValidationMessage() {
        // given
        LogisticType inbound = LogisticType.INBOUND;

        // when
        String message = inbound.getValidationMessage();

        // then
        assertThat(message).isEqualTo("INBOUND 작업은 INBOUND에서 WAREHOUSE로만 가능합니다.");
    }

    @Test
    @DisplayName("OUTBOUND 타입의 검증 메시지가 올바르다")
    void testOutboundValidationMessage() {
        // given
        LogisticType outbound = LogisticType.OUTBOUND;

        // when
        String message = outbound.getValidationMessage();

        // then
        assertThat(message).isEqualTo("OUTBOUND 작업은 WAREHOUSE에서 OUTBOUND로만 가능합니다.");
    }

    @Test
    @DisplayName("INNER 타입의 검증 메시지가 올바르다")
    void testInnerValidationMessage() {
        // given
        LogisticType inner = LogisticType.INNER;

        // when
        String message = inner.getValidationMessage();

        // then
        assertThat(message).isEqualTo("INNER 작업은 WAREHOUSE에서 WAREHOUSE로만 가능합니다.");
    }

    static Stream<Arguments> validLocationTypeCombinations() {
        return Stream.of(
                Arguments.of(LogisticType.INBOUND, LocationType.INBOUND, LocationType.WAREHOUSE),
                Arguments.of(LogisticType.OUTBOUND, LocationType.WAREHOUSE, LocationType.OUTBOUND),
                Arguments.of(LogisticType.INNER, LocationType.WAREHOUSE, LocationType.WAREHOUSE)
        );
    }

    static Stream<Arguments> invalidLocationTypeCombinations() {
        return Stream.of(
                // INBOUND 타입의 잘못된 조합들
                Arguments.of(LogisticType.INBOUND, LocationType.WAREHOUSE, LocationType.WAREHOUSE),
                Arguments.of(LogisticType.INBOUND, LocationType.OUTBOUND, LocationType.WAREHOUSE),
                Arguments.of(LogisticType.INBOUND, LocationType.INBOUND, LocationType.INBOUND),
                Arguments.of(LogisticType.INBOUND, LocationType.INBOUND, LocationType.OUTBOUND),

                // OUTBOUND 타입의 잘못된 조합들
                Arguments.of(LogisticType.OUTBOUND, LocationType.INBOUND, LocationType.OUTBOUND),
                Arguments.of(LogisticType.OUTBOUND, LocationType.WAREHOUSE, LocationType.WAREHOUSE),
                Arguments.of(LogisticType.OUTBOUND, LocationType.OUTBOUND, LocationType.OUTBOUND),
                Arguments.of(LogisticType.OUTBOUND, LocationType.WAREHOUSE, LocationType.INBOUND),

                // INNER 타입의 잘못된 조합들
                Arguments.of(LogisticType.INNER, LocationType.INBOUND, LocationType.WAREHOUSE),
                Arguments.of(LogisticType.INNER, LocationType.WAREHOUSE, LocationType.OUTBOUND),
                Arguments.of(LogisticType.INNER, LocationType.INBOUND, LocationType.OUTBOUND),
                Arguments.of(LogisticType.INNER, LocationType.INBOUND, LocationType.INBOUND),
                Arguments.of(LogisticType.INNER, LocationType.OUTBOUND, LocationType.OUTBOUND)
        );
    }
}
