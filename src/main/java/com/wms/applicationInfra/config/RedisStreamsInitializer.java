package com.wms.applicationInfra.config;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisStreamsInitializer {

	private final RedisTemplate<String, Object> redisTemplate;
	private final Set<String> initializedStreams = ConcurrentHashMap.newKeySet();

	/**
	 * 스트림별 Consumer Group을 동적으로 생성
	 */
	public void ensureConsumerGroupExists(String streamKey) {
		if (initializedStreams.contains(streamKey)) {
			return; // 이미 초기화됨
		}

		try {
			redisTemplate.opsForStream().createGroup(
					streamKey,
					ReadOffset.latest(), // 최신 메시지부터 시작
					"stock-processors"
			);

			initializedStreams.add(streamKey);
			log.info("Consumer Group 생성: {}", streamKey);

		} catch (Exception e) {
			if (e.getMessage() != null && e.getMessage().contains("BUSYGROUP")) {
				initializedStreams.add(streamKey);
				log.debug("Consumer Group 이미 존재: {}", streamKey);
			} else if (e.getMessage() != null && e.getMessage().contains("NOGROUP")) {
				log.warn("스트림이 존재하지 않음: {}", streamKey);
			} else {
				log.error("Consumer Group 생성 실패: {} - {}", streamKey, e.getMessage());
			}
		}
	}
}
