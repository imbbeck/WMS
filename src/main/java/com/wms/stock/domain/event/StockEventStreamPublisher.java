package com.wms.stock.domain.event;

import com.wms.applicationInfra.config.RedisStreamsInitializer;
import com.wms.applicationInfra.util.JsonUtils;
import com.wms.location.domain.exception.LocationException;
import com.wms.logisticTask.domain.event.LogisticTaskInitiatedEvent;
import com.wms.stock.application.TaskEventNotifier;
import com.wms.location.domain.model.Location;
import com.wms.location.domain.model.LocationType;
import com.wms.location.domain.repository.LocationRepository;
import com.wms.logisticTask.domain.event.LogisticTaskCompletedEvent;
import com.wms.notification.application.StockSyncNotificationAdapter;
import com.wms.notification.domain.model.StockSyncStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 재고 이벤트 스트림 발행자
 * 
 * 물류 작업의 시작/완료 이벤트를 수신하여 Redis Stream으로 재고 변경 이벤트를 발행합니다.
 * 새로운 알림 시스템을 사용하여 사용자에게 실시간으로 상태를 알립니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StockEventStreamPublisher {

	private final RedisTemplate<String, Object> redisTemplate;
	private final RedisStreamsInitializer streamsInitializer;
	private final TaskEventNotifier taskEventNotifier;
	private final LocationRepository locationRepository;

	private final StockSyncNotificationAdapter stockSyncNotificationAdapter;

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handleLogisticTaskInitiated(LogisticTaskInitiatedEvent event) {
		log.debug("물류작업 시작 이벤트 수신: taskId={}", event.getTaskId());

		try {
			// 출발지가 창고인지 확인
			Location fromLocation = locationRepository.findById(event.getFromLocationId())
					.orElseThrow(() -> LocationException.notFound(event.getFromLocationId()));

			if (fromLocation.getType() == LocationType.WAREHOUSE) {
				// 창고에서만 재고 감소
				String fromStockStream = generateStreamKey(event.getWareId(), event.getFromLocationId());

				StockChangeEvent decreaseEvent = StockChangeEvent.builder()
						.taskId(event.getTaskId())
						.wareId(event.getWareId())
						.locationId(event.getFromLocationId())
						.changeType(StockChangeType.DECREASE)
						.quantity(event.getQuantity())
						.timestamp(LocalDateTime.now())
						.build();

				publishToStream(fromStockStream, decreaseEvent);
				taskEventNotifier.notifyStockSyncQueued(event.getTaskId(), "출발지 재고 감소 대기");

			} else {
				// 입고처에서는 재고 처리 없음
				taskEventNotifier.notifyTaskInitiated(event.getTaskId());
				log.debug("입고처({})에서 시작 - 재고 처리 없음", fromLocation.getType());
			}

		} catch (Exception e) {
			log.error("물류작업 시작 이벤트 처리 실패: taskId={}", event.getTaskId(), e);
			taskEventNotifier.notifyStreamPublishFailed(event.getTaskId(), "initiation", e.getMessage());
		}
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handleLogisticTaskCompleted(LogisticTaskCompletedEvent event) {
		log.debug("물류작업 완료 이벤트 수신: taskId={}", event.getTaskId());

		try {
			// 도착지가 창고인지 확인
			Location toLocation = locationRepository.findById(event.getToLocationId())
					.orElseThrow(() -> LocationException.notFound(event.getToLocationId()));

			if (toLocation.getType() == LocationType.WAREHOUSE) {
				// 창고로만 재고 증가
				String toStockStream = generateStreamKey(event.getWareId(), event.getToLocationId());

				StockChangeEvent increaseEvent = StockChangeEvent.builder()
						.taskId(event.getTaskId())
						.wareId(event.getWareId())
						.locationId(event.getToLocationId())
						.changeType(StockChangeType.INCREASE)
						.quantity(event.getQuantity())
						.timestamp(LocalDateTime.now())
						.build();

				publishToStream(toStockStream, increaseEvent);
				taskEventNotifier.notifyStockSyncQueued(event.getTaskId(), "도착지 재고 증가 대기");

			} else {
				// 출고처로는 재고 처리 없음
				taskEventNotifier.notifyTaskCompleted(event.getTaskId());
				log.debug("출고처({})로 완료 - 재고 처리 없음", toLocation.getType());
			}

		} catch (Exception e) {
			log.error("재고 동기화 이벤트 발행 실패: taskId={}", event.getTaskId(), e);
			taskEventNotifier.notifyStreamPublishFailed(event.getTaskId(), "completion", e.getMessage());
		}
	}

	private void publishToStream(String streamKey, StockChangeEvent event) {
		try {
			// 1. Consumer Group 존재 확인 및 생성
			streamsInitializer.ensureConsumerGroupExists(streamKey);

			// 2. 이벤트 데이터 준비
			Map<String, Object> eventData = Map.of(
					"taskId", event.getTaskId(),
					"wareId", event.getWareId(),
					"locationId", event.getLocationId(),
					"changeType", event.getChangeType().name(),
					"quantity", event.getQuantity(),
					"timestamp", event.getTimestamp().toString(),
					"eventJson", JsonUtils.toJson(event)
			);

			// 3. 스트림에 이벤트 발행
			RecordId recordId = redisTemplate.opsForStream().add(streamKey, eventData);

			log.debug("이벤트 발행 성공: stream={}, recordId={}, taskId={}",
					streamKey, recordId, event.getTaskId());

		} catch (Exception e) {
			log.error("스트림 발행 실패: stream={}, taskId={}", streamKey, event.getTaskId(), e);
			handleStreamPublishFailure(streamKey, event, e);
		}
	}

	private void handleStreamPublishFailure(String streamKey, StockChangeEvent event, Exception e) {
		log.error("스트림 발행 실패 처리: stream={}, taskId={}, error={}",
				streamKey, event.getTaskId(), e.getMessage());

		// 실패 시 대체 처리 (필요시)
		// 1. 데이터베이스에 실패 로그 저장
		// 2. 별도 재시도 큐에 저장
		// 3. 알림 발송 등

		// 새로운 알림 시스템으로 실패 알림 전송
		stockSyncNotificationAdapter.broadcastStockSyncStatus(
				event.getTaskId(),
				StockSyncStatus.FAILED,
				"스트림 발행 실패: " + e.getMessage()
		);
	}

	/**
	 * 스트림 발행 성공 시 대기 상태 알림
	 */
	private void notifyStreamPublished(Long taskId, String operation) {
		stockSyncNotificationAdapter.broadcastStockSyncStatus(
				taskId,
				StockSyncStatus.QUEUED,
				operation + " 재고 동기화가 대기열에 등록되었습니다"
		);
	}

	/**
	 * 스트림 키 생성 (물품ID:장소ID)
	 */
	private String generateStreamKey(Long wareId, Long locationId) {
		return "stock:events:" + wareId + ":" + locationId;
	}

	/**
	 * 재고 변경 없는 작업 완료 직접 알림
	 */
	private void notifyTaskCompletedWithoutStockChange(Long taskId, String locationType) {
		stockSyncNotificationAdapter.broadcastStockSyncStatus(
				taskId,
				StockSyncStatus.COMPLETED,
				locationType + "에서의 작업이 완료되었습니다 (재고 변경 없음)"
		);
	}

	/**
	 * 향후 사용을 위한 편의 메서드들
	 */
	
	/**
	 * 특정 작업의 전체 재고 동기화 프로세스 시작 알림
	 */
	public void notifyStockSyncProcessStarted(Long taskId, String description) {
		stockSyncNotificationAdapter.broadcastStockSyncStatus(
				taskId,
				StockSyncStatus.WAITING,
				"재고 동기화 프로세스 시작: " + description
		);
	}

	/**
	 * 배치 재고 처리를 위한 다중 이벤트 발행
	 */
	public void publishBatchStockEvents(Long taskId, java.util.List<StockChangeEvent> events) {
		log.info("배치 재고 이벤트 발행 시작: taskId={}, eventCount={}", taskId, events.size());
		
		stockSyncNotificationAdapter.broadcastStockSyncStatus(
				taskId,
				StockSyncStatus.PROCESSING,
				events.size() + "개의 재고 변경 이벤트를 처리하고 있습니다..."
		);

		int successCount = 0;
		int failureCount = 0;

		for (StockChangeEvent event : events) {
			try {
				String streamKey = generateStreamKey(event.getWareId(), event.getLocationId());
				publishToStream(streamKey, event);
				successCount++;
			} catch (Exception e) {
				failureCount++;
				log.error("배치 이벤트 발행 실패: taskId={}, event={}", taskId, event, e);
			}
		}

		// 배치 처리 결과 알림
		if (failureCount == 0) {
			stockSyncNotificationAdapter.broadcastStockSyncStatus(
					taskId,
					StockSyncStatus.COMPLETED,
					String.format("배치 재고 처리 완료: 성공 %d개", successCount)
			);
		} else {
			stockSyncNotificationAdapter.broadcastStockSyncStatus(
					taskId,
					StockSyncStatus.FAILED,
					String.format("배치 재고 처리 완료: 성공 %d개, 실패 %d개", successCount, failureCount)
			);
		}
	}
}
