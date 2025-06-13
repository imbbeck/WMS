package com.example.wms.inventory.repository;

import com.example.wms.inventory.domain.Inventory;
import com.example.wms.location.domain.Location;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {
    Optional<Inventory> findByLocationAndWareId(Location location, Long wareId);
} 