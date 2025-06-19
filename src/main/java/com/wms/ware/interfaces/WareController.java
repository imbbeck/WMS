package com.wms.ware.interfaces;

import com.wms.ware.application.WareService;
import com.wms.ware.dto.WareRequest;
import com.wms.ware.dto.WareResponse;
import com.wms.ware.dto.WareUpdateRequest;
import com.wms.ware.mapper.WareMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/wares")
@RequiredArgsConstructor
public class WareController {

    private final WareService wareService;
    private final WareMapper wareMapper;

    @PostMapping
    public ResponseEntity<WareResponse> createWare(@Valid @RequestBody WareRequest request) {
        return ResponseEntity.ok(
            wareMapper.toResponse(wareService.createWare(request))
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<WareResponse> getWare(@PathVariable Long id) {
        return ResponseEntity.ok(
            wareMapper.toResponse(wareService.getWare(id))
        );
    }

    @GetMapping
    public ResponseEntity<List<WareResponse>> getAllWares() {
        return ResponseEntity.ok(
            wareService.getWares().stream()
                .map(wareMapper::toResponse)
                .toList()
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<WareResponse> updateWare(
            @PathVariable Long id,
            @Valid @RequestBody WareUpdateRequest request) {
        return ResponseEntity.ok(
            wareMapper.toResponse(wareService.updateWare(id, request))
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteWare(@PathVariable Long id) {
        wareService.deleteWare(id);
        return ResponseEntity.noContent().build();
    }
} 