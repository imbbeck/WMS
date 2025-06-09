package com.example.wms.moveorder.service;

import com.example.wms.inventory.service.VirtualInventoryService;
import com.example.wms.location.domain.Location;
import com.example.wms.location.repository.LocationRepository;
import com.example.wms.moveorder.domain.MoveOrder;
import com.example.wms.moveorder.dto.CreateMoveOrderRequest;
import com.example.wms.moveorder.repository.MoveOrderRepository;
import com.example.wms.ware.domain.Ware;
import com.example.wms.ware.repository.WareRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.wms.moveorder.dto.MoveOrderResponse;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MoveOrderService {

    private final MoveOrderRepository moveOrderRepository;
    private final LocationRepository locationRepository;
    private final WareRepository wareRepository;
    private final VirtualInventoryService virtualInventoryService;

    @Transactional(readOnly = true)
    public MoveOrderResponse findById(Long moveOrderId) {
        MoveOrder moveOrder = moveOrderRepository.findById(moveOrderId)
                .orElseThrow(() -> new IllegalArgumentException("주문 정보를 찾을 수 없습니다."));
        return new MoveOrderResponse(moveOrder);
    }

    @Transactional(readOnly = true)
    public List<MoveOrderResponse> findAll() {
        return moveOrderRepository.findAll().stream()
                .map(MoveOrderResponse::new)
                .collect(Collectors.toList());
    }

    @Transactional
    public void createOrder(CreateMoveOrderRequest request) {
        // 1. 재고 정합성 검증
        validateInventory(request);

        // 2. 엔티티 조회
        Location fromLocation = locationRepository.findById(request.getFromLocationId())
                .orElseThrow(() -> new IllegalArgumentException("출발지 정보를 찾을 수 없습니다."));
        Location toLocation = locationRepository.findById(request.getToLocationId())
                .orElseThrow(() -> new IllegalArgumentException("도착지 정보를 찾을 수 없습니다."));
        Ware ware = wareRepository.findById(request.getWareId())
                .orElseThrow(() -> new IllegalArgumentException("상품 정보를 찾을 수 없습니다."));

        // 3. 이동 오더 생성 및 저장
        MoveOrder moveOrder = new MoveOrder(
                request.getName(),
                request.getType(),
                fromLocation,
                toLocation,
                request.getScheduledDate(),
                ware,
                request.getQuantity()
        );
        moveOrderRepository.save(moveOrder);

        // 4. 예측 재고 업데이트
        virtualInventoryService.decrease(fromLocation.getId(), ware.getId(), request.getQuantity());
        virtualInventoryService.increase(toLocation.getId(), ware.getId(), request.getQuantity());
    }

    @Transactional
    public void cancelOrder(Long moveOrderId) {
        MoveOrder moveOrder = moveOrderRepository.findById(moveOrderId)
                .orElseThrow(() -> new IllegalArgumentException("주문 정보를 찾을 수 없습니다."));

        if (moveOrder.getStatus() != com.example.wms.moveorder.domain.MoveOrderStatus.PENDING) {
            throw new IllegalStateException("대기 상태의 주문만 취소할 수 있습니다.");
        }

        moveOrder.cancel();

        // 예측 재고 원복
        virtualInventoryService.increase(moveOrder.getFromLocation().getId(), moveOrder.getWare().getId(), moveOrder.getQuantity());
        virtualInventoryService.decrease(moveOrder.getToLocation().getId(), moveOrder.getWare().getId(), moveOrder.getQuantity());
    }

    private void validateInventory(CreateMoveOrderRequest request) {
        // 출발지의 예측 재고 >= 오더 수량
        Long fromLocationInventory = virtualInventoryService.getQuantity(request.getFromLocationId(), request.getWareId());
        if (fromLocationInventory < request.getQuantity()) {
            throw new IllegalArgumentException("출발지의 재고가 부족합니다.");
        }

        // 도착지의 예측 재고 + 오더 수량 <= 장소 용량
        Location toLocation = locationRepository.findById(request.getToLocationId())
                .orElseThrow(() -> new IllegalArgumentException("도착지 정보를 찾을 수 없습니다."));

        if (toLocation.getType().equals(com.example.wms.location.domain.LocationType.WAREHOUSE) || toLocation.getType().equals(com.example.wms.location.domain.LocationType.YARD)) {
            Long toLocationInventory = virtualInventoryService.getQuantity(request.getToLocationId(), request.getWareId());
            if (toLocationInventory + request.getQuantity() > toLocation.getCapacity()) {
                throw new IllegalArgumentException("도착지의 용량이 부족합니다.");
            }
        }
    }
} 