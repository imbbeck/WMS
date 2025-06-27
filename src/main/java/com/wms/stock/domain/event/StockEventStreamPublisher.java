package com.wms.stock.domain.event;

import com.wms.applicationInfra.config.RedisStreamsInitializer;
import com.wms.applicationInfra.util.JsonUtils;
import com.wms.location.domain.exception.LocationException;
import com.wms.logisticTask.domain.event.LogisticTaskInitiatedEvent;
import com.wms.stock.application.StockSyncEventService;
import com.wms.stock.application.TaskEventNotifier;
import com.wms.location.domain.model.Location;
import com.wms.location.domain.model.LocationType;
import com.wms.location.domain.repository.LocationRepository;
import com.wms.logisticTask.domain.event.LogisticTaskCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class StockEventStreamPublisher {

	private final RedisTemplate<String, Object> redisTemplate;
	private final RedisStreamsInitializer streamsInitializer;
	private final TaskEventNotifier taskEventNotifier;
	private final LocationRepository locationRepository;
	private final StockSyncEventService stockSyncEventService;

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

		// SSE로 실패 알림
		stockSyncEventService.broadcastSyncStatus(
				event.getTaskId(),
				StockSyncStatus.FAILED,
				"스트림 발행 실패: " + e.getMessage()
		);
	}

	private String generateStreamKey(Long wareId, Long locationId) {
		return "stock:events:" + wareId + ":" + locationId;
	}
}
