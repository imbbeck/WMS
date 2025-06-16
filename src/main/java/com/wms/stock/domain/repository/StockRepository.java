package com.wms.stock.domain.repository;

import com.wms.stock.domain.model.Stock;
import com.wms.ware.domain.model.Ware;
import com.wms.location.domain.model.Location;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StockRepository extends JpaRepository<Stock, Long> {
    
    @Query("SELECT s FROM Stock s WHERE s.ware.id = :wareId AND s.location.id = :locationId")
    Optional<Stock> findByWareIdAndLocationId(@Param("wareId") Long wareId, @Param("locationId") Long locationId);
    
    @Query("SELECT SUM(s.quantity) FROM Stock s WHERE s.ware.id = :wareId")
    Integer getTotalQuantityByWareId(@Param("wareId") Long wareId);

    List<Stock> findByWareId(Long wareId);
    List<Stock> findByLocationId(Long locationId);
    boolean existsByWareAndLocation(Ware ware, Location location);

    @Query("SELECT SUM(s.quantity) FROM Stock s WHERE s.ware.id = :wareId")
    Integer sumQuantityByWareId(@Param("wareId") Long wareId);

    @Query("SELECT SUM(s.quantity) FROM Stock s WHERE s.location.id = :locationId")
    Integer sumQuantityByLocationId(@Param("locationId") Long locationId);
} 