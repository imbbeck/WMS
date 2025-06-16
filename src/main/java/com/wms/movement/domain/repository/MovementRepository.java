package com.wms.movement.domain.repository;

import com.wms.movement.domain.model.Movement;
import com.wms.movement.domain.model.MovementStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MovementRepository extends JpaRepository<Movement, Long> {
    
    List<Movement> findByStatus(MovementStatus status);
    
    @Query("SELECT m FROM Movement m WHERE m.ware.id = :wareId AND m.status = :status")
    List<Movement> findByWareIdAndStatus(@Param("wareId") Long wareId, @Param("status") MovementStatus status);
    
    @Query("SELECT m FROM Movement m WHERE m.fromLocation.id = :locationId OR m.toLocation.id = :locationId")
    List<Movement> findByLocationId(@Param("locationId") Long locationId);

	List<Movement> findByWareId(Long wareId);

    List<Movement> findByFromLocationIdOrToLocationId(Long fromLocationId, Long toLocationId);

    List<Movement> getMovementsByFromLocation(Long fromLocationId);

    List<Movement> getMovementsByToLocation(Long toLocationId);

    List<Movement> findAllWithDetails();

    List<Movement> findByStatusWithDetails(MovementStatus movementStatus);

    List<Movement> findByWareIdWithDetails(Long wareId);

    List<Movement> getMovementsByFromLocationWithDetails(Long fromLocationId);

    List<Movement> getMovementsByToLocationWithDetails(Long toLocationId);
}