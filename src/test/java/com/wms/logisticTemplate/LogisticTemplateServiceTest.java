package com.wms.logisticTemplate;

import com.wms.location.domain.model.LocationConnection;
import com.wms.location.domain.repository.LocationConnectionRepository;
import com.wms.logisticTemplate.application.LogisticTemplateService;
import com.wms.logisticTemplate.domain.exception.LogisticTemplateException;
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
    private Location inboundLocation;
    private Location warehouseLocation;
    private Location outboundLocation;
	@Autowired
	private LocationConnectionRepository locationConnectionRepository;

    @BeforeEach
    void setUp() {
        ware = wareRepository.save(Ware.builder().name("물품").type("부품").paletteUnit(1).build());
        inboundLocation = locationRepository.save(Location.builder()
                .name("입고처").type(LocationType.INBOUND)
                .coordinateX(1).coordinateY(1).build());
        warehouseLocation = locationRepository.save(Location.builder()
                .name("창고").type(LocationType.WAREHOUSE).capacity(100)
                .coordinateX(2).coordinateY(2).build());
        outboundLocation = locationRepository.save(Location.builder()
                .name("출고처").type(LocationType.OUTBOUND)
                .coordinateX(3).coordinateY(3).build());
    }

    @Test
    @DisplayName("INBOUND 타입으로 LogisticTemplate를 성공적으로 생성한다")
    void createInboundTemplate_Success() {
        // given
        var req = LogisticTemplateDTO.CreateReq.builder()
                .name("입고 템플릿")
                .type(LogisticType.INBOUND)
                .wareId(ware.getId())
                .fromLocationId(inboundLocation.getId())
                .toLocationId(warehouseLocation.getId())
                .standardQuantity(10)
                .build();

        // when
        LogisticTemplate saved = service.create(req);

        // then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getName()).isEqualTo("입고 템플릿");
        assertThat(saved.getType()).isEqualTo(LogisticType.INBOUND);
        assertThat(saved.getWare().getId()).isEqualTo(ware.getId());
    }

    @Test
    @DisplayName("OUTBOUND 타입으로 LogisticTemplate를 성공적으로 생성한다")
    void createOutboundTemplate_Success() {
        // given
        var req = LogisticTemplateDTO.CreateReq.builder()
                .name("출고 템플릿")
                .type(LogisticType.OUTBOUND)
                .wareId(ware.getId())
                .fromLocationId(warehouseLocation.getId())
                .toLocationId(outboundLocation.getId())
                .standardQuantity(10)
                .build();

        // when
        LogisticTemplate saved = service.create(req);

        // then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getName()).isEqualTo("출고 템플릿");
        assertThat(saved.getType()).isEqualTo(LogisticType.OUTBOUND);
    }

    @Test
    @DisplayName("INNER 타입으로 LogisticTemplate를 성공적으로 생성한다")
    void createInnerTemplate_Success() {
        // given
        Location anotherWarehouse = locationRepository.save(Location.builder()
                .name("다른창고").type(LocationType.WAREHOUSE).capacity(100)
                .coordinateX(4).coordinateY(4).build());
        
        var req = LogisticTemplateDTO.CreateReq.builder()
                .name("내부 이동 템플릿")
                .type(LogisticType.INNER)
                .wareId(ware.getId())
                .fromLocationId(warehouseLocation.getId())
                .toLocationId(anotherWarehouse.getId())
                .standardQuantity(10)
                .build();

        // when
        LogisticTemplate saved = service.create(req);

        // then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getName()).isEqualTo("내부 이동 템플릿");
        assertThat(saved.getType()).isEqualTo(LogisticType.INNER);
    }

    @Test
    @DisplayName("INBOUND 타입에 잘못된 location 조합을 사용하면 예외가 발생한다")
    void createInboundTemplateWithInvalidLocations_ThrowsException() {
        // given
        var req = LogisticTemplateDTO.CreateReq.builder()
                .name("잘못된 입고 템플릿")
                .type(LogisticType.INBOUND)
                .wareId(ware.getId())
                .fromLocationId(warehouseLocation.getId()) // 잘못된 출발지 (창고가 아닌 입고처여야 함)
                .toLocationId(warehouseLocation.getId())
                .standardQuantity(10)
                .build();

        // when & then
        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(LogisticTemplateException.NotMatchedLocationWithTypeEx.class)
                .hasMessageContaining("INBOUND 작업은 INBOUND에서 WAREHOUSE로만 가능합니다.");
    }

    @Test
    @DisplayName("OUTBOUND 타입에 잘못된 location 조합을 사용하면 예외가 발생한다")
    void createOutboundTemplateWithInvalidLocations_ThrowsException() {
        // given
        var req = LogisticTemplateDTO.CreateReq.builder()
                .name("잘못된 출고 템플릿")
                .type(LogisticType.OUTBOUND)
                .wareId(ware.getId())
                .fromLocationId(inboundLocation.getId()) // 잘못된 출발지 (창고여야 함)
                .toLocationId(outboundLocation.getId())
                .standardQuantity(10)
                .build();

        // when & then
        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(LogisticTemplateException.NotMatchedLocationWithTypeEx.class)
                .hasMessageContaining("OUTBOUND 작업은 WAREHOUSE에서 OUTBOUND로만 가능합니다.");
    }

    @Test
    @DisplayName("INNER 타입에 잘못된 location 조합을 사용하면 예외가 발생한다")
    void createInnerTemplateWithInvalidLocations_ThrowsException() {
        // given
        var req = LogisticTemplateDTO.CreateReq.builder()
                .name("잘못된 내부 이동 템플릿")
                .type(LogisticType.INNER)
                .wareId(ware.getId())
                .fromLocationId(inboundLocation.getId()) // 잘못된 출발지 (창고여야 함)
                .toLocationId(warehouseLocation.getId())
                .standardQuantity(10)
                .build();

        // when & then
        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(LogisticTemplateException.NotMatchedLocationWithTypeEx.class)
                .hasMessageContaining("INNER 작업은 WAREHOUSE에서 WAREHOUSE로만 가능합니다.");
    }

    @Test
    @DisplayName("LogisticTemplate를 수정한다")
    void update_Success() {
        // given
        LogisticTemplate template = repository.save(LogisticTemplate.builder()
                .name("수정전")
                .type(LogisticType.INNER)
                .ware(ware)
                .fromLocation(warehouseLocation)
                .toLocation(warehouseLocation)
                .standardQuantity(10)
                .build());
        
        var req = LogisticTemplateDTO.UpdateReq.builder()
                .name("수정후")
                .trt(5) // TRT는 임의로 설정
                .standardQuantity(20)
                .build();

        // when
        LogisticTemplate updated = service.update(template.getId(), req);

        // then
        assertThat(updated.getName()).isEqualTo("수정후");
        assertThat(updated.getType()).isEqualTo(LogisticType.INNER);
        assertThat(updated.getStandardQuantity()).isEqualTo(20);
    }

    @Test
    @DisplayName("LogisticTemplate 단건 조회")
    void findById_Success() {
        // given
        LogisticTemplate template = repository.save(LogisticTemplate.builder()
                .name("조회")
                .type(LogisticType.INNER)
                .ware(ware)
                .fromLocation(warehouseLocation)
                .toLocation(warehouseLocation)
                .standardQuantity(10)
                .build());

        // when
        LogisticTemplate found = service.findById(template.getId());

        // then
        assertThat(found.getId()).isEqualTo(template.getId());
    }

    @Test
    @DisplayName("전체 페이징 조회")
    void getTemplates_Success() {
        // given
        repository.save(LogisticTemplate.builder().name("A").type(LogisticType.INNER).ware(ware).fromLocation(warehouseLocation).toLocation(warehouseLocation).standardQuantity(10).build());
        repository.save(LogisticTemplate.builder().name("B").type(LogisticType.INNER).ware(ware).fromLocation(warehouseLocation).toLocation(warehouseLocation).standardQuantity(20).build());

        // when
        Page<LogisticTemplate> page = service.getTemplates(PageRequest.of(0, 10));

        // then
        assertThat(page.getContent()).hasSizeGreaterThanOrEqualTo(2);
    }

    @Test
    @DisplayName("타입별 페이징 조회")
    void getTemplatesByType_Success() {
        // given
        repository.save(LogisticTemplate.builder().name("A").type(LogisticType.INNER).ware(ware).fromLocation(warehouseLocation).toLocation(warehouseLocation).standardQuantity(10).build());
        repository.save(LogisticTemplate.builder().name("B").type(LogisticType.INNER).ware(ware).fromLocation(warehouseLocation).toLocation(warehouseLocation).standardQuantity(20).build());

        // when
        Page<LogisticTemplate> page = service.getTemplatesByType(LogisticType.INNER, PageRequest.of(0, 10));

        // then
        assertThat(page.getContent()).allMatch(t -> t.getType() == LogisticType.INNER);
    }

    @Test
    @DisplayName("wareId별 페이징 조회")
    void getTemplatesByWareId_Success() {
        // given
        repository.save(LogisticTemplate.builder().name("A").type(LogisticType.INNER).ware(ware).fromLocation(warehouseLocation).toLocation(warehouseLocation).standardQuantity(10).build());

        // when
        Page<LogisticTemplate> page = service.getTemplatesByWareId(ware.getId(), PageRequest.of(0, 10));

        // then
        assertThat(page.getContent()).allMatch(t -> t.getWare().getId().equals(ware.getId()));
    }

    @Test
    @DisplayName("LogisticTemplate를 삭제한다")
    void delete_Success() {
        // given
        LogisticTemplate template = repository.save(LogisticTemplate.builder()
                .name("삭제")
                .type(LogisticType.INNER)
                .ware(ware)
                .fromLocation(warehouseLocation)
                .toLocation(warehouseLocation)
                .standardQuantity(10)
                .build());

        // when
        service.delete(template.getId());

        // then
        assertThat(repository.findById(template.getId())).isEmpty();
    }

    @Test
    @DisplayName("TRT 필드가 자동으로 설정된다 (LocationConnection 존재시)")
    void createTemplate_TrtAutoSet() {
        // When
        Location anotherWarehouse = locationRepository.save(Location.builder()
                .name("다른창고").type(LocationType.WAREHOUSE).capacity(100)
                .coordinateX(4).coordinateY(4).build());

        locationConnectionRepository.save(LocationConnection.builder()
                .locationId1(Math.min(anotherWarehouse.getId(), warehouseLocation.getId()))
                .locationId2(Math.max(anotherWarehouse.getId(), warehouseLocation.getId()))
                .trt(15) // 평균 소요시간 15분
                .build()
        );

        // given
        var req = LogisticTemplateDTO.CreateReq.builder()
                .name("TRT 테스트 템플릿")
                .type(LogisticType.INNER)
                .wareId(ware.getId())
                .fromLocationId(warehouseLocation.getId())
                .toLocationId(anotherWarehouse.getId())
                .standardQuantity(10)
                .build();

        // when
        LogisticTemplate saved = service.create(req);

        System.out.println(saved.getTrt()); // 디버깅용 출력

        // then
        assertThat(saved.getTrt()).isEqualTo(15); // LocationConnection에서 설정한 TRT 값이 자동으로 설정됨
    }
}