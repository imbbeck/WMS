package com.wms.logisticTask.application;

import com.wms.applicationInfra.idnameMapCashing.DomainCacheManager;
import com.wms.location.domain.exception.LocationException;
import com.wms.location.domain.model.Location;
import com.wms.location.domain.repository.LocationRepository;
import com.wms.logisticTask.domain.event.LogisticTaskCompletedEvent;
import com.wms.logisticTask.domain.event.LogisticTaskInitiatedEvent;
import com.wms.logisticTask.domain.exception.LogisticTaskException;
import com.wms.logisticTask.domain.model.LogisticTask;
import com.wms.logisticTask.domain.model.LogisticTaskStatus;
import com.wms.logisticTask.domain.repository.LogisticTaskRepository;
import com.wms.logisticTask.dto.LogisticTaskDTO;
import com.wms.userInfo.domain.exception.UserInfoException;
import com.wms.userInfo.domain.model.UserInfo;
import com.wms.userInfo.domain.model.UserType;
import com.wms.userInfo.domain.repository.UserInfoRepository;
import com.wms.ware.domain.exception.WareException;
import com.wms.ware.domain.model.Ware;
import com.wms.ware.domain.repository.WareRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 물류 작업 서비스
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class LogisticTaskService {

    private final LogisticTaskRepository logisticTaskRepository;
    private final UserInfoRepository userInfoRepository;
    private final WareRepository wareRepository;
    private final LocationRepository locationRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final LogisticTaskValidationService validationService;

    /**
     * 물류 작업 생성
     */
    @Transactional
    public LogisticTask create(LogisticTaskDTO.CreateReq request) {
        log.debug("물류 작업 생성 - name: {}, type: {}", request.getName(), request.getType());

        // 1. 엔티티 조회 및 검증
        UserInfo worker = userInfoRepository.findById(request.getWorkerId())
                .orElseThrow(() -> UserInfoException.notFound(request.getWorkerId()));
        
        Ware ware = wareRepository.findById(request.getWareId())
                .orElseThrow(() -> WareException.notFound(request.getWareId()));
        
        Location fromLocation = locationRepository.findById(request.getFromLocationId())
                .orElseThrow(() -> LocationException.notFound(request.getFromLocationId()));
        
        Location toLocation = locationRepository.findById(request.getToLocationId())
                .orElseThrow(() -> LocationException.notFound(request.getToLocationId()));

        // 2. 물류 작업 생성
        LogisticTask logisticTask = LogisticTask.builder()
                .name(request.getName())
                .type(request.getType())
                .worker(worker)
                .ware(ware)
                .fromLocation(fromLocation)
                .toLocation(toLocation)
                .quantity(request.getQuantity())
                .scheduledDate(request.getScheduledDate())
                .etd(request.getEtd())
                .eta(request.getEta())
                .templateIdSnapshot(request.getTemplateIdSnapshot())
                .build();

        LogisticTask logisticTask1 = request.toEntity(worker, ware, fromLocation, toLocation);

        // 3. 정합성 검증 (시뮬레이션)
        validationService.validateTaskCreation(logisticTask);

        // 4. 저장
        LogisticTask savedTask = logisticTaskRepository.save(logisticTask);
        
        log.info("물류 작업 생성 완료 - id: {}, name: {}", savedTask.getId(), savedTask.getName());
        return savedTask;
    }

    /**
     * 물류 작업 전체 수정
     */
    @Transactional
    public LogisticTask update(Long taskId, LogisticTaskDTO.UpdateReq request) {
        log.debug("물류 작업 수정 - taskId: {}, name: {}", taskId, request.getName());

        // 1. 기존 작업 조회
        LogisticTask existingTask = logisticTaskRepository.findById(taskId)
                .orElseThrow(() -> LogisticTaskException.notFound(taskId));

        // 2. 새 작업자 조회
        UserInfo newWorker = userInfoRepository.findById(request.getWorkerId())
                .orElseThrow(() -> UserInfoException.notFound(request.getWorkerId()));

        // 3. 수정된 작업 생성 (검증용)
        LogisticTask modifiedTask = LogisticTask.builder()
                .name(request.getName())
                .type(existingTask.getType())
                .worker(newWorker)
                .ware(existingTask.getWare())
                .fromLocation(existingTask.getFromLocation())
                .toLocation(existingTask.getToLocation())
                .quantity(request.getQuantity())
                .scheduledDate(existingTask.getScheduledDate())
                .etd(request.getEtd())
                .eta(request.getEta())
                .templateIdSnapshot(existingTask.getTemplateIdSnapshot())
                .build();

        // 4. 정합성 검증 (시뮬레이션)
        validationService.validateTaskModification(existingTask, modifiedTask);

        // 5. 실제 수정
        existingTask.modifyTask(request.getName(), newWorker, request.getQuantity(),
                               request.getEtd(), request.getEta());

        log.info("물류 작업 수정 완료 - id: {}, name: {}", existingTask.getId(), existingTask.getName());
        return existingTask;
    }

    /**
     * 물류 작업 부분 수정 (작업자와 시간만)
     */
    @Transactional
    public LogisticTask partialUpdate(Long taskId, LogisticTaskDTO.PartialUpdateReq request) {
        log.debug("물류 작업 부분 수정 - taskId: {}", taskId);

        // 1. 기존 작업 조회
        LogisticTask existingTask = logisticTaskRepository.findById(taskId)
                .orElseThrow(() -> LogisticTaskException.notFound(taskId));

        // 2. 새 작업자 조회
        UserInfo newWorker = userInfoRepository.findById(request.getWorkerId())
                .orElseThrow(() -> UserInfoException.notFound(request.getWorkerId()));

        // 3. 부분 수정 (작업자와 시간만)
        existingTask.modifyTask(newWorker, request.getEtd(), request.getEta());

        log.info("물류 작업 부분 수정 완료 - id: {}", existingTask.getId());
        return existingTask;
    }

    /**
     * 물류 작업 삭제
     */
    @Transactional
    public void delete(Long taskId) {
        log.debug("물류 작업 삭제 - taskId: {}", taskId);

        LogisticTask task = logisticTaskRepository.findById(taskId)
                .orElseThrow(() -> LogisticTaskException.notFound(taskId));

        // 정합성 검증 (시뮬레이션)
        validationService.validateTaskDeletion(task);

        logisticTaskRepository.delete(task);
        log.info("물류 작업 삭제 완료 - id: {}, name: {}", task.getId(), task.getName());
    }

    /**
     * 물류 작업 시작
     */
    @Transactional
    public LogisticTaskDTO.ActionRes initiateTask(Long taskId) {
        log.debug("물류 작업 시작 - taskId: {}", taskId);

        LogisticTask task = logisticTaskRepository.findById(taskId)
                .orElseThrow(() -> LogisticTaskException.notFound(taskId));

        // 작업자와 요청한 사용자가 일치하는지 검증
        isMatchedWorker(taskId, task);

        LogisticTaskStatus previousStatus = task.getStatus();
        task.initiateTask(LocalTime.now());

        // 재고 변경 이벤트 발행
        LogisticTaskInitiatedEvent event = LogisticTaskInitiatedEvent.builder()
                .taskId(task.getId())
                .wareId(task.getWare().getId())
                .fromLocationId(task.getFromLocation().getId())  // 출발지만
                .quantity(task.getQuantity())
                .build();

        eventPublisher.publishEvent(event);

        log.info("물류 작업 시작 완료 - id: {}, status: {} → {}", 
                task.getId(), previousStatus, task.getStatus());

        return LogisticTaskDTO.ActionRes.from(task, previousStatus);
    }


    /**
     * 물류 작업 완료
     */
    @Transactional
    public LogisticTaskDTO.ActionRes completeTask(Long taskId) {
        log.debug("물류 작업 완료 - taskId: {}", taskId);

        LogisticTask task = logisticTaskRepository.findById(taskId)
                .orElseThrow(() -> LogisticTaskException.notFound(taskId));

        // 작업자와 요청한 사용자가 일치하는지 검증
        isMatchedWorker(taskId, task);

        LogisticTaskStatus previousStatus = task.getStatus();
        task.completeTask(LocalTime.now());

        // 재고 변경 이벤트 발행
        LogisticTaskCompletedEvent event = LogisticTaskCompletedEvent.builder()
                .taskId(task.getId())
                .wareId(task.getWare().getId())
                .toLocationId(task.getToLocation().getId())      // 도착지만
                .quantity(task.getQuantity())
                .build();

        eventPublisher.publishEvent(event);

        log.info("물류 작업 완료 - id: {}, status: {} → {}", 
                task.getId(), previousStatus, task.getStatus());

        return LogisticTaskDTO.ActionRes.from(task, previousStatus);
    }

    /**
     * 물류 작업 취소
     */
    @Transactional
    public LogisticTaskDTO.ActionRes cancelTask(Long taskId) {
        log.debug("물류 작업 취소 - taskId: {}", taskId);

        LogisticTask task = logisticTaskRepository.findById(taskId)
                .orElseThrow(() -> LogisticTaskException.notFound(taskId));

        // 작업자와 요청한 사용자가 일치하는지 검증
        isMatchedWorker(taskId, task);

        LogisticTaskStatus previousStatus = task.getStatus();
        task.cancelTask();

        log.info("물류 작업 취소 완료 - id: {}, status: {} → {}", 
                task.getId(), previousStatus, task.getStatus());

        return LogisticTaskDTO.ActionRes.from(task, previousStatus);
    }

    /**
     * 물류 작업 실패 처리
     */
    @Transactional
    public LogisticTaskDTO.ActionRes failTask(Long taskId) {
        log.debug("물류 작업 실패 처리 - taskId: {}", taskId);

        LogisticTask task = logisticTaskRepository.findById(taskId)
                .orElseThrow(() -> LogisticTaskException.notFound(taskId));

        // 작업자와 요청한 사용자가 일치하는지 검증
        isMatchedWorker(taskId, task);

        LogisticTaskStatus previousStatus = task.getStatus();
        task.failTask();

        log.info("물류 작업 실패 처리 완료 - id: {}, status: {} → {}", 
                task.getId(), previousStatus, task.getStatus());

        return LogisticTaskDTO.ActionRes.from(task, previousStatus);
    }

    /**
     * 물류 작업 지연 처리
     */
    @Transactional
    public LogisticTaskDTO.ActionRes delayTask(Long taskId, boolean isInitiationDelay) {
        log.debug("물류 작업 지연 처리 - taskId: {}, isInitiationDelay: {}", taskId, isInitiationDelay);

        LogisticTask task = logisticTaskRepository.findById(taskId)
                .orElseThrow(() -> LogisticTaskException.notFound(taskId));

        // 작업자와 요청한 사용자가 일치하는지 검증
        isMatchedWorker(taskId, task);

        LogisticTaskStatus previousStatus = task.getStatus();
        
        if (isInitiationDelay) {
            task.delayInitiation();
        } else {
            task.delayCompletion();
        }

        log.info("물류 작업 지연 처리 완료 - id: {}, status: {} → {}", 
                task.getId(), previousStatus, task.getStatus());

        return LogisticTaskDTO.ActionRes.from(task, previousStatus);
    }

    /**
     * 물류 작업 상태변경 시 배정된 작업자와 요청한 사용자가 일치하는지 검증 TODO: 테스트 다되면 주석 제거
     */
    private static void isMatchedWorker(Long taskId, LogisticTask task) {
/*        // request한 사용자 정보 가져오기
        UserInfo authentication =  (UserInfo) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();

        if (!task.getWorker().getId().equals(authentication.getId())) {
            if (!authentication.getType().equals(UserType.ADMIN)) {
                log.warn("물류 작업 시작 실패 - 작업자 불일치: taskId={}, workerId={}, authId={}",
                        taskId, task.getWorker().getId(), authentication.getId());
                throw LogisticTaskException.workerMismatchEx(taskId, authentication.getId());
            }
        }*/
    }

    // ===== 조회 메서드들 =====

    /**
     * 물류 작업 단건 조회
     */
    public LogisticTask findById(Long taskId) {
        return logisticTaskRepository.findWithAllById(taskId)
                .orElseThrow(() -> LogisticTaskException.notFound(taskId));
    }

    /**
     * 모든 물류 작업 조회
     */
    public List<LogisticTask> findAll() {
        return logisticTaskRepository.findAllBy();
    }

    /**
     * 상태별 물류 작업 조회
     */
    public List<LogisticTask> findByStatus(LogisticTaskStatus status) {
        return logisticTaskRepository.findByStatus(status);
    }

    /**
     * 작업자별 물류 작업 조회
     */
    public List<LogisticTask> findByWorkerId(Long workerId) {
        UserInfo worker = userInfoRepository.findById(workerId)
                .orElseThrow(() -> UserInfoException.notFound(workerId));

        return logisticTaskRepository.findByWorker(worker);
    }

    /**
     * 날짜별 물류 작업 조회
     */
    public List<LogisticTask> findByScheduledDate(LocalDate scheduledDate) {
        return logisticTaskRepository.findByScheduledDate(scheduledDate);
    }

    /**
     * 날짜 범위별 물류 작업 조회
     */
    public List<LogisticTask> findByDateRange(LocalDate startDate, LocalDate endDate) {
        return logisticTaskRepository.findByScheduledDateBetween(startDate, endDate);
    }

    /**
     * 특정 장소가 포함된 물류 작업 조회 (출발지 또는 도착지)
     */
    public List<LogisticTask> findByLocation(Long locationId) {
        return logisticTaskRepository.findAllByFromLocationIdOrToLocationId(locationId, locationId);
    }

    /**
     * 특정 물품의 물류 작업 조회
     */
    public List<LogisticTask> findByWareId(Long wareId) {
        Ware ware = wareRepository.findById(wareId)
                .orElseThrow(() -> WareException.notFound(wareId));

        return logisticTaskRepository.findByWare(ware);
    }

    /**
     * 대시보드용 날짜별 작업자별 물류 작업 조회
     */
    public LogisticTaskDTO.DashboardRes getDailyDashboard(LocalDate date) {
        log.debug("일별 대시보드 조회: date={}", date);

        List<LogisticTask> dayTasks = logisticTaskRepository.findByScheduledDate(date);
        
        // 작업자별로 그룹핑
        Map<Long, List<LogisticTask>> tasksByWorker = dayTasks.stream()
                .collect(Collectors.groupingBy(task -> task.getWorker().getId()));

        List<LogisticTaskDTO.DashboardRes.WorkerSchedule> workerSchedules = tasksByWorker.entrySet().stream()
                .map(entry -> {
                    Long workerId = entry.getKey();
                    List<LogisticTask> workerTasks = entry.getValue();
                    
                    // 작업자명 조회 (첫 번째 작업에서)
                    String workerName = workerTasks.isEmpty() ? 
                            "Unknown" : workerTasks.get(0).getWorker().getName();

                    // 시간대별 슬롯 생성
                    List<LogisticTaskDTO.DashboardRes.TimeSlot> timeSlots = createTimeSlots(workerTasks);

                    return LogisticTaskDTO.DashboardRes.WorkerSchedule.builder()
                            .workerId(workerId)
                            .workerName(workerName)
                            .timeSlots(timeSlots)
                            .build();
                })
                .sorted(Comparator.comparing(LogisticTaskDTO.DashboardRes.WorkerSchedule::getWorkerName))
                .collect(Collectors.toList());

        return LogisticTaskDTO.DashboardRes.builder()
                .date(date)
                .workerSchedules(workerSchedules)
                .build();
    }

    /**
     * 타임라인 라이브러리에서 요구되는 작업자 별 시간대별 작업 조회
     */
    public LogisticTaskDTO.Dashboard2Res getDailyDashboard2(LocalDate date) {
        log.debug("작업자별 시간대별 작업 조회: date={}", date);

        List<LogisticTask> dayTasks = logisticTaskRepository.findByScheduledDate(date);
        List<LogisticTaskDTO.Dashboard2Res.TaskList> tastList = dayTasks.stream()
                .map(LogisticTaskDTO.Dashboard2Res.TaskList::from)
                .toList();

        Map<Long, String> userInfoMap = userInfoCacheManager.getIdNamePair();
        List<LogisticTaskDTO.Dashboard2Res.WorkerList> workerList = userInfoMap.entrySet().stream()
                .map(entry -> LogisticTaskDTO.Dashboard2Res.WorkerList.builder()
                        .id(entry.getKey())
                        .name(entry.getValue())
                        .build())
                .toList();

        LogisticTaskDTO.Dashboard2Res responseBuilder = LogisticTaskDTO.Dashboard2Res.builder()
                .resources(workerList)
                .data(tastList)
                .build();

        return responseBuilder;
    }


    /**
     * 작업자별 특정 날짜 작업 조회
     */
    public List<LogisticTask> findByWorkerAndDate(Long workerId, LocalDate date) {
        return logisticTaskRepository.findByScheduledDateAndWorker(date, workerId);
    }

    /**
     * 시간대별 슬롯 생성 (30분 단위)
     */
    private List<LogisticTaskDTO.DashboardRes.TimeSlot> createTimeSlots(List<LogisticTask> workerTasks) {
        List<LogisticTaskDTO.DashboardRes.TimeSlot> timeSlots = new ArrayList<>();
        
        // 08:00부터 18:00까지 30분 단위로 슬롯 생성
        LocalTime startTime = LocalTime.of(8, 0);
        LocalTime endTime = LocalTime.of(18, 0);
        
        LocalTime currentTime = startTime;
        while (!currentTime.isAfter(endTime)) {
            final LocalTime slotTime = currentTime;
            
            // 해당 시간에 해당하는 작업 찾기
            LogisticTask taskAtTime = workerTasks.stream()
                    .filter(task -> isTaskAtTime(task, slotTime))
                    .findFirst()
                    .orElse(null);

            String taskType = "EMPTY";
            LogisticTaskDTO.Res taskRes = null;
            
            if (taskAtTime != null) {
                taskRes = convertTaskToResponse(taskAtTime);
                
                // ETD 시간이면 START, ETA 시간이면 COMPLETE
                if (taskAtTime.getEtd().equals(slotTime)) {
                    taskType = "START";
                } else if (taskAtTime.getEta().equals(slotTime)) {
                    taskType = "COMPLETE";
                }
            }

            timeSlots.add(LogisticTaskDTO.DashboardRes.TimeSlot.builder()
                    .time(currentTime)
                    .task(taskRes)
                    .type(taskType)
                    .build());

            currentTime = currentTime.plusMinutes(10);
        }
        
        return timeSlots;
    }

    private final DomainCacheManager<Long, String> userInfoCacheManager;

    /**
     * 특정 시간에 작업이 해당하는지 확인
     */
    private boolean isTaskAtTime(LogisticTask task, LocalTime time) {
        return task.getEtd().equals(time) || task.getEta().equals(time);
    }

    /**
     * LogisticTask를 응답 DTO로 변환 (캐시 없이)
     */
    private LogisticTaskDTO.Res convertTaskToResponse(LogisticTask task) {
        return LogisticTaskDTO.Res.from(
                task,
                task.getWorker().getName(),
                task.getWare().getName(),
                task.getFromLocation().getName(),
                task.getToLocation().getName()
        );
    }

    /**
     * 전체 물류 작업 상태별 통계 조회
     */
    public Map<LogisticTaskStatus, Long> getTaskStatistics() {
        List<LogisticTask> allTasks = logisticTaskRepository.findAll();
        return allTasks.stream()
                .collect(Collectors.groupingBy(
                        LogisticTask::getStatus,
                        Collectors.counting()
                ));
    }

    /**
     * 특정 날짜의 물류 작업 상태별 통계 조회
     */
    public Map<LogisticTaskStatus, Long> getDailyTaskStatistics(LocalDate date) {
        List<LogisticTask> dayTasks = logisticTaskRepository.findByScheduledDate(date);
        return dayTasks.stream()
                .collect(Collectors.groupingBy(
                        LogisticTask::getStatus,
                        Collectors.counting()
                ));
    }

    /**
     * 조건에 따른 물류 작업 검색 (ID 기반 검색)
     */
    public List<LogisticTask> searchTasks(LogisticTaskDTO.SearchCriteria criteria) {
        log.debug("물류 작업 검색: {}", criteria);
        
        // ID 기반으로 직접 검색
        return logisticTaskRepository.findByIdBasedSearchCriteria(
                criteria.getName(),
                criteria.getWorkerId(),
                criteria.getWareId(),
                criteria.getFromLocationId(),
                criteria.getToLocationId(),
                criteria.getStatus(),
                criteria.getStartDate(),
                criteria.getEndDate()
        );
    }}
