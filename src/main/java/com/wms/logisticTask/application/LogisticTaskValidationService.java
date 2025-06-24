package com.wms.logisticTask.application;

import com.wms.location.domain.model.Location;
import com.wms.logisticTask.domain.exception.LogisticTaskException;
import com.wms.logisticTask.domain.model.LogisticTask;
import com.wms.logisticTask.domain.model.LogisticTaskStatus;
import com.wms.logisticTask.domain.repository.LogisticTaskRepository;
import com.wms.stock.domain.model.Stock;
import com.wms.stock.domain.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 물류작업의 정합성을 검증하는 서비스
 * 작업 생성/수정/삭제 시 창고 재고와 수용량을 검증
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class LogisticTaskValidationService {

	private final LogisticTaskRepository logisticTaskRepository;
	private final StockRepository stockRepository;

	/**
	 * 새로운 물류작업 생성 시 정합성 검증
	 */
	public void validateTaskCreation(LogisticTask newTask) {
		log.debug("새 물류작업 생성 검증 시작: {}", newTask.getName());

		// 1. 현재 재고 상태 조회
		Map<String, Integer> currentStockMap = getCurrentStockSnapshot(newTask.getScheduledDate());

		// 2. 해당 날짜의 모든 작업 조회 (시간순)
		List<LogisticTask> existingTasks = logisticTaskRepository.findByScheduledDateOrderByEtd(
				newTask.getScheduledDate(), LogisticTaskStatus.CANCELLED);

		// 3. 새 작업을 포함한 시뮬레이션
		validateTaskSequenceWithNewTask(existingTasks, newTask, currentStockMap);

		log.debug("새 물류작업 생성 검증 완료: {}", newTask.getName());
	}

	/**
	 * 기존 물류작업 수정 시 정합성 검증
	 */
	public void validateTaskModification(LogisticTask originalTask, LogisticTask modifiedTask) {
		log.debug("물류작업 수정 검증 시작: {}", originalTask.getName());

		// 1. 현재 재고 상태 조회
		Map<String, Integer> currentStockMap = getCurrentStockSnapshot(originalTask.getScheduledDate());

		// 2. 해당 날짜의 모든 작업 조회 (수정 대상 제외)
		List<LogisticTask> existingTasks = logisticTaskRepository.findByScheduledDateOrderByEtd(
				originalTask.getScheduledDate(), LogisticTaskStatus.CANCELLED);

		// 기존 작업 제거
		existingTasks.removeIf(task -> task.getId().equals(originalTask.getId()));

		// 3. 수정된 작업을 포함한 시뮬레이션
		validateTaskSequenceWithNewTask(existingTasks, modifiedTask, currentStockMap);

		log.debug("물류작업 수정 검증 완료: {}", originalTask.getName());
	}

	/**
	 * 물류작업 삭제/취소 시 정합성 검증 (후속 작업에 영향 확인)
	 */
	public void validateTaskDeletion(LogisticTask taskToDelete) {
		log.debug("물류작업 삭제 검증 시작: {}", taskToDelete.getName());

		// 1. 현재 재고 상태 조회
		Map<String, Integer> currentStockMap = getCurrentStockSnapshot(taskToDelete.getScheduledDate());

		// 2. 해당 날짜의 모든 작업 조회 (삭제 대상 제외)
		List<LogisticTask> remainingTasks = logisticTaskRepository.findByScheduledDateOrderByEtd(
				taskToDelete.getScheduledDate(), LogisticTaskStatus.CANCELLED);

		// 삭제 대상 작업 제거
		remainingTasks.removeIf(task -> task.getId().equals(taskToDelete.getId()));

		// 3. 삭제 후 남은 작업들의 정합성 검증
		validateTaskSequence(remainingTasks, currentStockMap);

		log.debug("물류작업 삭제 검증 완료: {}", taskToDelete.getName());
	}

	/**
	 * 작업 상태 변경 시 정합성 검증 (INITIATED, COMPLETED 등)
	 */
	public void validateTaskStatusChange(LogisticTask task, LogisticTaskStatus newStatus) {
		log.debug("작업 상태 변경 검증: {} -> {}", task.getStatus(), newStatus);

		switch (newStatus) {
			case INITIATED:
				validateTaskInitiation(task);
				break;
			case COMPLETED:
				validateTaskCompletion(task);
				break;
			default:
				// 다른 상태 변경은 별도 검증 없음
				break;
		}
	}

	/**
	 * 작업 시작 시 출발 창고 재고 검증
	 */
	private void validateTaskInitiation(LogisticTask task) {
		Optional<Stock> stockOpt = stockRepository.findByWarehouseIdAndWareId(
				task.getFromLocation().getId(), task.getWare().getId());

		if (stockOpt.isEmpty()) {
			throw LogisticTaskException.invalidTaskDataEx(
					String.format("출발 창고(%d)에 물품(%d) 재고가 없습니다.",
							task.getFromLocation().getId(), task.getWare().getId()));
		}

		Stock stock = stockOpt.get();
		if (stock.getQuantity() < task.getQuantity()) {
			throw LogisticTaskException.invalidTaskDataEx(
					String.format("출발 창고(%d)의 물품(%d) 재고 부족. 필요: %d, 보유: %d",
							task.getFromLocation().getId(), task.getWare().getId(),
							task.getQuantity(), stock.getQuantity()));
		}
	}

	/**
	 * 작업 완료 시 도착 창고 수용량 검증
	 */
	private void validateTaskCompletion(LogisticTask task) {
		// 도착 창고의 총 수용량 검증 (Location 엔티티에 capacity 필드가 있다고 가정)
		Location toLocation = task.getToLocation();
		Long currentTotalQuantity = stockRepository.getTotalQuantityByWarehouse(toLocation.getId());

		// 수용량 초과 검증 (예시: Location에 capacity 필드가 있다고 가정)
		 if (currentTotalQuantity + task.getQuantity() > toLocation.getCapacity()) {
		     throw LogisticTaskException.invalidTaskDataEx("도착 창고 수용량 초과");
		 }
	}

	/**
	 * 작업 시퀀스와 새 작업을 함께 검증
	 */
	private void validateTaskSequenceWithNewTask(List<LogisticTask> existingTasks,
			LogisticTask newTask,
			Map<String, Integer> initialStockMap) {

		// 새 작업을 적절한 위치에 삽입
		existingTasks.add(newTask);
		existingTasks.sort(Comparator.comparing(LogisticTask::getEtd));

		validateTaskSequence(existingTasks, initialStockMap);
	}

	/**
	 * 작업 시퀀스의 정합성 검증 (시간순 시뮬레이션)
	 */
	private void validateTaskSequence(List<LogisticTask> tasks, Map<String, Integer> stockMap) {
		Map<String, Integer> simulatedStock = new HashMap<>(stockMap);

		for (LogisticTask task : tasks) {
			if (task.getStatus() == LogisticTaskStatus.CANCELLED) {
				continue;
			}

			String fromKey = createStockKey(task.getFromLocation().getId(), task.getWare().getId());
			String toKey = createStockKey(task.getToLocation().getId(), task.getWare().getId());

			// 출발 창고 재고 검증 및 차감
			Integer fromStock = simulatedStock.getOrDefault(fromKey, 0);
			if (fromStock < task.getQuantity()) {
				throw LogisticTaskException.invalidTaskDataEx(
						String.format("작업 '%s' 실행 불가: 출발 창고 재고 부족 (필요: %d, 보유: %d)",
								task.getName(), task.getQuantity(), fromStock));
			}

			simulatedStock.put(fromKey, fromStock - task.getQuantity());

			// 도착 창고 재고 증가
			Integer toStock = simulatedStock.getOrDefault(toKey, 0);
			simulatedStock.put(toKey, toStock + task.getQuantity());

			// 수용량 검증 (Location에 capacity가 있다고 가정)
			validateWarehouseCapacity(task.getToLocation(), toStock + task.getQuantity());
		}
	}

	/**
	 * 창고 수용량 검증
	 */
	private void validateWarehouseCapacity(Location warehouse, Integer projectedQuantity) {
		// Location 엔티티에 capacity 필드가 있다고 가정
		 if (warehouse.getCapacity() != null && projectedQuantity > warehouse.getCapacity()) {
		     throw LogisticTaskException.invalidTaskDataEx(
		             String.format("창고 '%s' 수용량 초과 (용량: %d, 예상 재고: %d)",
		                     warehouse.getName(), warehouse.getCapacity(), projectedQuantity));
		 }
	}

	/**
	 * 특정 날짜 기준 현재 재고 상태 스냅샷 생성
	 */
	private Map<String, Integer> getCurrentStockSnapshot(LocalDate targetDate) {
		Map<String, Integer> stockMap = new HashMap<>();

		// 모든 재고 조회
		List<Stock> allStocks = stockRepository.findAll();

		for (Stock stock : allStocks) {
			String key = createStockKey(stock.getWarehouseId(), stock.getWareId());
			stockMap.put(key, stock.getQuantity());
		}

		// 이전 날짜까지 완료된 작업들의 영향을 반영(더 효율적인 방법 고려 필요)
		adjustStockForPreviousCompletedTasks(stockMap, targetDate);

		return stockMap;
	}

	/**
	 * 이전 날짜까지 완료된 작업들의 재고 영향 반영
	 */
	private void adjustStockForPreviousCompletedTasks(Map<String, Integer> stockMap, LocalDate targetDate) {
		// 이전 날짜까지의 완료된 작업들을 조회하여 재고에 반영(재고 이력 히스토리 테이블을 별도 테이블로 관리할 예정)
		LocalDate yesterday = targetDate.minusDays(1);
		List<LogisticTask> previousCompletedTasks = logisticTaskRepository.findByScheduledDateBetween(
				yesterday.minusDays(30), yesterday); // 최근 30일간 조회 (예시)

		for (LogisticTask task : previousCompletedTasks) {
			if (task.getStatus() == LogisticTaskStatus.COMPLETED) {
				// 이미 완료된 작업의 영향은 현재 재고에 반영되어 있으므로 별도 처리 불필요
				// 하지만 미완료 작업들의 영향을 제거해야 할 수도 있음
			}
		}
	}

	/**
	 * 재고 맵 키 생성 (창고ID_물품ID)
	 */
	private String createStockKey(Long warehouseId, Long wareId) {
		return warehouseId + "_" + wareId;
	}
}
