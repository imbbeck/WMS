package com.wms.logisticTask.domain.repository;

import com.wms.logisticTask.domain.model.LogisticTask;
import com.wms.logisticTask.domain.model.LogisticTaskStatus;
import com.wms.userInfo.domain.model.UserInfo;
import org.springframework.data.jpa.repository.EntityGraph;
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
	 * 기본 조회 시 연관 엔티티 모두 페치 (N+1 방지)
	 */
	@EntityGraph(attributePaths = {"worker", "ware", "fromLocation", "toLocation"})
	Optional<LogisticTask> findWithAllById(Long id);

	/**
	 * 모든 작업 조회 시 연관 엔티티 페치
	 */
	@EntityGraph(attributePaths = {"worker", "ware", "fromLocation", "toLocation"})
	List<LogisticTask> findAllBy();

	/**
	 * 특정 날짜의 물류작업을 ETD 시간순으로 조회 (연관 엔티티 포함)
	 */
	@EntityGraph(attributePaths = {"worker", "ware", "fromLocation", "toLocation"})
	@Query("SELECT lt FROM LogisticTask lt " +
			"WHERE lt.scheduledDate = :scheduledDate " +
			"AND lt.status != :excludeStatus " +
			"ORDER BY lt.etd ASC")
	List<LogisticTask> findByScheduledDateOrderByEtd(
			@Param("scheduledDate") LocalDate scheduledDate,
			@Param("excludeStatus") LogisticTaskStatus excludeStatus);

	/**
	 * 특정 날짜의 물류작업을 ETD 시간순으로 조회 (모든 상태 포함, 연관 엔티티 포함)
	 */
	@EntityGraph(attributePaths = {"worker", "ware", "fromLocation", "toLocation"})
	@Query("SELECT lt FROM LogisticTask lt " +
			"WHERE lt.scheduledDate = :scheduledDate " +
			"ORDER BY lt.etd ASC")
	List<LogisticTask> findByScheduledDateOrderByEtd(@Param("scheduledDate") LocalDate scheduledDate);

	/**
	 * 특정 날짜의 특정 창고에서 시작되는 물류작업을 시간순으로 조회 (페치 조인)
	 */
	@Query("SELECT lt FROM LogisticTask lt " +
			"JOIN FETCH lt.worker w " +
			"JOIN FETCH lt.ware ware " +
			"JOIN FETCH lt.fromLocation fl " +
			"JOIN FETCH lt.toLocation tl " +
			"WHERE lt.scheduledDate = :scheduledDate " +
			"AND fl.name = :fromLocationName " +
			"AND lt.status != :excludeStatus " +
			"ORDER BY lt.etd ASC")
	List<LogisticTask> findByScheduledDateAndFromLocationNameOrderByEtd(
			@Param("scheduledDate") LocalDate scheduledDate,
			@Param("fromLocationName") String fromLocationName,
			@Param("excludeStatus") LogisticTaskStatus excludeStatus);

	/**
	 * 특정 날짜의 특정 창고로 도착하는 물류작업을 시간순으로 조회 (페치 조인)
	 */
	@Query("SELECT lt FROM LogisticTask lt " +
			"JOIN FETCH lt.worker w " +
			"JOIN FETCH lt.ware ware " +
			"JOIN FETCH lt.fromLocation fl " +
			"JOIN FETCH lt.toLocation tl " +
			"WHERE lt.scheduledDate = :scheduledDate " +
			"AND tl.name = :toLocationName " +
			"AND lt.status != :excludeStatus " +
			"ORDER BY lt.eta ASC")
	List<LogisticTask> findByScheduledDateAndToLocationNameOrderByEta(
			@Param("scheduledDate") LocalDate scheduledDate,
			@Param("toLocationName") String toLocationName,
			@Param("excludeStatus") LogisticTaskStatus excludeStatus);

	/**
	 * 특정 날짜의 특정 물품에 대한 물류작업을 시간순으로 조회 (페치 조인)
	 */
	@Query("SELECT lt FROM LogisticTask lt " +
			"JOIN FETCH lt.worker w " +
			"JOIN FETCH lt.ware ware " +
			"JOIN FETCH lt.fromLocation fl " +
			"JOIN FETCH lt.toLocation tl " +
			"WHERE lt.scheduledDate = :scheduledDate " +
			"AND ware.name = :wareName " +
			"AND lt.status != :excludeStatus " +
			"ORDER BY lt.etd ASC")
	List<LogisticTask> findByScheduledDateAndWareNameOrderByEtd(
			@Param("scheduledDate") LocalDate scheduledDate,
			@Param("wareName") String wareName,
			@Param("excludeStatus") LogisticTaskStatus excludeStatus);

	/**
	 * 작업자명으로 검색 (페치 조인)
	 */
	@Query("SELECT lt FROM LogisticTask lt " +
			"JOIN FETCH lt.worker w " +
			"JOIN FETCH lt.ware ware " +
			"JOIN FETCH lt.fromLocation fl " +
			"JOIN FETCH lt.toLocation tl " +
			"WHERE w.name LIKE %:workerName% " +
			"ORDER BY lt.scheduledDate DESC, lt.etd DESC")
	List<LogisticTask> findByWorkerNameContaining(@Param("workerName") String workerName);

	/**
	 * 특정 작업보다 늦은 시간에 시작되는 같은 날짜의 작업들 조회
	 */
	@EntityGraph(attributePaths = {"worker", "ware", "fromLocation", "toLocation"})
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
	@EntityGraph(attributePaths = {"worker", "ware", "fromLocation", "toLocation"})
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
	 * 특정 상태의 작업들 조회 (연관 엔티티 포함)
	 */
	@EntityGraph(attributePaths = {"worker", "ware", "fromLocation", "toLocation"})
	List<LogisticTask> findByStatus(LogisticTaskStatus status);

	/**
	 * 특정 날짜 범위의 작업들 조회 (연관 엔티티 포함)
	 */
	@EntityGraph(attributePaths = {"worker", "ware", "fromLocation", "toLocation"})
	@Query("SELECT lt FROM LogisticTask lt " +
			"WHERE lt.scheduledDate BETWEEN :startDate AND :endDate " +
			"ORDER BY lt.scheduledDate ASC, lt.etd ASC")
	List<LogisticTask> findByScheduledDateBetween(
			@Param("startDate") LocalDate startDate,
			@Param("endDate") LocalDate endDate);

	/**
	 * 특정 작업자의 특정 날짜 작업들 조회 (연관 엔티티 포함)
	 */
	@EntityGraph(attributePaths = {"worker", "ware", "fromLocation", "toLocation"})
	@Query("SELECT lt FROM LogisticTask lt " +
			"WHERE lt.scheduledDate = :scheduledDate " +
			"AND lt.worker.id = :workerId " +
			"ORDER BY lt.etd ASC")
	List<LogisticTask> findByScheduledDateAndWorker(
			@Param("scheduledDate") LocalDate scheduledDate,
			@Param("workerId") Long workerId);

	/**
	 * 특정 템플릿 ID로 생성된 작업들 조회 (연관 엔티티 포함)
	 */
	@EntityGraph(attributePaths = {"worker", "ware", "fromLocation", "toLocation"})
	@Query("SELECT lt FROM LogisticTask lt " +
			"WHERE lt.templateIdSnapshot = :templateId " +
			"ORDER BY lt.scheduledDate ASC, lt.etd ASC")
	List<LogisticTask> findByTemplateIdSnapshot(@Param("templateId") Integer templateId);

	/**
	 * 특정 날짜의 물류작업을 여러 상태 제외하고 조회 (연관 엔티티 포함)
	 */
	@EntityGraph(attributePaths = {"worker", "ware", "fromLocation", "toLocation"})
	@Query("SELECT lt FROM LogisticTask lt " +
			"WHERE lt.scheduledDate = :scheduledDate " +
			"AND lt.status NOT IN :excludeStatuses " +
			"ORDER BY lt.etd ASC")
	List<LogisticTask> findByScheduledDateAndStatusNotIn(
			@Param("scheduledDate") LocalDate scheduledDate,
			@Param("excludeStatuses") List<LogisticTaskStatus> excludeStatuses);

	/**
	 * 특정 날짜의 모든 활성 물류작업 조회 (취소, 실패 제외, 연관 엔티티 포함)
	 */
	@EntityGraph(attributePaths = {"worker", "ware", "fromLocation", "toLocation"})
	@Query("SELECT lt FROM LogisticTask lt " +
			"WHERE lt.scheduledDate = :scheduledDate " +
			"AND lt.status NOT IN ('CANCELLED', 'FAILED') " +
			"ORDER BY lt.etd ASC")
	List<LogisticTask> findActiveTasksByScheduledDate(@Param("scheduledDate") LocalDate scheduledDate);

	/**
	 * 특정 날짜 범위의 완료된 작업들 조회 (재고 히스토리 추적용, 연관 엔티티 포함)
	 */
	@EntityGraph(attributePaths = {"worker", "ware", "fromLocation", "toLocation"})
	@Query("SELECT lt FROM LogisticTask lt " +
			"WHERE lt.scheduledDate BETWEEN :startDate AND :endDate " +
			"AND lt.status = 'COMPLETED' " +
			"ORDER BY lt.scheduledDate ASC, lt.etd ASC")
	List<LogisticTask> findCompletedTasksByDateRange(
			@Param("startDate") LocalDate startDate,
			@Param("endDate") LocalDate endDate);

	/**
	 * 특정 날짜에 완료된 작업들 중 특정 창고, 특정 물품과 연관된 작업들 조회 (재고 히스토리 추적용, 연관 엔티티 포함)
	 */
	@EntityGraph(attributePaths = {"worker", "ware", "fromLocation", "toLocation"})
	@Query("SELECT lt FROM LogisticTask lt " +
			"WHERE lt.scheduledDate = :targetDate " +
			"AND lt.status = 'COMPLETED' " +
			"AND (lt.fromLocation = :warehouseId OR lt.toLocation = :warehouseId) " +
			"AND lt.ware.id = :wareId " +
			"ORDER BY lt.scheduledDate ASC, lt.etd ASC")
	List<LogisticTask> findCompletedTasksByTargetDateAndStock(
			@Param("targetDate") LocalDate targetDate,
			@Param("warehouseId") Long warehouseId,
			@Param("wareId") Long wareId);

	/**
	 * 특정 창고의 총 재고량 조회 (캐시 fallback용)
	 */
	@Query("SELECT COALESCE(SUM(s.quantity), 0) FROM Stock s WHERE s.key.warehouseId = :warehouseId")
	Integer getTotalQuantityByWarehouse(@Param("warehouseId") Long warehouseId);

	/**
	 * 특정 창고의 파레트 총 개수 조회 (캐시 fallback용)
	 */
	@Query("SELECT COALESCE(SUM(s.quantity), 0) FROM Stock s WHERE s.key.warehouseId = :warehouseId")
	Integer getTotalPaletteCountByWarehouseId(@Param("warehouseId") Long warehouseId);

	/**
	 * 특정 장소가 포함된 작업 존재 여부 확인
	 */
	boolean existsByFromLocationIdOrToLocationId(Long locationId, Long locationId1);

	/**
	 * 특정 장소가 포함된 작업들 조회 (연관 엔티티 포함)
	 */
	@EntityGraph(attributePaths = {"worker", "ware", "fromLocation", "toLocation"})
	List<LogisticTask> findAllByFromLocationIdOrToLocationId(Long fromLocationId, Long fromLocationId1);

	/**
	 * 특정 작업자의 작업들 조회 (연관 엔티티 포함)
	 */
	@EntityGraph(attributePaths = {"worker", "ware", "fromLocation", "toLocation"})
	List<LogisticTask> findByWorker(UserInfo worker);

	/**
	 * 날짜별 물류 작업 조회 (연관 엔티티 포함)
	 */
	@EntityGraph(attributePaths = {"worker", "ware", "fromLocation", "toLocation"})
	List<LogisticTask> findByScheduledDate(LocalDate scheduledDate);

	/**
	 * 물품별 물류 작업 조회 (연관 엔티티 포함)
	 */
	@EntityGraph(attributePaths = {"worker", "ware", "fromLocation", "toLocation"})
	@Query("SELECT lt FROM LogisticTask lt WHERE lt.ware = :ware ORDER BY lt.scheduledDate DESC, lt.etd DESC")
	List<LogisticTask> findByWare(@Param("ware") com.wms.ware.domain.model.Ware ware);

	/**
	 * ID 기반 검색 - 페치 조인 사용
	 */
	@Query("SELECT DISTINCT lt FROM LogisticTask lt " +
			"JOIN FETCH lt.worker w " +
			"JOIN FETCH lt.ware ware " +
			"JOIN FETCH lt.fromLocation fl " +
			"JOIN FETCH lt.toLocation tl " +
			"WHERE (:taskName IS NULL OR LOWER(lt.name) LIKE LOWER(CONCAT('%', :taskName, '%'))) " +
			"AND (:workerId IS NULL OR w.id = :workerId) " +
			"AND (:wareId IS NULL OR ware.id = :wareId) " +
			"AND (:fromLocationId IS NULL OR fl.id = :fromLocationId) " +
			"AND (:toLocationId IS NULL OR tl.id = :toLocationId) " +
			"AND (:status IS NULL OR lt.status = :status) " +
			"AND (:startDate IS NULL OR lt.scheduledDate >= :startDate) " +
			"AND (:endDate IS NULL OR lt.scheduledDate <= :endDate) " +
			"ORDER BY lt.scheduledDate DESC, lt.etd DESC")
	List<LogisticTask> findByIdBasedSearchCriteria(
			@Param("taskName") String taskName,
			@Param("workerId") Long workerId,
			@Param("wareId") Long wareId,
			@Param("fromLocationId") Long fromLocationId,
			@Param("toLocationId") Long toLocationId,
			@Param("status") LogisticTaskStatus status,
			@Param("startDate") LocalDate startDate,
			@Param("endDate") LocalDate endDate);



}