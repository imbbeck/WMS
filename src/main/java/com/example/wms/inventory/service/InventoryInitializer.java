package com.example.wms.inventory.service;

import com.example.wms.inventory.domain.Inventory;
import com.example.wms.inventory.repository.InventoryRepository;
import com.example.wms.moveorder.domain.MoveOrder;
import com.example.wms.moveorder.domain.MoveOrderStatus;
import com.example.wms.moveorder.repository.MoveOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class InventoryInitializer implements ApplicationRunner {

    private final InventoryRepository inventoryRepository;
    private final MoveOrderRepository moveOrderRepository;
    private final VirtualInventoryService virtualInventoryService;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        // 1. DB의 현재고로 Redis 초기화
        List<Inventory> inventories = inventoryRepository.findAll();
        for (Inventory inventory : inventories) {
            virtualInventoryService.increase(
                    inventory.getLocation().getId(),
                    inventory.getWare().getId(),
                    inventory.getQuantity()
            );
        }

        // 2. PENDING 상태의 오더를 조회하여 예측 재고에 반영
        List<MoveOrder> pendingOrders = moveOrderRepository.findByStatus(MoveOrderStatus.PENDING);
        for (MoveOrder order : pendingOrders) {
            // 출발지 재고 감소
            virtualInventoryService.decrease(
                    order.getFromLocation().getId(),
                    order.getWare().getId(),
                    order.getQuantity()
            );
            // 도착지 재고 증가
            virtualInventoryService.increase(
                    order.getToLocation().getId(),
                    order.getWare().getId(),
                    order.getQuantity()
            );
        }
    }
} 