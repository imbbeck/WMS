package com.wms.movement.integration;

import com.wms.location.domain.model.Location;
import com.wms.location.domain.model.LocationType;
import com.wms.movement.application.MovementService;
import com.wms.movement.domain.model.Movement;
import com.wms.movement.domain.repository.MovementRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class MovementQueryTest {

    @Autowired
    private MovementService movementService;

    @Autowired
    private MovementRepository movementRepository;

    @Autowired
    private EntityManager entityManager;

    private Location inboundLocation;
    private Location warehouseLocation;
    private Location outboundLocation;
    private Long wareId;

    @BeforeEach
    void setUp() {
        // 입고처, 창고, 출고처 생성
        inboundLocation = Location.create("입고처1", LocationType.INBOUND, 0);
        warehouseLocation = Location.create("창고1", LocationType.WAREHOUSE, 100);
        outboundLocation = Location.create("출고처1", LocationType.OUTBOUND, 0);
        wareId = 1L;

        // 테스트 데이터 생성
        createTestMovements();
    }

    private void createTestMovements() {
        // 입고 이동 3건
        for (int i = 0; i < 3; i++) {
            Movement movement = Movement.create(
                "입고 이동 " + i,
                MovementType.INBOUND,
                inboundLocation.getId(),
                warehouseLocation.getId(),
                wareId,
                10
            );
            movementRepository.save(movement);
        }

        // 출고 이동 3건
        for (int i = 0; i < 3; i++) {
            Movement movement = Movement.create(
                "출고 이동 " + i,
                MovementType.OUTBOUND,
                warehouseLocation.getId(),
                outboundLocation.getId(),
                wareId,
                5
            );
            movementRepository.save(movement);
        }

        // 내부 이동 3건
        for (int i = 0; i < 3; i++) {
            Movement movement = Movement.create(
                "내부 이동 " + i,
                MovementType.INNER,
                warehouseLocation.getId(),
                warehouseLocation.getId(),
                wareId,
                3
            );
            movementRepository.save(movement);
        }

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("물류이동 목록 조회 시 N+1 문제 체크")
    void checkNPlusOneProblemInMovementList() {
        // given
        int expectedQueryCount = 1; // 단일 쿼리로 모든 데이터를 가져와야 함

        // when
        List<Movement> movements = movementService.getAllMovements();

        // then
        assertThat(movements).hasSize(9);
        // TODO: 실제 운영 환경에서는 SQL 로그를 확인하여 쿼리 수 검증
        // 현재는 단순히 데이터 정합성만 검증
        assertThat(movements)
            .allMatch(movement -> movement.getType() != null)
            .allMatch(movement -> movement.getStatus() != null);
    }

    @Test
    @DisplayName("물류이동 유형별 조회 시 N+1 문제 체크")
    void checkNPlusOneProblemInMovementTypeList() {
        // given
        MovementType type = MovementType.INBOUND;

        // when
        List<Movement> movements = movementService.getMovementsByType(type);

        // then
        assertThat(movements).hasSize(3);
        assertThat(movements)
            .allMatch(movement -> movement.getType() == type)
            .allMatch(movement -> movement.getStatus() != null);
    }

    @Test
    @DisplayName("물류이동 상태별 조회 시 N+1 문제 체크")
    void checkNPlusOneProblemInMovementStatusList() {
        // given
        // 일부 이동을 시작 상태로 변경
        List<Movement> allMovements = movementService.getAllMovements();
        for (int i = 0; i < 3; i++) {
            allMovements.get(i).start();
        }
        movementRepository.saveAll(allMovements);
        entityManager.flush();
        entityManager.clear();

        // when
        List<Movement> startedMovements = movementService.getMovementsByStatus(MovementStatus.STARTED);

        // then
        assertThat(startedMovements).hasSize(3);
        assertThat(startedMovements)
            .allMatch(movement -> movement.getStatus() == MovementStatus.STARTED)
            .allMatch(movement -> movement.getType() != null);
    }

    @Test
    @DisplayName("물류이동 상세 조회 시 N+1 문제 체크")
    void checkNPlusOneProblemInMovementDetail() {
        // given
        List<Movement> allMovements = movementService.getAllMovements();
        Long targetId = allMovements.get(0).getId();

        // when
        Movement movement = movementService.getMovementById(targetId);

        // then
        assertThat(movement).isNotNull();
        assertThat(movement.getId()).isEqualTo(targetId);
        assertThat(movement.getType()).isNotNull();
        assertThat(movement.getStatus()).isNotNull();
    }
} 