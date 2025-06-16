package com.wms.stock.interfaces;

import com.wms.stock.application.StockService;
import com.wms.stock.dto.StockRequest;
import com.wms.stock.dto.StockResponse;
import com.wms.stock.mapper.StockMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/stocks")
@RequiredArgsConstructor
public class StockController {

    private final StockService stockService;
    private final StockMapper stockMapper;

    @GetMapping("/{id}")
    public ResponseEntity<StockResponse> getStock(@PathVariable Long id) {
        return ResponseEntity.ok(
            stockMapper.toResponse(stockService.getStock(id))
        );
    }

    @GetMapping
    public ResponseEntity<List<StockResponse>> getAllStocks() {
        return ResponseEntity.ok(
            stockService.getAllStocks().stream()
                .map(stockMapper::toResponse)
                .toList()
        );
    }

    @GetMapping("/ware/{wareId}/location/{locationId}")
    public ResponseEntity<Integer> getStockQuantity(
            @PathVariable Long wareId,
            @PathVariable Long locationId) {
        return ResponseEntity.ok(stockService.getStockQuantity(locationId, wareId));
    }

    @GetMapping("/ware/{wareId}/total")
    public ResponseEntity<Integer> getTotalQuantityByWare(@PathVariable Long wareId) {
        return ResponseEntity.ok(stockService.getTotalQuantityByWare(wareId));
    }

    @GetMapping("/location/{locationId}/total")
    public ResponseEntity<Integer> getTotalQuantityByLocation(@PathVariable Long locationId) {
        return ResponseEntity.ok(stockService.getTotalQuantityByLocation(locationId));
    }

} 