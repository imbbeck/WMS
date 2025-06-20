package com.wms.ware;

import com.wms.ware.application.WareService;
import com.wms.ware.domain.model.Ware;
import com.wms.ware.domain.repository.WareRepository;
import com.wms.ware.dto.WareDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class WareServiceTest {

    @Autowired
    private WareService wareService;

    @Autowired
    private WareRepository wareRepository;

    @Test
    @DisplayName("물품을 성공적으로 생성한다.")
    void createWare_Success() {
        // given
        var request = WareDTO.CreateReq.builder()
                .name("노트북")
                .type("전자제품")
                .paletteUnit(10)
                .build();

        // when
        Ware savedWare = wareService.createWare(request);

        // then
        assertThat(savedWare.getId()).isNotNull();
        assertThat(savedWare.getName()).isEqualTo("노트북");
        assertThat(savedWare.getType()).isEqualTo("전자제품");
        assertThat(savedWare.getPaletteUnit()).isEqualTo(10);
    }

    @Test
    @DisplayName("이미 존재하는 이름으로 물품을 생성하면 예외가 발생한다.")
    void createWare_WithDuplicateName_ThrowsException() {
        // given
        var request1 = WareDTO.CreateReq.builder()
                .name("중복물품")
                .type("부품")
                .paletteUnit(5)
                .build();
        wareService.createWare(request1);

        var request2 = WareDTO.CreateReq.builder()
                .name("중복물품")
                .type("전자제품")
                .paletteUnit(10)
                .build();

        // when & then
        assertThatThrownBy(() -> wareService.createWare(request2))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    @DisplayName("물품 정보를 성공적으로 수정한다.")
    void updateWare_Success() {
        // given
        Ware savedWare = wareRepository.save(Ware.builder()
                .name("수정전")
                .type("부품")
                .paletteUnit(5)
                .build());
        var updateRequest = WareDTO.UpdateReq.builder()
                .name("수정후")
                .type("전자제품")
                .paletteUnit(20)
                .build();

        // when
        Ware updatedWare = wareService.updateWare(savedWare.getId(), updateRequest);

        // then
        assertThat(updatedWare.getName()).isEqualTo("수정후");
        assertThat(updatedWare.getType()).isEqualTo("전자제품");
        assertThat(updatedWare.getPaletteUnit()).isEqualTo(20);
    }

    @Test
    @DisplayName("물품을 삭제하면 정상적으로 삭제된다.")
    void deleteWare_Success() {
        // given
        Ware ware = wareRepository.save(Ware.builder()
                .name("삭제대상")
                .type("부품")
                .paletteUnit(5)
                .build());
        Long wareId = ware.getId();

        // when
        wareService.deleteWare(wareId);

        // then
        assertThat(wareRepository.findById(wareId)).isEmpty();
    }

    @Test
    @DisplayName("특정 물품을 정상적으로 조회한다.")
    void getWare_Success() {
        // given
        Ware ware = wareRepository.save(Ware.builder()
                .name("조회대상")
                .type("부품")
                .paletteUnit(5)
                .build());
        Long wareId = ware.getId();

        // when
        Ware found = wareService.getWare(wareId);

        // then
        assertThat(found.getId()).isEqualTo(wareId);
        assertThat(found.getName()).isEqualTo("조회대상");
    }

    @Test
    @DisplayName("존재하지 않는 물품 ID로 조회 시 예외가 발생한다.")
    void getWare_NotFound_ThrowsException() {
        // when & then
        assertThatThrownBy(() -> wareService.getWare(9999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("존재하지 않는 물품입니다");
    }

    @Test
    @DisplayName("모든 물품 목록을 정상적으로 조회한다.")
    void getWares_Success() {
        // given
        wareRepository.save(Ware.builder().name("A").type("부품").paletteUnit(1).build());
        wareRepository.save(Ware.builder().name("B").type("전자제품").paletteUnit(2).build());

        // when
        List<Ware> wares = wareService.getWares();

        // then
        assertThat(wares).hasSizeGreaterThanOrEqualTo(2);
    }

    @Test
    @DisplayName("특정 타입의 물품 목록을 정상적으로 조회한다.")
    void getWaresByType_Success() {
        // given
        wareRepository.save(Ware.builder().name("A").type("부품").paletteUnit(1).build());
        wareRepository.save(Ware.builder().name("B").type("전자제품").paletteUnit(2).build());

        // when
        List<Ware> parts = wareService.getWaresByType("부품");
        List<Ware> electronics = wareService.getWaresByType("전자제품");

        // then
        assertThat(parts).extracting("type").containsOnly("부품");
        assertThat(electronics).extracting("type").containsOnly("전자제품");
    }
} 