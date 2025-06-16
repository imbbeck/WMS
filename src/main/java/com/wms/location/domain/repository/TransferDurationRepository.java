package com.wms.location.domain.repository;

import com.wms.location.domain.model.Location;
import com.wms.location.domain.model.TransferDuration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TransferDurationRepository extends JpaRepository<TransferDuration, Long> {
    Optional<TransferDuration> findByFromLocationAndToLocation(Location fromLocation, Location toLocation);

	Optional<TransferDuration> findByFromLocationIdAndToLocationId(Long fromLocationId, Long fromLocationId1);
}