package com.example.wms.moveorder.controller;

import com.example.wms.moveorder.dto.CreateMoveOrderRequest;
import com.example.wms.moveorder.service.MoveOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.GetMapping;
import com.example.wms.moveorder.dto.MoveOrderResponse;
import java.util.List;

@RestController
@RequestMapping("/move-orders")
@RequiredArgsConstructor
public class MoveOrderController {

    private final MoveOrderService moveOrderService;

    @GetMapping("/{id}")
    public ResponseEntity<MoveOrderResponse> getMoveOrder(@PathVariable Long id) {
        MoveOrderResponse response = moveOrderService.findById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<MoveOrderResponse>> getAllMoveOrders() {
        List<MoveOrderResponse> responses = moveOrderService.findAll();
        return ResponseEntity.ok(responses);
    }

    @PostMapping
    public ResponseEntity<Void> createMoveOrder(@Valid @RequestBody CreateMoveOrderRequest request) {
        moveOrderService.createOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<Void> cancelMoveOrder(@PathVariable Long id) {
        moveOrderService.cancelOrder(id);
        return ResponseEntity.ok().build();
    }
} 