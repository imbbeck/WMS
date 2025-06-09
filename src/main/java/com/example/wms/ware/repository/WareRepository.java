package com.example.wms.ware.repository;

import com.example.wms.ware.domain.Ware;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WareRepository extends JpaRepository<Ware, Long> {
} 