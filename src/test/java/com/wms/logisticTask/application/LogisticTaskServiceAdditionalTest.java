package com.wms.logisticTask.application;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.any;
import static org.mockito.BDDMockito.doThrow;
import static org.mockito.BDDMockito.given;
import com.wms.location.domain.exception.LocationException;
import com.wms.location.domain.model.Location;
import com.wms.location.domain.model.LocationType;
import com.wms.location.domain.repository.LocationRepository;
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
import com.wms.ware.domain.exception.WareException;
import com.wms.ware.domain.model.Ware;
import com.wms.ware.domain.repository.WareRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
@DisplayName("LogisticTaskService 추가 테스트")
class LogisticTaskServiceAdditionalTest {

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
	private UserInfo admin;
	private Ware laptop;
	private Ware mouse;
	private Location warehouseA;
	private Location warehouseB;
	private Location inboundLocation;
	private Location outboundLocation;
	private LogisticTask task1;
	private LogisticTask task2;

	@BeforeEach
	void setUp() {
		worker = createUser(1L, "worker1", "김작업", UserType.WORKER);
		admin = createUser(2L, "admin1", "관리자", UserType.ADMIN);

		laptop = createWare(1L, "노트북", "전자제품", 20);
		mouse = createWare(2L, "마우스", "전자제품", 50);

		warehouseA = createLocation(1L, "창고A", LocationType.WAREHOUSE, 1000);
		warehouseB = createLocation(2L, "창고B", LocationType.WAREHOUSE, 500);
		inboundLocation = createLocation(3L, "입고장", LocationType.INBOUND, null);
		outboundLocation = createLocation(4L, "출고장", LocationType.OUTBOUND, null);

		task1 = createTask(1L, "작업1", worker, laptop, warehouseA, warehouseB, 10);
		task2 = createTask(2L, "작업2", worker, mouse, warehouseB, warehouseA, 5);
	}

	// ===== 생성 시 예외 처리 테스트 =====

	private UserInfo createUser(Long id, String username, String name, UserType type) {
		UserInfo user = UserInfo.builder()
				.username(username)
				.name(name)
				.email(username + "@test.com")
				.password(new Password("password"))
				.type(type)
				.build();
		setId(user, id);
		return user;
	}

	// ===== 수정 시 예외 처리 테스트 =====

	private Ware createWare(Long id, String name, String type, int paletteUnit) {
		Ware ware = Ware.builder()
				.name(name)
				.type(type)
				.paletteUnit(paletteUnit)
				.build();
		setId(ware, id);
		return ware;
	}

	// ===== 조회 메서드 테스트 =====

	private Location createLocation(Long id, String name, LocationType type, Integer capacity) {
		Location.LocationBuilder builder = Location.builder()
				.name(name)
				.type(type)
				.coordinateX(100)
				.coordinateY(100);

		if (capacity != null) {
			builder.capacity(capacity);
		}

		Location location = builder.build();
		setId(location, id);
		return location;
	}

	// ===== 대시보드 관련 테스트 =====

	private LogisticTask createTask(Long id, String name, UserInfo worker, Ware ware,
			Location from, Location to, int quantity) {
		LogisticTask task = LogisticTask.builder()
				.name(name)
				.type(LogisticType.INNER)
				.worker(worker)
				.ware(ware)
				.fromLocation(from)
				.toLocation(to)
				.quantity(quantity)
				.scheduledDate(LocalDate.of(2025, 1, 15))
				.etd(LocalTime.of(9, 0))
				.eta(LocalTime.of(10, 0))
				.templateIdSnapshot(1)
				.build();
		setId(task, id);
		return task;
	}

	// ===== 검색 관련 테스트 =====

	private List<LogisticTask> createLargeTaskList(int size) {
		return java.util.stream.IntStream.range(0, size)
				.mapToObj(i -> createTask((long) i, "작업" + i, worker, laptop, warehouseA, warehouseB, 1))
				.collect(java.util.stream.Collectors.toList());
	}

	// ===== 성능 및 경계값 테스트 =====

	private List<LogisticTask> createTasksForManyWorkers(int workerCount) {
		return java.util.stream.IntStream.range(0, workerCount)
				.mapToObj(i -> {
					UserInfo worker = createUser((long) i, "worker" + i, "작업자" + i, UserType.WORKER);
					return createTask((long) i, "작업" + i, worker, laptop, warehouseA, warehouseB, 1);
				})
				.collect(java.util.stream.Collectors.toList());
	}

	// ===== 헬퍼 메서드들 =====

	private void setId(Object entity, Long id) {
		try {
			java.lang.reflect.Field idField;
			try {
				idField = entity.getClass().getDeclaredField("id");
			}
			catch (NoSuchFieldException e) {
				idField = entity.getClass().getSuperclass().getDeclaredField("id");
			}
			idField.setAccessible(true);
			idField.set(entity, id);
		}
		catch (Exception e) {
			throw new RuntimeException("ID 설정 실패", e);
		}
	}

	@Nested
	@DisplayName("생성 시 예외 처리 테스트")
	class CreateExceptionTests {

		private LogisticTaskDTO.CreateReq validRequest;

		@BeforeEach
		void setUp() {
			validRequest = LogisticTaskDTO.CreateReq.builder()
					.name("테스트 작업")
					.type(LogisticType.INNER)
					.workerId(1L)
					.wareId(1L)
					.fromLocationId(1L)
					.toLocationId(2L)
					.quantity(10)
					.scheduledDate(LocalDate.now().plusDays(1))
					.etd(LocalTime.of(9, 0))
					.eta(LocalTime.of(10, 0))
					.templateIdSnapshot(1)
					.build();
		}

		@Test
		@DisplayName("존재하지 않는 물품으로 생성 시 예외 발생")
		void create_WareNotFound_ThrowsException() {
			// Given
			given(userInfoRepository.findById(1L)).willReturn(Optional.of(worker));
			given(wareRepository.findById(1L)).willReturn(Optional.empty());

			// When & Then
			assertThatThrownBy(() -> logisticTaskService.create(validRequest))
					.isInstanceOf(WareException.NotFoundEx.class);
		}

		@Test
		@DisplayName("존재하지 않는 출발지로 생성 시 예외 발생")
		void create_FromLocationNotFound_ThrowsException() {
			// Given
			given(userInfoRepository.findById(1L)).willReturn(Optional.of(worker));
			given(wareRepository.findById(1L)).willReturn(Optional.of(laptop));
			given(locationRepository.findById(1L)).willReturn(Optional.empty());

			// When & Then
			assertThatThrownBy(() -> logisticTaskService.create(validRequest))
					.isInstanceOf(LocationException.NotFoundEx.class);
		}

		@Test
		@DisplayName("존재하지 않는 도착지로 생성 시 예외 발생")
		void create_ToLocationNotFound_ThrowsException() {
			// Given
			given(userInfoRepository.findById(1L)).willReturn(Optional.of(worker));
			given(wareRepository.findById(1L)).willReturn(Optional.of(laptop));
			given(locationRepository.findById(1L)).willReturn(Optional.of(warehouseA));
			given(locationRepository.findById(2L)).willReturn(Optional.empty());

			// When & Then
			assertThatThrownBy(() -> logisticTaskService.create(validRequest))
					.isInstanceOf(LocationException.NotFoundEx.class);
		}

		@Test
		@DisplayName("검증 서비스 실패 시 예외 전파")
		void create_ValidationFailed_ThrowsException() {
			// Given
			given(userInfoRepository.findById(1L)).willReturn(Optional.of(worker));
			given(wareRepository.findById(1L)).willReturn(Optional.of(laptop));
			given(locationRepository.findById(1L)).willReturn(Optional.of(warehouseA));
			given(locationRepository.findById(2L)).willReturn(Optional.of(warehouseB));

			doThrow(new RuntimeException("재고 부족"))
					.when(validationService).validateTaskCreation(any(LogisticTask.class));

			// When & Then
			assertThatThrownBy(() -> logisticTaskService.create(validRequest))
					.isInstanceOf(RuntimeException.class)
					.hasMessageContaining("재고 부족");
		}
	}

	@Nested
	@DisplayName("수정 시 예외 처리 테스트")
	class UpdateExceptionTests {

		@Test
		@DisplayName("존재하지 않는 작업 수정 시 예외 발생")
		void update_TaskNotFound_ThrowsException() {
			// Given
			Long nonExistentTaskId = 999L;
			LogisticTaskDTO.UpdateReq request = LogisticTaskDTO.UpdateReq.builder()
					.name("수정된 작업")
					.workerId(1L)
					.quantity(15)
					.etd(LocalTime.of(10, 0))
					.eta(LocalTime.of(11, 0))
					.build();

			given(logisticTaskRepository.findById(nonExistentTaskId)).willReturn(Optional.empty());

			// When & Then
			assertThatThrownBy(() -> logisticTaskService.update(nonExistentTaskId, request))
					.isInstanceOf(RuntimeException.class); // LogisticTaskException.NotFoundEx
		}

		@Test
		@DisplayName("부분 수정 시 존재하지 않는 작업자로 예외 발생")
		void partialUpdate_WorkerNotFound_ThrowsException() {
			// Given
			LogisticTaskDTO.PartialUpdateReq request = LogisticTaskDTO.PartialUpdateReq.builder()
					.workerId(999L)
					.etd(LocalTime.of(14, 0))
					.eta(LocalTime.of(15, 0))
					.build();

			given(logisticTaskRepository.findById(1L)).willReturn(Optional.of(task1));
			given(userInfoRepository.findById(999L)).willReturn(Optional.empty());

			// When & Then
			assertThatThrownBy(() -> logisticTaskService.partialUpdate(1L, request))
					.isInstanceOf(UserInfoException.NotFoundEx.class);
		}
	}

	@Nested
	@DisplayName("조회 메서드 테스트")
	class QueryMethodTests {

		@Test
		@DisplayName("날짜 범위별 물류 작업 조회 성공")
		void findByDateRange_Success() {
			// Given
			LocalDate startDate = LocalDate.of(2025, 1, 1);
			LocalDate endDate = LocalDate.of(2025, 1, 31);
			List<LogisticTask> expectedTasks = Arrays.asList(task1, task2);

			given(logisticTaskRepository.findByScheduledDateBetween(startDate, endDate))
					.willReturn(expectedTasks);

			// When
			List<LogisticTask> result = logisticTaskService.findByDateRange(startDate, endDate);

			// Then
			assertThat(result).hasSize(2);
			assertThat(result).containsExactly(task1, task2);
		}

		@Test
		@DisplayName("날짜 범위 조회 - 결과 없음")
		void findByDateRange_NoResults_ReturnsEmptyList() {
			// Given
			LocalDate startDate = LocalDate.of(2025, 12, 1);
			LocalDate endDate = LocalDate.of(2025, 12, 31);

			given(logisticTaskRepository.findByScheduledDateBetween(startDate, endDate))
					.willReturn(Collections.emptyList());

			// When
			List<LogisticTask> result = logisticTaskService.findByDateRange(startDate, endDate);

			// Then
			assertThat(result).isEmpty();
		}

		@Test
		@DisplayName("장소별 물류 작업 조회 성공")
		void findByLocation_Success() {
			// Given
			Long locationId = 1L;
			List<LogisticTask> expectedTasks = Collections.singletonList(task1);

			given(logisticTaskRepository.findAllByFromLocationIdOrToLocationId(locationId, locationId))
					.willReturn(expectedTasks);

			// When
			List<LogisticTask> result = logisticTaskService.findByLocation(locationId);

			// Then
			assertThat(result).hasSize(1);
			assertThat(result).containsExactly(task1);
		}

		@Test
		@DisplayName("물품별 물류 작업 조회 - 물품 존재하지 않음")
		void findByWareId_WareNotFound_ThrowsException() {
			// Given
			Long nonExistentWareId = 999L;
			given(wareRepository.findById(nonExistentWareId)).willReturn(Optional.empty());

			// When & Then
			assertThatThrownBy(() -> logisticTaskService.findByWareId(nonExistentWareId))
					.isInstanceOf(WareException.NotFoundEx.class);
		}

		@Test
		@DisplayName("물품별 물류 작업 조회 성공")
		void findByWareId_Success() {
			// Given
			Long wareId = 1L;
			List<LogisticTask> expectedTasks = Collections.singletonList(task1);

			given(wareRepository.findById(wareId)).willReturn(Optional.of(laptop));
			given(logisticTaskRepository.findByWare(laptop)).willReturn(expectedTasks);

			// When
			List<LogisticTask> result = logisticTaskService.findByWareId(wareId);

			// Then
			assertThat(result).hasSize(1);
			assertThat(result).containsExactly(task1);
		}

		@Test
		@DisplayName("작업자별 특정 날짜 작업 조회")
		void findByWorkerAndDate_Success() {
			// Given
			Long workerId = 1L;
			LocalDate date = LocalDate.of(2025, 1, 15);
			List<LogisticTask> expectedTasks = Collections.singletonList(task1);

			given(logisticTaskRepository.findByScheduledDateAndWorker(date, workerId))
					.willReturn(expectedTasks);

			// When
			List<LogisticTask> result = logisticTaskService.findByWorkerAndDate(workerId, date);

			// Then
			assertThat(result).hasSize(1);
			assertThat(result).containsExactly(task1);
		}
	}

	@Nested
	@DisplayName("대시보드 관련 테스트")
	class DashboardTests {

		@Test
		@DisplayName("대시보드 조회 - 작업 없는 날짜")
		void getDailyDashboard_NoTasks_ReturnsEmptyDashboard() {
			// Given
			LocalDate emptyDate = LocalDate.of(2025, 12, 25);
			given(logisticTaskRepository.findByScheduledDate(emptyDate))
					.willReturn(Collections.emptyList());

			// When
			LogisticTaskDTO.DashboardRes result = logisticTaskService.getDailyDashboard(emptyDate);

			// Then
			assertThat(result).isNotNull();
			assertThat(result.getDate()).isEqualTo(emptyDate);
			assertThat(result.getWorkerSchedules()).isEmpty();
		}

		@Test
		@DisplayName("대시보드 조회 - 여러 작업자의 작업")
		void getDailyDashboard_MultipleWorkers_Success() {
			// Given
			LocalDate date = LocalDate.of(2025, 1, 15);
			UserInfo worker2 = createUser(3L, "worker2", "박작업", UserType.WORKER);
			LogisticTask task3 = createTask(3L, "작업3", worker2, laptop, warehouseA, warehouseB, 8);

			List<LogisticTask> dayTasks = Arrays.asList(task1, task2, task3);
			given(logisticTaskRepository.findByScheduledDate(date)).willReturn(dayTasks);

			// When
			LogisticTaskDTO.DashboardRes result = logisticTaskService.getDailyDashboard(date);

			// Then
			assertThat(result).isNotNull();
			assertThat(result.getDate()).isEqualTo(date);
			assertThat(result.getWorkerSchedules()).hasSize(2); // 2명의 작업자

			// 작업자명으로 정렬되는지 확인
			assertThat(result.getWorkerSchedules().get(0).getWorkerName()).isEqualTo("김작업");
			assertThat(result.getWorkerSchedules().get(1).getWorkerName()).isEqualTo("박작업");
		}

		@Test
		@DisplayName("일별 작업 통계 조회 - 작업 없음")
		void getDailyTaskStatistics_NoTasks_ReturnsEmptyMap() {
			// Given
			LocalDate emptyDate = LocalDate.of(2025, 12, 25);
			given(logisticTaskRepository.findByScheduledDate(emptyDate))
					.willReturn(Collections.emptyList());

			// When
			Map<LogisticTaskStatus, Long> result = logisticTaskService.getDailyTaskStatistics(emptyDate);

			// Then
			assertThat(result).isEmpty();
		}

		@Test
		@DisplayName("전체 작업 통계 조회 - 다양한 상태")
		void getTaskStatistics_VariousStatuses_Success() {
			// Given
			LogisticTask completedTask = createTask(3L, "완료작업", worker, laptop, warehouseA, warehouseB, 5);
			completedTask.initiateTask(LocalTime.of(9, 0));
			completedTask.completeTask(LocalTime.of(10, 0));

			LogisticTask cancelledTask = createTask(4L, "취소작업", worker, mouse, warehouseA, warehouseB, 3);
			cancelledTask.cancelTask();

			List<LogisticTask> allTasks = Arrays.asList(task1, task2, completedTask, cancelledTask);
			given(logisticTaskRepository.findAll()).willReturn(allTasks);

			// When
			Map<LogisticTaskStatus, Long> result = logisticTaskService.getTaskStatistics();

			// Then
			assertThat(result).hasSize(3);
			assertThat(result.get(LogisticTaskStatus.PENDING)).isEqualTo(2L);
			assertThat(result.get(LogisticTaskStatus.COMPLETED)).isEqualTo(1L);
			assertThat(result.get(LogisticTaskStatus.CANCELLED)).isEqualTo(1L);
		}
	}

	@Nested
	@DisplayName("검색 관련 테스트")
	class SearchTests {

		@Test
		@DisplayName("복합 조건 검색 - 모든 조건 포함")
		void searchTasks_ComplexCriteria_Success() {
			// Given
			LogisticTaskDTO.SearchCriteria criteria = LogisticTaskDTO.SearchCriteria.builder()
					.name("작업")
					.workerId(1L)
					.wareId(1L)
					.fromLocationId(1L)
					.toLocationId(2L)
					.status(LogisticTaskStatus.PENDING)
					.startDate(LocalDate.of(2025, 1, 1))
					.endDate(LocalDate.of(2025, 1, 31))
					.build();

			List<LogisticTask> expectedTasks = Collections.singletonList(task1);
			given(logisticTaskRepository.findByIdBasedSearchCriteria(
					"작업", 1L, 1L, 1L, 2L, LogisticTaskStatus.PENDING,
					LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31)))
					.willReturn(expectedTasks);

			// When
			List<LogisticTask> result = logisticTaskService.searchTasks(criteria);

			// Then
			assertThat(result).hasSize(1);
			assertThat(result).containsExactly(task1);
		}

		@Test
		@DisplayName("빈 조건으로 검색 - 전체 조회")
		void searchTasks_EmptyCriteria_ReturnsAll() {
			// Given
			LogisticTaskDTO.SearchCriteria criteria = LogisticTaskDTO.SearchCriteria.builder().build();
			List<LogisticTask> allTasks = Arrays.asList(task1, task2);

			given(logisticTaskRepository.findByIdBasedSearchCriteria(
					null, null, null, null, null, null, null, null))
					.willReturn(allTasks);

			// When
			List<LogisticTask> result = logisticTaskService.searchTasks(criteria);

			// Then
			assertThat(result).hasSize(2);
			assertThat(result).containsExactly(task1, task2);
		}

		@Test
		@DisplayName("검색 결과 없음")
		void searchTasks_NoResults_ReturnsEmptyList() {
			// Given
			LogisticTaskDTO.SearchCriteria criteria = LogisticTaskDTO.SearchCriteria.builder()
					.name("존재하지않는작업")
					.build();

			given(logisticTaskRepository.findByIdBasedSearchCriteria(
					"존재하지않는작업", null, null, null, null, null, null, null))
					.willReturn(Collections.emptyList());

			// When
			List<LogisticTask> result = logisticTaskService.searchTasks(criteria);

			// Then
			assertThat(result).isEmpty();
		}
	}

	@Nested
	@DisplayName("성능 및 경계값 테스트")
	class PerformanceAndBoundaryTests {

		@Test
		@DisplayName("대량 데이터 조회 - 성능 테스트")
		void findAll_LargeDataset_Performance() {
			// Given
			List<LogisticTask> largeTasks = createLargeTaskList(1000);
			given(logisticTaskRepository.findAllBy()).willReturn(largeTasks);

			// When
			long startTime = System.currentTimeMillis();
			List<LogisticTask> result = logisticTaskService.findAll();
			long endTime = System.currentTimeMillis();

			// Then
			assertThat(result).hasSize(1000);
			assertThat(endTime - startTime).isLessThan(1000); // 1초 이내
		}

		@Test
		@DisplayName("대시보드 조회 - 많은 작업자")
		void getDailyDashboard_ManyWorkers_Performance() {
			// Given
			List<LogisticTask> manyWorkerTasks = createTasksForManyWorkers(50);
			LocalDate date = LocalDate.of(2025, 1, 15);
			given(logisticTaskRepository.findByScheduledDate(date)).willReturn(manyWorkerTasks);

			// When
			long startTime = System.currentTimeMillis();
			LogisticTaskDTO.DashboardRes result = logisticTaskService.getDailyDashboard(date);
			long endTime = System.currentTimeMillis();

			// Then
			assertThat(result.getWorkerSchedules()).hasSize(50);
			assertThat(endTime - startTime).isLessThan(2000); // 2초 이내
		}

		@Test
		@DisplayName("시간대별 슬롯 생성 - 경계값 테스트")
		void getDailyDashboard_BoundaryTimeSlots_Success() {
			// Given
			LocalDate date = LocalDate.of(2025, 1, 15);
			LogisticTask earlyTask = LogisticTask.builder()
					.name("아침 일찍 작업")
					.type(LogisticType.INNER)
					.worker(worker)
					.ware(laptop)
					.fromLocation(warehouseA)
					.toLocation(warehouseB)
					.quantity(5)
					.scheduledDate(date)
					.etd(LocalTime.of(8, 0)) // 경계시간
					.eta(LocalTime.of(8, 30))
					.build();
			setId(earlyTask, 5L);

			LogisticTask lateTask = LogisticTask.builder()
					.name("늦은 작업")
					.type(LogisticType.INNER)
					.worker(worker)
					.ware(laptop)
					.fromLocation(warehouseA)
					.toLocation(warehouseB)
					.quantity(5)
					.scheduledDate(date)
					.etd(LocalTime.of(17, 30))
					.eta(LocalTime.of(18, 0)) // 경계시간
					.build();
			setId(lateTask, 6L);

			given(logisticTaskRepository.findByScheduledDate(date))
					.willReturn(Arrays.asList(earlyTask, lateTask));

			// When
			LogisticTaskDTO.DashboardRes result = logisticTaskService.getDailyDashboard(date);

			// Then
			assertThat(result.getWorkerSchedules()).hasSize(1);
			assertThat(result.getWorkerSchedules().get(0).getTimeSlots()).isNotEmpty();
		}
	}
}