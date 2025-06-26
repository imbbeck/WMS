package com.wms.stock.domain.repository;

import com.wms.stock.domain.model.Stock;
import com.wms.stock.domain.model.StockKey;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StockRepository extends JpaRepository<Stock, Long> {

	/**
	 * 특정 창고의 특정 물품 재고 조회
	 */
	Optional<Stock> findByKey(StockKey key);

	/**
	 * 특정 창고의 모든 재고 조회
	 */
	List<Stock> findAllByKeyWarehouseId(Long warehouseId);

	/**
	 * 특정 물품의 모든 창고별 재고 조회
	 */
	List<Stock> findAllByKeyWareId(Long wareId);

	/**
	 * 재고가 있는 창고들의 특정 물품 재고 조회
	 */
	List<Stock> findAllByKeyWareIdAndQuantityGreaterThan(Long wareId, int quantity);

	/**
	 * 재고 부족 상태인 항목 조회 (threshold 이하)
	 */
	@Query("SELECT s FROM Stock s WHERE s.quantity <= :threshold")
	List<Stock> findAllByQuantityLessThanEqual(@Param("threshold") Integer threshold);

	/**
	 * 특정 창고의 총 재고 수량 조회
	 */
	@Query("SELECT COALESCE(SUM(s.quantity), 0) FROM Stock s WHERE s.key.warehouseId = :warehouseId")
	Long getTotalQuantityByWarehouse(@Param("warehouseId") Long warehouseId);

	/**
	 * 특정 물품의 전체 재고 수량 조회
	 */
	@Query("SELECT COALESCE(SUM(s.quantity), 0) FROM Stock s WHERE s.key.wareId = :wareId")
	Long getTotalQuantityByWare(@Param("wareId") Long wareId);

	/**
	 * 전체 재고가 0인 항목들 조회
	 */
	List<Stock> findAllByQuantity(int quantity);

	/**
	 * 특정 창고의 재고가 0인 항목들 조회
	 */
	List<Stock> findAllByKey_WarehouseIdAndQuantity(Long warehouseId, int quantity);

	/**
	 * 복합 조건 재고 조회 (JPQL 사용)
	 */
	@Query("SELECT s FROM Stock s " +
			"WHERE (:wareId IS NULL OR s.key.wareId = :wareId) " +
			"AND (:warehouseId IS NULL OR s.key.warehouseId = :warehouseId) " +
			"AND (:minQuantity IS NULL OR s.quantity >= :minQuantity) " +
			"AND (:maxQuantity IS NULL OR s.quantity <= :maxQuantity)")
	List<Stock> findBySearchCriteria(
			@Param("wareId") Long wareId,
			@Param("warehouseId") Long warehouseId,
			@Param("minQuantity") Integer minQuantity,
			@Param("maxQuantity") Integer maxQuantity
	);

	/**
	 * 창고별 재고 요약 정보 조회
	 */
	@Query("SELECT s.key.warehouseId as warehouseId, " +
			"SUM(s.quantity) as totalQuantity, " +
			"COUNT(DISTINCT s.key.wareId) as wareTypeCount " +
			"FROM Stock s " +
			"WHERE s.key.warehouseId = :warehouseId " +
			"GROUP BY s.key.warehouseId")
	Optional<WarehouseStockSummary> findWarehouseStockSummary(@Param("warehouseId") Long warehouseId);

	/**
	 * 모든 창고별 재고 요약 정보 조회
	 */
	@Query("SELECT s.key.warehouseId as warehouseId, " +
			"SUM(s.quantity) as totalQuantity, " +
			"COUNT(DISTINCT s.key.wareId) as wareTypeCount " +
			"FROM Stock s " +
			"GROUP BY s.key.warehouseId")
	List<WarehouseStockSummary> findAllWarehouseStockSummaries();

	/**
	 * 물품별 재고 요약 정보 조회
	 */
	@Query("SELECT s.key.wareId as wareId, " +
			"SUM(s.quantity) as totalQuantity, " +
			"COUNT(DISTINCT s.key.warehouseId) as warehouseCount " +
			"FROM Stock s " +
			"WHERE s.key.wareId = :wareId " +
			"GROUP BY s.key.wareId")
	Optional<WareStockSummary> findWareStockSummary(@Param("wareId") Long wareId);

	/**
	 * 모든 물품별 재고 요약 정보 조회
	 */
	@Query("SELECT s.key.wareId as wareId, " +
			"SUM(s.quantity) as totalQuantity, " +
			"COUNT(DISTINCT s.key.warehouseId) as warehouseCount " +
			"FROM Stock s " +
			"GROUP BY s.key.wareId")
	List<WareStockSummary> findAllWareStockSummaries();

	/**
	 * 특정 창고의 총 파레트 수량 조회
	 */
	@Query("SELECT COALESCE(SUM(s.quantity), 0) FROM Stock s WHERE s.key.warehouseId = :warehouseId")
	Integer getTotalPaletteCountByWarehouseId(@Param("warehouseId") Long warehouseId);

	/**
	 * 창고별 재고 요약을 위한 인터페이스
	 */
	interface WarehouseStockSummary {
		Long getWarehouseId();
		Long getTotalQuantity();
		Long getWareTypeCount();
	}

	/**
	 * 물품별 재고 요약을 위한 인터페이스
	 */
	interface WareStockSummary {
		Long getWareId();
		Long getTotalQuantity();
		Long getWarehouseCount();
	}

	/**
	 * 배치 파티셔닝 용 메소드
	 */
	@Query("SELECT MIN(s.id) FROM Stock s")
	Long findMinId();

	@Query("SELECT MAX(s.id) FROM Stock s")
	Long findMaxId();

	@Query("SELECT COUNT(s) FROM Stock s")
	long count();

	Page<Stock> findByIdBetween(Long minId, Long maxId, Pageable pageable);

	boolean existsByKeyWarehouseId(Long locationId);

	/// ////


}