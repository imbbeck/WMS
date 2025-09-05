package com.wms.stock.application;

import com.wms.applicationInfra.util.JsonUtils;
import com.wms.notification.application.StockSyncNotificationAdapter;
import com.wms.notification.domain.model.StockSyncStatus;
import com.wms.stock.domain.event.StockChangeEvent;
import com.wms.stock.domain.event.StockChangeType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
@Slf4j
public class StockEventStreamProcessor {

	private final RedisTemplate<String, Object> redisTemplate;
	private final StockCtrlService stockCtrlService;
	private final StockSyncNotificationAdapter stockSyncNotificationAdapter;

	@Value("${stock.streams.enable:true}")
	private boolean streamsEnabled;

	@Value("${stock.streams.consumer-group:stock-processors}")
	private String consumerGroup;

	@Value("${stock.streams.consumer-name:${spring.application.name}-${random.uuid}}")
	private String consumerName;

	@Value("${stock.streams.batch-size:10}")
	private int batchSize;

	@Value("${stock.streams.poll-timeout:2s}")
	private Duration pollTimeout;

	private final Map<String, CompletableFuture<Void>> streamConsumers = new ConcurrentHashMap<>();
	private volatile boolean running = true;

	@PostConstruct
	public void startStreamConsumers() {
		if (!streamsEnabled) {
			log.info("Redis Streams 비활성화됨");
			return;
		}
		running = true;
		log.info("Redis Streams 소비자 시작: group={}, consumer={}", consumerGroup, consumerName);
		startDiscoveryTask();
	}

	public void forceStartStreamConsumers() {
		running = true;
		log.info("Redis Streams 소비자 강제 시작: group={}, consumer={}", consumerGroup, consumerName);
		startDiscoveryTask();
	}

	@PreDestroy
	public void stopStreamConsumers() {
		running = false;
		streamConsumers.values().forEach(future -> future.cancel(true));
		log.info("Redis Streams 소비자 종료");
	}

	private void startDiscoveryTask() {
		CompletableFuture.runAsync(() -> {
			while (running) {
				try {
					discoverAndStartNewStreamConsumers();
					Thread.sleep(30000); // 30초마다 새로운 스트림 확인
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					break;
				} catch (Exception e) {
					log.warn("스트림 발견 중 오류", e);
				}
			}
		});
	}

	private void discoverAndStartNewStreamConsumers() {
		try {
			// stock:events:* 패턴의 스트림 검색
			var streamKeys = redisTemplate.keys("stock:events:*");

			for (String streamKey : streamKeys) {
				if (!streamConsumers.containsKey(streamKey)) {
					startStreamConsumer(streamKey);
				}
			}
		} catch (Exception e) {
			log.error("스트림 발견 실패", e);
		}
	}

	private void startStreamConsumer(String streamKey) {
		CompletableFuture<Void> consumerTask = CompletableFuture.runAsync(() -> {
			log.info("스트림 소비자 시작: {}", streamKey);

			while (running) {
				try {
					consumeStreamEvents(streamKey);
				} catch (Exception e) {
					log.error("스트림 소비 중 오류: {}", streamKey, e);

					try {
						Thread.sleep(5000); // 오류 발생 시 5초 대기
					} catch (InterruptedException ie) {
						Thread.currentThread().interrupt();
						break;
					}
				}
			}

			log.info("스트림 소비자 종료: {}", streamKey);
		});

		streamConsumers.put(streamKey, consumerTask);
	}

	public void consumeStreamEvents(String streamKey) {
		try {
			// Consumer Group으로 읽기
			List<MapRecord<String, Object, Object>> records = redisTemplate.opsForStream().read(
					Consumer.from(consumerGroup, consumerName),
							StreamReadOptions.empty().count(batchSize).block(pollTimeout),
							StreamOffset.create(streamKey, ReadOffset.lastConsumed()));

			for (MapRecord<String, Object, Object> record : records) {
				processStreamRecord(streamKey, record);
			}

		} catch (Exception e) {
			if (!e.getMessage().contains("NOGROUP")) {
				log.error("스트림 읽기 실패: {}", streamKey, e);
			}
		}
	}

	public void processStreamRecord(String streamKey, MapRecord<String, Object, Object> record) {
		RecordId recordId = record.getId();

		try {
			// 이벤트 파싱
			String eventJson = (String) record.getValue().get("eventJson");
			if (eventJson == null) {
				log.warn("eventJson이 없는 레코드: {}", recordId);
				acknowledgeRecord(streamKey, recordId);
				return;
			}

			StockChangeEvent event = JsonUtils.fromJson(eventJson, StockChangeEvent.class);

			// 처리 시작 알림 - 새로운 시스템 사용
			stockSyncNotificationAdapter.broadcastStockSyncStatus(
					event.getTaskId(),
					StockSyncStatus.PROCESSING,
					"재고 데이터를 업데이트하고 있습니다..."
			);

			// 실제 재고 처리
			processStockChangeEvent(event);

			// 성공 알림 - 새로운 시스템 사용
			stockSyncNotificationAdapter.broadcastStockSyncStatus(
					event.getTaskId(),
					StockSyncStatus.COMPLETED,
					"재고 데이터가 성공적으로 업데이트되었습니다"
			);

			// 메시지 ACK
			acknowledgeRecord(streamKey, recordId);

			log.debug("이벤트 처리 완료: stream={}, recordId={}, taskId={}",
					streamKey, recordId, event.getTaskId());

		} catch (OptimisticLockingFailureException e) {
			handleOptimisticLockFailure(streamKey, record, e);

		} catch (Exception e) {
			handleProcessingError(streamKey, record, e);
		}
	}

	private void processStockChangeEvent(StockChangeEvent event) {
		if (event.getChangeType() == StockChangeType.DECREASE) {
			stockCtrlService.decreaseStock(
					event.getLocationId(), event.getWareId(), event.getQuantity()
			);
		} else if (event.getChangeType() == StockChangeType.INCREASE) {
			stockCtrlService.increaseStock(
					event.getLocationId(), event.getWareId(), event.getQuantity()
			);
		}
	}

	private void handleOptimisticLockFailure(String streamKey, MapRecord<String, Object, Object> record, Exception e) {
		Object taskIdObj = record.getValue().get("taskId");
		Long taskId = taskIdObj instanceof String ? Long.parseLong((String) taskIdObj) : ((Number) taskIdObj).longValue();

		log.warn("낙관적 락 충돌, 재시도 예정: stream={}, record={}, taskId={}",
				streamKey, record.getId(), taskId);

		// 재시도 알림 - 새로운 시스템 사용
		stockSyncNotificationAdapter.broadcastStockSyncStatus(
				taskId,
				StockSyncStatus.RETRYING,
				"동시성 충돌로 재시도 중입니다..."
		);

		// 메시지를 ACK하지 않고 나둬서 자동 재시도되도록 함
		// Redis Streams의 pending 메시지로 남아있다가 다시 처리됨
	}

	private void handleProcessingError(String streamKey, MapRecord<String, Object, Object> record, Exception e) {
		Object taskIdObj = record.getValue().get("taskId");
		Long taskId = taskIdObj instanceof String ? Long.parseLong((String) taskIdObj) : ((Number) taskIdObj).longValue();

		log.error("스트림 이벤트 처리 실패: stream={}, record={}, taskId={}",
				streamKey, record.getId(), taskId, e);

		// 실패 알림 - 새로운 시스템 사용
		stockSyncNotificationAdapter.broadcastStockSyncStatus(
				taskId,
				StockSyncStatus.FAILED,
				"재고 업데이트 중 오류가 발생했습니다: " + e.getMessage()
		);

		// 실패한 메시지 ACK (DLQ로 이동하거나 로그로 남김)
		acknowledgeRecord(streamKey, record.getId());
	}

	private void acknowledgeRecord(String streamKey, RecordId recordId) {
		try {
			redisTemplate.opsForStream().acknowledge(streamKey, consumerGroup, recordId);
//			redisTemplate.opsForStream().delete(streamKey, recordId);
//
//			try {
//				Long streamLength = redisTemplate.opsForStream().size(streamKey);
//				if (streamLength == 0) {
//					redisTemplate.delete(streamKey);
//					// Consumer도 정리
//					streamConsumers.remove(streamKey);
//					log.info("빈 스트림 및 Consumer 정리: {}", streamKey);
//				}
//			} catch (Exception e) {
//				log.debug("스트림 정리 중 오류 (무시 가능): {}", streamKey, e);
//			}
		} catch (Exception e) {
			log.error("메시지 ACK 실패: stream={}, recordId={}", streamKey, recordId, e);
		}
	}
}
