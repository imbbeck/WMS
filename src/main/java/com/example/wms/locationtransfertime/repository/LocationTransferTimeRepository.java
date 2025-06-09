package com.example.wms.locationtransfertime.repository;

import com.example.wms.locationtransfertime.domain.LocationTransferTime;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LocationTransferTimeRepository extends JpaRepository<LocationTransferTime, Long> {
} 