package com.wms.ware;

import com.wms.ware.domain.exception.WareException;
import com.wms.ware.domain.model.Ware;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("물품 도메인 테스트")
class WareTest {

    @Test
    @DisplayName("물품명이 null이면 예외가 발생한다")
    void testWareNameNull() {
        assertThatThrownBy(() -> Ware.builder()
                .name(null)
                .type("가전제품")
                .paletteUnit(10)
                .build()).isInstanceOf(WareException.ValidationEx.class);
    }

    @Test
    @DisplayName("물품명이 빈 문자열이면 예외가 발생한다")
    void testWareNameEmpty() {
        assertThatThrownBy(() -> Ware.builder()
                .name("")
                .type("가전제품")
                .paletteUnit(10)
                .build()).isInstanceOf(WareException.ValidationEx.class);
    }

    @Test
    @DisplayName("물품 타입이 null이면 예외가 발생한다")
    void testWareTypeNull() {
        assertThatThrownBy(() -> Ware.builder()
                .name("테스트 물품")
                .type(null)
                .paletteUnit(10)
                .build()).isInstanceOf(WareException.ValidationEx.class);
    }

    @Test
    @DisplayName("파레트당 물품 개수가 0이하면 예외가 발생한다")
    void testWarePaletteUnitInvalid() {
        assertThatThrownBy(() -> Ware.builder()
                .name("테스트 물품")
                .type("가전제품")
                .paletteUnit(0)
                .build()).isInstanceOf(WareException.ValidationEx.class);
    }

    @Test
    @DisplayName("유효한 물품 정보로 물품을 생성할 수 있다")
    void testValidWareCreation() {
        Ware ware = Ware.builder()
                .name("테스트 물품")
                .type("가전제품")
                .paletteUnit(10)
                .build();

        assertThat(ware.getName()).isEqualTo("테스트 물품");
        assertThat(ware.getType()).isEqualTo("가전제품");
        assertThat(ware.getPaletteUnit()).isEqualTo(10);
    }

    @Test
    @DisplayName("물품 정보를 업데이트할 수 있다")
    void testWareUpdate() {
        Ware ware = Ware.builder()
                .name("테스트 물품")
                .type("가전제품")
                .paletteUnit(10)
                .build();

        ware.update("업데이트된 물품", "전자제품", 20);

        assertThat(ware.getName()).isEqualTo("업데이트된 물품");
        assertThat(ware.getType()).isEqualTo("전자제품");
        assertThat(ware.getPaletteUnit()).isEqualTo(20);
    }
} 