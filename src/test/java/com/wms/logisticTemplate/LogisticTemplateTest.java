package com.wms.logisticTemplate;

import com.wms.logisticTemplate.domain.model.LogisticTemplate;
import com.wms.logisticTemplate.domain.model.LogisticType;
import com.wms.location.domain.model.Location;
import com.wms.ware.domain.model.Ware;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LogisticTemplateTest {

    private final Ware ware = Ware.builder().name("물품").type("부품").paletteUnit(1).build();
    private final Location from = Location.builder().name("출발지").type(com.wms.location.domain.model.LocationType.WAREHOUSE).capacity(10).coordinateX(1).coordinateY(1).build();
    private final Location to = Location.builder().name("도착지").type(com.wms.location.domain.model.LocationType.WAREHOUSE).capacity(10).coordinateX(2).coordinateY(2).build();

    @Test
    @DisplayName("이름이 없으면 예외가 발생한다.")
    void createTemplate_WithNullName_ThrowsException() {
        assertThatThrownBy(() -> LogisticTemplate.builder()
                .name(null)
                .type(LogisticType.INBOUND)
                .ware(ware)
                .fromLocation(from)
                .toLocation(to)
                .standardQuantity(10)
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("물류이동 템플릿 이름은 필수입니다.");
    }

    @Test
    @DisplayName("타입이 없으면 예외가 발생한다.")
    void createTemplate_WithNullType_ThrowsException() {
        assertThatThrownBy(() -> LogisticTemplate.builder()
                .name("템플릿")
                .type(null)
                .ware(ware)
                .fromLocation(from)
                .toLocation(to)
                .standardQuantity(10)
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("물류이동 템플릿 타입은 필수입니다.");
    }

    @Test
    @DisplayName("표준 수량이 0 이하이면 예외가 발생한다.")
    void createTemplate_WithInvalidStandardQuantity_ThrowsException() {
        assertThatThrownBy(() -> LogisticTemplate.builder()
                .name("템플릿")
                .type(LogisticType.INNER)
                .ware(ware)
                .fromLocation(from)
                .toLocation(to)
                .standardQuantity(0)
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("표준 수량은 0보다 커야 합니다.");

        assertThatThrownBy(() -> LogisticTemplate.builder()
                .name("템플릿")
                .type(LogisticType.INNER)
                .ware(ware)
                .fromLocation(from)
                .toLocation(to)
                .standardQuantity(-5)
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("표준 수량은 0보다 커야 합니다.");
    }
} 