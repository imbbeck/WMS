package com.wms.logisticTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wms.applicationInfra.config.TestSecurityConfig;
import com.wms.applicationInfra.idnameMapCashing.DomainCacheManager;
import com.wms.logisticTemplate.application.LogisticTemplateService;
import com.wms.logisticTemplate.domain.model.LogisticTemplate;
import com.wms.logisticTemplate.domain.model.LogisticType;
import com.wms.logisticTemplate.dto.LogisticTemplateDTO;
import com.wms.logisticTemplate.interfaces.LogisticTemplateController;
import com.wms.location.domain.model.Location;
import com.wms.location.domain.model.LocationType;
import com.wms.ware.domain.model.Ware;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Import(TestSecurityConfig.class)
@WebMvcTest(LogisticTemplateController.class)
class LogisticTemplateControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private LogisticTemplateService service;
    @Autowired
    private ObjectMapper objectMapper;
    @MockBean
    private DomainCacheManager<Long, String> domainCacheManager;

    private final Ware ware = Ware.builder().name("물품").type("부품").paletteUnit(1).build();
    private final Location from = Location.builder().name("출발지").type(LocationType.WAREHOUSE).capacity(10).coordinateX(1).coordinateY(1).build();
    private final Location to = Location.builder().name("도착지").type(LocationType.WAREHOUSE).capacity(10).coordinateX(2).coordinateY(2).build();

    @Test
    @DisplayName("POST /logistic-templates - 생성 성공 시 201 Created 반환")
    void create_Success() throws Exception {
        var requestDto = LogisticTemplateDTO.CreateReq.builder()
                .name("템플릿")
                .type(LogisticType.INBOUND)
                .wareId(1L)
                .fromLocationId(2L)
                .toLocationId(3L)
                .standardQuantity(10)
                .build();
        var mockTemplate = LogisticTemplate.builder()
                .name("템플릿")
                .type(LogisticType.INBOUND)
                .ware(ware)
                .fromLocation(from)
                .toLocation(to)
                .standardQuantity(10)
                .build();
        given(service.create(any(LogisticTemplateDTO.CreateReq.class))).willReturn(mockTemplate);

        mockMvc.perform(post("/logistic-templates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("템플릿"));
    }

    @Test
    @DisplayName("POST /logistic-templates - 유효성 검증 실패 시 400 Bad Request 반환")
    void create_WithInvalidInput_ReturnsBadRequest() throws Exception {
        var invalidRequestDto = new LogisticTemplateDTO.CreateReq("", null, null, null, null, null);
        mockMvc.perform(post("/logistic-templates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequestDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /logistic-templates/{id} - 단건 조회 200 OK")
    void getTemplate_Success() throws Exception {
        var mockTemplate = LogisticTemplate.builder().name("A").type(LogisticType.INBOUND).ware(ware).fromLocation(from).toLocation(to).standardQuantity(10).build();
        given(service.findById(1L)).willReturn(mockTemplate);
        mockMvc.perform(get("/logistic-templates/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath(".name").value("A"));
    }

    @Test
    @DisplayName("GET /logistic-templates - 전체 페이징 조회 200 OK")
    void getTemplates_Success() throws Exception {
        var mockTemplate = LogisticTemplate.builder().name("A").type(LogisticType.INBOUND).ware(ware).fromLocation(from).toLocation(to).standardQuantity(10).build();
        given(service.getTemplates(any(Pageable.class))).willReturn(new PageImpl<>(List.of(mockTemplate)));
        mockMvc.perform(get("/logistic-templates")
                        .param("page", "0").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("A"));
    }

    @Test
    @DisplayName("GET /logistic-templates/type?type=INBOUND - 타입별 페이징 조회 200 OK")
    void getTemplatesByType_Success() throws Exception {
        var mockTemplate = LogisticTemplate.builder().name("A").type(LogisticType.INBOUND).ware(ware).fromLocation(from).toLocation(to).standardQuantity(10).build();
        given(service.getTemplatesByType(eq(LogisticType.INBOUND), any(Pageable.class))).willReturn(new PageImpl<>(List.of(mockTemplate)));
        mockMvc.perform(get("/logistic-templates/type")
                        .param("type", "INBOUND")
                        .param("page", "0").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].type").value("INBOUND"));
    }

    @Test
    @DisplayName("GET /logistic-templates/ware?wareId=1 - wareId별 페이징 조회 200 OK")
    void getTemplatesByWareId_Success() throws Exception {
        var mockTemplate = LogisticTemplate.builder().name("A").type(LogisticType.INBOUND).ware(ware).fromLocation(from).toLocation(to).standardQuantity(10).build();
        given(service.getTemplatesByWareId(eq(1L), any(Pageable.class))).willReturn(new PageImpl<>(List.of(mockTemplate)));
        mockMvc.perform(get("/logistic-templates/ware")
                        .param("wareId", "1")
                        .param("page", "0").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].type").value("INBOUND"));
    }

    @Test
    @DisplayName("PUT /logistic-templates/{id} - 수정 200 OK")
    void update_Success() throws Exception {
        var requestDto = LogisticTemplateDTO.UpdateReq.builder()
                .name("수정된 템플릿")
                .type(LogisticType.OUTBOUND)
                .standardQuantity(20)
                .build();
        var updatedTemplate = LogisticTemplate.builder()
                .name("수정된 템플릿")
                .type(LogisticType.OUTBOUND)
                .ware(ware)
                .fromLocation(from)
                .toLocation(to)
                .standardQuantity(20)
                .build();
        given(service.update(eq(1L), any(LogisticTemplateDTO.UpdateReq.class))).willReturn(updatedTemplate);
        mockMvc.perform(put("/logistic-templates/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath(".name").value("수정된 템플릿"))
                .andExpect(jsonPath(".standardQuantity").value(20));
    }

    @Test
    @DisplayName("DELETE /logistic-templates/{id} - 삭제 204 No Content")
    void delete_Success() throws Exception {
        doNothing().when(service).delete(1L);
        mockMvc.perform(delete("/logistic-templates/{id}", 1L))
                .andExpect(status().isNoContent());
    }
} 