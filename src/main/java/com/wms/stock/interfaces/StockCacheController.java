package com.wms.stock.interfaces;

import com.wms.stock.application.StockCacheInitializer;
import com.wms.stock.application.StockCacheService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/stock/cache")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "재고 캐시 관리", description = "재고 캐시 초기화 및 관리 API")
public class StockCacheController {
    
    private final StockCacheInitializer stockCacheInitializer;
    private final RedisTemplate<String, Object> redisTemplate;
    
    @Operation(summary = "재고 캐시 수동 초기화", description = "모든 재고 데이터를 캐시에 일괄 업데이트합니다.")
    @PostMapping("/initialize")
    public ResponseEntity<Map<String, Object>> initializeCache() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            log.info("수동 재고 캐시 초기화 시작");
            
            // 비동기로 실행 (시간이 오래 걸릴 수 있음)
            new Thread(() -> stockCacheInitializer.initializeStockCache()).start();
            
            response.put("success", true);
            response.put("message", "재고 캐시 초기화가 시작되었습니다. 로그를 확인하세요.");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("재고 캐시 초기화 요청 실패", e);
            response.put("success", false);
            response.put("message", "재고 캐시 초기화 요청 실패: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    @Operation(summary = "재고 캐시 일관성 검증", description = "DB와 캐시 간의 데이터 일관성을 검증합니다.")
    @PostMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateCache() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            log.info("재고 캐시 일관성 검증 시작");
            
            // 비동기로 실행
            new Thread(() -> stockCacheInitializer.validateCacheConsistency()).start();
            
            response.put("success", true);
            response.put("message", "재고 캐시 일관성 검증이 시작되었습니다. 로그를 확인하세요.");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("재고 캐시 검증 요청 실패", e);
            response.put("success", false);
            response.put("message", "재고 캐시 검증 요청 실패: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    @Operation(summary = "재고 캐시 통계 조회", description = "현재 캐시된 재고 데이터의 통계를 조회합니다.")
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getCacheStatistics() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 각 타입별 캐시 키 개수 조회
            Set<String> stockKeys = redisTemplate.keys("current_stock:*");
            Set<String> warehouseSumKeys = redisTemplate.keys("warehouse:*:currentSum");
            Set<String> warehouseCapacityKeys = redisTemplate.keys("warehouse:*:capacity");
            
            Map<String, Object> statistics = new HashMap<>();
            statistics.put("individualStockCache", stockKeys != null ? stockKeys.size() : 0);
            statistics.put("warehouseSumCache", warehouseSumKeys != null ? warehouseSumKeys.size() : 0);
            statistics.put("warehouseCapacityCache", warehouseCapacityKeys != null ? warehouseCapacityKeys.size() : 0);
            statistics.put("totalCacheKeys", 
                (stockKeys != null ? stockKeys.size() : 0) + 
                (warehouseSumKeys != null ? warehouseSumKeys.size() : 0) + 
                (warehouseCapacityKeys != null ? warehouseCapacityKeys.size() : 0)
            );
            
            response.put("success", true);
            response.put("statistics", statistics);
            
            // 로그에도 출력
            stockCacheInitializer.logCacheStatistics();
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("재고 캐시 통계 조회 실패", e);
            response.put("success", false);
            response.put("message", "재고 캐시 통계 조회 실패: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    @Operation(summary = "재고 캐시 전체 삭제", description = "모든 재고 캐시를 삭제합니다. (주의: 성능에 영향을 줄 수 있음)")
    @DeleteMapping("/clear")
    public ResponseEntity<Map<String, Object>> clearCache() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 재고 관련 캐시 키 패턴들
            String[] patterns = {
                "current_stock:*",
                "warehouse:*:currentSum", 
                "warehouse:*:capacity"
            };
            
            int totalDeleted = 0;
            for (String pattern : patterns) {
                Set<String> keys = redisTemplate.keys(pattern);
                if (keys != null && !keys.isEmpty()) {
                    redisTemplate.delete(keys);
                    totalDeleted += keys.size();
                    log.info("삭제된 캐시 키 ({}): {}개", pattern, keys.size());
                }
            }
            
            response.put("success", true);
            response.put("message", "재고 캐시가 삭제되었습니다.");
            response.put("deletedKeys", totalDeleted);
            
            log.info("재고 캐시 전체 삭제 완료: {}개 키 삭제", totalDeleted);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("재고 캐시 삭제 실패", e);
            response.put("success", false);
            response.put("message", "재고 캐시 삭제 실패: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    @Operation(summary = "특정 창고 캐시 새로고침", description = "특정 창고의 재고 캐시를 새로고침합니다.")
    @PostMapping("/refresh/warehouse/{warehouseId}")
    public ResponseEntity<Map<String, Object>> refreshWarehouseCache(@PathVariable Long warehouseId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 해당 창고의 개별 재고 캐시 삭제
            Set<String> stockKeys = redisTemplate.keys("current_stock:*:" + warehouseId + ":*");
            if (stockKeys != null && !stockKeys.isEmpty()) {
                redisTemplate.delete(stockKeys);
            }
            
            // 창고 총량 캐시 삭제
            String warehouseSumKey = "warehouse:" + warehouseId + ":currentSum";
            redisTemplate.delete(warehouseSumKey);
            
            response.put("success", true);
            response.put("message", "창고 " + warehouseId + "의 캐시가 새로고침되었습니다.");
            response.put("deletedStockKeys", stockKeys != null ? stockKeys.size() : 0);
            
            log.info("창고 {}의 캐시 새로고침 완료", warehouseId);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("창고 {} 캐시 새로고침 실패", warehouseId, e);
            response.put("success", false);
            response.put("message", "창고 캐시 새로고침 실패: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
}
