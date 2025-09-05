package com.wms.logisticTask.interfaces;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wms.applicationInfra.config.TestSecurityConfig;
import com.wms.applicationInfra.config.TestConfig;
import com.wms.logisticTask.application.LogisticTaskService;
import com.wms.logisticTask.domain.exception.LogisticTaskException;
import com.wms.logisticTask.domain.model.LogisticTask;
import com.wms.logisticTask.domain.model.LogisticTaskStatus;
import com.wms.logisticTask.dto.LogisticTaskDTO;
import com.wms.logisticTemplate.domain.model.LogisticType;
import com.wms.location.domain.model.Location;
import com.wms.location.domain.model.LocationType;
import com.wms.userInfo.domain.model.UserInfo;
import com.wms.userInfo.domain.model.UserType;
import com.wms.userInfo.domain.model.Password;
import com.wms.ware.domain.model.Ware;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(LogisticTaskController.class)
@Import({TestSecurityConfig.class, TestConfig.class})
@DisplayName("LogisticTaskController 웹 레이어 테스트")
class LogisticTaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LogisticTaskService logisticTaskService;

    @Autowired
    private ObjectMapper objectMapper;

    // ===== CRUD 테스트 =====

    @Test
    @DisplayName("POST /logistic-tasks - 물류 작업 생성 성공")
    void createTask_Success() throws Exception {
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

        LogisticTask createdTask = createMockLogisticTask();
        given(logisticTaskService.create(any(LogisticTaskDTO.CreateReq.class)))
                .willReturn(createdTask);

        // When & Then
        mockMvc.perform(post("/logistic-tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("창고A → 창고B 노트북 이동"))
                .andExpect(jsonPath("$.type").value("INNER"))
                .andExpect(jsonPath("$.workerId").value(1L))
                .andExpect(jsonPath("$.wareId").value(1L))
                .andExpect(jsonPath("$.quantity").value(10))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @DisplayName("POST /logistic-tasks - 잘못된 요청 데이터로 400 반환")
    void createTask_InvalidRequest() throws Exception {
        // Given - 필수 필드 누락
        LogisticTaskDTO.CreateReq request = LogisticTaskDTO.CreateReq.builder()
                .name("") // 빈 이름
                .type(LogisticType.INNER)
                .quantity(-5) // 음수 수량
                .build();

        // When & Then
        mockMvc.perform(post("/logistic-tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /logistic-tasks/{taskId} - 물류 작업 전체 수정 성공")
    void updateTask_Success() throws Exception {
        // Given
        Long taskId = 1L;
        LogisticTaskDTO.UpdateReq request = LogisticTaskDTO.UpdateReq.builder()
                .name("창고A → 창고B 노트북 이동 (수정)")
                .quantity(15)
                .etd(LocalTime.of(10, 0))
                .eta(LocalTime.of(11, 0))
                .build();

        LogisticTask updatedTask = createMockLogisticTask();
        given(logisticTaskService.update(eq(taskId), any(LogisticTaskDTO.UpdateReq.class)))
                .willReturn(updatedTask);

        // When & Then
        mockMvc.perform(put("/logistic-tasks/{taskId}", taskId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("창고A → 창고B 노트북 이동"));
    }

    @Test
    @DisplayName("PATCH /logistic-tasks/{taskId} - 물류 작업 부분 수정 성공")
    void partialUpdateTask_Success() throws Exception {
        // Given
        Long taskId = 1L;
        LogisticTaskDTO.PartialUpdateReq request = LogisticTaskDTO.PartialUpdateReq.builder()
                .etd(LocalTime.of(14, 0))
                .eta(LocalTime.of(15, 0))
                .build();

        LogisticTask updatedTask = createMockLogisticTask();
        given(logisticTaskService.partialUpdate(eq(taskId), any(LogisticTaskDTO.PartialUpdateReq.class)))
                .willReturn(updatedTask);

        // When & Then
        mockMvc.perform(patch("/logistic-tasks/{taskId}", taskId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workerId").value(1L));
    }

    @Test
    @DisplayName("DELETE /logistic-tasks/{taskId} - 물류 작업 삭제 성공")
    void deleteTask_Success() throws Exception {
        // Given
        Long taskId = 1L;
        willDoNothing().given(logisticTaskService).delete(taskId);

        // When & Then
        mockMvc.perform(delete("/logistic-tasks/{taskId}", taskId))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /logistic-tasks/{taskId} - 존재하지 않는 작업으로 404 반환")
    void deleteTask_NotFound() throws Exception {
        // Given
        Long taskId = 999L;
        willThrow(LogisticTaskException.notFound(taskId))
                .given(logisticTaskService).delete(taskId);

        // When & Then
        mockMvc.perform(delete("/logistic-tasks/{taskId}", taskId))
                .andExpect(status().isNotFound());
    }

    // ===== 작업 상태 변경 테스트 =====

    @Test
    @DisplayName("POST /logistic-tasks/{taskId}/initiate - 물류 작업 시작 성공")
    void initiateTask_Success() throws Exception {
        // Given
        Long taskId = 1L;
        LogisticTaskDTO.ActionRes response = LogisticTaskDTO.ActionRes.builder()
                .id(taskId)
                .name("창고A → 창고B 노트북 이동")
                .previousStatus(LogisticTaskStatus.PENDING)
                .currentStatus(LogisticTaskStatus.INITIATED)
                .processedAt(LocalDateTime.now())
                .build();

        given(logisticTaskService.initiateTask(taskId)).willReturn(response);

        // When & Then
        mockMvc.perform(post("/logistic-tasks/{taskId}/initiate", taskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(taskId))
                .andExpect(jsonPath("$.previousStatus").value("PENDING"))
                .andExpect(jsonPath("$.currentStatus").value("INITIATED"));
    }

    @Test
    @DisplayName("POST /logistic-tasks/{taskId}/complete - 물류 작업 완료 성공")
    void completeTask_Success() throws Exception {
        // Given
        Long taskId = 1L;
        LogisticTaskDTO.ActionRes response = LogisticTaskDTO.ActionRes.builder()
                .id(taskId)
                .name("창고A → 창고B 노트북 이동")
                .previousStatus(LogisticTaskStatus.INITIATED)
                .currentStatus(LogisticTaskStatus.COMPLETED)
                .processedAt(LocalDateTime.now())
                .build();

        given(logisticTaskService.completeTask(taskId)).willReturn(response);

        // When & Then
        mockMvc.perform(post("/logistic-tasks/{taskId}/complete", taskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(taskId))
                .andExpect(jsonPath("$.previousStatus").value("INITIATED"))
                .andExpect(jsonPath("$.currentStatus").value("COMPLETED"));
    }

    @Test
    @DisplayName("POST /logistic-tasks/{taskId}/cancel - 물류 작업 취소 성공")
    void cancelTask_Success() throws Exception {
        // Given
        Long taskId = 1L;
        LogisticTaskDTO.ActionRes response = LogisticTaskDTO.ActionRes.builder()
                .id(taskId)
                .name("창고A → 창고B 노트북 이동")
                .previousStatus(LogisticTaskStatus.PENDING)
                .currentStatus(LogisticTaskStatus.CANCELLED)
                .processedAt(LocalDateTime.now())
                .build();

        given(logisticTaskService.cancelTask(taskId)).willReturn(response);

        // When & Then
        mockMvc.perform(post("/logistic-tasks/{taskId}/cancel", taskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(taskId))
                .andExpect(jsonPath("$.currentStatus").value("CANCELLED"));
    }

    @Test
    @DisplayName("POST /logistic-tasks/{taskId}/fail - 물류 작업 실패 처리 성공")
    void failTask_Success() throws Exception {
        // Given
        Long taskId = 1L;
        LogisticTaskDTO.ActionRes response = LogisticTaskDTO.ActionRes.builder()
                .id(taskId)
                .name("창고A → 창고B 노트북 이동")
                .previousStatus(LogisticTaskStatus.INITIATED)
                .currentStatus(LogisticTaskStatus.FAILED)
                .processedAt(LocalDateTime.now())
                .build();

        given(logisticTaskService.failTask(taskId)).willReturn(response);

        // When & Then
        mockMvc.perform(post("/logistic-tasks/{taskId}/fail", taskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(taskId))
                .andExpect(jsonPath("$.currentStatus").value("FAILED"));
    }

    @Test
    @DisplayName("POST /logistic-tasks/{taskId}/delay/initiation - 시작 지연 처리 성공")
    void delayInitiation_Success() throws Exception {
        // Given
        Long taskId = 1L;
        LogisticTaskDTO.ActionRes response = LogisticTaskDTO.ActionRes.builder()
                .id(taskId)
                .name("창고A → 창고B 노트북 이동")
                .previousStatus(LogisticTaskStatus.PENDING)
                .currentStatus(LogisticTaskStatus.INITIATE_DELAYED)
                .processedAt(LocalDateTime.now())
                .build();

        given(logisticTaskService.delayTask(taskId, true)).willReturn(response);

        // When & Then
        mockMvc.perform(post("/logistic-tasks/{taskId}/delay/initiation", taskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(taskId))
                .andExpect(jsonPath("$.currentStatus").value("INITIATE_DELAYED"));
    }

    @Test
    @DisplayName("POST /logistic-tasks/{taskId}/delay/completion - 완료 지연 처리 성공")
    void delayCompletion_Success() throws Exception {
        // Given
        Long taskId = 1L;
        LogisticTaskDTO.ActionRes response = LogisticTaskDTO.ActionRes.builder()
                .id(taskId)
                .name("창고A → 창고B 노트북 이동")
                .previousStatus(LogisticTaskStatus.INITIATED)
                .currentStatus(LogisticTaskStatus.COMPLETE_DELAYED)
                .processedAt(LocalDateTime.now())
                .build();

        given(logisticTaskService.delayTask(taskId, false)).willReturn(response);

        // When & Then
        mockMvc.perform(post("/logistic-tasks/{taskId}/delay/completion", taskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(taskId))
                .andExpect(jsonPath("$.currentStatus").value("COMPLETE_DELAYED"));
    }

    // ===== 조회 테스트 =====

    @Test
    @DisplayName("GET /logistic-tasks/{taskId} - 물류 작업 단건 조회 성공")
    void getTask_Success() throws Exception {
        // Given
        Long taskId = 1L;
        LogisticTask task = createMockLogisticTask();
        given(logisticTaskService.findById(taskId)).willReturn(task);

        // When & Then
        mockMvc.perform(get("/logistic-tasks/{taskId}", taskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(taskId))
                .andExpect(jsonPath("$.name").value("창고A → 창고B 노트북 이동"));
    }

    @Test
    @DisplayName("GET /logistic-tasks - 모든 물류 작업 조회 성공")
    void getAllTasks_Success() throws Exception {
        // Given
        List<LogisticTask> tasks = Arrays.asList(
                createMockLogisticTask(),
                createMockLogisticTask()
        );
        given(logisticTaskService.findAll()).willReturn(tasks);

        // When & Then
        mockMvc.perform(get("/logistic-tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @DisplayName("GET /logistic-tasks/status/{status} - 상태별 물류 작업 조회 성공")
    void getTasksByStatus_Success() throws Exception {
        // Given
        LogisticTaskStatus status = LogisticTaskStatus.PENDING;
        List<LogisticTask> tasks = Arrays.asList(createMockLogisticTask());
        given(logisticTaskService.findByStatus(status)).willReturn(tasks);

        // When & Then
        mockMvc.perform(get("/logistic-tasks/status/{status}", status))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /logistic-tasks/worker/{workerId} - 작업자별 물류 작업 조회 성공")
    void getTasksByWorkerId_Success() throws Exception {
        // Given
        Long workerId = 1L;
        List<LogisticTask> tasks = Arrays.asList(createMockLogisticTask());
        given(logisticTaskService.findByWorkerId(workerId)).willReturn(tasks);

        // When & Then
        mockMvc.perform(get("/logistic-tasks/worker/{workerId}", workerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    // ===== 유틸리티 메서드 =====

    private LogisticTask createMockLogisticTask() {
        // ID 설정을 위한 리플렉션 사용
        UserInfo worker = UserInfo.builder()
                .username("worker1")
                .name("김작업")
                .email("worker1@test.com")
                .password(new Password("password"))
                .type(UserType.WORKER)
                .build();
        setId(worker, 1L);

        Ware ware = Ware.builder()
                .name("노트북")
                .type("전자제품")
                .paletteUnit(20)
                .build();
        setId(ware, 1L);

        Location fromLocation = Location.builder()
                .name("창고A")
                .type(LocationType.WAREHOUSE)
                .capacity(1000)
                .coordinateX(100)
                .coordinateY(100)
                .build();
        setId(fromLocation, 1L);

        Location toLocation = Location.builder()
                .name("창고B")
                .type(LocationType.WAREHOUSE)
                .capacity(800)
                .coordinateX(200)
                .coordinateY(100)
                .build();
        setId(toLocation, 2L);

        LogisticTask task = LogisticTask.builder()
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

        return task;
    }

    /**
     * 리플렉션을 사용하여 ID 설정
     */
    private void setId(Object entity, Long id) {
        try {
            java.lang.reflect.Field idField = entity.getClass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(entity, id);
        } catch (Exception e) {
            // BaseEntity를 상속한 경우 부모 클래스에서 id 필드를 찾음
            try {
                java.lang.reflect.Field idField = entity.getClass().getSuperclass().getDeclaredField("id");
                idField.setAccessible(true);
                idField.set(entity, id);
            } catch (Exception ex) {
                throw new RuntimeException("ID 설정 실패", ex);
            }
        }
    }
}
