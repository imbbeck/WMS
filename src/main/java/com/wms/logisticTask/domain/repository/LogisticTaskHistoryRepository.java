package com.wms.logisticTask.domain.repository;

import com.wms.logisticTask.domain.model.LogisticTaskHistory;
import com.wms.logisticTask.domain.model.LogisticTaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface LogisticTaskHistoryRepository extends JpaRepository<LogisticTaskHistory, Long> {
    
    /**
     * 특정 결산일의 히스토리 조회
     */
    List<LogisticTaskHistory> findBySettlementDate(LocalDate settlementDate);
    
    /**
     * 특정 작업자의 히스토리 조회
     */
    List<LogisticTaskHistory> findByWorkerIdAndSettlementDateBetween(
            Long workerId, LocalDate startDate, LocalDate endDate);
    
    /**
     * 상태별 히스토리 통계
     */
    @Query("SELECT h.finalStatus, COUNT(h) FROM LogisticTaskHistory h " +
           "WHERE h.settlementDate = :settlementDate GROUP BY h.finalStatus")
    List<Object[]> findStatusStatsBySettlementDate(@Param("settlementDate") LocalDate settlementDate);
    
    /**
     * 특정 기간의 완료율 통계
     */
    @Query("SELECT COUNT(h) FROM LogisticTaskHistory h " +
           "WHERE h.settlementDate BETWEEN :startDate AND :endDate " +
           "AND h.finalStatus = 'COMPLETED'")
    Long countCompletedTasksBetween(@Param("startDate") LocalDate startDate, 
                                   @Param("endDate") LocalDate endDate);
    
    /**
     * 특정 기간의 전체 작업 수
     */
    @Query("SELECT COUNT(h) FROM LogisticTaskHistory h " +
           "WHERE h.settlementDate BETWEEN :startDate AND :endDate")
    Long countAllTasksBetween(@Param("startDate") LocalDate startDate, 
                             @Param("endDate") LocalDate endDate);
    
    /**
     * 특정 작업자의 특정 기간 완료율 통계
     */
    @Query("SELECT COUNT(h) FROM LogisticTaskHistory h " +
           "WHERE h.workerId = :workerId " +
           "AND h.settlementDate BETWEEN :startDate AND :endDate " +
           "AND h.finalStatus = 'COMPLETED'")
    Long countCompletedTasksByWorkerBetween(@Param("workerId") Long workerId,
                                           @Param("startDate") LocalDate startDate, 
                                           @Param("endDate") LocalDate endDate);
    
    /**
     * 특정 물품의 히스토리 조회
     */
    List<LogisticTaskHistory> findByWareIdAndSettlementDateBetween(
            Long wareId, LocalDate startDate, LocalDate endDate);
    
    /**
     * 특정 장소 관련 히스토리 조회
     */
    @Query("SELECT h FROM LogisticTaskHistory h " +
           "WHERE (h.fromLocationId = :locationId OR h.toLocationId = :locationId) " +
           "AND h.settlementDate BETWEEN :startDate AND :endDate " +
           "ORDER BY h.settlementDate DESC")
    List<LogisticTaskHistory> findByLocationAndSettlementDateBetween(
            @Param("locationId") Long locationId,
            @Param("startDate") LocalDate startDate, 
            @Param("endDate") LocalDate endDate);

    
    /**
     * 특정 결산일의 원본 작업 ID 목록 조회 (중복 체크용)
     */
    @Query("SELECT h.originalTaskId FROM LogisticTaskHistory h " +
           "WHERE h.settlementDate = :settlementDate")
    List<Long> findOriginalTaskIdsBySettlementDate(@Param("settlementDate") LocalDate settlementDate);
}
