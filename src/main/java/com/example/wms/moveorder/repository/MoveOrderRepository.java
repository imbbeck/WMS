package com.example.wms.moveorder.repository;

import com.example.wms.moveorder.domain.MoveOrder;
import com.example.wms.moveorder.domain.MoveOrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MoveOrderRepository extends JpaRepository<MoveOrder, Long> {
    List<MoveOrder> findByStatus(MoveOrderStatus status);
} 