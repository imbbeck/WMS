package com.wms.logisticTask.application;

import com.wms.location.domain.model.Location;
import com.wms.location.domain.model.LocationConnection;
import com.wms.location.domain.model.LocationType;
import com.wms.location.domain.repository.LocationConnectionRepository;
import com.wms.location.domain.repository.LocationRepository;
import com.wms.logisticTask.domain.exception.LogisticTaskException;
import com.wms.logisticTask.domain.model.EventType;
import com.wms.logisticTask.domain.model.LogisticTask;
import com.wms.logisticTask.domain.model.LogisticTaskStatus;
import com.wms.logisticTask.domain.model.SimulationEvent;
import com.wms.logisticTask.domain.model.SimulationStats;
import com.wms.logisticTask.domain.repository.LogisticTaskRepository;
import com.wms.stock.application.StockCacheService;
import com.wms.location.application.LocationCacheService;
import com.wms.stock.domain.model.StockKey;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 캐싱 기반 물류작업 정합성 검증 서비스
 * 재고 및 창고 용량 정보를 캐시에서 조회하여 효율적인 검증 수행
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class LogisticTaskValidationService {

	private final LogisticTaskRepository logisticTaskRepository;
	private final StockCacheService stockCacheService;
	private final LocationCacheService locationCacheService;
	private final LocationConnectionRepository locationConnectionRepository;
	private final LocationRepository locationRepository;

	/**
	 * 새로운 물류작업 생성 시 정합성 검증
	 */
	public void validateTaskCreation(LogisticTask newTask) {
		log.debug("새 물류작업 생성 검증 시작: {}", newTask.getName());

		try {
			// 1. 기본 필드 검증
			validateBasicTaskFields(newTask);

			// 2. 시뮬레이션 기반 정합성 검증
			runValidationSimulation(newTask, OperationType.CREATE);

			log.debug("새 물류작업 생성 검증 완료: {}", newTask.getName());

		} catch (Exception e) {
			log.error("물류작업 생성 검증 실패: {}", newTask.getName(), e);
			throw e;
		}
	}

	/**
	 * 기존 물류작업 수정 시 정합성 검증
	 */
	public void validateTaskModification(LogisticTask originalTask, LogisticTask modifiedTask) {
		log.debug("물류작업 수정 검증 시작: {}", originalTask.getName());

		try {
			// 1. 수정 가능 상태 검증
			if (!originalTask.isModifiable()) {
				throw LogisticTaskException.taskNotModifiableEx(originalTask.getStatus().toString());
			}

			// 2. 기본 필드 검증
			validateBasicTaskFields(modifiedTask);

			// 3. 시뮬레이션 기반 정합성 검증
			ValidationContext context = ValidationContext.builder()
					.originalTask(originalTask)
					.targetTask(modifiedTask)
					.operationType(OperationType.MODIFY)
					.build();

			runValidationSimulation(context);

			log.debug("물류작업 수정 검증 완료: {}", originalTask.getName());

		} catch (Exception e) {
			log.error("물류작업 수정 검증 실패: {}", originalTask.getName(), e);
			throw e;
		}
	}

	/**
	 * 물류작업 삭제/취소 시 정합성 검증
	 */
	public void validateTaskDeletion(LogisticTask taskToDelete) {
		log.debug("물류작업 삭제 검증 시작: {}", taskToDelete.getName());

		try {
			// 1. 삭제 가능 상태 검증
			if (!taskToDelete.isCancellable()) {
				throw LogisticTaskException.taskCancellationNotAllowedEx(taskToDelete.getStatus().toString());
			}

			// 2. 후속 작업들에 미치는 영향 검증
			ValidationContext context = ValidationContext.builder()
					.originalTask(taskToDelete)
					.operationType(OperationType.DELETE)
					.build();

			runValidationSimulation(context);

			log.debug("물류작업 삭제 검증 완료: {}", taskToDelete.getName());

		} catch (Exception e) {
			log.error("물류작업 삭제 검증 실패: {}", taskToDelete.getName(), e);
			throw e;
		}
	}

	/**
	 * 작업 상태 변경 시 실시간 검증 (INITIATED, COMPLETED 등)
	 */
	public void validateTaskStatusChange(LogisticTask task, LogisticTaskStatus newStatus) {
		log.debug("작업 상태 변경 검증: {} -> {}", task.getStatus(), newStatus);

		try {
			switch (newStatus) {
				case INITIATED:
					validateTaskInitiation(task);
					break;
				case COMPLETED:
					validateTaskCompletion(task);
					break;
				case FAILED:
				case CANCELLED:
					// 실패/취소는 별도 검증 없음 (재고 변경 없음)
					break;
				default:
					log.debug("상태 변경 검증 불필요: {}", newStatus);
					break;
			}
		} catch (Exception e) {
			log.error("작업 상태 변경 검증 실패: taskId={}, newStatus={}", task.getId(), newStatus, e);
			throw e;
		}
	}

	/**
	 * 기본 필드 유효성 검증
	 */
	private void validateBasicTaskFields(LogisticTask task) {
		if (task.getQuantity() == null || task.getQuantity() <= 0) {
			throw LogisticTaskException.validation("수량은 자연수여야 합니다.");
		}

		if (task.getEtd() == null || task.getEta() == null) {
			throw LogisticTaskException.validation("ETD 및 ETA는 필수입니다.");
		}

		if (task.getEtd().isAfter(task.getEta())) {
			throw LogisticTaskException.validation("출발 예정시간은 도착 예정시간보다 빨라야 합니다.");
		}

		// 출발지와 도착지가 같으면 안됨
		if (task.getFromLocation().getId().equals(task.getToLocation().getId())) {
			throw LogisticTaskException.validation("출발지와 도착지가 같을 수 없습니다.");
		}

		// **수정된 로직**: scheduledDate를 포함한 LocalDateTime으로 현재 시각 이전인지 검증
		LocalDateTime etdDateTime = task.getEtd().atDate(task.getScheduledDate());
		LocalDateTime etaDateTime = task.getEta().atDate(task.getScheduledDate());

		if (etdDateTime.isBefore(LocalDateTime.now()) || etaDateTime.isBefore(LocalDateTime.now())) {
			throw LogisticTaskException.validation("출발 예정시간 및 도착 예정시간은 현재 시각 이후여야 합니다.");
		}

		// 작업자 스케줄 충돌 검증 추가
		validateWorkerScheduleConflict(task);
	}

	/**
	 * 작업자 스케줄 충돌 검증
	 */
	private void validateWorkerScheduleConflict(LogisticTask newTask) {
		log.debug("작업자 스케줄 충돌 검증 시작 - workerId: {}, date: {}", 
				newTask.getWorker().getId(), newTask.getScheduledDate());

		// 같은 날짜의 해당 작업자의 기존 작업들 조회
		List<LogisticTask> workerTasks = logisticTaskRepository
				.findByScheduledDateAndWorker(newTask.getScheduledDate(), newTask.getWorker().getId());

		// 현재 검증 중인 작업이 수정인 경우, 원래 작업은 제외 (ID가 있는 경우만)
		if (newTask.getId() != null) {
			workerTasks.removeIf(task -> task.getId() != null && task.getId().equals(newTask.getId()));
		}

		// 시간 충돌 검사
		for (LogisticTask existingTask : workerTasks) {
			if (isTimeOverlapping(newTask, existingTask)) {
				throw LogisticTaskException.validation(
					String.format("작업자 '%s'의 스케줄 충돌: 새 작업(%s-%s)이 기존 작업 '%s'(%s-%s)와 시간이 겹칩니다.",
						newTask.getWorker().getName(),
						newTask.getEtd(), newTask.getEta(),
						existingTask.getName(),
						existingTask.getEtd(), existingTask.getEta()));
			}
		}

		log.debug("작업자 스케줄 충돌 검증 완료 - 충돌 없음");
	}

	/**
	 * 두 작업의 시간이 겹치는지 확인
	 */
	private boolean isTimeOverlapping(LogisticTask task1, LogisticTask task2) {
		LocalTime start1 = task1.getEtd();
		LocalTime start2 = task2.getEtd();

		LogisticTask formerTask = start1.isBefore(start2) ? task1 : task2;
		LogisticTask latterTask = start1.isBefore(start2) ? task2 : task1;

		Long formerToId = formerTask.getToLocation().getId();
		Long latterFromId = latterTask.getFromLocation().getId();

		// 전 작업의 도착지 ~ 후 작업의 출발지 간 이동소요시간 TODO: 이동소요시간 캐시화. 프론트에서도 자주 사용될듯함.
		int trt = locationConnectionRepository
				.findByLocationAIdAndLocationBId(Math.min(formerToId, latterFromId), Math.max(formerToId, latterFromId))
				.map(LocationConnection::getTrt)
				.orElse(0);

		LocalTime formerStart = formerTask.getEtd();
		LocalTime formerEnd = formerTask.getEta();
		LocalTime latterStart = latterTask.getEtd().minusMinutes(trt);
		LocalTime latterEnd = latterTask.getEta();

		// 시간 겹침 여부
		return formerStart.isBefore(latterEnd) && latterStart.isBefore(formerEnd);
	}

	/**
	 * 메인 시뮬레이션 실행 (단순한 CREATE 작업용)
	 */
	private void runValidationSimulation(LogisticTask newTask, OperationType operationType) {
		ValidationContext context = ValidationContext.builder()
				.targetTask(newTask)
				.operationType(operationType)
				.build();

		runValidationSimulation(context);
	}

	/**
	 * 메인 시뮬레이션 실행 (복잡한 컨텍스트용)
	 */
	private void runValidationSimulation(ValidationContext context) {
		try {
			// 1. 해당 날짜의 모든 작업 조회
			List<LogisticTask> allTasks = loadTasksForSimulation(context);

			// 2. 캐시 기반 현재 재고/용량 상태 스냅샷 조회
			Map<StockKey, Integer> currentStockMap = createStockSnapshotForTasks(allTasks);
			Map<Long, Integer> warehouseCapacityMap = createWarehouseCapacitySnapshot(allTasks);

			// 3. 시간순 이벤트 기반 시뮬레이션 실행
			executeTimeOrderedSimulation(allTasks, currentStockMap, warehouseCapacityMap);

		} catch (Exception e) {
			log.error("시뮬레이션 실행 중 오류: {}", e.getMessage(), e);
			throw LogisticTaskException.simulationFailedEx("작업 검증 중 오류가 발생했습니다: " + e.getMessage());
		}
	}

	/**
	 * 시뮬레이션에 필요한 재고만 조회 (특정 작업들 기준)
	 */
	private Map<StockKey, Integer> createStockSnapshotForTasks(List<LogisticTask> tasks) {
		Map<StockKey, Integer> stockMap = new HashMap<>();

		Set<StockKey> requiredStocks = tasks.stream()
				.flatMap(task -> Stream.of(
						StockKey.of(task.getWare().getId(), task.getFromLocation().getId()),
						StockKey.of(task.getWare().getId(), task.getToLocation().getId())
				))
				.filter(key -> isWarehouse(key.getWarehouseId()))
				.collect(Collectors.toSet());

		for (StockKey key : requiredStocks) {
			Integer quantity = stockCacheService.getInventoryQuantity(key);
			stockMap.put(key, quantity != null ? quantity : 0);
		}

		log.debug("작업 기반 재고 스냅샷 생성 완료: {} 개 재고", stockMap.size());
		return stockMap;
	}

	/**
	 * 캐시 기반 창고 용량 정보 스냅샷 생성
	 */
	private Map<Long, Integer> createWarehouseCapacitySnapshot(List<LogisticTask> tasks) {
		Set<Long> warehouseIds = tasks.stream()
				.flatMap(task -> Stream.of(task.getFromLocation().getId(), task.getToLocation().getId()))
				.filter(this::isWarehouse)
				.collect(Collectors.toSet());

		Map<Long, Integer> capacityMap = new HashMap<>();
		for (Long warehouseId : warehouseIds) {
			Integer capacity = locationCacheService.getWarehouseCapacity(warehouseId);
			if (capacity != null) {
				capacityMap.put(warehouseId, capacity);
			}
		}

		log.debug("창고 용량 스냅샷 생성 완료: {} 개 창고", capacityMap.size());
		return capacityMap;
	}

	/**
	 * 시뮬레이션용 작업 목록 로드
	 */
	private List<LogisticTask> loadTasksForSimulation(ValidationContext context) {
		LocalDate targetDate = context.getTargetTask().getScheduledDate();
		List<LogisticTask> existingTasks = logisticTaskRepository.findActiveTasksByScheduledDate(targetDate);

		switch (context.getOperationType()) {
			case CREATE:
				existingTasks.add(context.getTargetTask());
				break;
			case MODIFY:
				// ID가 있는 경우에만 제거 (저장된 작업만)
				if (context.getOriginalTask().getId() != null) {
					existingTasks.removeIf(task -> task.getId() != null && 
							task.getId().equals(context.getOriginalTask().getId()));
				}
				existingTasks.add(context.getTargetTask());
				break;
			case DELETE:
				// ID가 있는 경우에만 제거 (저장된 작업만)
				if (context.getOriginalTask().getId() != null) {
					existingTasks.removeIf(task -> task.getId() != null && 
							task.getId().equals(context.getOriginalTask().getId()));
				}
				break;
		}
		return existingTasks;
	}

	/**
	 * 시간순 이벤트 기반 시뮬레이션 실행 - 우선순위 큐 사용
	 */
	private void executeTimeOrderedSimulation(List<LogisticTask> tasks,
			Map<StockKey, Integer> stockSnapshot,
			Map<Long, Integer> capacitySnapshot) {

		long simulationStartTime = System.currentTimeMillis();
		Map<StockKey, Integer> simulatedStock = new HashMap<>(stockSnapshot);
		Map<Long, Integer> warehouseUsage = new HashMap<>();
		initializeWarehouseUsage(warehouseUsage, tasks);

		// 이벤트 타입별 카운트
		Map<EventType, Integer> eventTypeCounts = new EnumMap<>(EventType.class);

		// 우선순위 큐로 이벤트 관리 (Comparable 구현으로 자동 정렬)
		PriorityQueue<SimulationEvent> eventQueue = new PriorityQueue<>();

		// 시간 범위 추적
		LocalTime firstEventTime = null;
		LocalTime lastEventTime = null;

		// 모든 작업을 이벤트로 변환하여 큐에 추가
		for (LogisticTask task : tasks) {
			SimulationEvent startEvent = SimulationEvent.createStartEvent(task);
			SimulationEvent completeEvent = SimulationEvent.createCompleteEvent(task);

			eventQueue.offer(startEvent);
			eventQueue.offer(completeEvent);

			// 시간 범위 계산
			if (firstEventTime == null || task.getEtd().isBefore(firstEventTime)) {
				firstEventTime = task.getEtd();
			}
			if (lastEventTime == null || task.getEta().isAfter(lastEventTime)) {
				lastEventTime = task.getEta();
			}
		}

		int totalEvents = eventQueue.size();
		int processedEvents = 0;

		// 이벤트 처리 루프
		while (!eventQueue.isEmpty()) {
			SimulationEvent event = eventQueue.poll();
			processedEvents++;

			// 통계 수집
			eventTypeCounts.merge(event.getType(), 1, Integer::sum);

			log.debug("이벤트 처리 [{}/{}]: {}", processedEvents, totalEvents, event);

			try {
				// 이벤트 타입별 처리
				if (event.isStartEvent()) {
					simulateTaskStart(event.getTask(), simulatedStock, warehouseUsage);
				} else if (event.isCompleteEvent()) {
					simulateTaskCompletion(event.getTask(), simulatedStock, warehouseUsage, capacitySnapshot);
				}
				// 향후 DELAY, FAILURE 이벤트 처리 추가 가능

			} catch (Exception e) {
				log.error("이벤트 처리 중 오류: {}", event, e);

				// 실패 통계 생성
				long processingTime = System.currentTimeMillis() - simulationStartTime;
				SimulationStats failureStats = SimulationStats.failure(e.getMessage(), processingTime);
				log.error("시뮬레이션 실패: {}", failureStats);

				throw e;
			}
		}

		// 성공 통계 생성 및 로깅
		long processingTime = System.currentTimeMillis() - simulationStartTime;
		SimulationStats stats = SimulationStats.success(
				totalEvents, tasks.size(), firstEventTime, lastEventTime,
				processingTime, eventTypeCounts
		);

		log.debug("시뮬레이션 완료: {}", stats);

		// 성능 경고
		if (!stats.isPerformanceGood()) {
			log.warn("시뮬레이션 성능 경고: 등급={}, 시간={}ms",
					stats.getPerformanceGrade().getDescription(), processingTime);
		}
	}

	/**
	 * 작업 시작 시뮬레이션 (출발 창고 재고 차감)
	 */
	private void simulateTaskStart(LogisticTask task, Map<StockKey, Integer> stockMap, Map<Long, Integer> usageMap) {
		if (isWarehouse(task.getFromLocation().getId())) {
			StockKey fromKey = StockKey.of(task.getWare().getId(), task.getFromLocation().getId());
			int currentStock = stockMap.getOrDefault(fromKey, 0);

			if (currentStock < task.getQuantity()) {
				throw LogisticTaskException.stockValidationFailedEx(
						String.format("작업 '%s' 실행 불가: 출발 창고 재고 부족 (필요: %d, 보유: %d)",
								task.getName(), task.getQuantity(), currentStock));
			}

			stockMap.put(fromKey, currentStock - task.getQuantity());

			// 창고 사용량 업데이트 (출고 시 용량 감소)
			Long warehouseId = task.getFromLocation().getId();
			usageMap.put(warehouseId, usageMap.getOrDefault(warehouseId, 0) - task.getQuantity());
		}
	}

	/**
	 * 작업 완료 시뮬레이션 (도착 창고 재고 증가 및 용량 검증)
	 */
	private void simulateTaskCompletion(LogisticTask task, Map<StockKey, Integer> stockMap,
			Map<Long, Integer> usageMap, Map<Long, Integer> capacityMap) {
		if (isWarehouse(task.getToLocation().getId())) {
			StockKey toKey = StockKey.of(task.getWare().getId(), task.getToLocation().getId());
			int currentStock = stockMap.getOrDefault(toKey, 0);
			stockMap.put(toKey, currentStock + task.getQuantity());

			// 창고 수용량 검증
			Long warehouseId = task.getToLocation().getId();
			int newUsage = usageMap.getOrDefault(warehouseId, 0) + task.getQuantity();
			Integer capacity = capacityMap.get(warehouseId);

			if (capacity != null && newUsage > capacity) {
				throw LogisticTaskException.capacityValidationFailedEx(
						String.format("작업 '%s' 실행 불가: 창고 '%s' 수용량 초과 (용량: %d, 예상 사용량: %d)",
								task.getName(), task.getToLocation().getName(), capacity, newUsage));
			}

			usageMap.put(warehouseId, newUsage);
		}
	}

	/**
	 * 초기 창고 사용량 계산 (캐시에서 조회)
	 */
	private void initializeWarehouseUsage(Map<Long, Integer> usageMap, List<LogisticTask> tasks) {
		Set<Long> warehouseIds = tasks.stream()
				.flatMap(task -> Stream.of(task.getFromLocation().getId(), task.getToLocation().getId()))
				.filter(this::isWarehouse)
				.collect(Collectors.toSet());

		for (Long warehouseId : warehouseIds) {
			Integer currentUsage = stockCacheService.getWarehouseCurrentSum(warehouseId);
			usageMap.put(warehouseId, currentUsage != null ? currentUsage : 0);
		}

		log.debug("초기 창고 사용량 설정 완료: {} 개 창고", usageMap.size());
	}

	/**
	 * 작업 시작 시 즉시 검증 (실제 작업 시작 시점 - 캐시 기반)
	 */
	private void validateTaskInitiation(LogisticTask task) {
		if (isWarehouse(task.getFromLocation().getId())) {
			Integer currentStock = stockCacheService.getInventoryQuantity(
					StockKey.of(task.getWare().getId(), task.getFromLocation().getId()));

			if (currentStock == null || currentStock < task.getQuantity()) {
				throw LogisticTaskException.stockValidationFailedEx(
						String.format("출발 창고(%s)의 물품(%s) 재고 부족. 필요: %d, 보유: %d",
								task.getFromLocation().getName(), task.getWare().getName(),
								task.getQuantity(), (currentStock != null ? currentStock : 0)));
			}
		}
	}

	/**
	 * 작업 완료 시 즉시 검증 (실제 작업 완료 시점 - 캐시 기반)
	 */
	private void validateTaskCompletion(LogisticTask task) {
		if (isWarehouse(task.getToLocation().getId())) {
			Long warehouseId = task.getToLocation().getId();
			Integer currentUsage = stockCacheService.getWarehouseCurrentSum(warehouseId);
			Integer capacity = locationCacheService.getWarehouseCapacity(warehouseId);

			currentUsage = (currentUsage != null) ? currentUsage : 0;

			if (capacity != null && currentUsage + task.getQuantity() > capacity) {
				throw LogisticTaskException.capacityValidationFailedEx(
						String.format("창고 '%s' 수용량 초과 (용량: %d, 현재 사용량: %d, 추가 수량: %d)",
								task.getToLocation().getName(), capacity, currentUsage, task.getQuantity()));
			}
		}
	}

	/**
	 * 장소가 창고인지 확인
	 */
	private boolean isWarehouse(Long locationId) {
		return locationRepository.findById(locationId)
				.map(Location::getType)
				.map(type -> type == LocationType.WAREHOUSE)
				.orElse(false);
	}

	// ===== 내부 클래스 및 Enum =====

	private enum OperationType {
		CREATE, MODIFY, DELETE
	}

	@Getter
	private static class ValidationContext {
		private final LogisticTask originalTask;
		private final LogisticTask targetTask;
		private final OperationType operationType;

		private ValidationContext(Builder builder) {
			this.originalTask = builder.originalTask;
			this.targetTask = builder.targetTask;
			this.operationType = builder.operationType;
		}

		public static Builder builder() {
			return new Builder();
		}

		public static class Builder {
			private LogisticTask originalTask;
			private LogisticTask targetTask;
			private OperationType operationType;

			public Builder originalTask(LogisticTask originalTask) {
				this.originalTask = originalTask;
				return this;
			}

			public Builder targetTask(LogisticTask targetTask) {
				this.targetTask = targetTask;
				return this;
			}

			public Builder operationType(OperationType operationType) {
				this.operationType = operationType;
				return this;
			}

			public ValidationContext build() {
				return new ValidationContext(this);
			}
		}
	}
}