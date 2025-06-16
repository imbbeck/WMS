package com.wms.ware.domain.repository;

import com.wms.ware.domain.model.Ware;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
 
@Repository
public interface WareRepository extends JpaRepository<Ware, Long> {
} 