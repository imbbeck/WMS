package com.wms.stock.application;

import com.wms.location.domain.model.Location;
import com.wms.location.domain.model.LocationType;
import com.wms.location.domain.repository.LocationRepository;
import com.wms.stock.domain.model.Stock;
import com.wms.stock.domain.model.StockKey;
import com.wms.stock.domain.repository.StockRepository;
import com.wms.stock.dto.StockDTO;
import com.wms.ware.domain.model.Ware;
import com.wms.ware.domain.repository.WareRepository;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest(properties = {
        "spring.cache.type=redis",  // 이 테스트만 캐시 활성화
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.test.database.replace=none"
})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("StockCacheService 이벤트 기반 통합 테스트")
class StockCacheServiceIntegrationTest {

    @Autowired
    private StockCtrlService stockCtrlService;

    @Autowired
    private StockCacheService stockCacheService;

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private LocationRepository locationRepository;

    @Autowired
    private WareRepository wareRepository;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private PlatformTransactionManager transactionManager;

    // 테스트용 데이터
    private Location testWarehouse;
    private Ware testWare;

    // 실제 생성된 ID를 사용하는 헬퍼 메서드
    private Long getWareId() {
        return testWare.getId();
    }

    private Long getWarehouseId() {
        return testWarehouse.getId();
    }

    // 수동 트랜잭션 관리 헬퍼 메서드
    private void executeInTransaction(Runnable action) {
        DefaultTransactionDefinition def = new DefaultTransactionDefinition();
        def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
        TransactionStatus status = transactionManager.getTransaction(def);

        try {
            action.run();
            transactionManager.commit(status);
        } catch (Exception e) {
            transactionManager.rollback(status);
            throw e;
        }
    }

    // 반환값이 있는 트랜잭션 헬퍼 메서드
    private <T> T executeInTransactionWithReturn(java.util.function.Supplier<T> action) {
        DefaultTransactionDefinition def = new DefaultTransactionDefinition();
        def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
        TransactionStatus status = transactionManager.getTransaction(def);

        try {
            T result = action.get();
            transactionManager.commit(status);
            return result;
        } catch (Exception e) {
            transactionManager.rollback(status);
            throw e;
        }
    }

    @BeforeAll
    void setUpOnce() {
        cleanupDatabase();
        setupTestData();
    }

    @BeforeEach
    void setUp() {
        cleanupCache();
        cleanupTestSpecificData();
    }

    @AfterEach
    void tearDown() {
        cleanupCache();
        cleanupTestSpecificData();
    }

    private void setupTestData() {
        // 테스트용 창고 생성
        testWarehouse = Location.builder()
                .name("테스트 창고")
                .type(LocationType.WAREHOUSE)
                .capacity(1000)
                .coordinateX(100)
                .coordinateY(100)
                .build();
        testWarehouse = locationRepository.save(testWarehouse);

        // 테스트용 물품 생성
        testWare = Ware.builder()
                .name("테스트 물품")
                .type("전자제품")
                .paletteUnit(10)
                .build();
        testWare = wareRepository.save(testWare);
    }

    private void cleanupTestSpecificData() {
        // 재고만 정리 (마스터 데이터는 유지)
        stockRepository.deleteAll();
    }

    private void cleanupCache() {
        // Redis 캐시 정리
        Set<String> stockKeys = redisTemplate.keys("current_stock:*");
        if (!stockKeys.isEmpty()) {
            redisTemplate.delete(stockKeys);
        }

        Set<String> warehouseKeys = redisTemplate.keys("warehouse:*");
        if (!warehouseKeys.isEmpty()) {
            redisTemplate.delete(warehouseKeys);
        }

        // Spring Cache Manager 캐시 정리
        cacheManager.getCacheNames().forEach(cacheName -> {
            if (cacheManager.getCache(cacheName) != null) {
                cacheManager.getCache(cacheName).clear();
            }
        });
    }

    private void cleanupDatabase() {
        // 테스트용 재고 데이터 정리
        stockRepository.deleteAll();
        // 테스트용 마스터 데이터는 각 테스트에서 재생성
        locationRepository.deleteAll();
        wareRepository.deleteAll();
    }

    @Nested
    @DisplayName("재고 생성 이벤트 기반 캐시 처리")
    class StockCreationEventTest {

        @Test
        @DisplayName("재고 생성 시 이벤트를 통한 캐시 자동 업데이트")
        void createStock_EventDrivenCacheUpdate() {
            // Given
            StockDTO.CreateReq request = StockDTO.CreateReq.builder()
                    .wareId(getWareId())
                    .warehouseId(getWarehouseId())
                    .quantity(100)
                    .build();

            // When - 수동 트랜잭션으로 재고 생성 (이벤트 발행 보장)
            Stock createdStock = executeInTransactionWithReturn(() -> stockCtrlService.create(request));

            // Then - 이벤트가 비동기로 처리되어 캐시가 업데이트되었는지 확인
            Awaitility.await()
                    .atMost(Duration.ofSeconds(5))
                    .untilAsserted(() -> {
                        // 개별 재고 캐시 확인
                        Integer cachedQuantity = stockCacheService.getInventoryQuantity(StockKey.of(getWareId(), getWarehouseId()));
                        assertThat(cachedQuantity).isEqualTo(100);

                        // 창고 총량 캐시 확인
                        String warehouseKey = String.format("warehouse:%d:currentSum", getWarehouseId());
                        Integer warehouseTotal = (Integer) redisTemplate.opsForValue().get(warehouseKey);
                        assertThat(warehouseTotal).isEqualTo(100);
                    });

            // DB에도 정상적으로 저장되었는지 확인
            assertThat(createdStock).isNotNull();
            assertThat(createdStock.getQuantity()).isEqualTo(100);
        }

        @Test
        @DisplayName("여러 재고 생성 시 누적 캐시 업데이트")
        void multipleStockCreation_CumulativeCacheUpdate() {
            // Given - 다른 물품들도 추가 생성
            Ware ware2 = Ware.builder()
                    .name("테스트 물품999")
                    .type("가구")
                    .paletteUnit(5)
                    .build();
            ware2 = wareRepository.save(ware2);
            final Long ware2Id = ware2.getId(); // final 변수로 추출

            StockDTO.CreateReq request1 = StockDTO.CreateReq.builder()
                    .wareId(getWareId())
                    .warehouseId(getWarehouseId())
                    .quantity(60)
                    .build();

            StockDTO.CreateReq request2 = StockDTO.CreateReq.builder()
                    .wareId(ware2Id)
                    .warehouseId(getWarehouseId())
                    .quantity(40)
                    .build();

            // When - 순차적으로 재고 생성 (각각 트랜잭션 커밋)
            executeInTransaction(() -> stockCtrlService.create(request1));
            executeInTransaction(() -> stockCtrlService.create(request2));

            // Then - 창고 총량이 누적되어 업데이트되었는지 확인
            Awaitility.await()
                    .atMost(Duration.ofSeconds(5))
                    .untilAsserted(() -> {
                        String warehouseKey = String.format("warehouse:%d:currentSum", getWarehouseId());
                        Integer warehouseTotal = (Integer) redisTemplate.opsForValue().get(warehouseKey);
                        assertThat(warehouseTotal).isEqualTo(100); // 60 + 40

                        // 개별 재고도 확인
                        assertThat(stockCacheService.getInventoryQuantity(StockKey.of(getWareId(), getWarehouseId()))).isEqualTo(60);
                        assertThat(stockCacheService.getInventoryQuantity(StockKey.of(ware2Id, getWarehouseId()))).isEqualTo(40);
                    });
        }
    }

    @Nested
    @DisplayName("재고 수정 이벤트 기반 캐시 처리")
    class StockUpdateEventTest {

        @Test
        @DisplayName("재고 수정 시 이벤트를 통한 캐시 차등 업데이트 - 디버깅")
        void updateStock_EventDrivenCacheUpdate_Debug() throws InterruptedException {
            // Given - 먼저 재고 생성
            StockDTO.CreateReq createRequest = StockDTO.CreateReq.builder()
                    .wareId(getWareId())
                    .warehouseId(getWarehouseId())
                    .quantity(100)
                    .build();
            executeInTransaction(() -> stockCtrlService.create(createRequest));

            // 생성 이벤트 처리 완료 대기
            Awaitility.await()
                    .atMost(Duration.ofSeconds(10))
                    .untilAsserted(() -> {
                        Integer cachedQuantity = stockCacheService.getInventoryQuantity(StockKey.of(getWareId(), getWarehouseId()));
                        System.out.println("생성 후 캐시 값: " + cachedQuantity);
                        assertThat(cachedQuantity).isEqualTo(100);
                    });

            // When - 재고 수정 (100 → 150)
            StockDTO.UpdateReq updateRequest = StockDTO.UpdateReq.builder()
                    .quantity(150)
                    .build();

            System.out.println("재고 수정 실행 전 - wareId: " + getWareId() + ", warehouseId: " + getWarehouseId());
            executeInTransaction(() -> stockCtrlService.update(getWareId(), getWarehouseId(), updateRequest));
            System.out.println("재고 수정 실행 완료");

            // Then - 중간 확인
            Thread.sleep(5000); // 1초 대기

            // 직접 Redis에서 확인
            StockKey key = StockKey.of(getWareId(), getWarehouseId());

            Object redisValue = redisTemplate.opsForValue().get(key.toCacheKey());
            System.out.println("Redis 직접 조회 값: " + redisValue);

            // DB에서 직접 확인
            Optional<Stock> dbStock = stockRepository.findByKey(key);
            System.out.println("DB 값: " + (dbStock.isPresent() ? dbStock.get().getQuantity() : "없음"));

            // 캐시 서비스를 통한 조회
            Integer cachedQuantity = stockCacheService.getInventoryQuantity(key);
            System.out.println("캐시 서비스 조회 값: " + cachedQuantity);

            // 최종 검증
            Awaitility.await()
                    .atMost(Duration.ofSeconds(10))
                    .untilAsserted(() -> {
                        Integer quantity = stockCacheService.getInventoryQuantity(key);
                        assertThat(quantity).isEqualTo(150);
                    });
        }

        @Test
        @DisplayName("재고 수정으로 수량 감소 시 캐시 감소 처리")
        void updateStock_QuantityDecrease_CacheDecrease() {
            // Given - 재고 생성
            StockDTO.CreateReq createRequest = StockDTO.CreateReq.builder()
                    .wareId(getWareId())
                    .warehouseId(getWarehouseId())
                    .quantity(200)
                    .build();
            executeInTransaction(() -> stockCtrlService.create(createRequest));

            // 생성 완료 대기
            Awaitility.await()
                    .atMost(Duration.ofSeconds(3))
                    .untilAsserted(() -> {
                        String warehouseKey = String.format("warehouse:%d:currentSum", getWarehouseId());
                        assertThat(redisTemplate.opsForValue().get(warehouseKey)).isEqualTo(200);
                    });

            // When - 수량 감소 (200 → 80)
            StockDTO.UpdateReq updateRequest = StockDTO.UpdateReq.builder()
                    .quantity(80)
                    .build();
            executeInTransaction(() -> stockCtrlService.update(getWareId(), getWarehouseId(), updateRequest));

            // Then - 감소 반영 확인
            StockKey key = StockKey.of(getWareId(), getWarehouseId());

            Awaitility.await()
                    .atMost(Duration.ofSeconds(5))
                    .untilAsserted(() -> {
                        
                        assertThat(stockCacheService.getInventoryQuantity(key)).isEqualTo(80);

                        String warehouseKey = String.format("warehouse:%d:currentSum", getWarehouseId());
                        Integer warehouseTotal = (Integer) redisTemplate.opsForValue().get(warehouseKey);
                        assertThat(warehouseTotal).isEqualTo(80); // 200 - (200-80) = 80
                    });
        }
    }

    @Nested
    @DisplayName("재고 삭제 이벤트 기반 캐시 처리")
    class StockDeleteEventTest {

        @Test
        @DisplayName("재고 삭제 시 이벤트를 통한 캐시 무효화 및 총량 감소")
        void deleteStock_EventDrivenCacheInvalidation() {
            // Given - 재고 생성
            StockDTO.CreateReq createRequest = StockDTO.CreateReq.builder()
                    .wareId(getWareId())
                    .warehouseId(getWarehouseId())
                    .quantity(120)
                    .build();
            executeInTransaction(() -> stockCtrlService.create(createRequest));

            // 생성 완료 대기
            Awaitility.await()
                    .atMost(Duration.ofSeconds(10))
                    .untilAsserted(() -> assertThat(stockCacheService.getInventoryQuantity(StockKey.of(getWareId(), getWarehouseId()))).isEqualTo(120));

            // When - 재고 삭제
            executeInTransaction(() -> stockCtrlService.delete(getWareId(), getWarehouseId()));

            // Then - 삭제 이벤트 처리 확인
            StockKey key = StockKey.of(getWareId(), getWarehouseId());

            Awaitility.await()
                    .atMost(Duration.ofSeconds(10))
                    .untilAsserted(() -> {
                        // 개별 재고 캐시 무효화로 인한 DB 재조회 (0 반환)
                        Integer cachedQuantity = stockCacheService.getInventoryQuantity(key);
                        assertThat(cachedQuantity).isEqualTo(0);

                        // 창고 총량에서 삭제된 수량만큼 감소 (120 - 120 = 0)
                        String warehouseKey = String.format("warehouse:%d:currentSum", getWarehouseId());
                        Integer warehouseTotal = (Integer) redisTemplate.opsForValue().get(warehouseKey);
                        assertThat(warehouseTotal).isEqualTo(0);
                    });

            // DB에서도 실제 삭제 확인
            assertThat(stockRepository.findByKey(key)).isEmpty();
        }

        @Test
        @DisplayName("수량 0으로 수정 시 자동 삭제 및 캐시 처리")
        void updateToZero_AutoDeleteAndCacheHandling() {
            // Given - 재고 생성
            StockDTO.CreateReq createRequest = StockDTO.CreateReq.builder()
                    .wareId(getWareId())
                    .warehouseId(getWarehouseId())
                    .quantity(80)
                    .build();
            executeInTransaction(() -> stockCtrlService.create(createRequest));

            // 생성 완료 대기
            Awaitility.await()
                    .atMost(Duration.ofSeconds(5))
                    .untilAsserted(() -> assertThat(stockCacheService.getInventoryQuantity(StockKey.of(getWareId(), getWarehouseId()))).isEqualTo(80));

            // When - 수량을 0으로 수정 (자동 삭제)
            StockDTO.UpdateReq updateRequest = StockDTO.UpdateReq.builder()
                    .quantity(0)
                    .build();
            executeInTransaction(() -> stockCtrlService.update(getWareId(), getWarehouseId(), updateRequest));

            // 즉시 Redis 상태 확인
            StockKey key = StockKey.of(getWareId(), getWarehouseId());
            String cacheKey = key.toCacheKey();
            String warehouseKey = String.format("warehouse:%d:currentSum", getWarehouseId());

            System.out.println("=== 삭제 직후 Redis 상태 ===");
            System.out.println("Redis 모든 키: " + redisTemplate.keys("*"));
            System.out.println("개별 재고 캐시 존재 여부: " + redisTemplate.hasKey(cacheKey));
            System.out.println("개별 재고 캐시 값: " + redisTemplate.opsForValue().get(cacheKey));
            System.out.println("창고 총량 캐시 값: " + redisTemplate.opsForValue().get(warehouseKey));

            // Then - 시간 간격을 두고 여러 번 확인
            Awaitility.await()
                    .atMost(Duration.ofSeconds(5))
                    .pollInterval(Duration.ofMillis(500))
                    .untilAsserted(() -> {
                        System.out.println("=== 폴링 중 상태 ===");

                        // DB 확인
                        Optional<Stock> dbStock = stockRepository.findByKey(key);
                        System.out.println("DB 재고 존재: " + dbStock.isPresent());

                        // 캐시 직접 확인
                        boolean keyExists = redisTemplate.hasKey(cacheKey);
                        Object cachedValue = redisTemplate.opsForValue().get(cacheKey);
                        System.out.println("캐시 키 존재: " + keyExists);
                        System.out.println("캐시 값: " + cachedValue);

                        // 서비스 메서드 확인
                        Integer serviceResult = stockCacheService.getInventoryQuantity(key);
                        System.out.println("서비스 반환값: " + serviceResult);

                        // 창고 총량 확인
                        Integer warehouseTotal = (Integer) redisTemplate.opsForValue().get(warehouseKey);
                        System.out.println("창고 총량: " + warehouseTotal);

                        System.out.println("------------------------");

                        // 검증
                        assertThat(dbStock).isEmpty();
                        assertThat(serviceResult).isEqualTo(0);
                        assertThat(warehouseTotal).isEqualTo(0);
                    });
        }
    }

    @Nested
    @DisplayName("전체 재고 라이프사이클 이벤트 기반 테스트")
    class FullLifecycleEventTest {

        @Test
        @DisplayName("생성 → 수정 → 삭제 전체 플로우의 이벤트 기반 캐시 처리")
        void fullLifecycle_EventDrivenCacheFlow() {
            String warehouseKey = String.format("warehouse:%d:currentSum", getWarehouseId());

            // 1. 재고 생성 (0 → 100)
            StockDTO.CreateReq createRequest = StockDTO.CreateReq.builder()
                    .wareId(getWareId())
                    .warehouseId(getWarehouseId())
                    .quantity(100)
                    .build();
            executeInTransaction(() -> stockCtrlService.create(createRequest));

            StockKey key = StockKey.of(getWareId(), getWarehouseId());

            // 생성 이벤트 처리 확인
            Awaitility.await()
                    .atMost(Duration.ofSeconds(5))
                    .untilAsserted(() -> {
                        Integer cachedQuantity = stockCacheService.getInventoryQuantity(key);
                        Integer warehouseTotal = (Integer) redisTemplate.opsForValue().get(warehouseKey);

                        System.out.println("=== 생성 후 상태 ===");
                        System.out.println("개별 재고 캐시: " + cachedQuantity);
                        System.out.println("창고 총량 캐시: " + warehouseTotal);
                        System.out.println("기대값: 100");

                        assertThat(cachedQuantity).isEqualTo(100);
                        assertThat(warehouseTotal).isEqualTo(100);
                    });

            // 2. 재고 수정 (100 → 180)
            StockDTO.UpdateReq updateRequest = StockDTO.UpdateReq.builder()
                    .quantity(180)
                    .build();
            executeInTransaction(() -> stockCtrlService.update(getWareId(), getWarehouseId(), updateRequest));

            // 수정 이벤트 처리 확인

            Awaitility.await()
                    .atMost(Duration.ofSeconds(5))
                    .untilAsserted(() -> {
                        Integer cachedQuantity = stockCacheService.getInventoryQuantity(key);
                        Integer warehouseTotal = (Integer) redisTemplate.opsForValue().get(warehouseKey);

                        System.out.println("=== 수정 후 상태 ===");
                        System.out.println("개별 재고 캐시: " + cachedQuantity);
                        System.out.println("창고 총량 캐시: " + warehouseTotal);
                        System.out.println("기대값: 180");

                        assertThat(cachedQuantity).isEqualTo(180);
                        assertThat(warehouseTotal).isEqualTo(180);
                    });

            // 3. 재고 삭제
            executeInTransaction(() -> stockCtrlService.delete(getWareId(), getWarehouseId()));

            // 삭제 이벤트 처리 확인
            Awaitility.await()
                    .atMost(Duration.ofSeconds(5))
                    .untilAsserted(() -> {
                        Integer cachedQuantity = stockCacheService.getInventoryQuantity(key);
                        Integer warehouseTotal = (Integer) redisTemplate.opsForValue().get(warehouseKey);

                        System.out.println("=== 삭제 후 상태 ===");
                        System.out.println("개별 재고 캐시: " + cachedQuantity);
                        System.out.println("창고 총량 캐시: " + warehouseTotal);
                        System.out.println("기대값: 0");

                        assertThat(cachedQuantity).isEqualTo(0);
                        assertThat(warehouseTotal).isEqualTo(0);
                        assertThat(stockRepository.findByKey(key)).isEmpty();
                    });
        }

        @Test
        @DisplayName("복수 재고의 동시 처리 시 이벤트 기반 캐시 일관성")
        void multipleStocks_EventDrivenConsistency() {
            // Given - 추가 물품 생성
            Ware ware2 = Ware.builder()
                    .name("테스트 물품2")
                    .type("가구")
                    .paletteUnit(5)
                    .build();
            Ware ware3 = Ware.builder()
                    .name("테스트 물품3")
                    .type("도서")
                    .paletteUnit(20)
                    .build();
            ware2 = wareRepository.save(ware2);
            ware3 = wareRepository.save(ware3);
            final Long ware2Id = ware2.getId(); // final 변수로 추출
            final Long ware3Id = ware3.getId(); // final 변수로 추출

            // When - 여러 재고 동시 생성 (각각 트랜잭션 커밋)
            executeInTransaction(() -> stockCtrlService.create(StockDTO.CreateReq.builder()
                    .wareId(getWareId()).warehouseId(getWarehouseId()).quantity(50).build()));
            executeInTransaction(() -> stockCtrlService.create(StockDTO.CreateReq.builder()
                    .wareId(ware2Id).warehouseId(getWarehouseId()).quantity(30).build()));
            executeInTransaction(() -> stockCtrlService.create(StockDTO.CreateReq.builder()
                    .wareId(ware3Id).warehouseId(getWarehouseId()).quantity(70).build()));

            // Then - 모든 이벤트 처리 완료 후 일관성 확인
            Awaitility.await()
                    .atMost(Duration.ofSeconds(5))
                    .untilAsserted(() -> {
                        // 개별 재고 캐시 확인
                        assertThat(stockCacheService.getInventoryQuantity(StockKey.of(getWareId(), getWarehouseId()))).isEqualTo(50);
                        assertThat(stockCacheService.getInventoryQuantity(StockKey.of(ware2Id, getWarehouseId()))).isEqualTo(30);
                        assertThat(stockCacheService.getInventoryQuantity(StockKey.of(ware3Id, getWarehouseId()))).isEqualTo(70);

                        // 창고 총량 캐시 확인 (50 + 30 + 70 = 150)
                        String warehouseKey = String.format("warehouse:%d:currentSum", getWarehouseId());
                        assertThat(redisTemplate.opsForValue().get(warehouseKey)).isEqualTo(150);
                    });
        }
    }

    @Nested
    @DisplayName("이벤트 기반 캐시 예외 상황 테스트")
    class EventExceptionHandlingTest {

        @Test
        @DisplayName("DB 트랜잭션 롤백 시 이벤트 미발행으로 캐시 일관성 유지")
        void transactionRollback_NoEventEmission() {
            // Given - 존재하지 않는 창고 ID로 요청 (예외 발생 예정)
            StockDTO.CreateReq invalidRequest = StockDTO.CreateReq.builder()
                    .wareId(getWareId())
                    .warehouseId(999L) // 존재하지 않는 창고
                    .quantity(100)
                    .build();

            // When & Then - 예외 발생으로 트랜잭션 롤백
            assertThatThrownBy(() -> executeInTransaction(() -> stockCtrlService.create(invalidRequest)))
                    .isInstanceOf(RuntimeException.class);

            // Then - 이벤트가 발행되지 않아 캐시도 업데이트되지 않음
            Awaitility.await()
                    .during(Duration.ofSeconds(2)) // 2초 동안 변화 없음을 확인
                    .atMost(Duration.ofSeconds(3))
                    .untilAsserted(() -> {
                        // 캐시에 아무것도 저장되지 않음
                        String warehouseKey = String.format("warehouse:%d:currentSum", 999L);
                        assertThat(redisTemplate.opsForValue().get(warehouseKey)).isNull();
                    });
        }
    }
}