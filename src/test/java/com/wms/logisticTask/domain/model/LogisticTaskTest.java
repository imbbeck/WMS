package com.wms.logisticTask.domain.model;

import com.wms.location.domain.model.Location;
import com.wms.location.domain.model.LocationType;
import com.wms.logisticTask.domain.exception.LogisticTaskException;
import com.wms.logisticTemplate.domain.model.LogisticType;
import com.wms.userInfo.domain.model.Password;
import com.wms.userInfo.domain.model.UserInfo;
import com.wms.userInfo.domain.model.UserType;
import com.wms.ware.domain.model.Ware;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.*;

@DisplayName("LogisticTask 도메인 테스트")
class LogisticTaskTest {

    private UserInfo worker;
    private Ware ware;
    private Location fromLocation;
    private Location toLocation;
    private LocalDate scheduledDate;
    private LocalTime etd;
    private LocalTime eta;

    @BeforeEach
    void setUp() {
        worker = UserInfo.builder()
                .username("worker1")
                .name("김작업")
                .email("worker1@test.com")
                .password(new Password("password"))
                .type(UserType.WORKER)
                .build();

        ware = Ware.builder()
                .name("노트북")
                .type("전자제품")
                .paletteUnit(20)
                .build();

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
                .capacity(800)
                .coordinateX(200)
                .coordinateY(100)
                .build();

        scheduledDate = LocalDate.of(2025, 1, 15);
        etd = LocalTime.of(9, 0);
        eta = LocalTime.of(10, 0);
    }

    @Nested
    @DisplayName("생성 테스트")
    class CreationTest {

        @Test
        @DisplayName("정상적인 물류 작업 생성")
        void createLogisticTask_Success() {
            // When
            LogisticTask task = LogisticTask.builder()
                    .name("창고A → 창고B 노트북 이동")
                    .type(LogisticType.INNER)
                    .worker(worker)
                    .ware(ware)
                    .fromLocation(fromLocation)
                    .toLocation(toLocation)
                    .quantity(10)
                    .scheduledDate(scheduledDate)
                    .etd(etd)
                    .eta(eta)
                    .templateIdSnapshot(1)
                    .build();

            // Then
            assertThat(task.getName()).isEqualTo("창고A → 창고B 노트북 이동");
            assertThat(task.getType()).isEqualTo(LogisticType.INNER);
            assertThat(task.getStatus()).isEqualTo(LogisticTaskStatus.PENDING);
            assertThat(task.getQuantity()).isEqualTo(10);
            assertThat(task.getScheduledDate()).isEqualTo(scheduledDate);
            assertThat(task.getEtd()).isEqualTo(etd);
            assertThat(task.getEta()).isEqualTo(eta);
            assertThat(task.getTemplateIdSnapshot()).isEqualTo(1);
        }

        @Test
        @DisplayName("기본 상태는 PENDING")
        void createLogisticTask_DefaultStatus() {
            // When
            LogisticTask task = LogisticTask.builder()
                    .name("test")
                    .type(LogisticType.INNER)
                    .worker(worker)
                    .ware(ware)
                    .fromLocation(fromLocation)
                    .toLocation(toLocation)
                    .quantity(10)
                    .scheduledDate(scheduledDate)
                    .etd(etd)
                    .eta(eta)
                    .build();

            // Then
            assertThat(task.getStatus()).isEqualTo(LogisticTaskStatus.PENDING);
        }

        @Test
        @DisplayName("이름이 null일 때 예외 발생")
        void createLogisticTask_NullName() {
            // When & Then
            assertThatThrownBy(() -> LogisticTask.builder()
                    .name(null)
                    .type(LogisticType.INNER)
                    .worker(worker)
                    .ware(ware)
                    .fromLocation(fromLocation)
                    .toLocation(toLocation)
                    .quantity(10)
                    .scheduledDate(scheduledDate)
                    .etd(etd)
                    .eta(eta)
                    .build())
                    .isInstanceOf(LogisticTaskException.ValidationEx.class)
                    .hasMessageContaining("작업명은 필수입니다");
        }

        @Test
        @DisplayName("수량이 0일 때 예외 발생")
        void createLogisticTask_ZeroQuantity() {
            // When & Then
            assertThatThrownBy(() -> LogisticTask.builder()
                    .name("test")
                    .type(LogisticType.INNER)
                    .worker(worker)
                    .ware(ware)
                    .fromLocation(fromLocation)
                    .toLocation(toLocation)
                    .quantity(0)
                    .scheduledDate(scheduledDate)
                    .etd(etd)
                    .eta(eta)
                    .build())
                    .isInstanceOf(LogisticTaskException.ValidationEx.class)
                    .hasMessageContaining("수량은 0보다 커야 합니다");
        }

        @Test
        @DisplayName("수량이 음수일 때 예외 발생")
        void createLogisticTask_NegativeQuantity() {
            // When & Then
            assertThatThrownBy(() -> LogisticTask.builder()
                    .name("test")
                    .type(LogisticType.INNER)
                    .worker(worker)
                    .ware(ware)
                    .fromLocation(fromLocation)
                    .toLocation(toLocation)
                    .quantity(-5)
                    .scheduledDate(scheduledDate)
                    .etd(etd)
                    .eta(eta)
                    .build())
                    .isInstanceOf(LogisticTaskException.ValidationEx.class)
                    .hasMessageContaining("수량은 0보다 커야 합니다");
        }

        @Test
        @DisplayName("빈 문자열 이름일 때 예외 발생")
        void createLogisticTask_EmptyName() {
            // When & Then
            assertThatThrownBy(() -> LogisticTask.builder()
                    .name("")
                    .type(LogisticType.INNER)
                    .worker(worker)
                    .ware(ware)
                    .fromLocation(fromLocation)
                    .toLocation(toLocation)
                    .quantity(10)
                    .scheduledDate(scheduledDate)
                    .etd(etd)
                    .eta(eta)
                    .build())
                    .isInstanceOf(LogisticTaskException.ValidationEx.class)
                    .hasMessageContaining("작업명은 필수입니다");
        }

        @Test
        @DisplayName("공백만 있는 이름일 때 예외 발생")
        void createLogisticTask_WhitespaceOnlyName() {
            // When & Then
            assertThatThrownBy(() -> LogisticTask.builder()
                    .name("   ")
                    .type(LogisticType.INNER)
                    .worker(worker)
                    .ware(ware)
                    .fromLocation(fromLocation)
                    .toLocation(toLocation)
                    .quantity(10)
                    .scheduledDate(scheduledDate)
                    .etd(etd)
                    .eta(eta)
                    .build())
                    .isInstanceOf(LogisticTaskException.ValidationEx.class)
                    .hasMessageContaining("작업명은 필수입니다");
        }

        @Test
        @DisplayName("이름이 255자를 초과할 때 예외 발생")
        void createLogisticTask_NameTooLong() {
            // Given
            String longName = "a".repeat(256);

            // When & Then
            assertThatThrownBy(() -> LogisticTask.builder()
                    .name(longName)
                    .type(LogisticType.INNER)
                    .worker(worker)
                    .ware(ware)
                    .fromLocation(fromLocation)
                    .toLocation(toLocation)
                    .quantity(10)
                    .scheduledDate(scheduledDate)
                    .etd(etd)
                    .eta(eta)
                    .build())
                    .isInstanceOf(LogisticTaskException.ValidationEx.class)
                    .hasMessageContaining("작업명은 255자를 초과할 수 없습니다");
        }

        @Test
        @DisplayName("ETD가 null일 때 예외 발생")
        void createLogisticTask_NullEtd() {
            // When & Then
            assertThatThrownBy(() -> LogisticTask.builder()
                    .name("test")
                    .type(LogisticType.INNER)
                    .worker(worker)
                    .ware(ware)
                    .fromLocation(fromLocation)
                    .toLocation(toLocation)
                    .quantity(10)
                    .scheduledDate(scheduledDate)
                    .etd(null)
                    .eta(eta)
                    .build())
                    .isInstanceOf(LogisticTaskException.ValidationEx.class)
                    .hasMessageContaining("ETD 및 ETA는 필수입니다");
        }

        @Test
        @DisplayName("ETA가 null일 때 예외 발생")
        void createLogisticTask_NullEta() {
            // When & Then
            assertThatThrownBy(() -> LogisticTask.builder()
                    .name("test")
                    .type(LogisticType.INNER)
                    .worker(worker)
                    .ware(ware)
                    .fromLocation(fromLocation)
                    .toLocation(toLocation)
                    .quantity(10)
                    .scheduledDate(scheduledDate)
                    .etd(etd)
                    .eta(null)
                    .build())
                    .isInstanceOf(LogisticTaskException.ValidationEx.class)
                    .hasMessageContaining("ETD 및 ETA는 필수입니다");
        }

        @Test
        @DisplayName("ETD가 ETA보다 늦을 때 예외 발생")
        void createLogisticTask_EtdAfterEta() {
            // When & Then
            assertThatThrownBy(() -> LogisticTask.builder()
                    .name("test")
                    .type(LogisticType.INNER)
                    .worker(worker)
                    .ware(ware)
                    .fromLocation(fromLocation)
                    .toLocation(toLocation)
                    .quantity(10)
                    .scheduledDate(scheduledDate)
                    .etd(LocalTime.of(12, 0))
                    .eta(LocalTime.of(10, 0))
                    .build())
                    .isInstanceOf(LogisticTaskException.ValidationEx.class)
                    .hasMessageContaining("출발 예정시간은 도착 예정시간보다 빨라야 합니다");
        }

        @Test
        @DisplayName("명시적 상태로 생성 시 해당 상태 설정")
        void createLogisticTask_WithExplicitStatus() {
            // When
            LogisticTask task = LogisticTask.builder()
                    .name("test")
                    .type(LogisticType.INNER)
                    .worker(worker)
                    .ware(ware)
                    .fromLocation(fromLocation)
                    .toLocation(toLocation)
                    .quantity(10)
                    .scheduledDate(scheduledDate)
                    .etd(etd)
                    .eta(eta)
                    .status(LogisticTaskStatus.INITIATED)
                    .build();

            // Then
            assertThat(task.getStatus()).isEqualTo(LogisticTaskStatus.INITIATED);
        }
    }

    @Nested
    @DisplayName("상태 전환 테스트")
    class StatusTransitionTest {

        private LogisticTask task;

        @BeforeEach
        void setUp() {
            task = LogisticTask.builder()
                    .name("test task")
                    .type(LogisticType.INNER)
                    .worker(worker)
                    .ware(ware)
                    .fromLocation(fromLocation)
                    .toLocation(toLocation)
                    .quantity(10)
                    .scheduledDate(scheduledDate)
                    .etd(etd)
                    .eta(eta)
                    .build();
        }

        @Test
        @DisplayName("PENDING → INITIATED 전환 성공")
        void transitionFromPendingToInitiated() {
            // Given
            assertThat(task.getStatus()).isEqualTo(LogisticTaskStatus.PENDING);

            // When
            task.initiateTask(LocalTime.of(9, 5));

            // Then
            assertThat(task.getStatus()).isEqualTo(LogisticTaskStatus.INITIATED);
            assertThat(task.getAtd()).isEqualTo(LocalTime.of(9, 5));
        }

        @Test
        @DisplayName("INITIATED → COMPLETED 전환 성공")
        void transitionFromInitiatedToCompleted() {
            // Given
            task.initiateTask(LocalTime.of(9, 0));

            // When
            task.completeTask(LocalTime.of(10, 5));

            // Then
            assertThat(task.getStatus()).isEqualTo(LogisticTaskStatus.COMPLETED);
            assertThat(task.getAta()).isEqualTo(LocalTime.of(10, 5));
        }

        @Test
        @DisplayName("PENDING → CANCELLED 전환 성공")
        void transitionFromPendingToCancelled() {
            // Given
            assertThat(task.getStatus()).isEqualTo(LogisticTaskStatus.PENDING);

            // When
            task.cancelTask();

            // Then
            assertThat(task.getStatus()).isEqualTo(LogisticTaskStatus.CANCELLED);
        }

        @Test
        @DisplayName("INITIATED → FAILED 전환 성공")
        void transitionFromInitiatedToFailed() {
            // Given
            task.initiateTask(LocalTime.of(9, 0));
            assertThat(task.getStatus()).isEqualTo(LogisticTaskStatus.INITIATED);

            // When
            task.failTask();

            // Then
            assertThat(task.getStatus()).isEqualTo(LogisticTaskStatus.FAILED);
        }

        @Test
        @DisplayName("PENDING → INITIATE_DELAYED 전환 성공")
        void transitionFromPendingToInitiateDelayed() {
            // Given
            assertThat(task.getStatus()).isEqualTo(LogisticTaskStatus.PENDING);

            // When
            task.delayInitiation();

            // Then
            assertThat(task.getStatus()).isEqualTo(LogisticTaskStatus.INITIATE_DELAYED);
            assertThat(task.getAtd()).isNull(); // 지연 시 실제 시작 시간 초기화
        }

        @Test
        @DisplayName("INITIATED → COMPLETE_DELAYED 전환 성공")
        void transitionFromInitiatedToCompleteDelayed() {
            // Given
            task.initiateTask(LocalTime.of(9, 0));
            assertThat(task.getStatus()).isEqualTo(LogisticTaskStatus.INITIATED);

            // When
            task.delayCompletion();

            // Then
            assertThat(task.getStatus()).isEqualTo(LogisticTaskStatus.COMPLETE_DELAYED);
            assertThat(task.getAta()).isNull(); // 지연 시 실제 완료 시간 초기화
        }

        @Test
        @DisplayName("INITIATE_DELAYED → INITIATED 전환 성공")
        void transitionFromInitiateDelayedToInitiated() {
            // Given
            task.delayInitiation();
            assertThat(task.getStatus()).isEqualTo(LogisticTaskStatus.INITIATE_DELAYED);

            // When
            task.initiateTask(LocalTime.of(9, 30));

            // Then
            assertThat(task.getStatus()).isEqualTo(LogisticTaskStatus.INITIATED);
            assertThat(task.getAtd()).isEqualTo(LocalTime.of(9, 30));
        }

        @Test
        @DisplayName("COMPLETE_DELAYED → COMPLETED 전환 성공")
        void transitionFromCompleteDelayedToCompleted() {
            // Given
            task.initiateTask(LocalTime.of(9, 0));
            task.delayCompletion();
            assertThat(task.getStatus()).isEqualTo(LogisticTaskStatus.COMPLETE_DELAYED);

            // When
            task.completeTask(LocalTime.of(10, 30));

            // Then
            assertThat(task.getStatus()).isEqualTo(LogisticTaskStatus.COMPLETED);
            assertThat(task.getAta()).isEqualTo(LocalTime.of(10, 30));
        }

        @Test
        @DisplayName("INITIATE_DELAYED → CANCELLED 전환 성공")
        void transitionFromInitiateDelayedToCancelled() {
            // Given
            task.delayInitiation();
            assertThat(task.getStatus()).isEqualTo(LogisticTaskStatus.INITIATE_DELAYED);

            // When
            task.cancelTask();

            // Then
            assertThat(task.getStatus()).isEqualTo(LogisticTaskStatus.CANCELLED);
        }

        @Test
        @DisplayName("시작 시간이 null이면 현재 시간으로 설정")
        void initiateTask_WithNullTime_UsesCurrentTime() {
            // When
            task.initiateTask(null);

            // Then
            assertThat(task.getStatus()).isEqualTo(LogisticTaskStatus.INITIATED);
            assertThat(task.getAtd()).isNotNull();
        }

        @Test
        @DisplayName("완료 시간이 null이면 현재 시간으로 설정")
        void completeTask_WithNullTime_UsesCurrentTime() {
            // Given
            task.initiateTask(LocalTime.of(9, 0));

            // When
            task.completeTask(null);

            // Then
            assertThat(task.getStatus()).isEqualTo(LogisticTaskStatus.COMPLETED);
            assertThat(task.getAta()).isNotNull();
        }
    }

    @Nested
    @DisplayName("상태 전환 실패 테스트")
    class StatusTransitionFailureTest {

        private LogisticTask task;

        @BeforeEach
        void setUp() {
            task = LogisticTask.builder()
                    .name("test task")
                    .type(LogisticType.INNER)
                    .worker(worker)
                    .ware(ware)
                    .fromLocation(fromLocation)
                    .toLocation(toLocation)
                    .quantity(10)
                    .scheduledDate(scheduledDate)
                    .etd(etd)
                    .eta(eta)
                    .build();
        }

        @Test
        @DisplayName("COMPLETED 상태에서 시작 시도 시 예외 발생")
        void initiateTask_FromCompletedStatus_ThrowsException() {
            // Given
            task.initiateTask(LocalTime.of(9, 0));
            task.completeTask(LocalTime.of(10, 0));
            assertThat(task.getStatus()).isEqualTo(LogisticTaskStatus.COMPLETED);

            // When & Then
            assertThatThrownBy(() -> task.initiateTask(LocalTime.of(11, 0)))
                    .isInstanceOf(LogisticTaskException.ValidationEx.class)
                    .hasMessageContaining("현재 상태(COMPLETED)에서는 작업을 시작할 수 없습니다");
        }

        @Test
        @DisplayName("PENDING 상태에서 완료 시도 시 예외 발생")
        void completeTask_FromPendingStatus_ThrowsException() {
            // Given
            assertThat(task.getStatus()).isEqualTo(LogisticTaskStatus.PENDING);

            // When & Then
            assertThatThrownBy(() -> task.completeTask(LocalTime.of(10, 0)))
                    .isInstanceOf(LogisticTaskException.ValidationEx.class)
                    .hasMessageContaining("현재 상태(PENDING)에서는 작업을 완료할 수 없습니다");
        }

        @Test
        @DisplayName("INITIATED 상태에서 취소 시도 시 예외 발생")
        void cancelTask_FromInitiatedStatus_ThrowsException() {
            // Given
            task.initiateTask(LocalTime.of(9, 0));
            assertThat(task.getStatus()).isEqualTo(LogisticTaskStatus.INITIATED);

            // When & Then
            assertThatThrownBy(() -> task.cancelTask())
                    .isInstanceOf(LogisticTaskException.TaskCancellationNotAllowedEx.class);
        }

        @Test
        @DisplayName("PENDING 상태에서 실패 처리 시도 시 예외 발생")
        void failTask_FromPendingStatus_ThrowsException() {
            // Given
            assertThat(task.getStatus()).isEqualTo(LogisticTaskStatus.PENDING);

            // When & Then
            assertThatThrownBy(() -> task.failTask())
                    .isInstanceOf(LogisticTaskException.TaskFailureNotAllowedEx.class);
        }

        @Test
        @DisplayName("INITIATED 상태에서 시작 지연 처리 시도 시 예외 발생")
        void delayInitiation_FromInitiatedStatus_ThrowsException() {
            // Given
            task.initiateTask(LocalTime.of(9, 0));
            assertThat(task.getStatus()).isEqualTo(LogisticTaskStatus.INITIATED);

            // When & Then
            assertThatThrownBy(() -> task.delayInitiation())
                    .isInstanceOf(LogisticTaskException.ValidationEx.class)
                    .hasMessageContaining("현재 상태(INITIATED)에서는 시작 지연 처리를 할 수 없습니다");
        }

        @Test
        @DisplayName("PENDING 상태에서 완료 지연 처리 시도 시 예외 발생")
        void delayCompletion_FromPendingStatus_ThrowsException() {
            // Given
            assertThat(task.getStatus()).isEqualTo(LogisticTaskStatus.PENDING);

            // When & Then
            assertThatThrownBy(() -> task.delayCompletion())
                    .isInstanceOf(LogisticTaskException.ValidationEx.class)
                    .hasMessageContaining("현재 상태(PENDING)에서는 완료 지연 처리를 할 수 없습니다");
        }

        @Test
        @DisplayName("COMPLETED 상태에서 실패 처리 시도 시 예외 발생")
        void failTask_FromCompletedStatus_ThrowsException() {
            // Given
            task.initiateTask(LocalTime.of(9, 0));
            task.completeTask(LocalTime.of(10, 0));
            assertThat(task.getStatus()).isEqualTo(LogisticTaskStatus.COMPLETED);

            // When & Then
            assertThatThrownBy(() -> task.failTask())
                    .isInstanceOf(LogisticTaskException.TaskFailureNotAllowedEx.class);
        }
    }

    @Nested
    @DisplayName("작업 수정 테스트")
    class TaskModificationTest {

        private LogisticTask task;
        private UserInfo newWorker;

        @BeforeEach
        void setUp() {
            task = LogisticTask.builder()
                    .name("원본 작업")
                    .type(LogisticType.INNER)
                    .worker(worker)
                    .ware(ware)
                    .fromLocation(fromLocation)
                    .toLocation(toLocation)
                    .quantity(10)
                    .scheduledDate(scheduledDate)
                    .etd(etd)
                    .eta(eta)
                    .build();

            newWorker = UserInfo.builder()
                    .username("worker2")
                    .name("박작업")
                    .email("worker2@test.com")
                    .password(new Password("password"))
                    .type(UserType.WORKER)
                    .build();
        }

        @Test
        @DisplayName("전체 수정 성공")
        void modifyTask_Full_Success() {
            // When
            task.modifyTask("수정된 작업명", newWorker, 20, 
                           LocalTime.of(10, 0), LocalTime.of(11, 0));

            // Then
            assertThat(task.getName()).isEqualTo("수정된 작업명");
            assertThat(task.getWorker()).isEqualTo(newWorker);
            assertThat(task.getQuantity()).isEqualTo(20);
            assertThat(task.getEtd()).isEqualTo(LocalTime.of(10, 0));
            assertThat(task.getEta()).isEqualTo(LocalTime.of(11, 0));
            assertThat(task.getStatus()).isEqualTo(LogisticTaskStatus.PENDING); // 수정 시 PENDING으로 초기화
        }

        @Test
        @DisplayName("부분 수정 성공 (작업자, 시간만)")
        void modifyTask_Partial_Success() {
            // When
            task.modifyTask(newWorker, LocalTime.of(11, 0), LocalTime.of(12, 0));

            // Then
            assertThat(task.getName()).isEqualTo("원본 작업"); // 변경되지 않음
            assertThat(task.getWorker()).isEqualTo(newWorker);
            assertThat(task.getQuantity()).isEqualTo(10); // 변경되지 않음
            assertThat(task.getEtd()).isEqualTo(LocalTime.of(11, 0));
            assertThat(task.getEta()).isEqualTo(LocalTime.of(12, 0));
            assertThat(task.getStatus()).isEqualTo(LogisticTaskStatus.PENDING); // 수정 시 PENDING으로 초기화
        }

        @Test
        @DisplayName("INITIATE_DELAYED 상태에서 수정 성공")
        void modifyTask_FromInitiateDelayedStatus_Success() {
            // Given
            task.delayInitiation();
            assertThat(task.getStatus()).isEqualTo(LogisticTaskStatus.INITIATE_DELAYED);

            // When
            task.modifyTask("수정된 작업명", newWorker, 15,
                           LocalTime.of(10, 30), LocalTime.of(11, 30));

            // Then
            assertThat(task.getName()).isEqualTo("수정된 작업명");
            assertThat(task.getStatus()).isEqualTo(LogisticTaskStatus.PENDING); // PENDING으로 초기화
        }

        @Test
        @DisplayName("INITIATED 상태에서 수정 시도 시 예외 발생")
        void modifyTask_FromInitiatedStatus_ThrowsException() {
            // Given
            task.initiateTask(LocalTime.of(9, 0));
            assertThat(task.getStatus()).isEqualTo(LogisticTaskStatus.INITIATED);

            // When & Then
            assertThatThrownBy(() -> task.modifyTask("수정된 작업명", newWorker, 15,
                    LocalTime.of(10, 0), LocalTime.of(11, 0)))
                    .isInstanceOf(LogisticTaskException.TaskNotModifiableEx.class);
        }

        @Test
        @DisplayName("전체 수정 시 잘못된 이름으로 예외 발생")
        void modifyTask_Full_InvalidName_ThrowsException() {
            // When & Then
            assertThatThrownBy(() -> task.modifyTask("", newWorker, 20,
                    LocalTime.of(10, 0), LocalTime.of(11, 0)))
                    .isInstanceOf(LogisticTaskException.ValidationEx.class)
                    .hasMessageContaining("작업명은 필수입니다");
        }

        @Test
        @DisplayName("전체 수정 시 잘못된 수량으로 예외 발생")
        void modifyTask_Full_InvalidQuantity_ThrowsException() {
            // When & Then
            assertThatThrownBy(() -> task.modifyTask("수정된 작업명", newWorker, 0,
                    LocalTime.of(10, 0), LocalTime.of(11, 0)))
                    .isInstanceOf(LogisticTaskException.ValidationEx.class)
                    .hasMessageContaining("수량은 0보다 커야 합니다");
        }

        @Test
        @DisplayName("전체 수정 시 ETD가 ETA보다 늦을 때 예외 발생")
        void modifyTask_Full_EtdAfterEta_ThrowsException() {
            // When & Then
            assertThatThrownBy(() -> task.modifyTask("수정된 작업명", newWorker, 20,
                    LocalTime.of(12, 0), LocalTime.of(10, 0)))
                    .isInstanceOf(LogisticTaskException.ValidationEx.class)
                    .hasMessageContaining("출발 예정시간은 도착 예정시간보다 빨라야 합니다");
        }

        @Test
        @DisplayName("부분 수정 시 null ETD로 예외 발생")
        void modifyTask_Partial_NullEtd_ThrowsException() {
            // When & Then
            assertThatThrownBy(() -> task.modifyTask(newWorker, null, LocalTime.of(11, 0)))
                    .isInstanceOf(LogisticTaskException.ValidationEx.class)
                    .hasMessageContaining("ETD 및 ETA는 필수입니다");
        }

        @Test
        @DisplayName("부분 수정 시 null ETA로 예외 발생")
        void modifyTask_Partial_NullEta_ThrowsException() {
            // When & Then
            assertThatThrownBy(() -> task.modifyTask(newWorker, LocalTime.of(10, 0), null))
                    .isInstanceOf(LogisticTaskException.ValidationEx.class)
                    .hasMessageContaining("ETD 및 ETA는 필수입니다");
        }

        @Test
        @DisplayName("부분 수정 시 ETD가 ETA보다 늦을 때 예외 발생")
        void modifyTask_Partial_EtdAfterEta_ThrowsException() {
            // When & Then
            assertThatThrownBy(() -> task.modifyTask(newWorker, 
                    LocalTime.of(13, 0), LocalTime.of(11, 0)))
                    .isInstanceOf(LogisticTaskException.ValidationEx.class)
                    .hasMessageContaining("출발 예정시간은 도착 예정시간보다 빨라야 합니다");
        }
    }

    @Nested
    @DisplayName("상태 확인 메서드 테스트")
    class StatusCheckMethodsTest {

        private LogisticTask task;

        @BeforeEach
        void setUp() {
            task = LogisticTask.builder()
                    .name("test task")
                    .type(LogisticType.INNER)
                    .worker(worker)
                    .ware(ware)
                    .fromLocation(fromLocation)
                    .toLocation(toLocation)
                    .quantity(10)
                    .scheduledDate(scheduledDate)
                    .etd(etd)
                    .eta(eta)
                    .build();
        }

        @Test
        @DisplayName("PENDING 상태 확인 메서드들")
        void pendingStatusChecks() {
            // Given
            assertThat(task.getStatus()).isEqualTo(LogisticTaskStatus.PENDING);

            // Then
            assertThat(task.isModifiable()).isTrue();
            assertThat(task.isCancellable()).isTrue();
            assertThat(task.canInitiate()).isTrue();
            assertThat(task.canComplete()).isFalse();
            assertThat(task.canDelayInitiation()).isTrue();
            assertThat(task.canDelayCompletion()).isFalse();
            assertThat(task.canFail()).isFalse();
            assertThat(task.isInProgress()).isFalse();
            assertThat(task.isCompleted()).isFalse();
            assertThat(task.isFinished()).isFalse();
        }

        @Test
        @DisplayName("INITIATED 상태 확인 메서드들")
        void initiatedStatusChecks() {
            // Given
            task.initiateTask(LocalTime.of(9, 0));
            assertThat(task.getStatus()).isEqualTo(LogisticTaskStatus.INITIATED);

            // Then
            assertThat(task.isModifiable()).isFalse();
            assertThat(task.isCancellable()).isFalse();
            assertThat(task.canInitiate()).isFalse();
            assertThat(task.canComplete()).isTrue();
            assertThat(task.canDelayInitiation()).isFalse();
            assertThat(task.canDelayCompletion()).isTrue();
            assertThat(task.canFail()).isTrue();
            assertThat(task.isInProgress()).isTrue();
            assertThat(task.isCompleted()).isFalse();
            assertThat(task.isFinished()).isFalse();
        }

        @Test
        @DisplayName("COMPLETED 상태 확인 메서드들")
        void completedStatusChecks() {
            // Given
            task.initiateTask(LocalTime.of(9, 0));
            task.completeTask(LocalTime.of(10, 0));
            assertThat(task.getStatus()).isEqualTo(LogisticTaskStatus.COMPLETED);

            // Then
            assertThat(task.isModifiable()).isFalse();
            assertThat(task.isCancellable()).isFalse();
            assertThat(task.canInitiate()).isFalse();
            assertThat(task.canComplete()).isFalse();
            assertThat(task.canDelayInitiation()).isFalse();
            assertThat(task.canDelayCompletion()).isFalse();
            assertThat(task.canFail()).isFalse();
            assertThat(task.isInProgress()).isFalse();
            assertThat(task.isCompleted()).isTrue();
            assertThat(task.isFinished()).isTrue();
        }

        @Test
        @DisplayName("CANCELLED 상태 확인 메서드들")
        void cancelledStatusChecks() {
            // Given
            task.cancelTask();
            assertThat(task.getStatus()).isEqualTo(LogisticTaskStatus.CANCELLED);

            // Then
            assertThat(task.isModifiable()).isFalse();
            assertThat(task.isCancellable()).isFalse();
            assertThat(task.canInitiate()).isFalse();
            assertThat(task.canComplete()).isFalse();
            assertThat(task.canDelayInitiation()).isFalse();
            assertThat(task.canDelayCompletion()).isFalse();
            assertThat(task.canFail()).isFalse();
            assertThat(task.isInProgress()).isFalse();
            assertThat(task.isCompleted()).isFalse();
            assertThat(task.isFinished()).isTrue();
        }

        @Test
        @DisplayName("FAILED 상태 확인 메서드들")
        void failedStatusChecks() {
            // Given
            task.initiateTask(LocalTime.of(9, 0));
            task.failTask();
            assertThat(task.getStatus()).isEqualTo(LogisticTaskStatus.FAILED);

            // Then
            assertThat(task.isModifiable()).isFalse();
            assertThat(task.isCancellable()).isFalse();
            assertThat(task.canInitiate()).isFalse();
            assertThat(task.canComplete()).isFalse();
            assertThat(task.canDelayInitiation()).isFalse();
            assertThat(task.canDelayCompletion()).isFalse();
            assertThat(task.canFail()).isFalse();
            assertThat(task.isInProgress()).isFalse();
            assertThat(task.isCompleted()).isFalse();
            assertThat(task.isFinished()).isTrue();
        }

        @Test
        @DisplayName("INITIATE_DELAYED 상태 확인 메서드들")
        void initiateDelayedStatusChecks() {
            // Given
            task.delayInitiation();
            assertThat(task.getStatus()).isEqualTo(LogisticTaskStatus.INITIATE_DELAYED);

            // Then
            assertThat(task.isModifiable()).isTrue();
            assertThat(task.isCancellable()).isTrue();
            assertThat(task.canInitiate()).isTrue();
            assertThat(task.canComplete()).isFalse();
            assertThat(task.canDelayInitiation()).isFalse();
            assertThat(task.canDelayCompletion()).isFalse();
            assertThat(task.canFail()).isFalse();
            assertThat(task.isInProgress()).isFalse();
            assertThat(task.isCompleted()).isFalse();
            assertThat(task.isFinished()).isFalse();
        }

        @Test
        @DisplayName("COMPLETE_DELAYED 상태 확인 메서드들")
        void completeDelayedStatusChecks() {
            // Given
            task.initiateTask(LocalTime.of(9, 0));
            task.delayCompletion();
            assertThat(task.getStatus()).isEqualTo(LogisticTaskStatus.COMPLETE_DELAYED);

            // Then
            assertThat(task.isModifiable()).isFalse();
            assertThat(task.isCancellable()).isFalse();
            assertThat(task.canInitiate()).isFalse();
            assertThat(task.canComplete()).isTrue();
            assertThat(task.canDelayInitiation()).isFalse();
            assertThat(task.canDelayCompletion()).isFalse();
            assertThat(task.canFail()).isFalse();
            assertThat(task.isInProgress()).isTrue();
            assertThat(task.isCompleted()).isFalse();
            assertThat(task.isFinished()).isFalse();
        }
    }
}