package com.wms.logisticTask.application;

import com.wms.location.domain.model.Location;
import com.wms.location.domain.model.LocationType;
import com.wms.location.domain.repository.LocationRepository;
import com.wms.logisticTask.domain.event.LogisticTaskCompletedEvent;
import com.wms.logisticTask.domain.event.LogisticTaskInitiatedEvent;
import com.wms.logisticTask.domain.exception.LogisticTaskException;
import com.wms.logisticTask.domain.model.LogisticTask;
import com.wms.logisticTask.domain.model.LogisticTaskStatus;
import com.wms.logisticTask.domain.repository.LogisticTaskRepository;
import com.wms.logisticTask.dto.LogisticTaskDTO;
import com.wms.logisticTemplate.domain.model.LogisticType;
import com.wms.userInfo.domain.exception.UserInfoException;
import com.wms.userInfo.domain.model.Password;
import com.wms.userInfo.domain.model.UserInfo;
import com.wms.userInfo.domain.model.UserType;
import com.wms.userInfo.domain.repository.UserInfoRepository;
import com.wms.ware.domain.model.Ware;
import com.wms.ware.domain.repository.WareRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@DisplayName("LogisticTaskService 테스트")
class LogisticTaskServiceTest {

    @InjectMocks
    private LogisticTaskService logisticTaskService;

    @Mock
    private LogisticTaskRepository logisticTaskRepository;

    @Mock
    private UserInfoRepository userInfoRepository;

    @Mock
    private WareRepository wareRepository;

    @Mock
    private LocationRepository locationRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private LogisticTaskValidationService validationService;

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
        setId(worker, 1L);

        ware = Ware.builder()
                .name("노트북")
                .type("전자제품")
                .paletteUnit(20)
                .build();
        setId(ware, 1L);

        fromLocation = Location.builder()
                .name("창고A")
                .type(LocationType.WAREHOUSE)
                .capacity(1000)
                .coordinateX(100)
                .coordinateY(100)
                .build();
        setId(fromLocation, 1L);

        toLocation = Location.builder()
                .name("창고B")
                .type(LocationType.WAREHOUSE)
                .capacity(800)
                .coordinateX(200)
                .coordinateY(100)
                .build();
        setId(toLocation, 2L);

        task = LogisticTask.builder()
                .name("창고A → 창고B 노트북 이동")
                .type(LogisticType.INNER)
                .worker(worker)
                .ware(ware)
                .fromLocation(fromLocation)
                .toLocation(toLocation)
                .quantity(10)
                .scheduledDate(LocalDate.of(2025, 1, 15))
                .etd(LocalTime.of(9, 0))
                .eta(LocalTime.of(10, 0))
                .templateIdSnapshot(1)
                .build();
        setId(task, 1L);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(worker, null, null));
    }

    // ===== CRUD 테스트 =====

    @Test
    @DisplayName("물류 작업 생성 성공")
    void createTask_Success() {
        // Given
        LogisticTaskDTO.CreateReq request = LogisticTaskDTO.CreateReq.builder()
                .name("창고A → 창고B 노트북 이동")
                .type(LogisticType.INNER)
                .workerId(1L)
                .wareId(1L)
                .fromLocationId(1L)
                .toLocationId(2L)
                .quantity(10)
                .scheduledDate(LocalDate.of(2025, 1, 15))
                .etd(LocalTime.of(9, 0))
                .eta(LocalTime.of(10, 0))
                .templateIdSnapshot(1)
                .build();

        given(userInfoRepository.findById(1L)).willReturn(Optional.of(worker));
        given(wareRepository.findById(1L)).willReturn(Optional.of(ware));
        given(locationRepository.findById(1L)).willReturn(Optional.of(fromLocation));
        given(locationRepository.findById(2L)).willReturn(Optional.of(toLocation));
        given(logisticTaskRepository.save(any(LogisticTask.class))).willReturn(task);

        // When
        LogisticTask result = logisticTaskService.create(request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("창고A → 창고B 노트북 이동");
        verify(validationService).validateTaskCreation(any(LogisticTask.class));
        verify(logisticTaskRepository).save(any(LogisticTask.class));
    }

    @Test
    @DisplayName("물류 작업 시작 성공")
    void initiateTask_Success() {
        // Given
        given(logisticTaskRepository.findById(1L)).willReturn(Optional.of(task));

        // When
        LogisticTaskDTO.ActionRes result = logisticTaskService.initiateTask(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getCurrentStatus()).isEqualTo(LogisticTaskStatus.INITIATED);
        verify(eventPublisher).publishEvent(any(LogisticTaskInitiatedEvent.class));
    }

    @Test
    @DisplayName("물류 작업 완료 성공")
    void completeTask_Success() {
        // Given
        task.initiateTask(LocalTime.of(9, 0));
        given(logisticTaskRepository.findById(1L)).willReturn(Optional.of(task));

        // When
        LogisticTaskDTO.ActionRes result = logisticTaskService.completeTask(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getCurrentStatus()).isEqualTo(LogisticTaskStatus.COMPLETED);
        assertThat(result.getPreviousStatus()).isEqualTo(LogisticTaskStatus.INITIATED);
        verify(eventPublisher).publishEvent(any(LogisticTaskCompletedEvent.class));
    }

    @Test
    @DisplayName("물류 작업 취소 성공")
    void cancelTask_Success() {
        // Given
        given(logisticTaskRepository.findById(1L)).willReturn(Optional.of(task));

        // When
        LogisticTaskDTO.ActionRes result = logisticTaskService.cancelTask(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getCurrentStatus()).isEqualTo(LogisticTaskStatus.CANCELLED);
        assertThat(result.getPreviousStatus()).isEqualTo(LogisticTaskStatus.PENDING);
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("물류 작업 실패 처리 성공")
    void failTask_Success() {
        // Given
        task.initiateTask(LocalTime.of(9, 0));
        given(logisticTaskRepository.findById(1L)).willReturn(Optional.of(task));

        // When
        LogisticTaskDTO.ActionRes result = logisticTaskService.failTask(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getCurrentStatus()).isEqualTo(LogisticTaskStatus.FAILED);
        assertThat(result.getPreviousStatus()).isEqualTo(LogisticTaskStatus.INITIATED);
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("물류 작업 지연 처리 성공")
    void delayTask_Success() {
        // Given
        given(logisticTaskRepository.findById(1L)).willReturn(Optional.of(task));

        // When
        LogisticTaskDTO.ActionRes result = logisticTaskService.delayTask(1L, true);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getCurrentStatus()).isEqualTo(LogisticTaskStatus.INITIATE_DELAYED);
        assertThat(result.getPreviousStatus()).isEqualTo(LogisticTaskStatus.PENDING);
        verify(eventPublisher, never()).publishEvent(any());
    }

    // ===== 권한 검증 테스트 =====

    @Test
    @DisplayName("작업자 권한 검증 - 다른 작업자가 작업 시작 시도 시 예외")
    void initiateTask_DifferentWorker_ThrowsException() {
        // Given
        UserInfo differentWorker = createWorker("worker2", "박작업", 2L);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(differentWorker, null, null));
        given(logisticTaskRepository.findById(1L)).willReturn(Optional.of(task));

        // When & Then
        assertThatThrownBy(() -> logisticTaskService.initiateTask(1L))
                .isInstanceOf(LogisticTaskException.WorkerMismatchEx.class);
    }

    @Test
    @DisplayName("관리자는 다른 작업자의 작업도 시작 가능")
    void initiateTask_Admin_Success() {
        // Given
        UserInfo admin = createAdmin("admin", "관리자", 3L);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(admin, null, null));
        given(logisticTaskRepository.findById(1L)).willReturn(Optional.of(task));

        // When
        LogisticTaskDTO.ActionRes result = logisticTaskService.initiateTask(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getCurrentStatus()).isEqualTo(LogisticTaskStatus.INITIATED);
    }

    // ===== 예외 처리 테스트 =====

    @Test
    @DisplayName("존재하지 않는 작업 조회 시 예외 발생")
    void findById_NotFound() {
        // Given
        given(logisticTaskRepository.findWithAllById(999L)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> logisticTaskService.findById(999L))
                .isInstanceOf(LogisticTaskException.NotFoundEx.class);
    }

    @Test
    @DisplayName("존재하지 않는 작업자로 생성 시 예외")
    void createTask_WorkerNotFound_ThrowsException() {
        // Given
        LogisticTaskDTO.CreateReq request = createTaskRequest(999L, 1L, 1L, 2L);
        given(userInfoRepository.findById(999L)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> logisticTaskService.create(request))
                .isInstanceOf(UserInfoException.NotFoundEx.class);
    }

    // ===== 조회 메서드 테스트 =====

    @Test
    @DisplayName("모든 물류 작업 조회")
    void findAll_Success() {
        // Given
        given(logisticTaskRepository.findAllBy()).willReturn(List.of(task));

        // When
        List<LogisticTask> result = logisticTaskService.findAll();

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(task);
    }

    @Test
    @DisplayName("상태별 물류 작업 조회")
    void findByStatus_Success() {
        // Given
        given(logisticTaskRepository.findByStatus(LogisticTaskStatus.PENDING))
                .willReturn(List.of(task));

        // When
        List<LogisticTask> result = logisticTaskService.findByStatus(LogisticTaskStatus.PENDING);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(task);
    }

    @Test
    @DisplayName("대시보드용 날짜별 작업자별 물류 작업 조회")
    void getDailyDashboard_Success() {
        // Given
        LocalDate date = LocalDate.of(2025, 1, 15);
        given(logisticTaskRepository.findByScheduledDate(date)).willReturn(List.of(task));

        // When
        LogisticTaskDTO.DashboardRes result = logisticTaskService.getDailyDashboard(date);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getDate()).isEqualTo(date);
        assertThat(result.getWorkerSchedules()).hasSize(1);
        assertThat(result.getWorkerSchedules().get(0).getWorkerId()).isEqualTo(1L);
        assertThat(result.getWorkerSchedules().get(0).getWorkerName()).isEqualTo("김작업");
    }

    @Test
    @DisplayName("전체 물류 작업 상태별 통계 조회")
    void getTaskStatistics_Success() {
        // Given
        given(logisticTaskRepository.findAll()).willReturn(List.of(task));

        // When
        Map<LogisticTaskStatus, Long> result = logisticTaskService.getTaskStatistics();

        // Then
        assertThat(result).isNotEmpty();
        assertThat(result.get(LogisticTaskStatus.PENDING)).isEqualTo(1L);
    }

    @Test
    @DisplayName("검색 기능 - 기본 검색")
    void searchTasks_BasicSearch_Success() {
        // Given
        LogisticTaskDTO.SearchCriteria criteria = LogisticTaskDTO.SearchCriteria.builder()
                .name("노트북")
                .status(LogisticTaskStatus.PENDING)
                .build();

        given(logisticTaskRepository.findByIdBasedSearchCriteria(
                "노트북", null, null, null, null, LogisticTaskStatus.PENDING, null, null))
                .willReturn(List.of(task));

        // When
        List<LogisticTask> result = logisticTaskService.searchTasks(criteria);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(task);
    }

    @Test
    @DisplayName("검색 기능 - ID 기반 검색")
    void searchTasks_IdBasedSearch_Success() {
        // Given
        LogisticTaskDTO.SearchCriteria criteria = LogisticTaskDTO.SearchCriteria.builder()
                .workerId(1L)
                .wareId(1L)
                .fromLocationId(1L)
                .toLocationId(2L)
                .status(LogisticTaskStatus.PENDING)
                .startDate(LocalDate.of(2025, 1, 1))
                .endDate(LocalDate.of(2025, 1, 31))
                .build();
        given(logisticTaskRepository.findByIdBasedSearchCriteria(
                null, 1L, 1L, 1L, 2L, LogisticTaskStatus.PENDING, 
                LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31)))
                .willReturn(List.of(task));

        // When
        List<LogisticTask> result = logisticTaskService.searchTasks(criteria);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(task);
        verify(logisticTaskRepository).findByIdBasedSearchCriteria(
                null, 1L, 1L, 1L, 2L, LogisticTaskStatus.PENDING, 
                LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31));
    }

    @Test
    @DisplayName("검색 기능 - 빈 조건으로 검색")
    void searchTasks_EmptyCriteria_Success() {
        // Given
        LogisticTaskDTO.SearchCriteria criteria = LogisticTaskDTO.SearchCriteria.builder().build();
        given(logisticTaskRepository.findByIdBasedSearchCriteria(
                null, null, null, null, null, null, null, null))
                .willReturn(List.of(task));

        // When
        List<LogisticTask> result = logisticTaskService.searchTasks(criteria);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(task);
    }

    // ===== 헬퍼 메서드 =====

    private UserInfo createWorker(String username, String name, Long id) {
        UserInfo user = UserInfo.builder()
                .username(username)
                .name(name)
                .email(username + "@test.com")
                .password(new Password("password"))
                .type(UserType.WORKER)
                .build();
        setId(user, id);
        return user;
    }

    private UserInfo createAdmin(String username, String name, Long id) {
        UserInfo user = UserInfo.builder()
                .username(username)
                .name(name)
                .email(username + "@test.com")
                .password(new Password("password"))
                .type(UserType.ADMIN)
                .build();
        setId(user, id);
        return user;
    }

    private LogisticTaskDTO.CreateReq createTaskRequest(Long workerId, Long wareId, 
                                                       Long fromLocationId, Long toLocationId) {
        return LogisticTaskDTO.CreateReq.builder()
                .name("테스트 작업")
                .type(LogisticType.INNER)
                .workerId(workerId)
                .wareId(wareId)
                .fromLocationId(fromLocationId)
                .toLocationId(toLocationId)
                .quantity(10)
                .scheduledDate(LocalDate.of(2025, 1, 15))
                .etd(LocalTime.of(9, 0))
                .eta(LocalTime.of(10, 0))
                .build();
    }

    private void setId(Object entity, Long id) {
        try {
            java.lang.reflect.Field idField;
            try {
                idField = entity.getClass().getDeclaredField("id");
            } catch (NoSuchFieldException e) {
                idField = entity.getClass().getSuperclass().getDeclaredField("id");
            }
            idField.setAccessible(true);
            idField.set(entity, id);
        } catch (Exception e) {
            throw new RuntimeException("ID 설정 실패", e);
        }
    }
}
