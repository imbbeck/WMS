package com.wms.logisticTask.batch;

import com.wms.location.domain.model.Location;
import com.wms.location.domain.model.LocationType;
import com.wms.logisticTask.domain.model.LogisticTask;
import com.wms.logisticTask.domain.model.LogisticTaskHistory;
import com.wms.logisticTask.domain.model.LogisticTaskStatus;
import com.wms.logisticTemplate.domain.model.LogisticType;
import com.wms.userInfo.domain.model.UserInfo;
import com.wms.userInfo.domain.model.UserType;
import com.wms.userInfo.domain.model.Password;
import com.wms.ware.domain.model.Ware;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class LogisticTaskHistoryProcessorTest {

    @InjectMocks
    private LogisticTaskHistoryProcessor processor;

    @Test
    void process_정상변환() throws Exception {
        // Given
        LogisticTask task = createTestTask();

        // When
        LogisticTaskHistory result = processor.process(task);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("테스트 작업");
        assertThat(result.getOriginalStatus()).isEqualTo(LogisticTaskStatus.PENDING);
        assertThat(result.getFinalStatus()).isEqualTo(LogisticTaskStatus.EXPIRED);
        assertThat(result.getSettlementDate()).isEqualTo(LocalDate.now());
    }

    @Test
    void process_완료된작업() throws Exception {
        // Given
        LogisticTask task = createTestTask();
        task.initiateTask(LocalTime.of(9, 5));
        task.completeTask(LocalTime.of(10, 15));

        // When
        LogisticTaskHistory result = processor.process(task);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getFinalStatus()).isEqualTo(LogisticTaskStatus.COMPLETED);
        assertThat(result.getAtd()).isEqualTo(LocalTime.of(9, 5));
        assertThat(result.getAta()).isEqualTo(LocalTime.of(10, 15));
    }

    @Test
    void process_상태변환_검증() throws Exception {
        // PENDING -> EXPIRED
        LogisticTask pendingTask = createTestTaskWithStatus(LogisticTaskStatus.PENDING);
        LogisticTaskHistory pendingResult = processor.process(pendingTask);
        assertThat(pendingResult.getFinalStatus()).isEqualTo(LogisticTaskStatus.EXPIRED);

        // COMPLETED -> COMPLETED
        LogisticTask completedTask = createTestTaskWithStatus(LogisticTaskStatus.COMPLETED);
        LogisticTaskHistory completedResult = processor.process(completedTask);
        assertThat(completedResult.getFinalStatus()).isEqualTo(LogisticTaskStatus.COMPLETED);

        // FAILED -> FAILED
        LogisticTask failedTask = createTestTaskWithStatus(LogisticTaskStatus.FAILED);
        LogisticTaskHistory failedResult = processor.process(failedTask);
        assertThat(failedResult.getFinalStatus()).isEqualTo(LogisticTaskStatus.FAILED);
    }

    @Test
    void process_templateIdSnapshot_null처리() throws Exception {
        // Given
        LogisticTask task = createTestTaskWithNullTemplate();

        // When
        LogisticTaskHistory result = processor.process(task);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTemplateIdSnapshot()).isNull();
    }

    private LogisticTask createTestTask() {
        return createTestTaskWithStatus(LogisticTaskStatus.PENDING);
    }

    private LogisticTask createTestTaskWithStatus(LogisticTaskStatus status) {
        UserInfo worker = createWorker();
        Ware ware = createWare();
        Location fromLocation = createLocation(1L, "출발지");
        Location toLocation = createLocation(2L, "도착지");

        LogisticTask task = LogisticTask.builder()
                .name("테스트 작업")
                .type(LogisticType.INNER)
                .worker(worker)
                .ware(ware)
                .fromLocation(fromLocation)
                .toLocation(toLocation)
                .quantity(10)
                .scheduledDate(LocalDate.now().minusDays(1))
                .etd(LocalTime.of(9, 0))
                .eta(LocalTime.of(10, 0))
                .status(status)
                .templateIdSnapshot(100)
                .build();

        setField(task, "id", 1L);
        return task;
    }

    private LogisticTask createTestTaskWithNullTemplate() {
        UserInfo worker = createWorker();
        Ware ware = createWare();
        Location fromLocation = createLocation(1L, "출발지");
        Location toLocation = createLocation(2L, "도착지");

        LogisticTask task = LogisticTask.builder()
                .name("테스트 작업")
                .type(LogisticType.INNER)
                .worker(worker)
                .ware(ware)
                .fromLocation(fromLocation)
                .toLocation(toLocation)
                .quantity(10)
                .scheduledDate(LocalDate.now().minusDays(1))
                .etd(LocalTime.of(9, 0))
                .eta(LocalTime.of(10, 0))
                .status(LogisticTaskStatus.PENDING)
                .templateIdSnapshot(null)
                .build();

        setField(task, "id", 1L);
        return task;
    }

    private UserInfo createWorker() {
        UserInfo worker = UserInfo.builder()
                .username("testworker")
                .name("테스트 작업자")
                .email("test@test.com")
                .password(Password.builder().value("password").build())
                .type(UserType.WORKER)
                .build();
        setField(worker, "id", 1L);
        return worker;
    }

    private Ware createWare() {
        Ware ware = Ware.builder()
                .name("테스트 물품")
                .type("전자제품")
                .paletteUnit(10)
                .build();
        setField(ware, "id", 1L);
        return ware;
    }

    private Location createLocation(Long id, String name) {
        Location location = Location.builder()
                .name(name)
                .type(LocationType.WAREHOUSE)
                .capacity(1000)
                .coordinateX(100)
                .coordinateY(200)
                .build();
        setField(location, "id", id);
        return location;
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            var field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (NoSuchFieldException e) {
            try {
                var field = target.getClass().getSuperclass().getDeclaredField(fieldName);
                field.setAccessible(true);
                field.set(target, value);
            } catch (Exception ex) {
                // 필드 설정 실패 시 무시
            }
        } catch (Exception e) {
            // 필드 설정 실패 시 무시
        }
    }
}
