package com.wms.logisticTask.domain.exception;

import com.wms.applicationInfra.domain.FieldEnum;
import com.wms.applicationInfra.exception.BusinessException;
import com.wms.applicationInfra.exception.DomainExceptionHelper;

public final class LogisticTaskException {
	private LogisticTaskException() {}

	// ValidationEx
	public static class ValidationEx extends BusinessException.ValidationException {
		public ValidationEx(String message) {
			super(message);
		}
	}

	public static ValidationEx validation(String field, String additionalMessage) {
		return new ValidationEx(DomainExceptionHelper.validation(field, additionalMessage));
	}

	public static ValidationEx validation(FieldEnum fieldEnum) {
		return new ValidationEx(DomainExceptionHelper.validation(fieldEnum));
	}

	public static ValidationEx validation(String message) {
		return new ValidationEx(message);
	}

	public static ValidationEx simulationFailedEx(String message) {
		return new ValidationEx("시뮬레이션 검증 실패: " + message);
	}

	public static ValidationEx stockValidationFailedEx(String message) {
		return new ValidationEx("재고 검증 실패: " + message);
	}

	public static ValidationEx capacityValidationFailedEx(String message) {
		return new ValidationEx("창고 용량 검증 실패: " + message);
	}

	public static ValidationEx nameRequired() {
		return new ValidationEx("작업명은 필수입니다");
	}

	public static ValidationEx nameTooLong(int maxLength) {
		return new ValidationEx("작업명은 " + maxLength + "자를 초과할 수 없습니다");
	}

	public static ValidationEx quantityMustBePositive() {
		return new ValidationEx("수량은 0보다 커야 합니다");
	}

	public static ValidationEx timeRequired() {
		return new ValidationEx("ETD 및 ETA는 필수입니다");
	}

	public static ValidationEx etdMustBeBeforeEta() {
		return new ValidationEx("출발 예정시간은 도착 예정시간보다 빨라야 합니다");
	}

	public static ValidationEx cannotInitiateTask(String currentStatus) {
		return new ValidationEx("현재 상태(" + currentStatus + ")에서는 작업을 시작할 수 없습니다");
	}

	public static ValidationEx cannotCompleteTask(String currentStatus) {
		return new ValidationEx("현재 상태(" + currentStatus + ")에서는 작업을 완료할 수 없습니다");
	}

	public static ValidationEx cannotDelayInitiation(String currentStatus) {
		return new ValidationEx("현재 상태(" + currentStatus + ")에서는 시작 지연 처리를 할 수 없습니다");
	}

	public static ValidationEx cannotDelayCompletion(String currentStatus) {
		return new ValidationEx("현재 상태(" + currentStatus + ")에서는 완료 지연 처리를 할 수 없습니다");
	}

	// NotFoundEx
	public static class NotFoundEx extends BusinessException.NotFoundException {
		public NotFoundEx(String message) {
			super(message);
		}
	}

	public static NotFoundEx notFound(Long taskId) {
		return new NotFoundEx(DomainExceptionHelper.notFound(taskId));
	}

	// ConflictEx
	public static class ConflictEx extends BusinessException.ConflictException {
		public ConflictEx(String message) {
			super(message);
		}
	}

	public static ConflictEx duplicate(FieldEnum fieldEnum, String value) {
		return new ConflictEx(DomainExceptionHelper.duplicate(fieldEnum, value));
	}

	// TaskNotModifiableEx
	public static class TaskNotModifiableEx extends BusinessException.BadRequestException {
		public TaskNotModifiableEx(String message) {
			super(message);
		}
	}

	public static TaskNotModifiableEx taskNotModifiableEx(String currentStatus) {
		return new TaskNotModifiableEx("현재 상태(" + currentStatus + ")에서는 작업을 수정할 수 없습니다. (PENDING 또는 INITIATE_DELAYED만 가능)");
	}

	// TaskCancellationNotAllowedEx
	public static class TaskCancellationNotAllowedEx extends BusinessException.BadRequestException {
		public TaskCancellationNotAllowedEx(String message) {
			super(message);
		}
	}

	public static TaskCancellationNotAllowedEx taskCancellationNotAllowedEx(String currentStatus) {
		return new TaskCancellationNotAllowedEx("현재 상태(" + currentStatus + ")에서는 작업을 취소할 수 없습니다. (PENDING 또는 INITIATE_DELAYED만 가능)");
	}

	// TaskFailureNotAllowedEx
	public static class TaskFailureNotAllowedEx extends BusinessException.BadRequestException {
		public TaskFailureNotAllowedEx(String message) {
			super(message);
		}
	}

	public static TaskFailureNotAllowedEx taskFailureNotAllowedEx(String currentStatus) {
		return new TaskFailureNotAllowedEx("현재 상태(" + currentStatus + ")에서는 작업을 실패 처리할 수 없습니다. (INITIATED만 가능)");
	}

	// InvalidTaskDataEx
	public static class InvalidTaskDataEx extends BusinessException.BadRequestException {
		public InvalidTaskDataEx(String message) {
			super(message);
		}
	}

	public static InvalidTaskDataEx invalidTaskDataEx(String message) {
		return new InvalidTaskDataEx("잘못된 작업 데이터: " + message);
	}

	// WorkerMismatchEx
	public static class WorkerMismatchEx extends BusinessException.ForbiddenException {
		public WorkerMismatchEx(String message) {
			super(message);
		}
	}

	public static WorkerMismatchEx workerMismatchEx(Long taskId, Long id) {
		return new WorkerMismatchEx("작업 ID " + taskId + "와 일치하지 않는 작업자 ID: " + id);
	}

}

