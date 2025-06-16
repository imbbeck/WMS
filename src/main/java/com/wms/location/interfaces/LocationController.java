package com.wms.location.interfaces;

import com.wms.location.application.LocationService;
import com.wms.location.application.TransferDurationService;
import com.wms.location.domain.model.LocationType;
import com.wms.location.dto.*;
import com.wms.location.mapper.LocationMapper;
import com.wms.location.mapper.TransferDurationMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/locations")
@RequiredArgsConstructor
public class LocationController {

    private final LocationService locationService;
    private final TransferDurationService transferDurationService;
    private final LocationMapper locationMapper;
    private final TransferDurationMapper transferDurationMapper;

    @PostMapping
    public ResponseEntity<LocationResponse> createLocation(@Valid @RequestBody LocationRequest request) {
        return ResponseEntity.ok(locationMapper.toResponse(locationService.createLocation(request)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<LocationResponse> getLocation(@PathVariable Long id) {
        return ResponseEntity.ok(locationMapper.toResponse(locationService.getLocation(id)));
    }

    @GetMapping("/type/{type}")
    public ResponseEntity<List<LocationResponse>> getLocationsByType(@PathVariable LocationType type) {
        return ResponseEntity.ok(
            locationService.getLocationsByType(type).stream()
                .map(locationMapper::toResponse)
                .toList()
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<LocationResponse> updateLocation(
            @PathVariable Long id,
            @Valid @RequestBody LocationUpdateRequest request) {
        return ResponseEntity.ok(
            locationMapper.toResponse(locationService.updateLocation(id, request))
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLocation(@PathVariable Long id) {
        locationService.deleteLocation(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/transfer-duration")
    public ResponseEntity<TransferDurationResponse> createTransferDuration(
            @Valid @RequestBody TransferDurationRequest request) {
        return ResponseEntity.ok(
            transferDurationMapper.toResponse(transferDurationService.createTransferDuration(request))
        );
    }

    @GetMapping("/transfer-duration/{id}")
    public ResponseEntity<TransferDurationResponse> getTransferDuration(@PathVariable Long id) {
        return ResponseEntity.ok(
            transferDurationMapper.toResponse(transferDurationService.getTransferDuration(id))
        );
    }

    @GetMapping("/transfer-duration")
    public ResponseEntity<TransferDurationResponse> getTransferDurationByLocations(
            @RequestParam Long fromLocationId,
            @RequestParam Long toLocationId) {
        return ResponseEntity.ok(
            transferDurationMapper.toResponse(
                transferDurationService.getTransferDurationByLocations(fromLocationId, toLocationId)
            )
        );
    }

    @PutMapping("/transfer-duration/{id}")
    public ResponseEntity<TransferDurationResponse> updateTransferDuration(
            @PathVariable Long id,
            @Valid @RequestBody TransferDurationUpdateRequest request) {
        return ResponseEntity.ok(
            transferDurationMapper.toResponse(
                transferDurationService.updateTransferDuration(id, request)
            )
        );
    }

    @DeleteMapping("/transfer-duration/{id}")
    public ResponseEntity<Void> deleteTransferDuration(@PathVariable Long id) {
        transferDurationService.deleteTransferDuration(id);
        return ResponseEntity.noContent().build();
    }
} 