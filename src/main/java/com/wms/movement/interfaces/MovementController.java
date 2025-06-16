package com.wms.movement.interfaces;

import com.wms.movement.application.MovementService;
import com.wms.movement.dto.MovementRequest;
import com.wms.movement.dto.MovementResponse;
import com.wms.movement.mapper.MovementMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/movements")
@RequiredArgsConstructor
public class MovementController {

    private final MovementService movementService;
    private final MovementMapper movementMapper;

    @PostMapping
    public ResponseEntity<MovementResponse> createMovement(@Valid @RequestBody MovementRequest request) {
        return ResponseEntity.ok(
            movementMapper.toResponse(movementService.createMovement(request))
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<MovementResponse> getMovement(@PathVariable Long id) {
        return ResponseEntity.ok(
            movementMapper.toResponse(movementService.getMovement(id))
        );
    }

    @GetMapping
    public ResponseEntity<List<MovementResponse>> getAllMovements() {
        return ResponseEntity.ok(
            movementService.getAllMovements().stream()
                .map(movementMapper::toResponse)
                .toList()
        );
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<MovementResponse>> getMovementsByStatus(@PathVariable String status) {
        return ResponseEntity.ok(
            movementService.getMovementsByStatus(status).stream()
                .map(movementMapper::toResponse)
                .toList()
        );
    }

    @GetMapping("/ware/{wareId}")
    public ResponseEntity<List<MovementResponse>> getMovementsByWare(@PathVariable Long wareId) {
        return ResponseEntity.ok(
            movementService.getMovementsByWare(wareId).stream()
                .map(movementMapper::toResponse)
                .toList()
        );
    }

    @GetMapping("/fromLocationId/{fromLocationId}")
    public ResponseEntity<List<MovementResponse>> getMovementsByFromLocation(@PathVariable Long fromLocationId) {
        return ResponseEntity.ok(
            movementService.getMovementsByFromLocation(fromLocationId).stream()
                .map(movementMapper::toResponse)
                .toList()
        );
    }

    @PostMapping("/{id}/start")
    public ResponseEntity<MovementResponse> startMovement(@PathVariable Long id) {
        return ResponseEntity.ok(
            movementMapper.toResponse(movementService.startMovement(id))
        );
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<MovementResponse> completeMovement(@PathVariable Long id) {
        return ResponseEntity.ok(
            movementMapper.toResponse(movementService.completeMovement(id))
        );
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<MovementResponse> cancelMovement(@PathVariable Long id) {
        return ResponseEntity.ok(
            movementMapper.toResponse(movementService.cancelMovement(id))
        );
    }
} 