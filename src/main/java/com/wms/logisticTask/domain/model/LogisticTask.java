package com.wms.logisticTask.domain.model;

import com.wms.applicationInfra.domain.BaseEntity;
import com.wms.logisticTask.domain.exception.LogisticTaskException;
import com.wms.location.domain.model.Location;
import com.wms.logisticTemplate.domain.model.LogisticType;
import com.wms.userInfo.domain.model.UserInfo;
import com.wms.ware.domain.model.Ware;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 물류 작업 엔티티
 */
@Entity
@Table(name = "logistic_task")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class LogisticTask extends BaseEntity {

	@Column(nullable = false, length = 255)
	private String name;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private LogisticType type;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "worker_id", nullable = false)
	private UserInfo worker;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "ware_id", nullable = false)
	private Ware ware;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "from_location_id", nullable = false)
	private Location fromLocation;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "to_location_id", nullable = false)
	private Location toLocation;

	@Column(nullable = false)
	private Integer quantity;

	@Column(nullable = false)
	private LocalDate scheduledDate;

	@Column(nullable = false)
	private LocalTime etd; // Estimated Time of Departure

	@Column(nullable = false)
	private LocalTime eta; // Estimated Time of Arrival

	private LocalTime atd; // Actual Time of Departure

	private LocalTime ata; // Actual Time of Arrival

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private LogisticTaskStatus status;

	private Integer templateIdSnapshot; // 참조한 템플릿 ID (추적용)

	@Builder
	public LogisticTask(String name, LogisticType type, UserInfo worker, Ware ware,
			Location fromLocation, Location toLocation, Integer quantity,
			LocalDate scheduledDate, LocalTime etd, LocalTime eta,
			LogisticTaskStatus status, Integer templateIdSnapshot) {
		validateBasicFields(name, quantity, etd, eta);

		this.name = name;
		this.type = type;
		this.worker = worker;
		this.ware = ware;
		this.fromLocation = fromLocation;
		this.toLocation = toLocation;
		this.quantity = quantity;
		this.scheduledDate = scheduledDate;
		this.etd = etd;
		this.eta = eta;
		this.status = status != null ? status : LogisticTaskStatus.PENDING;
		this.templateIdSnapshot = templateIdSnapshot;
	}

	/**
	 * 물류 작업 전체 수정 (이름, 작업자, 수량, 시간 변경)
	 */
	public void modifyTask(String name, UserInfo worker, Integer quantity, LocalTime etd, LocalTime eta) {
		validateModificationPossible();
		validateBasicFields(name, quantity, etd, eta);

		this.name = name;
		this.worker = worker;
		this.quantity = quantity;
		this.etd = etd;
		this.eta = eta;
		this.status = LogisticTaskStatus.PENDING; // 수정 시 PENDING으로 초기화
	}

	/**
	 * 물류 작업 부분 수정 (작업자, 시간만 변경)
	 */
	public void modifyTask(UserInfo worker, LocalTime etd, LocalTime eta) {
		validateModificationPossible();

		if (etd == null || eta == null) {
			throw new IllegalArgumentException("ETD 및 ETA는 필수입니다.");
		}
		if (etd.isAfter(eta)) {
			throw new IllegalArgumentException("출발 예정시간은 도착 예정시간보다 빨라야 합니다.");
		}

		this.worker = worker;
		this.etd = etd;
		this.eta = eta;
		this.status = LogisticTaskStatus.PENDING; // 수정 시 PENDING으로 초기화
	}

	/**
	 * 물류 작업 시작 처리
	 */
	public void initiateTask(LocalTime atd) {
		if (!canInitiate()) {
			throw LogisticTaskException.validation("현재 상태(" + this.status + ")에서는 작업을 시작할 수 없습니다.");
		}

		changeStatus(LogisticTaskStatus.INITIATED);
		this.atd = atd != null ? atd : LocalTime.now();
	}

	/**
	 * 물류 작업 완료 처리
	 */
	public void completeTask(LocalTime ata) {
		if (!canComplete()) {
			throw LogisticTaskException.validation("현재 상태(" + this.status + ")에서는 작업을 완료할 수 없습니다.");
		}

		changeStatus(LogisticTaskStatus.COMPLETED);
		this.ata = ata != null ? ata : LocalTime.now();
	}

	/**
	 * 물류 작업 시작 지연 처리
	 */
	public void delayInitiation() {
		if (!canDelayInitiation()) {
			throw LogisticTaskException.validation("현재 상태(" + this.status + ")에서는 시작 지연 처리를 할 수 없습니다.");
		}

		changeStatus(LogisticTaskStatus.INITIATE_DELAYED);
		this.atd = null; // 지연 시 실제 시작 시간 초기화
	}

	/**
	 * 물류 작업 완료 지연 처리
	 */
	public void delayCompletion() {
		if (!canDelayCompletion()) {
			throw LogisticTaskException.validation("현재 상태(" + this.status + ")에서는 완료 지연 처리를 할 수 없습니다.");
		}

		changeStatus(LogisticTaskStatus.COMPLETE_DELAYED);
		this.ata = null; // 지연 시 실제 완료 시간 초기화
	}

	/**
	 * 물류 작업 취소 처리
	 */
	public void cancelTask() {
		if (!isCancellable()) {
			throw LogisticTaskException.taskCancellationNotAllowedEx(this.status.toString());
		}

		changeStatus(LogisticTaskStatus.CANCELLED);
	}

	/**
	 * 물류 작업 실패 처리
	 */
	public void failTask() {
		if (!canFail()) {
			throw LogisticTaskException.taskFailureNotAllowedEx(this.status.toString());
		}

		changeStatus(LogisticTaskStatus.FAILED);
	}

	// ===== 상태 확인 메서드들 =====

	/**
	 * 작업 수정 가능 여부 확인
	 */
	public boolean isModifiable() {
		return this.status == LogisticTaskStatus.PENDING ||
				this.status == LogisticTaskStatus.INITIATE_DELAYED;
	}

	/**
	 * 작업 취소 가능 여부 확인
	 */
	public boolean isCancellable() {
		return this.status == LogisticTaskStatus.PENDING ||
				this.status == LogisticTaskStatus.INITIATE_DELAYED;
	}

	/**
	 * 작업 시작 가능 여부 확인
	 */
	public boolean canInitiate() {
		return this.status == LogisticTaskStatus.PENDING ||
				this.status == LogisticTaskStatus.INITIATE_DELAYED;
	}

	/**
	 * 작업 완료 가능 여부 확인
	 */
	public boolean canComplete() {
		return this.status == LogisticTaskStatus.INITIATED ||
				this.status == LogisticTaskStatus.COMPLETE_DELAYED;
	}

	/**
	 * 시작 지연 처리 가능 여부 확인
	 */
	public boolean canDelayInitiation() {
		return this.status == LogisticTaskStatus.PENDING;
	}

	/**
	 * 완료 지연 처리 가능 여부 확인
	 */
	public boolean canDelayCompletion() {
		return this.status == LogisticTaskStatus.INITIATED;
	}

	/**
	 * 실패 처리 가능 여부 확인
	 */
	public boolean canFail() {
		return this.status == LogisticTaskStatus.INITIATED;
	}

	/**
	 * 작업이 진행 중인지 확인 (재고에 영향을 주는 상태)
	 */
	public boolean isInProgress() {
		return this.status == LogisticTaskStatus.INITIATED ||
				this.status == LogisticTaskStatus.COMPLETE_DELAYED;
	}

	/**
	 * 작업이 완료되었는지 확인
	 */
	public boolean isCompleted() {
		return this.status == LogisticTaskStatus.COMPLETED;
	}

	/**
	 * 작업이 종료되었는지 확인 (완료, 취소, 실패)
	 */
	public boolean isFinished() {
		return this.status == LogisticTaskStatus.COMPLETED ||
				this.status == LogisticTaskStatus.CANCELLED ||
				this.status == LogisticTaskStatus.FAILED;
	}

	// ===== Private 메서드들 =====

	private void changeStatus(LogisticTaskStatus newStatus) {
		this.status = newStatus;
	}

	private void validateModificationPossible() {
		if (!isModifiable()) {
			throw LogisticTaskException.taskNotModifiableEx(this.status.toString());
		}
	}

	private void validateBasicFields(String name, Integer quantity, LocalTime etd, LocalTime eta) {
		if (name == null || name.trim().isEmpty()) {
			throw new IllegalArgumentException("작업명은 필수입니다.");
		}
		if (name.length() > 255) {
			throw new IllegalArgumentException("작업명은 255자를 초과할 수 없습니다.");
		}
		if (quantity == null || quantity <= 0) {
			throw new IllegalArgumentException("수량은 0보다 커야 합니다.");
		}
		if (etd == null || eta == null) {
			throw new IllegalArgumentException("ETD 및 ETA는 필수입니다.");
		}
		if (etd.isAfter(eta)) {
			throw new IllegalArgumentException("출발 예정시간은 도착 예정시간보다 빨라야 합니다.");
		}
	}
}