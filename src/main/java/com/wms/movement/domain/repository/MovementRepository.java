package com.wms.movement.domain.repository;

import com.wms.location.domain.model.Location;
import com.wms.movement.domain.model.Movement;
import com.wms.movement.domain.model.MovementStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MovementRepository extends JpaRepository<Movement, Long> {

    List<Movement> findAllByStatus(MovementStatus movementStatus);

    List<Movement> findAllByWareId(Long wareId);

    @Query("SELECT m FROM Movement m WHERE m.fromLocation.id = :fromLocationId")
    List<Movement> findByFromLocation(@Param("fromLocationId") Long fromLocationId);

    @Query("SELECT m FROM Movement m WHERE m.toLocation.id = :toLocationId")
    List<Movement> findByToLocation(@Param("toLocationId") Long toLocationId);
}