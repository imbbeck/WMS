package com.example.wms.moveorder.service;

import com.example.wms.inventory.repository.InventoryRepository;
import com.example.wms.inventory.service.InventoryCacheService;
import com.example.wms.location.domain.Location;
import com.example.wms.location.repository.LocationRepository;
import com.example.wms.moveorder.domain.MoveOrder;
import com.example.wms.moveorder.domain.MoveOrderStatus;
import com.example.wms.moveorder.domain.event.MoveOrderExecutedEvent;
import com.example.wms.moveorder.dto.CreateMoveOrderRequest;
import com.example.wms.moveorder.repository.MoveOrderRepository;
import com.example.wms.ware.domain.Ware;
import com.example.wms.ware.repository.WareRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MoveOrderService {

    private final MoveOrderRepository moveOrderRepository;
    private final LocationRepository locationRepository;
    private final WareRepository wareRepository;
    private final InventoryRepository inventoryRepository;
    private final InventoryCacheService inventoryCacheService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public MoveOrder findById(Long moveOrderId) {
        return moveOrderRepository.findById(moveOrderId)
                .orElseThrow(() -> new IllegalArgumentException("주문 정보를 찾을 수 없습니다."));
    }

    @Transactional(readOnly = true)
    public List<MoveOrder> findAll() {
        return moveOrderRepository.findAll();
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
        inventoryRepository.decrease(fromLocation.getId(), ware.getId(), request.getQuantity());
        inventoryRepository.increase(toLocation.getId(), ware.getId(), request.getQuantity());
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
        inventoryRepository.increase(moveOrder.getFromLocation().getId(), moveOrder.getWare().getId(), moveOrder.getQuantity());
        inventoryRepository.decrease(moveOrder.getToLocation().getId(), moveOrder.getWare().getId(), moveOrder.getQuantity());
    }

    private void validateInventory(CreateMoveOrderRequest request) {
        // 출발지의 예측 재고 >= 오더 수량
        Long fromLocationInventory = inventoryRepository.getQuantity(request.getFromLocationId(), request.getWareId());
        if (fromLocationInventory < request.getQuantity()) {
            throw new IllegalArgumentException("출발지의 재고가 부족합니다.");
        }

        // 도착지의 예측 재고 + 오더 수량 <= 장소 용량
        Location toLocation = locationRepository.findById(request.getToLocationId())
                .orElseThrow(() -> new IllegalArgumentException("도착지 정보를 찾을 수 없습니다."));

        if (toLocation.getType().equals(com.example.wms.location.domain.LocationType.WAREHOUSE) || toLocation.getType().equals(com.example.wms.location.domain.LocationType.YARD)) {
            Long toLocationInventory = inventoryRepository.getQuantity(request.getToLocationId(), request.getWareId());
            if (toLocationInventory + request.getQuantity() > toLocation.getCapacity()) {
                throw new IllegalArgumentException("도착지의 용량이 부족합니다.");
            }
        }
    }

    @Transactional
    public void executeMoveOrder(Long moveOrderId) {
        MoveOrder moveOrder = moveOrderRepository.findById(moveOrderId)
                .orElseThrow(() -> new EntityNotFoundException("이동 오더를 찾을 수 없습니다."));

        ValidationResult validationResult = validateExecution(moveOrder);
        if (!validationResult.isValid()) {
            moveOrder.fail();
            throw new IllegalStateException(validationResult.getErrorMessage());
        }

        try {
            // 이벤트 발행
            eventPublisher.publishEvent(new MoveOrderExecutedEvent(
                moveOrder.getFromLocation(),
                moveOrder.getToLocation(),
                moveOrder.getWare(),
                moveOrder.getQuantity()
            ));

            // 캐시 무효화
            inventoryCacheService.invalidateCache(moveOrder.getFromLocation().getId(), moveOrder.getWare().getId());
            inventoryCacheService.invalidateCache(moveOrder.getToLocation().getId(), moveOrder.getWare().getId());

            // 오더 상태 변경
            moveOrder.complete();
        } catch (Exception e) {
            moveOrder.fail();
            throw new IllegalStateException("이동 오더 실행 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }

    private ValidationResult validateExecution(MoveOrder moveOrder) {
        // 1. 상태 검증
        if (moveOrder.getStatus() != MoveOrderStatus.PENDING) {
            return ValidationResult.invalid("대기 중인 오더만 실행할 수 있습니다.");
        }

        // 2. 출발지 재고 검증
        Long currentQuantity = inventoryCacheService.getQuantity(
            moveOrder.getFromLocation().getId(), 
            moveOrder.getWare().getId()
        );

        if (currentQuantity < moveOrder.getQuantity()) {
            return ValidationResult.invalid(
                String.format("출발지의 재고가 부족합니다. (필요: %d, 보유: %d)",
                    moveOrder.getQuantity(), currentQuantity)
            );
        }

        return ValidationResult.valid();
    }

    private static class ValidationResult {
        private final boolean valid;
        private final String errorMessage;

        private ValidationResult(boolean valid, String errorMessage) {
            this.valid = valid;
            this.errorMessage = errorMessage;
        }

        public static ValidationResult valid() {
            return new ValidationResult(true, null);
        }

        public static ValidationResult invalid(String errorMessage) {
            return new ValidationResult(false, errorMessage);
        }

        public boolean isValid() {
            return valid;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }
} 