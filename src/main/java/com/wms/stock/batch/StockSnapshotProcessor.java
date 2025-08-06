package com.wms.stock.batch;

import com.wms.location.domain.model.Location;
import com.wms.logisticTask.domain.model.LogisticTask;
import com.wms.logisticTask.domain.repository.LogisticTaskRepository;
import com.wms.stock.application.StockCacheService;
import com.wms.stock.domain.model.Stock;
import com.wms.stock.domain.model.StockDailySnapshot;
import com.wms.stock.domain.model.StockKey;
import com.wms.stock.domain.model.StockSnapshotKey;
import com.wms.stock.domain.repository.StockDailySnapshotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Component
@Slf4j
@RequiredArgsConstructor
public class StockSnapshotProcessor implements ItemProcessor<Stock, StockDailySnapshot> {

	private final StockDailySnapshotRepository snapshotRepository;
	private final StockCacheService stockCacheService;
	private final LogisticTaskRepository logisticTaskRepository;

	@Override
	public StockDailySnapshot process(Stock stock) {
		LocalDate today = LocalDate.now();
		LocalDate yesterday = today.minusDays(1);

		StockKey stockKey = stock.getKey();

		// 어제 스냅샷을 찾기 위해 그저께 날짜로 조회
		StockSnapshotKey yesterdayKey = StockSnapshotKey.of(stockKey, today.minusDays(2));

		Optional<StockDailySnapshot> ySnapshot = snapshotRepository.findByKey(yesterdayKey);
		int yQty = ySnapshot.map(StockDailySnapshot::getQuantity).orElse(0);

		// 재고 정합성 검증 및 캐시 동기화
		dailyStockCheck(yQty, stock);

		return StockDailySnapshot.builder()
				.key(StockSnapshotKey.of(stock.getKey(), yesterday))
				.quantity(stock.getQuantity())
				.changeFromYesterday(stock.getQuantity() - yQty)
				.build();
	}

	/**
	 * 그제의 재고 스냅샷 + 어제의 물류작업들의 결과 = 자정 현재 재고 인지 확인하고 캐시 업데이트. 기준은 재고의 현재 상태.
	 */
	private void dailyStockCheck(Integer previousQty, Stock stock) {
		StockKey stockKey = stock.getKey();
		LocalDate targetDate = LocalDate.now().minusDays(1); // 결산 대상일 (어제)

		try {
			// 어제의 물류작업 조회 및 재고 변동 계산
			StockMovement movement = calculateStockMovement(stockKey, targetDate);

			// 재고 정합성 검증
			validateStockConsistency(stockKey, previousQty, movement, stock.getQuantity());

			// 캐시 동기화
			synchronizeCache(stockKey, stock.getQuantity());

		} catch (Exception e) {
			log.error("재고 정합성 검증 중 오류 발생: stockKey={}, error={}",
					stockKey, e.getMessage(), e);
		}
	}

	/**
	 * 특정 날짜의 물류작업을 통한 재고 변동량 계산
	 */
	private StockMovement calculateStockMovement(StockKey stockKey, LocalDate targetDate) {
		Long warehouseId = stockKey.getWarehouseId();
		Long wareId = stockKey.getWareId();

		List<LogisticTask> tasks = logisticTaskRepository.findCompletedTasksByTargetDateAndStock(
				targetDate, warehouseId, wareId
		);

		if (tasks.isEmpty()) {
			return new StockMovement(0, 0, List.of(), List.of());
		}

		// 출고 작업 (해당 창고에서 나가는 물량)
		List<LogisticTask> outboundTasks = tasks.stream()
				.filter(task -> isOutboundTask(task, warehouseId))
				.toList();

		// 입고 작업 (해당 창고로 들어오는 물량)
		List<LogisticTask> inboundTasks = tasks.stream()
				.filter(task -> isInboundTask(task, warehouseId))
				.toList();

		int outboundQty = outboundTasks.stream()
				.mapToInt(LogisticTask::getQuantity)
				.sum();

		int inboundQty = inboundTasks.stream()
				.mapToInt(LogisticTask::getQuantity)
				.sum();

		return new StockMovement(outboundQty, inboundQty, outboundTasks, inboundTasks);
	}

	/**
	 * 출고 작업 여부 확인
	 */
	private boolean isOutboundTask(LogisticTask task, Long warehouseId) {
		return task.getFromLocation() != null &&
				task.getFromLocation().getId().equals(warehouseId);
	}

	/**
	 * 입고 작업 여부 확인
	 */
	private boolean isInboundTask(LogisticTask task, Long warehouseId) {
		return task.getToLocation() != null &&
				task.getToLocation().getId().equals(warehouseId);
	}

	/**
	 * 재고 정합성 검증
	 */
	private void validateStockConsistency(StockKey stockKey, Integer previousQty, StockMovement movement, Integer actualQty) {

		int expectedQty = previousQty - movement.outboundQty() + movement.inboundQty();

		if (expectedQty != actualQty) {
			log.warn("재고 불일치 발견: stockKey={}, 이전재고={}, 출고={}, 입고={}, 예상재고={}, 실제재고={}, 차이={}",
					stockKey, previousQty, movement.outboundQty(), movement.inboundQty(), expectedQty, actualQty, actualQty - expectedQty);
		}
	}

	/**
	 * 캐시 동기화
	 */
	private void synchronizeCache(StockKey stockKey, Integer actualQty) {
		try {
			Integer cacheQty = stockCacheService.getInventoryQuantity(stockKey);

			if (!actualQty.equals(cacheQty)) {
				log.warn("캐시 재고 불일치: stockKey={}, 캐시재고={}, 실제재고={}",
						stockKey, cacheQty, actualQty);

				stockCacheService.updateInventoryQuantity(stockKey, actualQty);
				log.info("캐시 재고 동기화 완료: stockKey={}, 업데이트된재고={}",
						stockKey, actualQty);

			}
		} catch (Exception e) {
			log.error("캐시 동기화 중 오류 발생: stockKey={}, error={}",
					stockKey, e.getMessage(), e);
		}
	}

	/**
	 * 재고 변동 정보를 담는 레코드
	 */
	private record StockMovement(
			int outboundQty,
			int inboundQty,
			List<LogisticTask> outboundTasks,
			List<LogisticTask> inboundTasks
	) {}
}