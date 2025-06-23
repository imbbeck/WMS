package com.wms.stock.domain.repository;

import com.wms.stock.domain.model.Stock;
import com.wms.stock.dto.StockDTO;
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
	Optional<Stock> findByWarehouseIdAndWareId(Long warehouseId, Long wareId);

	/**
	 * 특정 창고의 모든 재고 조회
	 */
	List<Stock> findAllByWarehouseId(Long warehouseId);

	/**
	 * 특정 물품의 모든 창고별 재고 조회
	 */
	List<Stock> findAllByWareId(Long wareId);

	/**
	 * 재고가 있는 창고들의 특정 물품 재고 조회
	 */
	List<Stock> findAllByWareIdAndQuantityGreaterThan(Long wareId, int quantity);


	/**
	 * 재고 부족 상태인 항목 조회 (threshold 이하)
	 */
	@Query("SELECT s FROM Stock s WHERE s.quantity <= :threshold")
	List<Stock> findAllByQuantityLessThanEqual(@Param("threshold") Integer threshold);

	/**
	 * 특정 창고의 총 재고 수량 조회
	 */
	@Query("SELECT COALESCE(SUM(s.quantity), 0) FROM Stock s WHERE s.warehouseId = :warehouseId")
	Long getTotalQuantityByWarehouse(@Param("warehouseId") Long warehouseId);

	/**
	 * 특정 물품의 전체 재고 수량 조회
	 */
	@Query("SELECT COALESCE(SUM(s.quantity), 0) FROM Stock s WHERE s.wareId = :wareId")
	Long getTotalQuantityByWare(@Param("wareId") Long wareId);

	/**
	 * 전체 재고가 0인 항목들 조회
	 */
	List<Stock> findAllByQuantity(int quantity);

	/**
	 * 특정 창고의 재고가 0인 항목들 조회
	 */
	List<Stock> findAllByWarehouseIdAndQuantity(Long warehouseId, int quantity);

	/**
	 * 복합 조건 재고 조회 (JPQL 사용)
	 */
	@Query("SELECT s FROM Stock s " +
			"WHERE (:wareId IS NULL OR s.wareId = :wareId) " +
			"AND (:warehouseId IS NULL OR s.warehouseId = :warehouseId) " +
			"AND (:minQuantity IS NULL OR s.quantity >= :minQuantity) " +
			"AND (:maxQuantity IS NULL OR s.quantity <= :maxQuantity)")
	Page<Stock> findBySearchCriteria(@Param("wareId") Long wareId,
			@Param("warehouseId") Long warehouseId,
			@Param("minQuantity") Integer minQuantity,
			@Param("maxQuantity") Integer maxQuantity,
			Pageable pageable);

	/**
	 * 창고별 재고 요약 정보 조회
	 */
	@Query("SELECT s.warehouseId as warehouseId, " +
			"SUM(s.quantity) as totalQuantity, " +
			"COUNT(DISTINCT s.wareId) as wareTypeCount " +
			"FROM Stock s " +
			"WHERE s.warehouseId = :warehouseId " +
			"GROUP BY s.warehouseId")
	Optional<WarehouseStockSummary> findWarehouseStockSummary(@Param("warehouseId") Long warehouseId);

	/**
	 * 모든 창고별 재고 요약 정보 조회
	 */
	@Query("SELECT s.warehouseId as warehouseId, " +
			"SUM(s.quantity) as totalQuantity, " +
			"COUNT(DISTINCT s.wareId) as wareTypeCount " +
			"FROM Stock s " +
			"GROUP BY s.warehouseId")
	List<WarehouseStockSummary> findAllWarehouseStockSummaries();

	/**
	 * 물품별 재고 요약 정보 조회
	 */
	@Query("SELECT s.wareId as wareId, " +
			"SUM(s.quantity) as totalQuantity, " +
			"COUNT(DISTINCT s.warehouseId) as warehouseCount " +
			"FROM Stock s " +
			"WHERE s.wareId = :wareId " +
			"GROUP BY s.wareId")
	Optional<WareStockSummary> findWareStockSummary(@Param("wareId") Long wareId);

	/**
	 * 모든 물품별 재고 요약 정보 조회
	 */
	@Query("SELECT s.wareId as wareId, " +
			"SUM(s.quantity) as totalQuantity, " +
			"COUNT(DISTINCT s.warehouseId) as warehouseCount " +
			"FROM Stock s " +
			"GROUP BY s.wareId")
	List<WareStockSummary> findAllWareStockSummaries();

	/**
	 * 특정 창고의 총 파레트 수량 조회
	 */
	@Query("SELECT COALESCE(SUM(s.quantity), 0) FROM Stock s WHERE s.warehouseId = :warehouseId")
	Integer getTotalPaletteCountByWarehouseId(@Param("warehouseId") Long warehouseId);

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

	boolean existsByWarehouseId(Long locationId);

//    @Query("SELECT s FROM Stock s WHERE s.ware.id = :wareId AND s.warehouse.id = :locationId")
//    Optional<Stock> findByWareIdAndLocationId(@Param("wareId") Long wareId, @Param("locationId") Long locationId);
//
//    @Query("SELECT SUM(s.quantity) FROM Stock s WHERE s.ware.id = :wareId")
//    Integer getTotalQuantityByWareId(@Param("wareId") Long wareId);
//
//    List<Stock> findByWareId(Long wareId);
//    List<Stock> findByWarehouseId(Long locationId);
//    boolean existsByWareAndWarehouse(Ware ware, Location location);
//
//    @Query("SELECT SUM(s.quantity) FROM Stock s WHERE s.ware.id = :wareId")
//    Integer sumQuantityByWareId(@Param("wareId") Long wareId);
//
//    @Query("SELECT SUM(s.quantity) FROM Stock s WHERE s.warehouse.id = :locationId")
//    Integer sumQuantityByWarehouseId(@Param("locationId") Long locationId);

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
}