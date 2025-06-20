package com.wms.logisticTemplate;

import com.wms.logisticTemplate.domain.model.LogisticTemplate;
import com.wms.logisticTemplate.domain.model.LogisticType;
import com.wms.logisticTemplate.domain.repository.LogisticTemplateRepository;
import com.wms.location.domain.model.Location;
import com.wms.location.domain.model.LocationType;
import com.wms.location.domain.repository.LocationRepository;
import com.wms.ware.domain.model.Ware;
import com.wms.ware.domain.repository.WareRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(com.wms.applicationInfra.config.QuerydslConfig.class)
class LogisticTemplateRepositoryTest {
    @Autowired
    private TestEntityManager entityManager;
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
    @DisplayName("LogisticTemplate 저장 및 단건 조회")
    void saveAndFind_Success() {
        LogisticTemplate template = repository.save(LogisticTemplate.builder()
                .name("템플릿")
                .type(LogisticType.INBOUND)
                .ware(ware)
                .fromLocation(from)
                .toLocation(to)
                .standardQuantity(10)
                .build());
        LogisticTemplate found = repository.findById(template.getId()).orElse(null);
        assertThat(found).isNotNull();
        assertThat(found.getName()).isEqualTo("템플릿");
    }

    @Test
    @DisplayName("EntityGraph로 조회")
    void findWithEntityGraphById_Success() {
        LogisticTemplate template = repository.save(LogisticTemplate.builder()
                .name("템플릿")
                .type(LogisticType.INBOUND)
                .ware(ware)
                .fromLocation(from)
                .toLocation(to)
                .standardQuantity(10)
                .build());
        LogisticTemplate found = repository.findWithEntityGraphById(template.getId()).orElse(null);
        assertThat(found).isNotNull();
        assertThat(found.getWare().getId()).isEqualTo(ware.getId());
    }

    @Test
    @DisplayName("타입별 페이징 조회")
    void findAllByType_Success() {
        repository.save(LogisticTemplate.builder().name("A").type(LogisticType.INBOUND).ware(ware).fromLocation(from).toLocation(to).standardQuantity(10).build());
        repository.save(LogisticTemplate.builder().name("B").type(LogisticType.OUTBOUND).ware(ware).fromLocation(from).toLocation(to).standardQuantity(20).build());
        Page<LogisticTemplate> page = repository.findAllByType(LogisticType.INBOUND, PageRequest.of(0, 10));
        assertThat(page.getContent()).allMatch(t -> t.getType() == LogisticType.INBOUND);
    }

    @Test
    @DisplayName("wareId별 페이징 조회")
    void findAllByWareId_Success() {
        repository.save(LogisticTemplate.builder().name("A").type(LogisticType.INBOUND).ware(ware).fromLocation(from).toLocation(to).standardQuantity(10).build());
        Page<LogisticTemplate> page = repository.findAllByWareId(ware.getId(), PageRequest.of(0, 10));
        assertThat(page.getContent()).allMatch(t -> t.getWare().getId().equals(ware.getId()));
    }

    @Test
    @DisplayName("전체 페이징 조회")
    void findAll_Paging_Success() {
        repository.save(LogisticTemplate.builder().name("A").type(LogisticType.INBOUND).ware(ware).fromLocation(from).toLocation(to).standardQuantity(10).build());
        repository.save(LogisticTemplate.builder().name("B").type(LogisticType.OUTBOUND).ware(ware).fromLocation(from).toLocation(to).standardQuantity(20).build());
        Page<LogisticTemplate> page = repository.findAll(PageRequest.of(0, 10));
        assertThat(page.getContent()).hasSizeGreaterThanOrEqualTo(2);
    }
} 