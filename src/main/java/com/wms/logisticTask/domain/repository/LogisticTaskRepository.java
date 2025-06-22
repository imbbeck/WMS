package com.wms.logisticTask.domain.repository;

import com.wms.logisticTask.domain.model.LogisticTask;
import com.wms.logisticTask.domain.model.LogisticTaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface LogisticTaskRepository extends JpaRepository<LogisticTask, Long> {

	/**
	 * 특정 날짜의 물류작업을 ETD 시간순으로 조회
	 */
	@Query("SELECT lt FROM LogisticTask lt " +
			"WHERE lt.scheduledDate = :scheduledDate " +
			"AND lt.status != :excludeStatus " +
			"ORDER BY lt.etd ASC")
	List<LogisticTask> findByScheduledDateOrderByEtd(
			@Param("scheduledDate") LocalDate scheduledDate,
			@Param("excludeStatus") LogisticTaskStatus excludeStatus);

	/**
	 * 특정 날짜의 물류작업을 ETD 시간순으로 조회 (모든 상태 포함)
	 */
	@Query("SELECT lt FROM LogisticTask lt " +
			"WHERE lt.scheduledDate = :scheduledDate " +
			"ORDER BY lt.etd ASC")
	List<LogisticTask> findByScheduledDateOrderByEtd(@Param("scheduledDate") LocalDate scheduledDate);

	/**
	 * 특정 날짜의 특정 창고에서 시작되는 물류작업을 시간순으로 조회
	 */
	@Query("SELECT lt FROM LogisticTask lt " +
			"WHERE lt.scheduledDate = :scheduledDate " +
			"AND lt.fromLocation.id = :locationId " +
			"AND lt.status != :excludeStatus " +
			"ORDER BY lt.etd ASC")
	List<LogisticTask> findByScheduledDateAndFromLocationOrderByEtd(
			@Param("scheduledDate") LocalDate scheduledDate,
			@Param("locationId") Long locationId,
			@Param("excludeStatus") LogisticTaskStatus excludeStatus);

	/**
	 * 특정 날짜의 특정 창고로 도착하는 물류작업을 시간순으로 조회
	 */
	@Query("SELECT lt FROM LogisticTask lt " +
			"WHERE lt.scheduledDate = :scheduledDate " +
			"AND lt.toLocation.id = :locationId " +
			"AND lt.status != :excludeStatus " +
			"ORDER BY lt.eta ASC")
	List<LogisticTask> findByScheduledDateAndToLocationOrderByEta(
			@Param("scheduledDate") LocalDate scheduledDate,
			@Param("locationId") Long locationId,
			@Param("excludeStatus") LogisticTaskStatus excludeStatus);

	/**
	 * 특정 날짜의 특정 물품에 대한 물류작업을 시간순으로 조회
	 */
	@Query("SELECT lt FROM LogisticTask lt " +
			"WHERE lt.scheduledDate = :scheduledDate " +
			"AND lt.ware.id = :wareId " +
			"AND lt.status != :excludeStatus " +
			"ORDER BY lt.etd ASC")
	List<LogisticTask> findByScheduledDateAndWareOrderByEtd(
			@Param("scheduledDate") LocalDate scheduledDate,
			@Param("wareId") Long wareId,
			@Param("excludeStatus") LogisticTaskStatus excludeStatus);

	/**
	 * 특정 작업보다 늦은 시간에 시작되는 같은 날짜의 작업들 조회
	 */
	@Query("SELECT lt FROM LogisticTask lt " +
			"WHERE lt.scheduledDate = :scheduledDate " +
			"AND lt.etd > :etd " +
			"AND lt.id != :excludeTaskId " +
			"AND lt.status != :excludeStatus " +
			"ORDER BY lt.etd ASC")
	List<LogisticTask> findSubsequentTasks(
			@Param("scheduledDate") LocalDate scheduledDate,
			@Param("etd") java.time.LocalTime etd,
			@Param("excludeTaskId") Long excludeTaskId,
			@Param("excludeStatus") LogisticTaskStatus excludeStatus);

	/**
	 * 특정 작업과 관련된 창고/물품 조합의 후속 작업들 조회
	 */
	@Query("SELECT lt FROM LogisticTask lt " +
			"WHERE lt.scheduledDate = :scheduledDate " +
			"AND lt.etd > :etd " +
			"AND lt.id != :excludeTaskId " +
			"AND lt.status != :excludeStatus " +
			"AND ((lt.fromLocation.id = :fromLocationId AND lt.ware.id = :wareId) " +
			"     OR (lt.toLocation.id = :toLocationId AND lt.ware.id = :wareId)) " +
			"ORDER BY lt.etd ASC")
	List<LogisticTask> findRelatedSubsequentTasks(
			@Param("scheduledDate") LocalDate scheduledDate,
			@Param("etd") java.time.LocalTime etd,
			@Param("excludeTaskId") Long excludeTaskId,
			@Param("fromLocationId") Long fromLocationId,
			@Param("toLocationId") Long toLocationId,
			@Param("wareId") Long wareId,
			@Param("excludeStatus") LogisticTaskStatus excludeStatus);

	/**
	 * 특정 상태의 작업들 조회
	 */
	List<LogisticTask> findByStatus(LogisticTaskStatus status);

	/**
	 * 특정 날짜 범위의 작업들 조회
	 */
	@Query("SELECT lt FROM LogisticTask lt " +
			"WHERE lt.scheduledDate BETWEEN :startDate AND :endDate " +
			"ORDER BY lt.scheduledDate ASC, lt.etd ASC")
	List<LogisticTask> findByScheduledDateBetween(
			@Param("startDate") LocalDate startDate,
			@Param("endDate") LocalDate endDate);

	/**
	 * 특정 작업자의 특정 날짜 작업들 조회
	 */
	@Query("SELECT lt FROM LogisticTask lt " +
			"WHERE lt.scheduledDate = :scheduledDate " +
			"AND lt.worker.id = :workerId " +
			"ORDER BY lt.etd ASC")
	List<LogisticTask> findByScheduledDateAndWorker(
			@Param("scheduledDate") LocalDate scheduledDate,
			@Param("workerId") Long workerId);

	/**
	 * 특정 템플릿 ID로 생성된 작업들 조회
	 */
	@Query("SELECT lt FROM LogisticTask lt " +
			"WHERE lt.templateIdSnapshot = :templateId " +
			"ORDER BY lt.scheduledDate ASC, lt.etd ASC")
	List<LogisticTask> findByTemplateIdSnapshot(@Param("templateId") Integer templateId);
}