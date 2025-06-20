package com.wms.logisticTemplate;

import com.wms.logisticTemplate.application.LogisticTemplateService;
import com.wms.logisticTemplate.domain.model.LogisticTemplate;
import com.wms.logisticTemplate.domain.model.LogisticType;
import com.wms.logisticTemplate.domain.repository.LogisticTemplateRepository;
import com.wms.logisticTemplate.dto.LogisticTemplateDTO;
import com.wms.location.domain.model.Location;
import com.wms.location.domain.model.LocationType;
import com.wms.location.domain.repository.LocationRepository;
import com.wms.ware.domain.model.Ware;
import com.wms.ware.domain.repository.WareRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class LogisticTemplateServiceTest {
    @Autowired
    private LogisticTemplateService service;
    @Autowired
    private LogisticTemplateRepository repository;
    @Autowired
    private WareRepository wareRepository;
    @Autowired
    private LocationRepository locationRepository;

    private Ware ware;
    private Location from;
    private Location to;

    @BeforeEach
    void setUp() {
        ware = wareRepository.save(Ware.builder().name("물품").type("부품").paletteUnit(1).build());
        from = locationRepository.save(Location.builder().name("출발지").type(LocationType.WAREHOUSE).capacity(10).coordinateX(1).coordinateY(1).build());
        to = locationRepository.save(Location.builder().name("도착지").type(LocationType.WAREHOUSE).capacity(10).coordinateX(2).coordinateY(2).build());
    }

    @Test
    @DisplayName("LogisticTemplate를 성공적으로 생성한다.")
    void create_Success() {
        var req = LogisticTemplateDTO.CreateReq.builder()
                .name("템플릿")
                .type(LogisticType.INBOUND)
                .wareId(ware.getId())
                .fromLocationId(from.getId())
                .toLocationId(to.getId())
                .standardQuantity(10)
                .build();
        LogisticTemplate saved = service.create(req);
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getName()).isEqualTo("템플릿");
        assertThat(saved.getType()).isEqualTo(LogisticType.INBOUND);
        assertThat(saved.getWare().getId()).isEqualTo(ware.getId());
    }

    @Test
    @DisplayName("LogisticTemplate를 수정한다.")
    void update_Success() {
        LogisticTemplate template = repository.save(LogisticTemplate.builder()
                .name("수정전")
                .type(LogisticType.INBOUND)
                .ware(ware)
                .fromLocation(from)
                .toLocation(to)
                .standardQuantity(10)
                .build());
        var req = LogisticTemplateDTO.UpdateReq.builder()
                .name("수정후")
                .type(LogisticType.OUTBOUND)
                .standardQuantity(20)
                .build();
        LogisticTemplate updated = service.update(template.getId(), req);
        assertThat(updated.getName()).isEqualTo("수정후");
        assertThat(updated.getType()).isEqualTo(LogisticType.OUTBOUND);
        assertThat(updated.getStandardQuantity()).isEqualTo(20);
    }

    @Test
    @DisplayName("LogisticTemplate 단건 조회")
    void findById_Success() {
        LogisticTemplate template = repository.save(LogisticTemplate.builder()
                .name("조회")
                .type(LogisticType.INNER)
                .ware(ware)
                .fromLocation(from)
                .toLocation(to)
                .standardQuantity(10)
                .build());
        LogisticTemplate found = service.findById(template.getId());
        assertThat(found.getId()).isEqualTo(template.getId());
    }

    @Test
    @DisplayName("전체 페이징 조회")
    void getTemplates_Success() {
        repository.save(LogisticTemplate.builder().name("A").type(LogisticType.INBOUND).ware(ware).fromLocation(from).toLocation(to).standardQuantity(10).build());
        repository.save(LogisticTemplate.builder().name("B").type(LogisticType.OUTBOUND).ware(ware).fromLocation(from).toLocation(to).standardQuantity(20).build());
        Page<LogisticTemplate> page = service.getTemplates(PageRequest.of(0, 10));
        assertThat(page.getContent()).hasSizeGreaterThanOrEqualTo(2);
    }

    @Test
    @DisplayName("타입별 페이징 조회")
    void getTemplatesByType_Success() {
        repository.save(LogisticTemplate.builder().name("A").type(LogisticType.INBOUND).ware(ware).fromLocation(from).toLocation(to).standardQuantity(10).build());
        repository.save(LogisticTemplate.builder().name("B").type(LogisticType.OUTBOUND).ware(ware).fromLocation(from).toLocation(to).standardQuantity(20).build());
        Page<LogisticTemplate> page = service.getTemplatesByType(LogisticType.INBOUND, PageRequest.of(0, 10));
        assertThat(page.getContent()).allMatch(t -> t.getType() == LogisticType.INBOUND);
    }

    @Test
    @DisplayName("wareId별 페이징 조회")
    void getTemplatesByWareId_Success() {
        repository.save(LogisticTemplate.builder().name("A").type(LogisticType.INBOUND).ware(ware).fromLocation(from).toLocation(to).standardQuantity(10).build());
        Page<LogisticTemplate> page = service.getTemplatesByWareId(ware.getId(), PageRequest.of(0, 10));
        assertThat(page.getContent()).allMatch(t -> t.getWare().getId().equals(ware.getId()));
    }

    @Test
    @DisplayName("LogisticTemplate를 삭제한다.")
    void delete_Success() {
        LogisticTemplate template = repository.save(LogisticTemplate.builder()
                .name("삭제")
                .type(LogisticType.INBOUND)
                .ware(ware)
                .fromLocation(from)
                .toLocation(to)
                .standardQuantity(10)
                .build());
        service.delete(template.getId());
        assertThat(repository.findById(template.getId())).isEmpty();
    }
} 