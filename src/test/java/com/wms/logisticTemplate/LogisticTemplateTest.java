package com.wms.logisticTemplate;

import com.wms.location.domain.model.Location;
import com.wms.location.domain.model.LocationType;
import com.wms.logisticTemplate.domain.exception.LogisticTemplateException;
import com.wms.logisticTemplate.domain.model.LogisticTemplate;
import com.wms.logisticTemplate.domain.model.LogisticType;
import com.wms.ware.domain.model.Ware;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("물류 템플릿 도메인 테스트")
class LogisticTemplateTest {

    private final Ware testWare = Ware.builder()
            .name("테스트 물품")
            .type("가전제품")
            .paletteUnit(10)
            .build();

    private final Location inboundLocation = Location.builder()
            .name("입고처")
            .type(LocationType.INBOUND)
            .coordinateX(100)
            .coordinateY(100)
            .build();

    private final Location warehouseLocation = Location.builder()
            .name("창고")
            .type(LocationType.WAREHOUSE)
            .capacity(1000)
            .coordinateX(200)
            .coordinateY(200)
            .build();

    private final Location outboundLocation = Location.builder()
            .name("출고처")
            .type(LocationType.OUTBOUND)
            .coordinateX(300)
            .coordinateY(300)
            .build();

    @Test
    @DisplayName("템플릿 이름이 null이면 예외가 발생한다")
    void testTemplateNameNull() {
        assertThatThrownBy(() -> LogisticTemplate.builder()
                .name(null)
                .type(LogisticType.INNER)
                .ware(testWare)
                .fromLocation(warehouseLocation)
                .toLocation(warehouseLocation)
                .standardQuantity(100)
                .build()).isInstanceOf(LogisticTemplateException.ValidationEx.class);
    }

    @Test
    @DisplayName("템플릿 타입이 null이면 예외가 발생한다")
    void testTemplateTypeNull() {
        assertThatThrownBy(() -> LogisticTemplate.builder()
                .name("테스트 템플릿")
                .type(null)
                .ware(testWare)
                .fromLocation(warehouseLocation)
                .toLocation(warehouseLocation)
                .standardQuantity(100)
                .build()).isInstanceOf(LogisticTemplateException.ValidationEx.class);
    }

    @Test
    @DisplayName("표준 수량이 0이하면 예외가 발생한다")
    void testStandardQuantityInvalid() {
        assertThatThrownBy(() -> LogisticTemplate.builder()
                .name("테스트 템플릿")
                .type(LogisticType.INNER)
                .ware(testWare)
                .fromLocation(warehouseLocation)
                .toLocation(warehouseLocation)
                .standardQuantity(0)
                .build()).isInstanceOf(LogisticTemplateException.ValidationEx.class);
    }

    @Test
    @DisplayName("유효한 템플릿 정보로 템플릿을 생성할 수 있다")
    void testValidTemplateCreation() {
        LogisticTemplate template = LogisticTemplate.builder()
                .name("테스트 템플릿")
                .type(LogisticType.INNER)
                .ware(testWare)
                .fromLocation(warehouseLocation)
                .toLocation(warehouseLocation)
                .trt(30)
                .standardQuantity(100)
                .build();

        assertThat(template.getName()).isEqualTo("테스트 템플릿");
        assertThat(template.getType()).isEqualTo(LogisticType.INNER);
        assertThat(template.getTrt()).isEqualTo(30);
        assertThat(template.getStandardQuantity()).isEqualTo(100);
    }

    @Test
    @DisplayName("템플릿 정보를 업데이트할 수 있다")
    void testTemplateUpdate() {
        LogisticTemplate template = LogisticTemplate.builder()
                .name("테스트 템플릿")
                .type(LogisticType.INNER)
                .ware(testWare)
                .fromLocation(warehouseLocation)
                .toLocation(warehouseLocation)
                .standardQuantity(100)
                .build();

        template.update("업데이트된 템플릿", 100, 200);

        assertThat(template.getName()).isEqualTo("업데이트된 템플릿");
        assertThat(template.getTrt()).isEqualTo(100);
        assertThat(template.getStandardQuantity()).isEqualTo(200);
    }

    @Test
    @DisplayName("trt 필드가 null이어도 템플릿 생성이 가능하다")
    void testTemplateWithNullTrt() {
        LogisticTemplate template = LogisticTemplate.builder()
                .name("TRT 없는 템플릿")
                .type(LogisticType.INNER)
                .ware(testWare)
                .fromLocation(warehouseLocation)
                .toLocation(warehouseLocation)
                .trt(null)
                .standardQuantity(100)
                .build();

        assertThat(template.getTrt()).isNull();
        assertThat(template.getName()).isEqualTo("TRT 없는 템플릿");
    }
} 