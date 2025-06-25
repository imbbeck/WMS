package com.wms.applicationInfra.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;

import java.util.function.Supplier;

/**
 * 낙관적 락 실패 시 지수적 백오프로 재시도하는 유틸리티
 */
@Slf4j
public class OptimisticLockRetryUtil {

	private static final int MAX_RETRY_COUNT = 5;
	private static final long[] RETRY_DELAYS_MS = {100L, 300L, 700L, 1500L, 3000L};

	/**
	 * 낙관적 락 재시도 실행
	 *
	 * @param operation 실행할 작업
	 * @param operationName 로깅용 작업 이름
	 * @param <T> 반환 타입
	 * @return 작업 결과
	 * @throws ConflictException 최대 재시도 횟수 초과 시
	 */
	public static <T> T executeWithRetry(Supplier<T> operation, String operationName) {
		for (int attempt = 0; attempt < MAX_RETRY_COUNT; attempt++) {
			try {
				log.debug("[{}] 시도 중... ({}회차)", operationName, attempt + 1);
				return operation.get();

			} catch (OptimisticLockingFailureException e) {
				log.warn("[{}] 낙관적 락 충돌 발생 ({}회차) - {}", operationName, attempt + 1, e.getMessage());

				if (attempt == MAX_RETRY_COUNT - 1) {
					// 최대 재시도 횟수 초과
					log.error("[{}] 최대 재시도 횟수({})를 초과했습니다. 사용자 재입력이 필요합니다.",
							operationName, MAX_RETRY_COUNT);
					throw new ConflictException("다른 사용자가 먼저 작업을 완료했습니다. 화면을 새로고침한 후 다시 시도해주세요.");
				}

				// 지수적 백오프 대기
				long delayMs = RETRY_DELAYS_MS[attempt];
				log.debug("[{}] {}ms 대기 후 재시도 예정...", operationName, delayMs);

				try {
					Thread.sleep(delayMs);
				} catch (InterruptedException ie) {
					Thread.currentThread().interrupt();
					throw new RuntimeException("재시도 대기 중 인터럽트 발생", ie);
				}
			}
		}

		// 여기 도달하면 안됨
		throw new IllegalStateException("예상치 못한 재시도 로직 오류");
	}

	/**
	 * 반환값이 없는 작업용 재시도 실행
	 *
	 * @param operation 실행할 작업
	 * @param operationName 로깅용 작업 이름
	 * @throws ConflictException 최대 재시도 횟수 초과 시
	 */
	public static void executeWithRetry(Runnable operation, String operationName) {
		executeWithRetry(() -> {
			operation.run();
			return null;
		}, operationName);
	}

	/**
	 * 충돌 예외 클래스
	 */
	public static class ConflictException extends RuntimeException {
		public ConflictException(String message) {
			super(message);
		}

		public ConflictException(String message, Throwable cause) {
			super(message, cause);
		}
	}
}