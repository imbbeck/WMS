package com.wms.logisticTemplate.domain.exception;

public class LogisticTaskException extends RuntimeException {
	public LogisticTaskException(String message) {
		super(message);
	}

	public static class TaskNotModifiableException extends LogisticTaskException {
		public TaskNotModifiableException(String currentStatus) {
			super("현재 상태(" + currentStatus + ")에서는 작업을 수정할 수 없습니다. (PENDING 또는 INITIATE_DELAYED만 가능)");
		}
	}

	public static class InvalidTaskDataException extends LogisticTaskException {
		public InvalidTaskDataException(String message) {
			super("잘못된 작업 데이터: " + message);
		}
	}

	public static class TaskCancellationNotAllowedException extends LogisticTaskException {
		public TaskCancellationNotAllowedException(String currentStatus) {
			super("현재 상태(" + currentStatus + ")에서는 작업을 취소할 수 없습니다. (PENDING 또는 INITIATE_DELAYED만 가능)");
		}
	}

	public static class TaskFailureNotAllowedException extends LogisticTaskException {
		public TaskFailureNotAllowedException(String currentStatus) {
			super("현재 상태(" + currentStatus + ")에서는 작업을 실패 처리할 수 없습니다. (INITIATED만 가능)");
		}
	}
}

