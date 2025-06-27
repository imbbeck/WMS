package com.wms.logisticTask.domain.repository;

import com.wms.applicationInfra.config.QuerydslConfig;
import com.wms.location.domain.model.Location;
import com.wms.location.domain.model.LocationType;
import com.wms.logisticTask.domain.model.LogisticTask;
import com.wms.logisticTask.domain.model.LogisticTaskStatus;
import com.wms.logisticTemplate.domain.model.LogisticType;
import com.wms.userInfo.domain.model.Password;
import com.wms.userInfo.domain.model.UserInfo;
import com.wms.userInfo.domain.model.UserType;
import com.wms.ware.domain.model.Ware;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@Import(QuerydslConfig.class)
@DisplayName("LogisticTaskRepository 테스트")
class LogisticTaskRepositoryTest {

    @Autowired
    private LogisticTaskRepository logisticTaskRepository;

    @Autowired
    private EntityManager em;

    private UserInfo worker;
    private Ware ware;
    private Location fromLocation;
    private Location toLocation;
    private LogisticTask task;

    @BeforeEach
    void setUp() {
        worker = UserInfo.builder()
                .username("worker1")
                .name("김작업")
                .email("worker1@test.com")
                .password(new Password("password"))
                .type(UserType.WORKER)
                .build();
        em.persist(worker);

        ware = Ware.builder()
                .name("전자제품A")
                .type("전자제품")
                .paletteUnit(10)
                .build();
        em.persist(ware);

        fromLocation = Location.builder()
                .name("창고A")
                .type(LocationType.WAREHOUSE)
                .capacity(1000)
                .coordinateX(100)
                .coordinateY(100)
                .build();
        toLocation = Location.builder()
                .name("창고B")
                .type(LocationType.WAREHOUSE)
                .capacity(500)
                .coordinateX(200)
                .coordinateY(100)
                .build();
        em.persist(fromLocation);
        em.persist(toLocation);

        task = LogisticTask.builder()
                .name("창고A → 창고B 전자제품A 이동")
                .type(LogisticType.INNER)
                .worker(worker)
                .ware(ware)
                .fromLocation(fromLocation)
                .toLocation(toLocation)
                .quantity(50)
                .scheduledDate(LocalDate.of(2025, 1, 15))
                .etd(LocalTime.of(9, 0))
                .eta(LocalTime.of(10, 0))
                .templateIdSnapshot(1)
                .build();
        em.persist(task);

        em.flush();
        em.clear();
    }

    @Test
    @DisplayName("연관 엔티티와 함께 ID로 조회")
    void findWithAllById_Success() {
        // When
        Optional<LogisticTask> result = logisticTaskRepository.findWithAllById(task.getId());

        // Then
        assertThat(result).isPresent();
        LogisticTask foundTask = result.get();
        assertThat(foundTask.getName()).isEqualTo("창고A → 창고B 전자제품A 이동");
        assertThat(foundTask.getWorker().getName()).isEqualTo("김작업");
        assertThat(foundTask.getWare().getName()).isEqualTo("전자제품A");
    }

    @Test
    @DisplayName("모든 작업 조회")
    void findAllBy_Success() {
        // When
        List<LogisticTask> result = logisticTaskRepository.findAllBy();

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("창고A → 창고B 전자제품A 이동");
    }

    @Test
    @DisplayName("특정 날짜의 작업들 조회")
    void findByScheduledDate_Success() {
        // Given
        LocalDate targetDate = LocalDate.of(2025, 1, 15);

        // When
        List<LogisticTask> result = logisticTaskRepository.findByScheduledDate(targetDate);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getScheduledDate()).isEqualTo(targetDate);
    }

    @Test
    @DisplayName("특정 상태의 작업들 조회")
    void findByStatus_Success() {
        // When
        List<LogisticTask> result = logisticTaskRepository.findByStatus(LogisticTaskStatus.PENDING);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo(LogisticTaskStatus.PENDING);
    }

    @Test
    @DisplayName("작업자별 작업들 조회")
    void findByWorker_Success() {
        // When
        List<LogisticTask> result = logisticTaskRepository.findByWorker(worker);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getWorker().getName()).isEqualTo("김작업");
    }

    @Test
    @DisplayName("특정 장소가 포함된 작업 존재 여부 확인")
    void existsByLocation_Success() {
        // Given
        Long locationId = fromLocation.getId();

        // When
        boolean exists = logisticTaskRepository.existsByFromLocationIdOrToLocationId(locationId, locationId);

        // Then
        assertThat(exists).isTrue();
    }
}