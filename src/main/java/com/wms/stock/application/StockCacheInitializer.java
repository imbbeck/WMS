package com.wms.stock.application;

import com.wms.location.domain.model.Location;
import com.wms.location.domain.model.LocationType;
import com.wms.location.domain.repository.LocationRepository;
import com.wms.stock.domain.model.Stock;
import com.wms.stock.domain.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StockCacheInitializer {
    
    private final StockRepository stockRepository;
    private final LocationRepository locationRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    
    /**
     * 애플리케이션 시작 완료 후 재고 캐시 일괄 초기화
     */
    @EventListener(ApplicationReadyEvent.class)
    public void initializeStockCache() {
        LocalDateTime startTime = LocalDateTime.now();
        log.info("=== 재고 캐시 일괄 초기화 시작 ===");
        
        try {
            // 1. 모든 재고 데이터 조회
            List<Stock> allStocks = stockRepository.findAll();
            log.info("총 {}개의 재고 항목을 찾았습니다.", allStocks.size());
            
            if (allStocks.isEmpty()) {
                log.info("재고 데이터가 없습니다. 캐시 초기화를 건너뜁니다.");
                return;
            }
            
            // 2. 개별 재고 캐시 업데이트
            int stockCacheCount = initializeIndividualStockCache(allStocks);
            
            // 3. 창고별 총량 캐시 업데이트
            int warehouseCacheCount = initializeWarehouseCache(allStocks);
            
            // 4. 창고 용량 캐시 업데이트
            int capacityCacheCount = initializeWarehouseCapacityCache();
            
            LocalDateTime endTime = LocalDateTime.now();
            long durationSeconds = java.time.Duration.between(startTime, endTime).toSeconds();
            
            log.info("=== 재고 캐시 일괄 초기화 완료 ===");
            log.info("소요 시간: {}초", durationSeconds);
            log.info("개별 재고 캐시: {}개", stockCacheCount);
            log.info("창고 총량 캐시: {}개", warehouseCacheCount);
            log.info("창고 용량 캐시: {}개", capacityCacheCount);
            
        } catch (Exception e) {
            log.error("재고 캐시 초기화 중 오류 발생", e);
        }
    }
    
    /**
     * 개별 재고 캐시 초기화
     */
    private int initializeIndividualStockCache(List<Stock> stocks) {
        log.info("개별 재고 캐시 초기화 중...");
        
        int count = 0;
        for (Stock stock : stocks) {
            try {
                String cacheKey = stock.getKey().toCacheKey();
                redisTemplate.opsForValue().set(cacheKey, stock.getQuantity());
                count++;
                
                if (count % 100 == 0) {
                    log.debug("개별 재고 캐시 {}개 처리 완료", count);
                }
                
            } catch (Exception e) {
                log.warn("재고 캐시 설정 실패: {}", stock.getKey(), e);
            }
        }
        
        log.info("개별 재고 캐시 초기화 완료: {}개", count);
        return count;
    }
    
    /**
     * 창고별 총량 캐시 초기화
     */
    private int initializeWarehouseCache(List<Stock> stocks) {
        log.info("창고별 총량 캐시 초기화 중...");
        
        // 창고별로 재고 합계 계산
        Map<Long, Integer> warehouseTotals = stocks.stream()
                .collect(Collectors.groupingBy(
                        stock -> stock.getKey().getWarehouseId(),
                        Collectors.summingInt(Stock::getQuantity)
                ));
        
        int count = 0;
        for (Map.Entry<Long, Integer> entry : warehouseTotals.entrySet()) {
            try {
                Long warehouseId = entry.getKey();
                Integer totalQuantity = entry.getValue();
                
                String cacheKey = String.format("warehouse:%d:currentSum", warehouseId);
                redisTemplate.opsForValue().set(cacheKey, totalQuantity);
                count++;
                
                log.debug("창고 {}의 총 재고량: {} 팔레트", warehouseId, totalQuantity);
                
            } catch (Exception e) {
                log.warn("창고 총량 캐시 설정 실패: warehouseId={}", entry.getKey(), e);
            }
        }
        
        log.info("창고별 총량 캐시 초기화 완료: {}개", count);
        return count;
    }
    
    /**
     * 창고 용량 캐시 초기화
     */
    private int initializeWarehouseCapacityCache() {
        log.info("창고 용량 캐시 초기화 중...");
        
        List<Location> warehouses = locationRepository.findByType(LocationType.WAREHOUSE);
        
        int count = 0;
        for (Location warehouse : warehouses) {
            try {
                if (warehouse.getCapacity() != null) {
                    String cacheKey = String.format("warehouse:%d:capacity", warehouse.getId());
                    redisTemplate.opsForValue().set(cacheKey, warehouse.getCapacity());
                    count++;
                    
                    log.debug("창고 {}({})의 용량: {} 팔레트",
                            warehouse.getId(), warehouse.getName(), warehouse.getCapacity());
                }
                
            } catch (Exception e) {
                log.warn("창고 용량 캐시 설정 실패: warehouseId={}", warehouse.getId(), e);
            }
        }
        
        log.info("창고 용량 캐시 초기화 완료: {}개", count);
        return count;
    }
    
    /**
     * 재고 캐시 상태 검증 (선택적)
     */
    public void validateCacheConsistency() {
        log.info("=== 재고 캐시 일관성 검증 시작 ===");
        
        List<Stock> allStocks = stockRepository.findAll();
        int inconsistentCount = 0;
        
        for (Stock stock : allStocks) {
            try {
                String cacheKey = stock.getKey().toCacheKey();
                Integer cachedQuantity = (Integer) redisTemplate.opsForValue().get(cacheKey);
                
                if (!stock.getQuantity().equals(cachedQuantity)) {
                    log.warn("캐시 불일치 발견: {} - DB: {}, Cache: {}",
                            stock.getKey(), stock.getQuantity(), cachedQuantity);
                    inconsistentCount++;
                }
                
            } catch (Exception e) {
                log.warn("캐시 검증 실패: {}", stock.getKey(), e);
                inconsistentCount++;
            }
        }
        
        if (inconsistentCount == 0) {
            log.info("재고 캐시 일관성 검증 완료: 모든 캐시가 일치합니다.");
        } else {
            log.warn("재고 캐시 일관성 검증 완료: {}개의 불일치 항목 발견", inconsistentCount);
        }
    }
    
    /**
     * 캐시 통계 조회
     */
    public void logCacheStatistics() {
        try {
            // Redis에서 재고 캐시 키 개수 조회
            Long stockCacheCount = redisTemplate.countExistingKeys(
                    redisTemplate.keys("current_stock:*"));
            Long warehouseCacheCount = redisTemplate.countExistingKeys(
                    redisTemplate.keys("warehouse:*:currentSum"));
            Long capacityCacheCount = redisTemplate.countExistingKeys(
                    redisTemplate.keys("warehouse:*:capacity"));
            
            log.info("=== 현재 캐시 통계 ===");
            log.info("개별 재고 캐시: {}개", stockCacheCount);
            log.info("창고 총량 캐시: {}개", warehouseCacheCount);
            log.info("창고 용량 캐시: {}개", capacityCacheCount);
            
        } catch (Exception e) {
            log.warn("캐시 통계 조회 실패", e);
        }
    }
}
