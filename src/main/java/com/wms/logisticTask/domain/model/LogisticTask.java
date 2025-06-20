package com.wms.logisticTask.domain.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import com.wms.applicationInfra.domain.BaseEntity;
import com.wms.location.domain.model.Location;
import com.wms.logisticTemplate.domain.model.LogisticType;
import com.wms.userInfo.domain.model.UserInfo;
import com.wms.ware.domain.model.Ware;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "logistic_task")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LogisticTask extends BaseEntity {

	@Size(max = 255)
	@Column(name = "name", nullable = false)
	private String name;

	@Enumerated(EnumType.STRING)
	@Column(name = "type", nullable = false)
	private LogisticType type;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "worker_id", nullable = false)
	private UserInfo worker;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "ware_id", nullable = false)
	private Ware ware;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "from_location_id", nullable = false)
	private Location fromLocation;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "to_location_id", nullable = false)
	private Location toLocation;

	@Column(name = "quantity", nullable = false)
	private Integer quantity;

	@Column(name = "scheduled_date", nullable = false)
	private LocalDate scheduledDate;

	@Column(name = "etd", nullable = false)
	private LocalTime etd;

	@Column(name = "eta", nullable = false)
	private LocalTime eta;

	@Column(name = "atd", nullable = false)
	private LocalTime atd;

	@Column(name = "ata", nullable = false)
	private LocalTime ata;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false)
	private LogisticTaskStatus status;

	@Column(name = "template_id_snapshot")
	private Integer templateIdSnapshot;

	@Builder
	public LogisticTask(String name, LogisticType type, UserInfo worker, Ware ware, Location fromLocation, Location toLocation, Integer quantity, LocalDate scheduledDate, LocalTime etd, LocalTime eta, Integer templateIdSnapshot) {
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
		this.atd = null;
		this.ata = null;
		this.status = LogisticTaskStatus.PENDING;
		this.templateIdSnapshot = templateIdSnapshot;
	}

	public void update(String name, UserInfo worker, Integer quantity, LocalTime etd, LocalTime eta) {
		if (name == null || name.isBlank()) {
			throw new IllegalArgumentException("이름은 필수입니다.");
		}
		if (quantity == null || quantity <= 0) {
			throw new IllegalArgumentException("수량은 0보다 커야 합니다.");
		}
		if (etd == null || eta == null) {
			throw new IllegalArgumentException("ETD 및 ETA는 필수입니다.");
		}

		if (!(this.status == LogisticTaskStatus.PENDING || this.status == LogisticTaskStatus.INITIATE_DELAYED )) {
			throw new IllegalArgumentException("change task only in (PENDING, INITIATE_DELAYED) status. current status is " + this.status + " .");
		}

		this.name = name;
		this.worker = worker;
		this.quantity = quantity;
		this.etd = etd;
		this.eta = eta;
	}

	public void modifyTask(UserInfo worker, LocalTime etd, LocalTime eta) {
		if (!(this.status == LogisticTaskStatus.PENDING || this.status == LogisticTaskStatus.INITIATE_DELAYED )) {
			throw new IllegalArgumentException("change task only in (PENDING, INITIATE_DELAYED) status. current status is " + this.status + " .");
		}
		this.worker = worker;
		this.etd = etd;
		this.eta = eta;
		this.status = LogisticTaskStatus.PENDING;
	}

	public void initiateTask(LocalTime atd) {
		changeStatus(LogisticTaskStatus.INITIATED);
		this.atd = atd;
	}

	public void completeTask(LocalTime ata) {
		changeStatus(LogisticTaskStatus.COMPLETED);
		this.ata = ata;
	}

	public void deleyInitateTask() {
		changeStatus(LogisticTaskStatus.INITIATE_DELAYED);
		this.atd = null;
	}

	public void deleyCompleteTask() {
		changeStatus(LogisticTaskStatus.COMPLETE_DELAYED);
		this.atd = null;
	}

	public void cancleTask() {
		if (!(this.status == LogisticTaskStatus.PENDING || this.status == LogisticTaskStatus.INITIATE_DELAYED)) {
			throw new IllegalArgumentException("cancel task only in (PENDING, INITIATE_DELAYED) status. current status is " + this.status + " .");
		}
		changeStatus(LogisticTaskStatus.CANCELLED);
	}

	public void failTask() {
		if (!(this.status == LogisticTaskStatus.INITIATED)) {
			throw new IllegalArgumentException("fail task only in (INITIATED) status. current status is " + this.status + " .");
		}
		changeStatus(LogisticTaskStatus.FAILED);
	}

	private void changeStatus(LogisticTaskStatus status) {
		this.status = status;
	}
}