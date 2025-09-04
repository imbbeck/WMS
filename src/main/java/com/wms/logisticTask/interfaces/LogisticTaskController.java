package com.wms.logisticTask.interfaces;

import com.wms.logisticTask.application.LogisticTaskService;
import com.wms.logisticTask.domain.model.LogisticTask;
import com.wms.logisticTask.domain.model.LogisticTaskStatus;
import com.wms.logisticTask.dto.LogisticTaskDTO;
import com.wms.logisticTemplate.domain.model.LogisticType;
import com.wms.userInfo.domain.model.UserInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 물류 작업 관리 컨트롤러
 * - 물류 작업 CRUD 및 상태 변경 처리
 * - 작업자별 권한 관리
 * - 실시간 작업 상태 변경 (시작, 완료, 취소, 실패, 지연)
 */
@RestController
@RequestMapping("/logistic-tasks")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "LogisticTask", description = "물류 작업 관리 API")
public class LogisticTaskController {

    private final LogisticTaskService logisticTaskService;

    // ===== CRUD 기본 작업 =====

    /**
     * 물류 작업 생성
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "물류 작업 생성", description = "새로운 물류 작업을 생성합니다. 정합성 검증을 포함합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "물류 작업 생성 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
            @ApiResponse(responseCode = "403", description = "권한 없음 (ADMIN 전용)"),
            @ApiResponse(responseCode = "409", description = "정합성 검증 실패")
    })
    public ResponseEntity<LogisticTaskDTO.Res> createTask(
            @Valid @RequestBody LogisticTaskDTO.CreateReq request) {
        
        log.info("물류 작업 생성 요청: {}", request.getName());
        
        LogisticTask createdTask = logisticTaskService.create(request);
        LogisticTaskDTO.Res response = convertToResponse(createdTask);
        
        log.info("물류 작업 생성 완료: id={}, name={}", createdTask.getId(), createdTask.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 물류 작업 전체 수정
     */
    @PutMapping("/{taskId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "물류 작업 전체 수정", description = "물류 작업의 모든 정보를 수정합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "물류 작업 수정 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
            @ApiResponse(responseCode = "403", description = "권한 없음 (ADMIN 전용)"),
            @ApiResponse(responseCode = "404", description = "물류 작업을 찾을 수 없음"),
            @ApiResponse(responseCode = "409", description = "수정 불가능한 상태 또는 정합성 검증 실패")
    })
    public ResponseEntity<LogisticTaskDTO.Res> updateTask(
            @Parameter(description = "물류 작업 ID") @PathVariable Long taskId,
            @Valid @RequestBody LogisticTaskDTO.UpdateReq request) {
        
        log.info("물류 작업 전체 수정 요청: taskId={}, name={}", taskId, request.getName());
        
        LogisticTask updatedTask = logisticTaskService.update(taskId, request);
        LogisticTaskDTO.Res response = convertToResponse(updatedTask);
        
        log.info("물류 작업 전체 수정 완료: taskId={}", taskId);
        return ResponseEntity.ok(response);
    }

    /**
     * 물류 작업 부분 수정 (작업자 및 시간만)
     */
    @PatchMapping("/{taskId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "물류 작업 부분 수정", description = "물류 작업의 예정 시간만 수정합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "물류 작업 부분 수정 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
            @ApiResponse(responseCode = "403", description = "권한 없음 (ADMIN 전용)"),
            @ApiResponse(responseCode = "404", description = "물류 작업을 찾을 수 없음"),
            @ApiResponse(responseCode = "409", description = "수정 불가능한 상태")
    })
    public ResponseEntity<LogisticTaskDTO.Res> partialUpdateTask(
            @Parameter(description = "물류 작업 ID") @PathVariable Long taskId,
            @Valid @RequestBody LogisticTaskDTO.PartialUpdateReq request) {
        
        log.info("물류 작업 부분 수정 요청: taskId={}", taskId);
        
        LogisticTask updatedTask = logisticTaskService.partialUpdate(taskId, request);
        LogisticTaskDTO.Res response = convertToResponse(updatedTask);
        
        log.info("물류 작업 부분 수정 완료: taskId={}", taskId);
        return ResponseEntity.ok(response);
    }

    /**
     * 물류 작업 삭제
     */
    @DeleteMapping("/{taskId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "물류 작업 삭제", description = "물류 작업을 삭제합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "물류 작업 삭제 성공"),
            @ApiResponse(responseCode = "403", description = "권한 없음 (ADMIN 전용)"),
            @ApiResponse(responseCode = "404", description = "물류 작업을 찾을 수 없음"),
            @ApiResponse(responseCode = "409", description = "삭제 불가능한 상태")
    })
    public ResponseEntity<Void> deleteTask(
            @Parameter(description = "물류 작업 ID") @PathVariable Long taskId) {
        
        log.info("물류 작업 삭제 요청: taskId={}", taskId);
        
        logisticTaskService.delete(taskId);
        
        log.info("물류 작업 삭제 완료: taskId={}", taskId);
        return ResponseEntity.noContent().build();
    }

    // ===== 작업 상태 변경 작업 =====

    /**
     * 물류 작업 시작
     */
    @PostMapping("/{taskId}/initiate")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('WORKER') and @logisticTaskService.findById(#taskId).worker.id == authentication.principal.id)")
    @Operation(summary = "물류 작업 시작", description = "물류 작업을 시작합니다. 배정된 작업자만 실행 가능합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "물류 작업 시작 성공"),
            @ApiResponse(responseCode = "403", description = "권한 없음 (배정된 작업자가 아님)"),
            @ApiResponse(responseCode = "404", description = "물류 작업을 찾을 수 없음"),
            @ApiResponse(responseCode = "409", description = "시작 불가능한 상태 또는 재고 부족")
    })
    public ResponseEntity<LogisticTaskDTO.ActionRes> initiateTask(
            @Parameter(description = "물류 작업 ID") @PathVariable Long taskId) {
        
        log.info("물류 작업 시작 요청: taskId={}", taskId);
        
        LogisticTaskDTO.ActionRes response = logisticTaskService.initiateTask(taskId);
        
        log.info("물류 작업 시작 완료: taskId={}, status={}", taskId, response.getCurrentStatus());
        return ResponseEntity.ok(response);
    }

    /**
     * 물류 작업 완료
     */
    @PostMapping("/{taskId}/complete")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('WORKER') and @logisticTaskService.findById(#taskId).worker.id == authentication.principal.id)")
    @Operation(summary = "물류 작업 완료", description = "물류 작업을 완료합니다. 배정된 작업자만 실행 가능합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "물류 작업 완료 성공"),
            @ApiResponse(responseCode = "403", description = "권한 없음 (배정된 작업자가 아님)"),
            @ApiResponse(responseCode = "404", description = "물류 작업을 찾을 수 없음"),
            @ApiResponse(responseCode = "409", description = "완료 불가능한 상태 또는 창고 용량 초과")
    })
    public ResponseEntity<LogisticTaskDTO.ActionRes> completeTask(
            @Parameter(description = "물류 작업 ID") @PathVariable Long taskId) {
        
        log.info("물류 작업 완료 요청: taskId={}", taskId);
        
        LogisticTaskDTO.ActionRes response = logisticTaskService.completeTask(taskId);
        
        log.info("물류 작업 완료: taskId={}, status={}", taskId, response.getCurrentStatus());
        return ResponseEntity.ok(response);
    }

    /**
     * 물류 작업 취소
     */
    @PostMapping("/{taskId}/cancel")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('WORKER') and @logisticTaskService.findById(#taskId).worker.id == authentication.principal.id)")
    @Operation(summary = "물류 작업 취소", description = "물류 작업을 취소합니다. 배정된 작업자만 실행 가능합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "물류 작업 취소 성공"),
            @ApiResponse(responseCode = "403", description = "권한 없음 (배정된 작업자가 아님)"),
            @ApiResponse(responseCode = "404", description = "물류 작업을 찾을 수 없음"),
            @ApiResponse(responseCode = "409", description = "취소 불가능한 상태")
    })
    public ResponseEntity<LogisticTaskDTO.ActionRes> cancelTask(
            @Parameter(description = "물류 작업 ID") @PathVariable Long taskId) {
        
        log.info("물류 작업 취소 요청: taskId={}", taskId);
        
        LogisticTaskDTO.ActionRes response = logisticTaskService.cancelTask(taskId);
        
        log.info("물류 작업 취소 완료: taskId={}, status={}", taskId, response.getCurrentStatus());
        return ResponseEntity.ok(response);
    }

    /**
     * 물류 작업 실패 처리
     */
    @PostMapping("/{taskId}/fail")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('WORKER') and @logisticTaskService.findById(#taskId).worker.id == authentication.principal.id)")
    @Operation(summary = "물류 작업 실패 처리", description = "물류 작업을 실패로 처리합니다. 배정된 작업자만 실행 가능합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "물류 작업 실패 처리 성공"),
            @ApiResponse(responseCode = "403", description = "권한 없음 (배정된 작업자가 아님)"),
            @ApiResponse(responseCode = "404", description = "물류 작업을 찾을 수 없음"),
            @ApiResponse(responseCode = "409", description = "실패 처리 불가능한 상태")
    })
    public ResponseEntity<LogisticTaskDTO.ActionRes> failTask(
            @Parameter(description = "물류 작업 ID") @PathVariable Long taskId) {
        
        log.info("물류 작업 실패 처리 요청: taskId={}", taskId);
        
        LogisticTaskDTO.ActionRes response = logisticTaskService.failTask(taskId);
        
        log.info("물류 작업 실패 처리 완료: taskId={}, status={}", taskId, response.getCurrentStatus());
        return ResponseEntity.ok(response);
    }

    /**
     * 물류 작업 지연 처리 (시작 지연)
     */
    @PostMapping("/{taskId}/delay/initiation")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('WORKER') and @logisticTaskService.findById(#taskId).worker.id == authentication.principal.id)")
    @Operation(summary = "물류 작업 시작 지연 처리", description = "물류 작업의 시작을 지연으로 처리합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "시작 지연 처리 성공"),
            @ApiResponse(responseCode = "403", description = "권한 없음 (배정된 작업자가 아님)"),
            @ApiResponse(responseCode = "404", description = "물류 작업을 찾을 수 없음"),
            @ApiResponse(responseCode = "409", description = "지연 처리 불가능한 상태")
    })
    public ResponseEntity<LogisticTaskDTO.ActionRes> delayInitiation(
            @Parameter(description = "물류 작업 ID") @PathVariable Long taskId) {
        
        log.info("물류 작업 시작 지연 처리 요청: taskId={}", taskId);
        
        LogisticTaskDTO.ActionRes response = logisticTaskService.delayTask(taskId, true);
        
        log.info("물류 작업 시작 지연 처리 완료: taskId={}, status={}", taskId, response.getCurrentStatus());
        return ResponseEntity.ok(response);
    }

    /**
     * 물류 작업 지연 처리 (완료 지연)
     */
    @PostMapping("/{taskId}/delay/completion")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('WORKER') and @logisticTaskService.findById(#taskId).worker.id == authentication.principal.id)")
    @Operation(summary = "물류 작업 완료 지연 처리", description = "물류 작업의 완료를 지연으로 처리합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "완료 지연 처리 성공"),
            @ApiResponse(responseCode = "403", description = "권한 없음 (배정된 작업자가 아님)"),
            @ApiResponse(responseCode = "404", description = "물류 작업을 찾을 수 없음"),
            @ApiResponse(responseCode = "409", description = "지연 처리 불가능한 상태")
    })
    public ResponseEntity<LogisticTaskDTO.ActionRes> delayCompletion(
            @Parameter(description = "물류 작업 ID") @PathVariable Long taskId) {
        
        log.info("물류 작업 완료 지연 처리 요청: taskId={}", taskId);
        
        LogisticTaskDTO.ActionRes response = logisticTaskService.delayTask(taskId, false);
        
        log.info("물류 작업 완료 지연 처리 완료: taskId={}, status={}", taskId, response.getCurrentStatus());
        return ResponseEntity.ok(response);
    }

    // ===== 조회 작업 =====

    /**
     * 물류 작업 단건 조회
     */
    @GetMapping("/{taskId}")
    @Operation(summary = "물류 작업 단건 조회", description = "물류 작업의 상세 정보를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "물류 작업 조회 성공"),
            @ApiResponse(responseCode = "404", description = "물류 작업을 찾을 수 없음")
    })
    public ResponseEntity<LogisticTaskDTO.Res> getTask(
            @Parameter(description = "물류 작업 ID") @PathVariable Long taskId) {
        
        LogisticTask task = logisticTaskService.findById(taskId);
        LogisticTaskDTO.Res response = convertToResponse(task);
        
        return ResponseEntity.ok(response);
    }

    /**
     * 모든 물류 작업 조회
     */
    @GetMapping
    @Operation(summary = "모든 물류 작업 조회", description = "시스템의 모든 물류 작업을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "물류 작업 목록 조회 성공")
    public ResponseEntity<List<LogisticTaskDTO.Res>> getAllTasks() {
        
        List<LogisticTask> tasks = logisticTaskService.findAll();
        List<LogisticTaskDTO.Res> responses = tasks.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(responses);
    }

    /**
     * 상태별 물류 작업 조회
     */
    @GetMapping("/status/{status}")
    @Operation(summary = "상태별 물류 작업 조회", description = "특정 상태의 물류 작업들을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "상태별 물류 작업 조회 성공")
    public ResponseEntity<List<LogisticTaskDTO.Res>> getTasksByStatus(
            @Parameter(description = "물류 작업 상태") @PathVariable LogisticTaskStatus status) {
        
        List<LogisticTask> tasks = logisticTaskService.findByStatus(status);
        List<LogisticTaskDTO.Res> responses = tasks.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(responses);
    }

    /**
     * 작업자별 물류 작업 조회
     */
    @GetMapping("/worker/{workerId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('WORKER')")
    @Operation(summary = "작업자별 물류 작업 조회", description = "특정 작업자에게 배정된 물류 작업들을 조회합니다. WORKER는 자신의 작업만, ADMIN은 모든 작업자의 작업을 조회할 수 있습니다.")
    @ApiResponses({
		    @ApiResponse(responseCode = "200", description = "작업자별 물류 작업 조회 성공"),
		    @ApiResponse(responseCode = "403", description = "권한 없음"),
		    @ApiResponse(responseCode = "404", description = "작업자를 찾을 수 없음")
    })
    public ResponseEntity<List<LogisticTaskDTO.Res>> getTasksByWorkerId(
		    @Parameter(description = "작업자 ID (ADMIN만 사용)") @PathVariable Long workerId,
		    Authentication authentication) {

	    Long targetWorkerId;

	    // ADMIN 권한이 있으면 요청된 workerId 사용, 아니면 현재 사용자 ID 사용
	    if (authentication.getAuthorities().stream()
			    .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"))) {
		    targetWorkerId = workerId;
		    log.debug("ADMIN 사용자가 작업자 ID {} 조회", workerId);
	    } else {
		    // WORKER인 경우 자신의 ID만 사용 (요청 파라미터 무시)
		    UserInfo currentUser = (UserInfo) authentication.getPrincipal();
		    targetWorkerId = currentUser.getId();
		    log.debug("WORKER 사용자가 자신의 작업 조회 (요청 파라미터 {} 무시, 실제 조회 ID: {})",
				    workerId, targetWorkerId);
	    }

	    List<LogisticTask> tasks = logisticTaskService.findByWorkerId(targetWorkerId);
	    List<LogisticTaskDTO.Res> responses = tasks.stream()
			    .map(this::convertToResponse)
			    .collect(Collectors.toList());

	    return ResponseEntity.ok(responses);
    }

    /**
     * 날짜별 물류 작업 조회
     */
    @GetMapping("/date/{date}")
    @Operation(summary = "날짜별 물류 작업 조회", description = "특정 날짜의 모든 물류 작업을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "날짜별 물류 작업 조회 성공")
    public ResponseEntity<List<LogisticTaskDTO.Res>> getTasksByDate(
            @Parameter(description = "조회할 날짜 (YYYY-MM-DD)") @PathVariable String date) {
        
        LocalDate targetDate = LocalDate.parse(date);
        List<LogisticTask> tasks = logisticTaskService.findByScheduledDate(targetDate);
        List<LogisticTaskDTO.Res> responses = tasks.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(responses);
    }

    /**
     * 날짜 범위별 물류 작업 조회
     */
    @GetMapping("/date-range")
    @Operation(summary = "날짜 범위별 물류 작업 조회", description = "지정된 날짜 범위의 물류 작업들을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "날짜 범위별 물류 작업 조회 성공")
    public ResponseEntity<List<LogisticTaskDTO.Res>> getTasksByDateRange(
            @Parameter(description = "시작 날짜 (YYYY-MM-DD)") @RequestParam String startDate,
            @Parameter(description = "종료 날짜 (YYYY-MM-DD)") @RequestParam String endDate) {
        
        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);
        List<LogisticTask> tasks = logisticTaskService.findByDateRange(start, end);
        List<LogisticTaskDTO.Res> responses = tasks.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(responses);
    }

    /**
     * 특정 장소가 포함된 물류 작업 조회
     */
    @GetMapping("/location/{locationId}")
    @Operation(summary = "장소별 물류 작업 조회", description = "특정 장소가 출발지 또는 도착지인 물류 작업들을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "장소별 물류 작업 조회 성공")
    public ResponseEntity<List<LogisticTaskDTO.Res>> getTasksByLocation(
            @Parameter(description = "장소 ID") @PathVariable Long locationId) {
        
        List<LogisticTask> tasks = logisticTaskService.findByLocation(locationId);
        List<LogisticTaskDTO.Res> responses = tasks.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(responses);
    }

    /**
     * 특정 물품의 물류 작업 조회
     */
    @GetMapping("/ware/{wareId}")
    @Operation(summary = "물품별 물류 작업 조회", description = "특정 물품의 물류 작업들을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "물품별 물류 작업 조회 성공")
    public ResponseEntity<List<LogisticTaskDTO.Res>> getTasksByWare(
            @Parameter(description = "물품 ID") @PathVariable Long wareId) {
        
        List<LogisticTask> tasks = logisticTaskService.findByWareId(wareId);
        List<LogisticTaskDTO.Res> responses = tasks.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(responses);
    }

    /**
     * 대시보드용 날짜별 작업자별 물류 작업 조회
     */
    @GetMapping("/dashboard/daily")
    @Operation(summary = "대시보드용 일별 물류 작업 조회", description = "특정 날짜의 모든 작업자별 물류 작업을 시간순으로 조회합니다.")
    @ApiResponse(responseCode = "200", description = "일별 대시보드 조회 성공")
    public ResponseEntity<LogisticTaskDTO.DashboardRes> getDailyDashboard(
            @Parameter(description = "조회할 날짜 (YYYY-MM-DD)") @RequestParam String date) {
        
        LocalDate targetDate = LocalDate.parse(date);
        LogisticTaskDTO.DashboardRes dashboard = logisticTaskService.getDailyDashboard(targetDate);
        
        return ResponseEntity.ok(dashboard);
    }

    /**
     * 대시보드용 날짜별 작업자별 물류 작업 조회 - 라이브러리 특화
     */
    @GetMapping("/dashboard2/daily")
    @Operation(summary = "대시보드용 일별 물류 작업 조회 (라이브러리 특화)", description = "특정 날짜의 모든 작업자별 물류 작업을 시간순으로 조회합니다. 라이브러리 특화 버전.")
    @ApiResponse(responseCode = "200", description = "일별 대시보드 조회 성공")
    public ResponseEntity<LogisticTaskDTO.Dashboard2Res> getDailyDashboard2(
            @Parameter(description = "조회할 날짜 (YYYY-MM-DD)") @RequestParam String date) {

        LocalDate targetDate = LocalDate.parse(date);
        LogisticTaskDTO.Dashboard2Res dashboard = logisticTaskService.getDailyDashboard2(targetDate);

        return ResponseEntity.ok(dashboard);
    }

    /**
     * 작업자 대시보드용 개인 작업 조회
     */
    @GetMapping("/dashboard/worker/{workerId}")
    @PreAuthorize("hasRole('ADMIN') or authentication.principal.id == #workerId")
    @Operation(summary = "작업자 개인 대시보드", description = "작업자 개인의 금일 배정된 작업들을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "작업자 개인 대시보드 조회 성공")
    public ResponseEntity<List<LogisticTaskDTO.WorkerDashboardRes>> getWorkerDashboard(
            @Parameter(description = "작업자 ID") @PathVariable Long workerId,
            @Parameter(description = "조회할 날짜 (기본값: 오늘)") @RequestParam(required = false) String date) {
        
        LocalDate targetDate = date != null ? LocalDate.parse(date) : LocalDate.now();
        List<LogisticTask> tasks = logisticTaskService.findByWorkerAndDate(workerId, targetDate);
        List<LogisticTaskDTO.WorkerDashboardRes> responses = tasks.stream()
				.map(task -> LogisticTaskDTO.WorkerDashboardRes.from(
						task,
						task.getWare().getName(),
						task.getFromLocation().getName(),
						task.getToLocation().getName()))
				.collect(Collectors.toList());

        
        return ResponseEntity.ok(responses);
    }

    /**
     * 물류 작업 상태별 통계 조회
     */
    @GetMapping("/statistics")
    @Operation(summary = "물류 작업 상태별 통계", description = "전체 물류 작업의 상태별 통계를 조회합니다.")
    @ApiResponse(responseCode = "200", description = "통계 조회 성공")
    public ResponseEntity<Map<LogisticTaskStatus, Long>> getTaskStatistics() {
        
        Map<LogisticTaskStatus, Long> statistics = logisticTaskService.getTaskStatistics();
        
        return ResponseEntity.ok(statistics);
    }

    /**
     * 특정 날짜의 물류 작업 상태별 통계 조회
     */
    @GetMapping("/statistics/daily")
    @Operation(summary = "일별 물류 작업 통계", description = "특정 날짜의 물류 작업 상태별 통계를 조회합니다.")
    @ApiResponse(responseCode = "200", description = "일별 통계 조회 성공")
    public ResponseEntity<Map<LogisticTaskStatus, Long>> getDailyTaskStatistics(
            @Parameter(description = "조회할 날짜 (YYYY-MM-DD)") @RequestParam String date) {
        
        LocalDate targetDate = LocalDate.parse(date);
        Map<LogisticTaskStatus, Long> statistics = logisticTaskService.getDailyTaskStatistics(targetDate);
        
        return ResponseEntity.ok(statistics);
    }

    /**
     * 물류 작업 검색
     */
    @GetMapping("/search")
    @Operation(summary = "물류 작업 검색", description = "다양한 조건으로 물류 작업을 검색합니다.")
    @ApiResponse(responseCode = "200", description = "검색 성공")
    public ResponseEntity<List<LogisticTaskDTO.Res>> searchTasks(
            @Parameter(description = "작업명 (부분 검색)") @RequestParam(required = false) String name,
            @Parameter(description = "물류 타입") @RequestParam(required = false) LogisticType type,
            @Parameter(description = "작업자 ID") @RequestParam(required = false) Long workerId,
            @Parameter(description = "물품 ID") @RequestParam(required = false) Long wareId,
            @Parameter(description = "출발지 ID") @RequestParam(required = false) Long fromLocationId,
            @Parameter(description = "도착지 ID") @RequestParam(required = false) Long toLocationId,
            @Parameter(description = "상태") @RequestParam(required = false) LogisticTaskStatus status,
            @Parameter(description = "시작 날짜") @RequestParam(required = false) String startDate,
            @Parameter(description = "종료 날짜") @RequestParam(required = false) String endDate) {
        
        LogisticTaskDTO.SearchCriteria criteria = LogisticTaskDTO.SearchCriteria.builder()
                .name(name)
                .type(type)
                .workerId(workerId)
                .wareId(wareId)
                .fromLocationId(fromLocationId)
                .toLocationId(toLocationId)
                .status(status)
                .startDate(startDate != null ? LocalDate.parse(startDate) : null)
                .endDate(endDate != null ? LocalDate.parse(endDate) : null)
                .build();
        
        List<LogisticTask> tasks = logisticTaskService.searchTasks(criteria);
        List<LogisticTaskDTO.Res> responses = tasks.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(responses);
    }

    // ===== Private 유틸리티 메서드 =====

    /**
     * LogisticTask 엔티티를 응답 DTO로 변환
     * 연관 엔티티에서 직접 이름 조회 (캐시 불필요)
     */
    private LogisticTaskDTO.Res convertToResponse(LogisticTask task) {
        return LogisticTaskDTO.Res.from(
                task, 
                task.getWorker().getName(),
                task.getWare().getName(),
                task.getFromLocation().getName(),
                task.getToLocation().getName()
        );
    }
}
