package com.wms.ware;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wms.applicationInfra.config.TestSecurityConfig;
import com.wms.applicationInfra.config.TestConfig;
import com.wms.applicationInfra.idnameMapCashing.DomainCacheManager;
import com.wms.ware.application.WareService;
import com.wms.ware.domain.model.Ware;
import com.wms.ware.dto.WareDTO;
import com.wms.ware.interfaces.WareController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Import({TestSecurityConfig.class, TestConfig.class})
@WebMvcTest(WareController.class)
class WareControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private WareService wareService;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean(name = "wareCacheManager")
    private DomainCacheManager<Long, String> wareCacheManager;

    @Test
    @DisplayName("POST /wares - 물품 생성 성공 시 201 Created 반환")
    void createWare_Success() throws Exception {
        // given
        var requestDto = WareDTO.CreateReq.builder()
                .name("노트북")
                .type("전자제품")
                .paletteUnit(10)
                .build();
        var mockWare = Ware.builder()
                .name("노트북")
                .type("전자제품")
                .paletteUnit(10)
                .build();
        given(wareService.createWare(any(WareDTO.CreateReq.class))).willReturn(mockWare);

        // when & then
        mockMvc.perform(post("/wares")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("노트북"));
    }

    @Test
    @DisplayName("POST /wares - 유효성 검증 실패 시 400 Bad Request 반환")
    void createWare_WithInvalidInput_ReturnsBadRequest() throws Exception {
        // given
        var invalidRequestDto = new WareDTO.CreateReq("", "", null); // 이름, 타입, 개수 모두 비어있음

        // when & then
        mockMvc.perform(post("/wares")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequestDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /wares - 모든 물품 목록 조회 200 OK")
    void getWares_Success() throws Exception {
        // given
        var mockWare1 = Ware.builder().name("A").type("부품").paletteUnit(1).build();
        var mockWare2 = Ware.builder().name("B").type("전자제품").paletteUnit(2).build();
        var wareList = List.of(mockWare1, mockWare2);
        given(wareService.getAllWares()).willReturn(wareList);

        // when & then
        mockMvc.perform(get("/wares"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("A"))
                .andExpect(jsonPath("$[1].name").value("B"))
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @DisplayName("GET /wares/type?type=부품 - 타입별 물품 목록 조회 200 OK")
    void getWaresByType_Success() throws Exception {
        // given
        var mockWare = Ware.builder().name("A").type("부품").paletteUnit(1).build();
        given(wareService.getWaresByType("부품")).willReturn(List.of(mockWare));

        // when & then
        mockMvc.perform(get("/wares/type")
                        .param("type", "부품")  // ✅ 변경된 부분
                        .characterEncoding("UTF-8"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("부품"));
    }

    @Test
    @DisplayName("GET /wares/{id} - 단일 물품 조회 200 OK")
    void getWare_Success() throws Exception {
        // given
        var mockWare = Ware.builder().name("A").type("부품").paletteUnit(1).build();
        given(wareService.getWareById(1L)).willReturn(mockWare);

        // when & then
        mockMvc.perform(get("/wares/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("A"));
    }

    @Test
    @DisplayName("PUT /wares/{id} - 물품 수정 200 OK")
    void updateWare_Success() throws Exception {
        // given
        var requestDto = WareDTO.UpdateReq.builder()
                .name("수정된 물품")
                .type("전자제품")
                .paletteUnit(20)
                .build();
        var updatedWare = Ware.builder()
                .name("수정된 물품")
                .type("전자제품")
                .paletteUnit(20)
                .build();
        given(wareService.updateWare(eq(1L), any(WareDTO.UpdateReq.class))).willReturn(updatedWare);

        // when & then
        mockMvc.perform(put("/wares/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("수정된 물품"))
                .andExpect(jsonPath("$.paletteUnit").value(20));
    }

    @Test
    @DisplayName("DELETE /wares/{id} - 물품 삭제 204 No Content")
    void deleteWare_Success() throws Exception {
        // given
        doNothing().when(wareService).deleteWare(1L);

        // when & then
        mockMvc.perform(delete("/wares/{id}", 1L))
                .andExpect(status().isNoContent());
    }
} 