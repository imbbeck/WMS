package com.wms.ware;

import com.wms.ware.domain.model.Ware;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WareTest {

    @Test
    @DisplayName("물품 이름이 없으면 예외가 발생한다.")
    void createWare_WithNullName_ThrowsException() {
        // when & then
        assertThatThrownBy(() -> Ware.builder()
                .name(null)
                .type("전자제품")
                .paletteUnit(10)
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("물품 이름은 필수입니다.");
    }

    @Test
    @DisplayName("물품 타입이 없으면 예외가 발생한다.")
    void createWare_WithNullType_ThrowsException() {
        // when & then
        assertThatThrownBy(() -> Ware.builder()
                .name("노트북")
                .type(null)
                .paletteUnit(10)
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("물품 타입은 필수입니다.");
    }

    @Test
    @DisplayName("파레트 당 물품 개수가 0 이하이면 예외가 발생한다.")
    void createWare_WithInvalidPaletteUnit_ThrowsException() {
        // when & then
        assertThatThrownBy(() -> Ware.builder()
                .name("노트북")
                .type("전자제품")
                .paletteUnit(0)
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("파레트 당 물품 개수는 0보다 커야 합니다.");

        assertThatThrownBy(() -> Ware.builder()
                .name("노트북")
                .type("전자제품")
                .paletteUnit(-5)
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("파레트 당 물품 개수는 0보다 커야 합니다.");
    }
} 