package com.wms.logisticTask.batch;

import com.wms.location.domain.model.Location;
import com.wms.location.domain.model.LocationType;
import com.wms.logisticTask.domain.model.LogisticTask;
import com.wms.logisticTask.domain.model.LogisticTaskStatus;
import com.wms.logisticTask.domain.repository.LogisticTaskRepository;
import com.wms.logisticTemplate.domain.model.LogisticType;
import com.wms.userInfo.domain.model.UserInfo;
import com.wms.userInfo.domain.model.UserType;
import com.wms.userInfo.domain.model.Password;
import com.wms.ware.domain.model.Ware;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LogisticTaskHistoryReaderTest {

    @Mock
    private LogisticTaskRepository logisticTaskRepository;

    @InjectMocks
    private LogisticTaskHistoryReader reader;

    private LogisticTask testTask1;
    private LogisticTask testTask2;

    @BeforeEach
    void setUp() throws Exception {
        setupTestEntities();
        
        // StepExecutionContext 값들을 리플렉션으로 설정
        setField(reader, "minId", 1L);
        setField(reader, "maxId", 100L);
        setField(reader, "targetDateStr", "2025-01-01");
    }

    @Test
    void read_정상데이터_반환() throws Exception {
        // Given
        Page<LogisticTask> page = new PageImpl<>(Arrays.asList(testTask1, testTask2));
        when(logisticTaskRepository.findByScheduledDateAndIdBetween(
                any(LocalDate.class), eq(1L), eq(100L), any(Pageable.class)))
                .thenReturn(page);

        // When
        LogisticTask result1 = reader.read();
        LogisticTask result2 = reader.read();
        LogisticTask result3 = reader.read();

        // Then
        assertThat(result1).isEqualTo(testTask1);
        assertThat(result2).isEqualTo(testTask2);
        assertThat(result3).isNull(); // 더 이상 데이터 없음
    }

    @Test
    void read_빈데이터_null반환() throws Exception {
        // Given
        Page<LogisticTask> emptyPage = new PageImpl<>(Collections.emptyList());
        when(logisticTaskRepository.findByScheduledDateAndIdBetween(
                any(LocalDate.class), eq(1L), eq(100L), any(Pageable.class)))
                .thenReturn(emptyPage);

        // When
        LogisticTask result = reader.read();

        // Then
        assertThat(result).isNull();
    }

    @Test
    void read_targetDateStr이_null인경우_어제날짜사용() throws Exception {
        // Given
        setField(reader, "targetDateStr", null);
        setField(reader, "initialized", false); // 재초기화를 위해
        
        LocalDate expectedDate = LocalDate.now().minusDays(1);
        
        Page<LogisticTask> page = new PageImpl<>(Arrays.asList(testTask1));
        when(logisticTaskRepository.findByScheduledDateAndIdBetween(
                eq(expectedDate), eq(1L), eq(100L), any(Pageable.class)))
                .thenReturn(page);

        // When
        LogisticTask result = reader.read();

        // Then
        assertThat(result).isEqualTo(testTask1);
    }

    @Test
    void read_minId나_maxId가_null인경우_null반환() throws Exception {
        // Given
        setField(reader, "minId", null);
        setField(reader, "initialized", false);

        // When
        LogisticTask result = reader.read();

        // Then
        assertThat(result).isNull();
    }

    private void setupTestEntities() {
        UserInfo testWorker = UserInfo.builder()
                .username("testworker")
                .name("테스트 작업자")
                .email("test@test.com")
                .password(Password.builder().value("password").build())
                .type(UserType.WORKER)
                .build();
        setIdField(testWorker, 1L);

        Ware testWare = Ware.builder()
                .name("테스트 물품")
                .type("전자제품")
                .paletteUnit(10)
                .build();
        setIdField(testWare, 1L);

        Location fromLocation = Location.builder()
                .name("출발지")
                .type(LocationType.WAREHOUSE)
                .capacity(1000)
                .coordinateX(100)
                .coordinateY(200)
                .build();
        setIdField(fromLocation, 1L);

        Location toLocation = Location.builder()
                .name("도착지")
                .type(LocationType.WAREHOUSE)
                .capacity(800)
                .coordinateX(300)
                .coordinateY(400)
                .build();
        setIdField(toLocation, 2L);

        testTask1 = createTestTask(1L, "작업1", testWorker, testWare, fromLocation, toLocation);
        testTask2 = createTestTask(2L, "작업2", testWorker, testWare, fromLocation, toLocation);
    }

    private LogisticTask createTestTask(Long id, String name, UserInfo worker, Ware ware, 
                                       Location fromLocation, Location toLocation) {
        LogisticTask task = LogisticTask.builder()
                .name(name)
                .type(LogisticType.INNER)
                .worker(worker)
                .ware(ware)
                .fromLocation(fromLocation)
                .toLocation(toLocation)
                .quantity(10)
                .scheduledDate(LocalDate.of(2025, 1, 1))
                .etd(LocalTime.of(9, 0))
                .eta(LocalTime.of(10, 0))
                .status(LogisticTaskStatus.PENDING)
                .templateIdSnapshot(100)
                .build();
        
        return setIdField(task, id);
    }

    private <T> T setIdField(T entity, Long id) {
        try {
            var field = entity.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
            return entity;
        } catch (Exception e) {
            return entity;
        }
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        var field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
