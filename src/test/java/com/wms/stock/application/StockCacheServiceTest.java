package com.wms.stock.application;

import com.wms.stock.domain.event.StockCreatedEvent;
import com.wms.stock.domain.event.StockDeletedEvent;
import com.wms.stock.domain.event.StockUpdatedEvent;
import com.wms.stock.domain.model.Stock;
import com.wms.stock.domain.model.StockKey;
import com.wms.stock.domain.repository.StockRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("StockCacheService 단위 테스트")
class StockCacheServiceTest {

    @InjectMocks
    private StockCacheService stockCacheService;

    @Mock
    private StockRepository stockRepository;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    private final Long wareId = 10L;
    private final Long warehouseId = 100L;
    private final Integer quantity = 50;
    private final StockKey key = StockKey.of(wareId, warehouseId);
    private final String cacheKey = key.toCacheKey();

    @BeforeEach
    void setUp() {
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
    }

    @Nested
    @DisplayName("개별 재고 캐시 테스트")
    class InventoryQuantityTest {

        @Test
        @DisplayName("재고 수량 조회 - DB에서 재고 찾음")
        void getInventoryQuantity_Found() {
            // Given
            Stock stock = Stock.builder()
                    .key(key)
                    .quantity(quantity)
                    .build();

            given(stockRepository.findByKey(key))
                    .willReturn(Optional.of(stock));

            // When
            Integer result = stockCacheService.getInventoryQuantity(key);

            // Then
            assertThat(result).isEqualTo(quantity);
            verify(stockRepository).findByKey(key);
        }

        @Test
        @DisplayName("재고 수량 조회 - DB에서 재고 없음 (0 반환)")
        void getInventoryQuantity_NotFound() {
            // Given
            given(stockRepository.findByKey(key))
                    .willReturn(Optional.empty());

            // When
            Integer result = stockCacheService.getInventoryQuantity(key);

            // Then
            assertThat(result).isEqualTo(0);
            verify(stockRepository).findByKey(key);
        }

        @Test
        @DisplayName("재고 수량 갱신")
        void updateInventoryQuantity() {
            // When
            Integer result = stockCacheService.updateInventoryQuantity(key, quantity);

            // Then
            assertThat(result).isEqualTo(quantity);
        }

        @Test
        @DisplayName("재고 캐시 증가 - 성공")
        void incrementStockCache_Success() {
            // Given
            int increment = 10;

            // When
            stockCacheService.incrementStockCache(key, increment);

            // Then
            verify(valueOperations).increment(cacheKey, increment);
        }

        @Test
        @DisplayName("재고 캐시 증가 - Redis 예외 발생")
        void incrementStockCache_RedisException() {
            // Given
            int increment = 10;

            given(valueOperations.increment(cacheKey, increment))
                    .willThrow(new RuntimeException("Redis connection failed"));

            // When & Then - 예외가 삼켜지고 로그로만 기록됨
            assertThatCode(() -> stockCacheService.incrementStockCache(key, increment))
                    .doesNotThrowAnyException();

            verify(valueOperations).increment(cacheKey, increment);
        }

        @Test
        @DisplayName("재고 캐시 감소 - 성공")
        void decrementStockCache_Success() {
            // Given
            Integer decrement = 5;

            // When
            stockCacheService.decrementStockCache(key, decrement);

            // Then
            verify(valueOperations).increment(cacheKey, -decrement);
        }

        @Test
        @DisplayName("재고 캐시 감소 - Redis 예외 발생")
        void decrementStockCache_RedisException() {
            // Given
            Integer decrement = 5;

            given(valueOperations.increment(cacheKey, -decrement))
                    .willThrow(new RuntimeException("Redis connection failed"));

            // When & Then
            assertThatCode(() -> stockCacheService.decrementStockCache(key, decrement))
                    .doesNotThrowAnyException();

            verify(valueOperations).increment(cacheKey, -decrement);
        }

        @Test
        @DisplayName("재고 캐시 무효화")
        void invalidateInventoryQuantity() {
            // When & Then - 메서드 호출 자체가 @CacheEvict 동작
            assertThatCode(() -> stockCacheService.invalidateInventoryQuantity(key))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("창고 총 사용량 캐시 테스트")
    class WarehouseCurrentSumTest {

        @Test
        @DisplayName("창고 총 사용량 조회 - DB에서 데이터 찾음")
        void getWarehouseCurrentSum_Found() {
            // Given
            Integer totalCount = 150;
            given(stockRepository.getTotalPaletteCountByWarehouseId(warehouseId))
                    .willReturn(totalCount);

            // When
            Integer result = stockCacheService.getWarehouseCurrentSum(warehouseId);

            // Then
            assertThat(result).isEqualTo(totalCount);
            verify(stockRepository).getTotalPaletteCountByWarehouseId(warehouseId);
        }

        @Test
        @DisplayName("창고 총 사용량 갱신")
        void updateWarehouseCurrentSum() {
            // Given
            Integer currentSum = 200;

            // When
            Integer result = stockCacheService.updateWarehouseCurrentSum(warehouseId, currentSum);

            // Then
            assertThat(result).isEqualTo(currentSum);
        }

        @Test
        @DisplayName("창고 총 사용량 캐시 증가 - 성공")
        void incrementWarehouseCurrentSumCache_Success() {
            // Given
            int increment = 20;
            String expectedKey = String.format("warehouse:%d:currentSum", warehouseId);

            // When
            stockCacheService.incrementWarehouseCurrentSumCache(warehouseId, increment);

            // Then
            verify(valueOperations).increment(expectedKey, increment);
        }

        @Test
        @DisplayName("창고 총 사용량 캐시 증가 - Redis 예외 발생")
        void incrementWarehouseCurrentSumCache_RedisException() {
            // Given
            int increment = 20;
            String expectedKey = String.format("warehouse:%d:currentSum", warehouseId);

            given(valueOperations.increment(expectedKey, increment))
                    .willThrow(new RuntimeException("Redis connection failed"));

            // When & Then
            assertThatCode(() -> stockCacheService.incrementWarehouseCurrentSumCache(warehouseId, increment))
                    .doesNotThrowAnyException();

            verify(valueOperations).increment(expectedKey, increment);
        }

        @Test
        @DisplayName("창고 총 사용량 캐시 감소 - 성공")
        void decrementWarehouseCurrentSumCache_Success() {
            // Given
            Integer decrement = 15;
            String expectedKey = String.format("warehouse:%d:currentSum", warehouseId);

            // When
            stockCacheService.decrementWarehouseCurrentSumCache(warehouseId, decrement);

            // Then
            verify(valueOperations).increment(expectedKey, -decrement);
        }

        @Test
        @DisplayName("창고 총 사용량 캐시 감소 - Redis 예외 발생")
        void decrementWarehouseCurrentSumCache_RedisException() {
            // Given
            Integer decrement = 15;
            String expectedKey = String.format("warehouse:%d:currentSum", warehouseId);

            given(valueOperations.increment(expectedKey, -decrement))
                    .willThrow(new RuntimeException("Redis connection failed"));

            // When & Then
            assertThatCode(() -> stockCacheService.decrementWarehouseCurrentSumCache(warehouseId, decrement))
                    .doesNotThrowAnyException();

            verify(valueOperations).increment(expectedKey, -decrement);
        }

        @Test
        @DisplayName("창고 총 사용량 캐시 무효화")
        void invalidateWarehouseCurrentSum() {
            // When & Then - 메서드 호출 자체가 @CacheEvict 동작
            assertThatCode(() -> stockCacheService.invalidateWarehouseCurrentSum(warehouseId))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("이벤트 리스너 테스트")
    class EventListenerTest {

        @Test
        @DisplayName("재고 생성 이벤트 처리")
        void onStockCreated() {
            // Given
            StockCreatedEvent event = new StockCreatedEvent(
                    Stock.builder()
                            .key(key)
                            .quantity(quantity)
                            .build()
            );

            // When
            stockCacheService.onStockCreated(event);

            // Then
            // 개별 재고 캐시 갱신 확인은 실제로는 @CachePut으로 처리되므로 직접 검증하기 어려움
            // 창고 총 사용량 캐시 증가 확인
            String expectedKey = String.format("warehouse:%d:currentSum", warehouseId);
            verify(valueOperations).increment(expectedKey, quantity);
        }

        @Test
        @DisplayName("재고 수정 이벤트 처리 - 수량 증가")
        void onStockUpdated_QuantityIncrease() {
            // Given
            Integer oldQuantity = 30;
            Integer newQuantity = 50;

            Stock stock = Stock.builder()
                    .key(key)
                    .quantity(newQuantity)
                    .build();

            StockUpdatedEvent event = new StockUpdatedEvent(stock, oldQuantity);

            // When
            stockCacheService.onStockUpdated(event);

            // Then
            // 창고 총 사용량 캐시 증가 확인 (50 - 30 = 20)
            String expectedKey = String.format("warehouse:%d:currentSum", warehouseId);
            verify(valueOperations).increment(expectedKey, 20);
        }

        @Test
        @DisplayName("재고 수정 이벤트 처리 - 수량 감소")
        void onStockUpdated_QuantityDecrease() {
            // Given
            Integer oldQuantity = 70;
            Integer newQuantity = 40;

            Stock stock = Stock.builder()
                    .key(key)
                    .quantity(newQuantity)
                    .build();

            StockUpdatedEvent event = new StockUpdatedEvent(stock, oldQuantity);

            // When
            stockCacheService.onStockUpdated(event);

            // Then
            // 창고 총 사용량 캐시 감소 확인 (70 - 40 = 30, 감소이므로 -30)
            String expectedKey = String.format("warehouse:%d:currentSum", warehouseId);
            verify(valueOperations).increment(expectedKey, -30);
        }

        @Test
        @DisplayName("재고 수정 이벤트 처리 - 수량 동일 (변화 없음)")
        void onStockUpdated_QuantitySame() {
            // Given
            Integer oldQuantity = 50;
            Integer newQuantity = 50;

            Stock stock = Stock.builder()
                    .key(key)
                    .quantity(newQuantity)
                    .build();

            StockUpdatedEvent event = new StockUpdatedEvent(stock, oldQuantity);

            // When
            stockCacheService.onStockUpdated(event);

            // Then
            // 수량 변화가 없으므로 increment 호출되지 않음
            String expectedKey = String.format("warehouse:%d:currentSum", warehouseId);
            verify(valueOperations, never()).increment(eq(expectedKey), anyInt());
        }

        @Test
        @DisplayName("재고 삭제 이벤트 처리")
        void onStockDeleted() {
            // Given
            Stock stock = Stock.builder()
                    .key(key)
                    .quantity(quantity)
                    .build();

            StockDeletedEvent event = new StockDeletedEvent(stock);
            
            // Redis에서 현재 창고 총량이 충분하다고 가정
            String warehouseKey = String.format("warehouse:%d:currentSum", warehouseId);
            given(redisTemplate.opsForValue()).willReturn(valueOperations);
            given(valueOperations.get(warehouseKey)).willReturn(100); // 50보다 큰 값

            // When
            stockCacheService.onStockDeleted(event);

            // Then
            // 재고 캐시 완전 삭제 확인
            verify(redisTemplate).delete(key.toCacheKey());
            
            // 창고 총 사용량이 충분할 때만 감소 처리 확인
            verify(valueOperations).increment(warehouseKey, -quantity);
        }
    }

    @Nested
    @DisplayName("캐시 키 생성 테스트")
    class CacheKeyTest {

        @Test
        @DisplayName("개별 재고 캐시 키 형식 검증")
        void stockCacheKeyFormat() {
            // Given
            Long wareId = 123L;
            Long warehouseId = 456L;
            int increment = 10;
            StockKey key = StockKey.of(wareId, warehouseId);
            

            // When
            stockCacheService.incrementStockCache(key, increment);

            // Then
            String cacheKey = "current_stock:456:123";
            verify(valueOperations).increment(cacheKey, increment);
        }

        @Test
        @DisplayName("창고 총량 캐시 키 형식 검증")
        void warehouseCurrentSumCacheKeyFormat() {
            // Given
            Long warehouseId = 789L;
            int increment = 20;

            // When
            stockCacheService.incrementWarehouseCurrentSumCache(warehouseId, increment);

            // Then
            String expectedKey = "warehouse:789:currentSum";
            verify(valueOperations).increment(expectedKey, increment);
        }
    }

    @Nested
    @DisplayName("이벤트 필드 접근 테스트")
    class EventFieldAccessTest {

        @Test
        @DisplayName("StockCreatedEvent 필드 접근")
        void stockCreatedEventFields() {
            // Given
            Stock stock = Stock.builder()
                    .key(StockKey.of(wareId, warehouseId))
                    .quantity(quantity)
                    .build();

            StockCreatedEvent event = new StockCreatedEvent(stock);

            // When & Then
            assertThat(event.getKey().getWareId()).isEqualTo(wareId);
            assertThat(event.getKey().getWarehouseId()).isEqualTo(warehouseId);
            assertThat(event.getQuantity()).isEqualTo(quantity);
        }

        @Test
        @DisplayName("StockUpdatedEvent 필드 접근")
        void stockUpdatedEventFields() {
            // Given
            Integer oldQuantity = 30;
            Integer newQuantity = 60;

            Stock stock = Stock.builder()
                    .key(StockKey.of(wareId, warehouseId))
                    .quantity(newQuantity)
                    .build();

            StockUpdatedEvent event = new StockUpdatedEvent(stock, oldQuantity);

            // When & Then
            assertThat(event.getKey().getWareId()).isEqualTo(wareId);
            assertThat(event.getKey().getWarehouseId()).isEqualTo(warehouseId);
            assertThat(event.getNewQuantity()).isEqualTo(newQuantity);
            assertThat(event.getOldQuantity()).isEqualTo(oldQuantity);
        }

        @Test
        @DisplayName("StockDeletedEvent 필드 접근")
        void stockDeletedEventFields() {
            // Given
            Stock stock = Stock.builder()
                    .key(StockKey.of(wareId, warehouseId))
                    .quantity(quantity)
                    .build();

            StockDeletedEvent event = new StockDeletedEvent(stock);

            // When & Then
            assertThat(event.getKey().getWareId()).isEqualTo(wareId);
            assertThat(event.getKey().getWarehouseId()).isEqualTo(warehouseId);
            assertThat(event.getQuantity()).isEqualTo(quantity);
        }
    }

    @Nested
    @DisplayName("경계값 테스트")
    class BoundaryValueTest {

        @Test
        @DisplayName("큰 수량 값 처리")
        void handleLargeQuantity() {
            // Given
            Integer largeQuantity = Integer.MAX_VALUE;

            // When
            stockCacheService.incrementWarehouseCurrentSumCache(warehouseId, largeQuantity);

            // Then
            String expectedKey = String.format("warehouse:%d:currentSum", warehouseId);
            verify(valueOperations).increment(expectedKey, (long) largeQuantity);
        }
    }

    @Nested
    @DisplayName("통합 시나리오 테스트")
    class IntegrationScenarioTest {

        @Test
        @DisplayName("전체 재고 라이프사이클 캐시 처리")
        void stockLifecycleCacheHandling() {
            // Given
            Stock stock = Stock.builder()
                    .key(StockKey.of(wareId, warehouseId))
                    .quantity(100)
                    .build();

            String expectedKey = String.format("warehouse:%d:currentSum", warehouseId);

            // 1. 재고 생성
            StockCreatedEvent createEvent = new StockCreatedEvent(stock);
            stockCacheService.onStockCreated(createEvent);

            // 2. 재고 수정 (100 -> 150)
            stock.updateQuantityAndCheckDeletion(150);
            StockUpdatedEvent updateEvent = new StockUpdatedEvent(stock, 100);
            stockCacheService.onStockUpdated(updateEvent);

            // 3. 재고 삭제 - 창고에 충분한 재고가 있다고 가정
            given(redisTemplate.opsForValue()).willReturn(valueOperations); // 추가
            given(valueOperations.get(expectedKey)).willReturn(200); // 150보다 큰 값으로 설정
            StockDeletedEvent deleteEvent = new StockDeletedEvent(stock);
            stockCacheService.onStockDeleted(deleteEvent);

            // Then - 창고 총량 캐시 변화 추적
            // 생성 시 +100, 수정 시 +50 (차이), 삭제 시 -150
            ArgumentCaptor<Long> incrementCaptor = ArgumentCaptor.forClass(Long.class);
            verify(valueOperations, times(3)).increment(eq(expectedKey), incrementCaptor.capture());

            assertThat(incrementCaptor.getAllValues()).containsExactly(100L, 50L, -150L);
        }
    }
}