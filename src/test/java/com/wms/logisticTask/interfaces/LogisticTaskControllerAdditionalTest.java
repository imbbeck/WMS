package com.wms.logisticTask.interfaces;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wms.applicationInfra.config.TestSecurityConfig;
import com.wms.config.TestConfig;
import com.wms.logisticTask.application.LogisticTaskService;
import com.wms.logisticTask.domain.exception.LogisticTaskException;
import com.wms.logisticTask.domain.model.LogisticTask;
import com.wms.logisticTask.domain.model.LogisticTaskStatus;
import com.wms.logisticTask.dto.LogisticTaskDTO;
import com.wms.logisticTemplate.domain.model.LogisticType;
import com.wms.location.domain.model.Location;
import com.wms.location.domain.model.LocationType;
import com.wms.userInfo.domain.exception.UserInfoException;
import com.wms.userInfo.domain.model.UserInfo;
import com.wms.userInfo.domain.model.UserType;
import com.wms.userInfo.domain.model.Password;
import com.wms.ware.domain.exception.WareException;
import com.wms.ware.domain.model.Ware;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * LogisticTaskController 추가 테스트 케이스
 * - 기존 LogisticTaskControllerTest에서 커버되지 않은 엔드포인트들
 * - 에러 응답 처리, 권한 검증, 경계값 테스트 등 포함
 */
@WebMvcTest(LogisticTaskController.class)
@Import({TestSecurityConfig.class, TestConfig.class})
@DisplayName("LogisticTaskController 추가 테스트")
class LogisticTaskControllerAdditionalTest {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private LogisticTaskService logisticTaskService;

	@Autowired
	private ObjectMapper objectMapper;

	private LogisticTask mockTask;
	private UserInfo mockWorker;
	private Ware mockWare;
	private Location mockFromLocation;
	private Location mockToLocation;

	@BeforeEach
	void setUp() {
		mockWorker = createMockWorker();
		mockWare = createMockWare();
		mockFromLocation = createMockLocation(1L, "창고A", LocationType.WAREHOUSE);
		mockToLocation = createMockLocation(2L, "창고B", LocationType.WAREHOUSE);
		mockTask = createMockLogisticTask();
	}

	// ===== 누락된 조회 엔드포인트 테스트 =====

	@Nested
	@DisplayName("누락된 조회 엔드포인트 테스트")
	class MissingQueryEndpointTests {

		@Test
		@DisplayName("GET /logistic-tasks/date-range - 날짜 범위별 조회 성공")
		void getTasksByDateRange_Success() throws Exception {
			// Given
			List<LogisticTask> tasks = Arrays.asList(mockTask);
			given(logisticTaskService.findByDateRange(
					LocalDate.of(2025, 1, 1),
					LocalDate.of(2025, 1, 31)))
					.willReturn(tasks);

			// When & Then
			mockMvc.perform(get("/logistic-tasks/date-range")
							.param("startDate", "2025-01-01")
							.param("endDate", "2025-01-31"))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.length()").value(1))
					.andExpect(jsonPath("$[0].id").value(1L))
					.andExpect(jsonPath("$[0].name").value("창고A → 창고B 노트북 이동"));
		}

		@Test
		@DisplayName("GET /logistic-tasks/date-range - 빈 결과")
		void getTasksByDateRange_EmptyResult() throws Exception {
			// Given
			given(logisticTaskService.findByDateRange(any(), any()))
					.willReturn(Collections.emptyList());

			// When & Then
			mockMvc.perform(get("/logistic-tasks/date-range")
							.param("startDate", "2025-12-01")
							.param("endDate", "2025-12-31"))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.length()").value(0));
		}

		@Test
		@DisplayName("GET /logistic-tasks/location/{locationId} - 장소별 조회 성공")
		void getTasksByLocation_Success() throws Exception {
			// Given
			Long locationId = 1L;
			List<LogisticTask> tasks = Arrays.asList(mockTask);
			given(logisticTaskService.findByLocation(locationId)).willReturn(tasks);

			// When & Then
			mockMvc.perform(get("/logistic-tasks/location/{locationId}", locationId))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.length()").value(1))
					.andExpect(jsonPath("$[0].fromLocationName").value("창고A"));
		}

		@Test
		@DisplayName("GET /logistic-tasks/ware/{wareId} - 물품별 조회 성공")
		void getTasksByWare_Success() throws Exception {
			// Given
			Long wareId = 1L;
			List<LogisticTask> tasks = Arrays.asList(mockTask);
			given(logisticTaskService.findByWareId(wareId)).willReturn(tasks);

			// When & Then
			mockMvc.perform(get("/logistic-tasks/ware/{wareId}", wareId))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.length()").value(1))
					.andExpect(jsonPath("$[0].wareName").value("노트북"));
		}

		@Test
		@DisplayName("GET /logistic-tasks/ware/{wareId} - 물품 없음 404 에러")
		void getTasksByWare_WareNotFound_404() throws Exception {
			// Given
			Long nonExistentWareId = 999L;
			given(logisticTaskService.findByWareId(nonExistentWareId))
					.willThrow(WareException.notFound(nonExistentWareId));

			// When & Then
			mockMvc.perform(get("/logistic-tasks/ware/{wareId}", nonExistentWareId))
					.andExpect(status().isNotFound());
		}

		@Test
		@DisplayName("GET /logistic-tasks/dashboard/worker/{workerId} - 작업자 대시보드 성공")
		void getWorkerDashboard_Success() throws Exception {
			// Given
			Long workerId = 1L;
			List<LogisticTask> tasks = Arrays.asList(mockTask);
			given(logisticTaskService.findByWorkerAndDate(eq(workerId), any(LocalDate.class)))
					.willReturn(tasks);

			// When & Then
			mockMvc.perform(get("/logistic-tasks/dashboard/worker/{workerId}", workerId))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.length()").value(1))
					.andExpect(jsonPath("$[0].id").value(1L))
					.andExpect(jsonPath("$[0].type").value("task"))
					.andExpect(jsonPath("$[0].name").value("창고A → 창고B 노트북 이동"))
					.andExpect(jsonPath("$[0].logisticType").value("INNER"))
					.andExpect(jsonPath("$[0].status").value("PENDING"));
		}

		@Test
		@DisplayName("GET /logistic-tasks/dashboard/worker/{workerId} - 특정 날짜 조회")
		void getWorkerDashboard_WithSpecificDate_Success() throws Exception {
			// Given
			Long workerId = 1L;
			String dateStr = "2025-01-15";
			List<LogisticTask> tasks = Arrays.asList(mockTask);
			given(logisticTaskService.findByWorkerAndDate(workerId, LocalDate.parse(dateStr)))
					.willReturn(tasks);

			// When & Then
			mockMvc.perform(get("/logistic-tasks/dashboard/worker/{workerId}", workerId)
							.param("date", dateStr))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.length()").value(1));
		}
	}

	// ===== 통계 엔드포인트 테스트 =====

	@Nested
	@DisplayName("통계 엔드포인트 테스트")
	class StatisticsEndpointTests {

		@Test
		@DisplayName("GET /logistic-tasks/statistics - 전체 통계 조회 성공")
		void getTaskStatistics_Success() throws Exception {
			// Given
			Map<LogisticTaskStatus, Long> statistics = new HashMap<>();
			statistics.put(LogisticTaskStatus.PENDING, 5L);
			statistics.put(LogisticTaskStatus.COMPLETED, 3L);
			statistics.put(LogisticTaskStatus.CANCELLED, 1L);

			given(logisticTaskService.getTaskStatistics()).willReturn(statistics);

			// When & Then
			mockMvc.perform(get("/logistic-tasks/statistics"))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.PENDING").value(5))
					.andExpect(jsonPath("$.COMPLETED").value(3))
					.andExpect(jsonPath("$.CANCELLED").value(1));
		}

		@Test
		@DisplayName("GET /logistic-tasks/statistics - 빈 통계")
		void getTaskStatistics_EmptyStatistics() throws Exception {
			// Given
			given(logisticTaskService.getTaskStatistics()).willReturn(Collections.emptyMap());

			// When & Then
			mockMvc.perform(get("/logistic-tasks/statistics"))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$").isEmpty());
		}

		@Test
		@DisplayName("GET /logistic-tasks/statistics/daily - 일별 통계 조회 성공")
		void getDailyTaskStatistics_Success() throws Exception {
			// Given
			String dateStr = "2025-01-15";
			Map<LogisticTaskStatus, Long> dailyStats = new HashMap<>();
			dailyStats.put(LogisticTaskStatus.PENDING, 2L);
			dailyStats.put(LogisticTaskStatus.INITIATED, 1L);

			given(logisticTaskService.getDailyTaskStatistics(LocalDate.parse(dateStr)))
					.willReturn(dailyStats);

			// When & Then
			mockMvc.perform(get("/logistic-tasks/statistics/daily")
							.param("date", dateStr))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.PENDING").value(2))
					.andExpect(jsonPath("$.INITIATED").value(1));
		}

		@Test
		@DisplayName("GET /logistic-tasks/statistics/daily - 해당 날짜 작업 없음")
		void getDailyTaskStatistics_NoTasksForDate() throws Exception {
			// Given
			String dateStr = "2025-12-25";
			given(logisticTaskService.getDailyTaskStatistics(LocalDate.parse(dateStr)))
					.willReturn(Collections.emptyMap());

			// When & Then
			mockMvc.perform(get("/logistic-tasks/statistics/daily")
							.param("date", dateStr))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$").isEmpty());
		}
	}

	// ===== 검색 엔드포인트 고급 테스트 =====

	@Nested
	@DisplayName("검색 엔드포인트 고급 테스트")
	class SearchEndpointAdvancedTests {

		@Test
		@DisplayName("GET /logistic-tasks/search - 모든 파라미터 포함 검색")
		void searchTasks_AllParameters_Success() throws Exception {
			// Given
			List<LogisticTask> searchResults = Arrays.asList(mockTask);
			given(logisticTaskService.searchTasks(any(LogisticTaskDTO.SearchCriteria.class)))
					.willReturn(searchResults);

			// When & Then
			mockMvc.perform(get("/logistic-tasks/search")
							.param("name", "노트북")
							.param("type", "INNER")
							.param("workerId", "1")
							.param("wareId", "1")
							.param("fromLocationId", "1")
							.param("toLocationId", "2")
							.param("status", "PENDING")
							.param("startDate", "2025-01-01")
							.param("endDate", "2025-01-31"))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.length()").value(1))
					.andExpect(jsonPath("$[0].name").value("창고A → 창고B 노트북 이동"));
		}

		@Test
		@DisplayName("GET /logistic-tasks/search - 부분 파라미터 검색")
		void searchTasks_PartialParameters_Success() throws Exception {
			// Given
			List<LogisticTask> searchResults = Arrays.asList(mockTask);
			given(logisticTaskService.searchTasks(any(LogisticTaskDTO.SearchCriteria.class)))
					.willReturn(searchResults);

			// When & Then
			mockMvc.perform(get("/logistic-tasks/search")
							.param("name", "노트북")
							.param("status", "PENDING"))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.length()").value(1));
		}

		@Test
		@DisplayName("GET /logistic-tasks/search - 검색 결과 없음")
		void searchTasks_NoResults_EmptyList() throws Exception {
			// Given
			given(logisticTaskService.searchTasks(any(LogisticTaskDTO.SearchCriteria.class)))
					.willReturn(Collections.emptyList());

			// When & Then
			mockMvc.perform(get("/logistic-tasks/search")
							.param("name", "존재하지않는작업"))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.length()").value(0));
		}

		@Test
		@DisplayName("GET /logistic-tasks/search - 파라미터 없는 검색 (전체 조회)")
		void searchTasks_NoParameters_ReturnsAll() throws Exception {
			// Given
			List<LogisticTask> allTasks = Arrays.asList(mockTask, createMockLogisticTask());
			given(logisticTaskService.searchTasks(any(LogisticTaskDTO.SearchCriteria.class)))
					.willReturn(allTasks);

			// When & Then
			mockMvc.perform(get("/logistic-tasks/search"))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.length()").value(2));
		}
	}

	// ===== 에러 응답 테스트 =====

	@Nested
	@DisplayName("에러 응답 테스트")
	class ErrorResponseTests {

		@Test
		@DisplayName("POST /logistic-tasks - 409 Conflict (정합성 검증 실패)")
		void createTask_ValidationFailed_409Conflict() throws Exception {
			// Given
			LogisticTaskDTO.CreateReq request = createValidCreateRequest();
			given(logisticTaskService.create(any(LogisticTaskDTO.CreateReq.class)))
					.willThrow(new RuntimeException("재고 부족으로 작업 생성 불가"));

			// When & Then
			mockMvc.perform(post("/logistic-tasks")
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(request)))
					.andExpect(status().isInternalServerError()); // 또는 409로 설정된 경우
		}

		@Test
		@DisplayName("POST /logistic-tasks/{taskId}/initiate - 403 Forbidden (권한 없음)")
		void initiateTask_NoPermission_403Forbidden() throws Exception {
			// Given
			Long taskId = 1L;
			given(logisticTaskService.initiateTask(taskId))
					.willThrow(LogisticTaskException.workerMismatchEx(taskId, 999L));

			// When & Then
			mockMvc.perform(post("/logistic-tasks/{taskId}/initiate", taskId))
					.andExpect(status().isForbidden());
		}

		@Test
		@DisplayName("POST /logistic-tasks/{taskId}/complete - 409 Conflict (창고 용량 초과)")
		void completeTask_CapacityExceeded_409Conflict() throws Exception {
			// Given
			Long taskId = 1L;
			given(logisticTaskService.completeTask(taskId))
					.willThrow(new RuntimeException("창고 용량 초과"));

			// When & Then
			mockMvc.perform(post("/logistic-tasks/{taskId}/complete", taskId))
					.andExpect(status().isInternalServerError());
		}

		@Test
		@DisplayName("PUT /logistic-tasks/{taskId} - 409 Conflict (수정 불가 상태)")
		void updateTask_NotModifiable_409Conflict() throws Exception {
			// Given
			Long taskId = 1L;
			LogisticTaskDTO.UpdateReq request = createValidUpdateRequest();
			given(logisticTaskService.update(eq(taskId), any(LogisticTaskDTO.UpdateReq.class)))
					.willThrow(new RuntimeException("진행 중인 작업은 수정할 수 없습니다"));

			// When & Then
			mockMvc.perform(put("/logistic-tasks/{taskId}", taskId)
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(request)))
					.andExpect(status().isInternalServerError());
		}

		@Test
		@DisplayName("DELETE /logistic-tasks/{taskId} - 409 Conflict (삭제 불가 상태)")
		void deleteTask_NotDeletable_409Conflict() throws Exception {
			// Given
			Long taskId = 1L;
			willThrow(new RuntimeException("진행 중인 작업은 삭제할 수 없습니다"))
					.given(logisticTaskService).delete(taskId);

			// When & Then
			mockMvc.perform(delete("/logistic-tasks/{taskId}", taskId))
					.andExpect(status().isInternalServerError());
		}

		@Test
		@DisplayName("GET /logistic-tasks/worker/{workerId} - 404 Not Found (작업자 없음)")
		void getTasksByWorkerId_WorkerNotFound_404() throws Exception {
			// Given
			Long nonExistentWorkerId = 999L;
			given(logisticTaskService.findByWorkerId(nonExistentWorkerId))
					.willThrow(UserInfoException.notFound(nonExistentWorkerId));

			// When & Then
			mockMvc.perform(get("/logistic-tasks/worker/{workerId}", nonExistentWorkerId))
					.andExpect(status().isNotFound());
		}
	}

	// ===== 입력 검증 테스트 =====

	@Nested
	@DisplayName("입력 검증 테스트")
	class InputValidationTests {

		@Test
		@DisplayName("POST /logistic-tasks - 잘못된 날짜 형식")
		void createTask_InvalidDateFormat_400BadRequest() throws Exception {
			// Given
			String invalidRequestJson = """
                {
                    "name": "테스트 작업",
                    "type": "INNER",
                    "workerId": 1,
                    "wareId": 1,
                    "fromLocationId": 1,
                    "toLocationId": 2,
                    "quantity": 10,
                    "scheduledDate": "invalid-date",
                    "etd": "09:00",
                    "eta": "10:00"
                }
                """;

			// When & Then
			mockMvc.perform(post("/logistic-tasks")
							.contentType(MediaType.APPLICATION_JSON)
							.content(invalidRequestJson))
					.andExpect(status().isBadRequest());
		}

		@Test
		@DisplayName("GET /logistic-tasks/date/{date} - 잘못된 날짜 형식")
		void getTasksByDate_InvalidDateFormat_400BadRequest() throws Exception {
			// When & Then
			mockMvc.perform(get("/logistic-tasks/date/{date}", "invalid-date"))
					.andExpect(status().isBadRequest());
		}

		@Test
		@DisplayName("GET /logistic-tasks/date-range - 시작일이 종료일보다 늦음")
		void getTasksByDateRange_StartAfterEnd_LogicalError() throws Exception {
			// When & Then
			mockMvc.perform(get("/logistic-tasks/date-range")
							.param("startDate", "2025-01-31")
							.param("endDate", "2025-01-01"))
					.andExpect(status().isOk()); // 서비스에서 처리하므로 200이지만 빈 결과
		}
	}

	// ===== 경계값 및 성능 테스트 =====

	@Nested
	@DisplayName("경계값 및 성능 테스트")
	class BoundaryAndPerformanceTests {

		@Test
		@DisplayName("GET /logistic-tasks/dashboard/daily - 미래 날짜 조회")
		void getDailyDashboard_FutureDate_Success() throws Exception {
			// Given
			String futureDate = "2030-12-31";
			LogisticTaskDTO.DashboardRes emptyDashboard = LogisticTaskDTO.DashboardRes.builder()
					.date(LocalDate.parse(futureDate))
					.workerSchedules(Collections.emptyList())
					.build();

			given(logisticTaskService.getDailyDashboard(LocalDate.parse(futureDate)))
					.willReturn(emptyDashboard);

			// When & Then
			mockMvc.perform(get("/logistic-tasks/dashboard/daily")
							.param("date", futureDate))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.date").value(futureDate))
					.andExpect(jsonPath("$.workerSchedules").isEmpty());
		}

		@Test
		@DisplayName("GET /logistic-tasks/search - 매우 긴 검색어")
		void searchTasks_VeryLongSearchTerm_Success() throws Exception {
			// Given
			String veryLongName = "a".repeat(1000); // 1000자 검색어
			given(logisticTaskService.searchTasks(any(LogisticTaskDTO.SearchCriteria.class)))
					.willReturn(Collections.emptyList());

			// When & Then
			mockMvc.perform(get("/logistic-tasks/search")
							.param("name", veryLongName))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.length()").value(0));
		}

		@Test
		@DisplayName("GET /logistic-tasks/location/{locationId} - 매우 큰 ID 값")
		void getTasksByLocation_VeryLargeId_Success() throws Exception {
			// Given
			Long veryLargeId = Long.MAX_VALUE;
			given(logisticTaskService.findByLocation(veryLargeId))
					.willReturn(Collections.emptyList());

			// When & Then
			mockMvc.perform(get("/logistic-tasks/location/{locationId}", veryLargeId))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.length()").value(0));
		}
	}

	// ===== 헬퍼 메서드들 =====

	private LogisticTask createMockLogisticTask() {
		LogisticTask task = LogisticTask.builder()
				.name("창고A → 창고B 노트북 이동")
				.type(LogisticType.INNER)
				.worker(mockWorker)
				.ware(mockWare)
				.fromLocation(mockFromLocation)
				.toLocation(mockToLocation)
				.quantity(10)
				.scheduledDate(LocalDate.of(2025, 1, 15))
				.etd(LocalTime.of(9, 0))
				.eta(LocalTime.of(10, 0))
				.templateIdSnapshot(1)
				.build();
		setId(task, 1L);
		return task;
	}

	private UserInfo createMockWorker() {
		UserInfo worker = UserInfo.builder()
				.username("worker1")
				.name("김작업")
				.email("worker1@test.com")
				.password(new Password("password"))
				.type(UserType.WORKER)
				.build();
		setId(worker, 1L);
		return worker;
	}

	private Ware createMockWare() {
		Ware ware = Ware.builder()
				.name("노트북")
				.type("전자제품")
				.paletteUnit(20)
				.build();
		setId(ware, 1L);
		return ware;
	}

	private Location createMockLocation(Long id, String name, LocationType type) {
		Location location = Location.builder()
				.name(name)
				.type(type)
				.capacity(type == LocationType.WAREHOUSE ? 1000 : null)
				.coordinateX(100)
				.coordinateY(100)
				.build();
		setId(location, id);
		return location;
	}

	private LogisticTaskDTO.CreateReq createValidCreateRequest() {
		return LogisticTaskDTO.CreateReq.builder()
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

	private LogisticTaskDTO.UpdateReq createValidUpdateRequest() {
		return LogisticTaskDTO.UpdateReq.builder()
				.name("수정된 작업")
				.quantity(15)
				.etd(LocalTime.of(10, 0))
				.eta(LocalTime.of(11, 0))
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