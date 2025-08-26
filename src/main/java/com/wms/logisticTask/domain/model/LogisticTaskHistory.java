package com.wms.logisticTask.domain.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import com.wms.applicationInfra.domain.BaseEntity;
import com.wms.logisticTemplate.domain.model.LogisticType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Entity
@Table(name = "logistic_task_history")
@Slf4j
public class LogisticTaskHistory {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Long id;

	@Column(name = "original_task_id", nullable = false)
	private Long originalTaskId;

	@Column(name = "name", nullable = false)
	private String name;

	@Enumerated(EnumType.STRING)
	@Column(name = "type", nullable = false)
	private LogisticType type;

	@Column(name = "worker_id", nullable = false)
	private Long workerId;

	@Column(name = "worker_name", nullable = false, length = 100)
	private String workerName;

	@Column(name = "ware_id", nullable = false)
	private Long wareId;

	@Column(name = "ware_name", nullable = false, length = 100)
	private String wareName;

	@Column(name = "from_location_id", nullable = false)
	private Long fromLocationId;

	@Column(name = "from_location_name", nullable = false, length = 100)
	private String fromLocationName;

	@Column(name = "to_location_id", nullable = false)
	private Long toLocationId;

	@Column(name = "to_location_name", nullable = false, length = 100)
	private String toLocationName;

	@Column(name = "quantity", nullable = false)
	private Integer quantity;

	@Column(name = "scheduled_date", nullable = false)
	private LocalDate scheduledDate;

	@Column(name = "etd", nullable = false)
	private LocalTime etd;

	@Column(name = "eta", nullable = false)
	private LocalTime eta;

	@Column(name = "atd")
	private LocalTime atd;

	@Column(name = "ata")
	private LocalTime ata;

	@Enumerated(EnumType.STRING)
	@Column(name = "final_status", nullable = false)
	private LogisticTaskStatus finalStatus;

	@Enumerated(EnumType.STRING)
	@Column(name = "original_status", nullable = false)
	private LogisticTaskStatus originalStatus;

	@Column(name = "template_id_snapshot")
	private Long templateIdSnapshot;

	@Column(name = "settlement_date", nullable = false)
	private LocalDate settlementDate;

	/**
	 * LogisticTask에서 히스토리로 변환하는 팩토리 메서드
	 */
	public static LogisticTaskHistory fromLogisticTask(LogisticTask task, LocalDate settlementDate) {
		LogisticTaskStatus finalStatus = determineFinalStatus(task.getStatus());
		
		log.debug("Converting LogisticTask ID: {} from status {} to final status {}", 
				task.getId(), task.getStatus(), finalStatus);
		
		return LogisticTaskHistory.builder()
				.originalTaskId(task.getId())
				.name(task.getName())
				.type(task.getType())
				.workerId(task.getWorker().getId())
				.workerName(task.getWorker().getName())
				.wareId(task.getWare().getId())
				.wareName(task.getWare().getName())
				.fromLocationId(task.getFromLocation().getId())
				.fromLocationName(task.getFromLocation().getName())
				.toLocationId(task.getToLocation().getId())
				.toLocationName(task.getToLocation().getName())
				.quantity(task.getQuantity())
				.scheduledDate(task.getScheduledDate())
				.etd(task.getEtd())
				.eta(task.getEta())
				.atd(task.getAtd())
				.ata(task.getAta())
				.finalStatus(finalStatus)
				.originalStatus(task.getStatus())
				.templateIdSnapshot(task.getTemplateIdSnapshot() != null ? 
					Long.valueOf(task.getTemplateIdSnapshot()) : null)
				.settlementDate(settlementDate)
				.build();
	}
	
	/**
	 * 상태별 최종 상태 결정 로직
	 */
	private static LogisticTaskStatus determineFinalStatus(LogisticTaskStatus originalStatus) {
		return switch (originalStatus) {
			case PENDING, INITIATE_DELAYED -> LogisticTaskStatus.EXPIRED;
			case INITIATED, COMPLETE_DELAYED, FAILED -> LogisticTaskStatus.FAILED;
			default -> originalStatus;
		};
	}

}
