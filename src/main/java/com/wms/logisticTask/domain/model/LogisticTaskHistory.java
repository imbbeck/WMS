package com.wms.logisticTask.domain.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import com.wms.logisticTemplate.domain.model.LogisticType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

@Getter
@Setter
@Entity
@Table(name = "logistic_task_history")
public class LogisticTaskHistory {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", nullable = false)
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

	@Column(name = "template_id_snapshot")
	private Integer templateIdSnapshot;

	@Column(name = "completed_at")
	private LocalDateTime completedAt;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	@ColumnDefault("CURRENT_TIMESTAMP(6)")
	@Column(name = "archived_at", nullable = false)
	private LocalDateTime archivedAt;

}