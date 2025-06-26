package com.wms.stock.application;

import com.wms.applicationInfra.idnameMapCashing.concrete.LocationCacheManager;
import com.wms.applicationInfra.idnameMapCashing.concrete.WareCacheManager;
import com.wms.location.application.LocationCacheService;
import com.wms.location.domain.exception.LocationException;
import com.wms.location.domain.repository.LocationRepository;
import com.wms.stock.domain.model.Stock;
import com.wms.stock.domain.model.StockKey;
import com.wms.stock.domain.repository.StockRepository;
import com.wms.stock.dto.StockQueryDTO;
import com.wms.ware.domain.exception.WareException;
import com.wms.ware.domain.repository.WareRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.ValueOperations;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("StockQueryService 단위 테스트")
class StockQueryServiceTest {

	@InjectMocks
	private StockQueryService stockQueryService;

	@Mock
	private StockRepository stockRepository;

	@Mock
	private LocationRepository locationRepository;

	@Mock
	private WareRepository wareRepository;

	@Mock
	private StockCacheService stockCacheService;

	@Mock
	private LocationCacheService locationCacheService;

	@Mock
	private RedisTemplate<String, Object> redisTemplate;

	@Mock
	private LocationCacheManager locationCacheManager;

	@Mock
	private WareCacheManager wareCacheManager;

	@Mock
	private ValueOperations<String, Object> valueOperations;

	@Mock
	private Cursor<String> cursor;

	@BeforeEach
	void setUp() {
		// Redis 기본 Mock 설정 - 모든 SCAN 관련 테스트에서 기본적으로 빈 결과 반환
		given(redisTemplate.scan(any(ScanOptions.class))).willReturn(cursor);
		given(redisTemplate.opsForValue()).willReturn(valueOperations);
		given(cursor.hasNext()).willReturn(false);
	}

	// ========== 기본 조회 메서드 테스트 ==========

	@Test
	@DisplayName("재고 수량 조회 - 캐시 히트")
	void getStockQuantity_CacheHit() {
		// Given
		Long warehouseId = 1L;
		Long wareId = 2L;
		StockKey key = StockKey.of(wareId, warehouseId);

		Integer expectedQuantity = 100;

		given(stockCacheService.getInventoryQuantity(key))
				.willReturn(expectedQuantity);

		// When
		Integer result = stockQueryService.getStockQuantity(warehouseId, wareId);

		// Then
		assertThat(result).isEqualTo(expectedQuantity);
		verify(stockCacheService).getInventoryQuantity(key);
	}

	@Test
	@DisplayName("재고 수량 조회 - 캐시 미스")
	void getStockQuantity_CacheMiss() {
		// Given
		Long warehouseId = 1L;
		Long wareId = 2L;
		StockKey key = StockKey.of(wareId, warehouseId);

		given(stockCacheService.getInventoryQuantity(key))
				.willReturn(null);

		// When
		Integer result = stockQueryService.getStockQuantity(warehouseId, wareId);

		// Then
		assertThat(result).isNull();
		verify(stockCacheService).getInventoryQuantity(key);
	}

	@Test
	@DisplayName("창고-물품별 재고 조회 - 캐시 우선")
	void getStockResByWarehouseAndWare_CacheFirst() {
		// Given
		Long warehouseId = 1L;
		Long wareId = 2L;
		StockKey key = StockKey.of(wareId, warehouseId);
		Integer quantity = 100;

		given(stockCacheService.getInventoryQuantity(key))
				.willReturn(quantity);
		given(wareCacheManager.getName(wareId)).willReturn("물품A");
		given(locationCacheManager.getName(warehouseId)).willReturn("창고1");

		// When
		StockQueryDTO.Res result = stockQueryService.getStockResByWarehouseAndWare(warehouseId, wareId);

		// Then
		// 수정: null 체크 제거하고 직접 검증
		assertThat(result).isNotNull();
		assertThat(result.getWareId()).isEqualTo(wareId);
		assertThat(result.getWarehouseId()).isEqualTo(warehouseId);
		assertThat(result.getQuantity()).isEqualTo(quantity);
		assertThat(result.getWareName()).isEqualTo("물품A");
		assertThat(result.getWarehouseName()).isEqualTo("창고1");

		verify(stockRepository, never()).findByKey(any(StockKey.class));
	}

	@Test
	@DisplayName("창고-물품별 재고 조회 - 캐시 미스로 DB 조회")
	void getStockResByWarehouseAndWare_DBFallback() {
		// Given
		Long warehouseId = 1L;
		Long wareId = 2L;
		StockKey key = StockKey.of(wareId, warehouseId);

		Stock dbStock = Stock.builder()
				.key(key)
				.quantity(50)
				.build();

		given(stockCacheService.getInventoryQuantity(key))
				.willReturn(0); // 캐시에서 0 반환
		given(stockRepository.findByKey(key))
				.willReturn(Optional.of(dbStock));
		given(wareCacheManager.getName(wareId)).willReturn("물품A");
		given(locationCacheManager.getName(warehouseId)).willReturn("창고1");

		// When
		StockQueryDTO.Res result = stockQueryService.getStockResByWarehouseAndWare(warehouseId, wareId);

		// Then
		// 수정: null 체크 제거하고 직접 검증
		assertThat(result).isNotNull();
		assertThat(result.getQuantity()).isEqualTo(50);
		verify(stockRepository).findByKey(key);
	}

	@Test
	@DisplayName("창고별 재고 조회 - DB 조회 (캐시 없음)")
	void getStocksByWarehouse_DBFallback() {
		// Given
		Long warehouseId = 1L;

		List<Stock> dbStocks = Arrays.asList(
				Stock.builder().key(StockKey.of(10L, warehouseId)).quantity(100).build(),
				Stock.builder().key(StockKey.of(20L, warehouseId)).quantity(50).build()
		);

		given(locationRepository.existsById(warehouseId)).willReturn(true);
		// Redis SCAN은 setUp()에서 빈 결과로 설정됨
		given(stockRepository.findAllByKeyWarehouseId(warehouseId)).willReturn(dbStocks);
		given(wareCacheManager.getName(10L)).willReturn("물품A");
		given(wareCacheManager.getName(20L)).willReturn("물품B");
		given(locationCacheManager.getName(warehouseId)).willReturn("창고1");

		// When
		List<StockQueryDTO.Res> result = stockQueryService.getStocksByWarehouse(warehouseId);

		// Then
		assertThat(result).hasSize(2);
		assertThat(result).extracting("wareId").containsExactlyInAnyOrder(10L, 20L);
		assertThat(result).extracting("quantity").containsExactlyInAnyOrder(100, 50);
		verify(stockRepository).findAllByKeyWarehouseId(warehouseId);
	}

	@Test
	@DisplayName("물품별 재고 조회 - DB 직접 조회")
	void getStocksByWare_DirectDB() {
		// Given
		Long wareId = 1L;

		List<Stock> dbStocks = Arrays.asList(
				Stock.builder().key(StockKey.of(wareId, 10L)).quantity(100).build(),
				Stock.builder().key(StockKey.of(wareId, 20L)).quantity(50).build()
		);

		given(wareRepository.existsById(wareId)).willReturn(true);
		given(stockRepository.findAllByKeyWareId(wareId)).willReturn(dbStocks);
		given(wareCacheManager.getName(wareId)).willReturn("물품A");
		given(locationCacheManager.getName(10L)).willReturn("창고1");
		given(locationCacheManager.getName(20L)).willReturn("창고2");

		// When
		List<StockQueryDTO.Res> result = stockQueryService.getStocksByWare(wareId);

		// Then
		assertThat(result).hasSize(2);
		assertThat(result).extracting("warehouseId").containsExactlyInAnyOrder(10L, 20L);
		// 수정: quantity 기댓값을 100, 50으로 변경 (Mock 데이터와 일치)
		assertThat(result).extracting("quantity").containsExactlyInAnyOrder(100, 50);
		verify(stockRepository).findAllByKeyWareId(wareId);
	}

	// ========== 집계 조회 메서드 테스트 ==========

	@Test
	@DisplayName("창고 집계 조회 - 빈 창고")
	void getWarehouseAggregation_EmptyWarehouse() {
		// Given
		Long warehouseId = 1L;
		String warehouseName = "빈창고";
		Integer capacity = 500;

		given(locationRepository.existsById(warehouseId)).willReturn(true);
		given(locationCacheManager.getName(warehouseId)).willReturn(warehouseName);
		given(locationCacheService.getWarehouseCapacity(warehouseId)).willReturn(capacity);
		given(stockCacheService.getWarehouseCurrentSum(warehouseId)).willReturn(0);

		// When
		StockQueryDTO.WarehouseAggregationRes result = stockQueryService.getWarehouseAggregation(warehouseId);

		// Then
		assertThat(result.getWarehouseId()).isEqualTo(warehouseId);
		assertThat(result.getWarehouseName()).isEqualTo(warehouseName);
		assertThat(result.getTotalQuantity()).isEqualTo(0);
		assertThat(result.getCapacity()).isEqualTo(capacity);
		assertThat(result.getWareTypeCount()).isEqualTo(0);
		assertThat(result.getStockList()).isEmpty();
	}

	@Test
	@DisplayName("창고 집계 조회 - DB 폴백")
	void getWarehouseAggregation_DBFallback() {
		// Given
		Long warehouseId = 1L;
		String warehouseName = "창고1";
		Integer capacity = 1000;
		Integer totalQuantity = 300;

		// DB 데이터
		StockRepository.WarehouseStockSummary mockSummary = mock(StockRepository.WarehouseStockSummary.class);
		given(mockSummary.getWarehouseId()).willReturn(warehouseId);
		given(mockSummary.getTotalQuantity()).willReturn(300L);
		given(mockSummary.getWareTypeCount()).willReturn(2L);

		List<Stock> stocks = Arrays.asList(
				Stock.builder().key(StockKey.of(10L, warehouseId)).quantity(100).build(),
				Stock.builder().key(StockKey.of(20L, warehouseId)).quantity(50).build()
		);

		given(locationRepository.existsById(warehouseId)).willReturn(true);
		given(locationCacheManager.getName(warehouseId)).willReturn(warehouseName);
		given(locationCacheService.getWarehouseCapacity(warehouseId)).willReturn(capacity);
		given(stockCacheService.getWarehouseCurrentSum(warehouseId)).willReturn(totalQuantity);
		// Redis SCAN은 빈 결과 (setUp에서 설정됨)
		given(stockRepository.findWarehouseStockSummary(warehouseId)).willReturn(Optional.of(mockSummary));
		given(stockRepository.findAllByKeyWarehouseId(warehouseId)).willReturn(stocks);
		given(wareCacheManager.getName(10L)).willReturn("물품A");
		given(wareCacheManager.getName(20L)).willReturn("물품B");

		// When
		StockQueryDTO.WarehouseAggregationRes result = stockQueryService.getWarehouseAggregation(warehouseId);

		// Then
		assertThat(result.getWarehouseId()).isEqualTo(warehouseId);
		assertThat(result.getWarehouseName()).isEqualTo(warehouseName);
		assertThat(result.getTotalQuantity()).isEqualTo(totalQuantity);
		assertThat(result.getCapacity()).isEqualTo(capacity);
		assertThat(result.getWareTypeCount()).isEqualTo(2);
		assertThat(result.getStockList()).hasSize(2);
	}

	@Test
	@DisplayName("물품 집계 조회")
	void getWareAggregation() {
		// Given
		Long wareId = 1L;
		String wareName = "전자제품A";

		StockRepository.WareStockSummary mockSummary = mock(StockRepository.WareStockSummary.class);
		given(mockSummary.getWareId()).willReturn(wareId);
		given(mockSummary.getTotalQuantity()).willReturn(200L);
		given(mockSummary.getWarehouseCount()).willReturn(3L);

		List<Stock> stocks = Arrays.asList(
				Stock.builder().key(StockKey.of(wareId, 10L)).quantity(100).build(),
				Stock.builder().key(StockKey.of(wareId, 20L)).quantity(50).build(),
				Stock.builder().key(StockKey.of(wareId, 30L)).quantity(50).build()
		);

		given(wareRepository.existsById(wareId)).willReturn(true);
		given(wareCacheManager.getName(wareId)).willReturn(wareName);
		given(stockRepository.findWareStockSummary(wareId)).willReturn(Optional.of(mockSummary));
		given(stockRepository.findAllByKeyWareId(wareId)).willReturn(stocks);
		given(locationCacheManager.getName(10L)).willReturn("창고1");
		given(locationCacheManager.getName(20L)).willReturn("창고2");
		given(locationCacheManager.getName(30L)).willReturn("창고3");

		// When
		StockQueryDTO.WareAggregationRes result = stockQueryService.getWareAggregation(wareId);

		// Then
		assertThat(result.getWareId()).isEqualTo(wareId);
		assertThat(result.getWareName()).isEqualTo(wareName);
		assertThat(result.getTotalQuantity()).isEqualTo(200);
		assertThat(result.getWarehouseCount()).isEqualTo(3);
		assertThat(result.getStockList()).hasSize(3);
	}

	// ========== 유틸리티 메서드 테스트 ==========

	@Test
	@DisplayName("창고 총 재고량 조회")
	void getWarehouseTotalQuantity() {
		// Given
		Long warehouseId = 1L;
		Integer expectedTotal = 350;

		given(stockCacheService.getWarehouseCurrentSum(warehouseId)).willReturn(expectedTotal);

		// When
		Integer result = stockQueryService.getWarehouseTotalQuantity(warehouseId);

		// Then
		assertThat(result).isEqualTo(expectedTotal);
		verify(stockCacheService).getWarehouseCurrentSum(warehouseId);
	}

	@Test
	@DisplayName("물품 총 재고량 조회")
	void getWareTotalQuantity() {
		// Given
		Long wareId = 1L;

		List<Stock> stocks = Arrays.asList(
				Stock.builder().key(StockKey.of(wareId, 10L)).quantity(100).build(),
				Stock.builder().key(StockKey.of(wareId, 20L)).quantity(80).build(),
				Stock.builder().key(StockKey.of(wareId, 30L)).quantity(70).build()
		);

		given(wareRepository.existsById(wareId)).willReturn(true);
		given(stockRepository.findAllByKeyWareId(wareId)).willReturn(stocks);

		// When
		Integer result = stockQueryService.getWareTotalQuantity(wareId);

		// Then
		assertThat(result).isEqualTo(250); // 100 + 80 + 70
	}

	@Test
	@DisplayName("창고 사용률 계산")
	void getWarehouseUtilizationRate() {
		// Given
		Long warehouseId = 1L;
		Integer capacity = 1000;
		Integer currentSum = 750;

		given(locationCacheService.getWarehouseCapacity(warehouseId)).willReturn(capacity);
		given(stockCacheService.getWarehouseCurrentSum(warehouseId)).willReturn(currentSum);

		// When
		Double result = stockQueryService.getWarehouseUtilizationRate(warehouseId);

		// Then
		assertThat(result).isEqualTo(75.0); // (750 / 1000) * 100
	}

	@Test
	@DisplayName("창고 수용 가능 여부 확인 - 가능")
	void canAddStockToWarehouse_Possible() {
		// Given
		Long warehouseId = 1L;
		Integer additionalQuantity = 100;
		Integer capacity = 1000;
		Integer currentSum = 800;

		given(locationCacheService.getWarehouseCapacity(warehouseId)).willReturn(capacity);
		given(stockCacheService.getWarehouseCurrentSum(warehouseId)).willReturn(currentSum);

		// When
		boolean result = stockQueryService.canAddStockToWarehouse(warehouseId, additionalQuantity);

		// Then
		assertThat(result).isTrue(); // 800 + 100 = 900 <= 1000
	}

	@Test
	@DisplayName("창고 수용 가능 여부 확인 - 불가능")
	void canAddStockToWarehouse_Impossible() {
		// Given
		Long warehouseId = 1L;
		Integer additionalQuantity = 300;
		Integer capacity = 1000;
		Integer currentSum = 800;

		given(locationCacheService.getWarehouseCapacity(warehouseId)).willReturn(capacity);
		given(stockCacheService.getWarehouseCurrentSum(warehouseId)).willReturn(currentSum);

		// When
		boolean result = stockQueryService.canAddStockToWarehouse(warehouseId, additionalQuantity);

		// Then
		assertThat(result).isFalse(); // 800 + 300 = 1100 > 1000
	}

	// ========== 예외 처리 테스트 ==========

	@Test
	@DisplayName("존재하지 않는 창고 조회 시 예외")
	void validateWarehouseExists_NotFound() {
		// Given
		Long nonExistentWarehouseId = 999L;

		given(locationRepository.existsById(nonExistentWarehouseId)).willReturn(false);

		// When & Then
		assertThatThrownBy(() -> stockQueryService.getStocksByWarehouse(nonExistentWarehouseId))
				.isInstanceOf(LocationException.NotFoundEx.class);
	}

	@Test
	@DisplayName("존재하지 않는 물품 조회 시 예외")
	void validateWareExists_NotFound() {
		// Given
		Long nonExistentWareId = 999L;

		given(wareRepository.existsById(nonExistentWareId)).willReturn(false);

		// When & Then
		assertThatThrownBy(() -> stockQueryService.getStocksByWare(nonExistentWareId))
				.isInstanceOf(WareException.NotFoundEx.class);
	}

	// ========== Redis 캐시 기능 테스트 (기본적인 것만) ==========

	@Test
	@DisplayName("Redis SCAN - 빈 결과")
	void getAllStocksFromCache_EmptyResult() {
		// Given - setUp()에서 이미 빈 결과로 설정됨

		// When
		List<StockQueryService.CachedStockInfo> result = stockQueryService.getAllStocksFromCache();

		// Then
		assertThat(result).isEmpty();
		verify(redisTemplate).scan(any(ScanOptions.class));
	}

	@Test
	@DisplayName("Redis SCAN - 데이터 있음 (간단한 케이스)")
	void getAllStocksFromCache_WithSimpleData() {
		// Given
		String key1 = "current_stock:1:10";
		String key2 = "current_stock:2:20";

		// Redis SCAN Mock 재설정
		given(cursor.hasNext())
				.willReturn(true)   // 첫 번째 키
				.willReturn(true)   // 두 번째 키
				.willReturn(false); // 종료
		given(cursor.next())
				.willReturn(key1)
				.willReturn(key2);
		given(valueOperations.get(key1)).willReturn(100);
		given(valueOperations.get(key2)).willReturn(50);

		// When
		List<StockQueryService.CachedStockInfo> result = stockQueryService.getAllStocksFromCache();

		// Then
		assertThat(result).hasSize(2);
		assertThat(result).extracting("wareId").containsExactlyInAnyOrder(10L, 20L);
		assertThat(result).extracting("warehouseId").containsExactlyInAnyOrder(1L, 2L);
		assertThat(result).extracting("quantity").containsExactlyInAnyOrder(100, 50);
		verify(redisTemplate).scan(any(ScanOptions.class));
	}

	// ========== Mock 검증 테스트 ==========

	@Test
	@DisplayName("캐시 우선 전략 검증")
	void verifyCacheFirstStrategy() {
		// Given
		Long warehouseId = 1L;
		Long wareId = 2L;
		StockKey key = StockKey.of(wareId, warehouseId);
		Integer cachedQuantity = 150;

		given(stockCacheService.getInventoryQuantity(key))
				.willReturn(cachedQuantity);
		given(wareCacheManager.getName(wareId)).willReturn("물품A");
		given(locationCacheManager.getName(warehouseId)).willReturn("창고1");

		// When
		StockQueryDTO.Res result = stockQueryService.getStockResByWarehouseAndWare(warehouseId, wareId);

		// Then
		// 수정: null 체크 제거하고 직접 검증
		assertThat(result).isNotNull();
		assertThat(result.getQuantity()).isEqualTo(cachedQuantity);

		verify(stockCacheService).getInventoryQuantity(key);
		verify(stockRepository, never()).findByKey(any(StockKey.class));
	}

	@Test
	@DisplayName("서비스 메서드 호출 순서 검증")
	void verifyMethodCallOrder() {
		// Given
		Long warehouseId = 1L;

		given(locationRepository.existsById(warehouseId)).willReturn(true);
		given(stockRepository.findAllByKeyWarehouseId(warehouseId))
				.willReturn(Collections.emptyList());

		// When
		stockQueryService.getStocksByWarehouse(warehouseId);

		// Then - 호출 순서 검증
		var inOrder = inOrder(locationRepository, stockRepository);
		inOrder.verify(locationRepository).existsById(warehouseId);
		inOrder.verify(stockRepository).findAllByKeyWarehouseId(warehouseId);
	}

	@Test
	@DisplayName("전체 창고 집계 조회 - 빈 캐시로 DB 조회")
	void getAllWarehouseAggregations_DBFallback() {
		// Given
		List<StockRepository.WarehouseStockSummary> summaries = Arrays.asList(
				createWarehouseSummary(1L, 300L, 2L),
				createWarehouseSummary(2L, 150L, 1L)
		);

		// Redis SCAN은 빈 결과 (setUp에서 설정됨)
		given(stockRepository.findAllWarehouseStockSummaries()).willReturn(summaries);
		given(locationCacheManager.getName(1L)).willReturn("창고1");
		given(locationCacheManager.getName(2L)).willReturn("창고2");
		given(locationCacheService.getWarehouseCapacity(1L)).willReturn(1000);
		given(locationCacheService.getWarehouseCapacity(2L)).willReturn(500);

		// When
		List<StockQueryDTO.WarehouseAggregationRes> result = stockQueryService.getAllWarehouseAggregations();

		// Then
		assertThat(result).hasSize(2);
		assertThat(result).extracting("warehouseId").containsExactlyInAnyOrder(1L, 2L);
		assertThat(result).extracting("totalQuantity").containsExactlyInAnyOrder(300, 150);
		verify(stockRepository).findAllWarehouseStockSummaries();
	}

	@Test
	@DisplayName("전체 물품 집계 조회 - 빈 캐시로 DB 조회")
	void getAllWareAggregations_DBFallback() {
		// Given
		List<StockRepository.WareStockSummary> summaries = Arrays.asList(
				createWareSummary(10L, 200L, 2L),
				createWareSummary(20L, 100L, 1L)
		);

		// Redis SCAN은 빈 결과 (setUp에서 설정됨)
		given(stockRepository.findAllWareStockSummaries()).willReturn(summaries);
		given(wareCacheManager.getName(10L)).willReturn("물품A");
		given(wareCacheManager.getName(20L)).willReturn("물품B");

		// When
		List<StockQueryDTO.WareAggregationRes> result = stockQueryService.getAllWareAggregations();

		// Then
		assertThat(result).hasSize(2);
		assertThat(result).extracting("wareId").containsExactlyInAnyOrder(10L, 20L);
		assertThat(result).extracting("totalQuantity").containsExactlyInAnyOrder(200, 100);
		verify(stockRepository).findAllWareStockSummaries();
	}

	// ========== 테스트 헬퍼 메서드 ==========

	private StockRepository.WarehouseStockSummary createWarehouseSummary(Long warehouseId, Long totalQuantity, Long wareTypeCount) {
		StockRepository.WarehouseStockSummary summary = mock(StockRepository.WarehouseStockSummary.class);
		given(summary.getWarehouseId()).willReturn(warehouseId);
		given(summary.getTotalQuantity()).willReturn(totalQuantity);
		given(summary.getWareTypeCount()).willReturn(wareTypeCount);
		return summary;
	}

	private StockRepository.WareStockSummary createWareSummary(Long wareId, Long totalQuantity, Long warehouseCount) {
		StockRepository.WareStockSummary summary = mock(StockRepository.WareStockSummary.class);
		given(summary.getWareId()).willReturn(wareId);
		given(summary.getTotalQuantity()).willReturn(totalQuantity);
		given(summary.getWarehouseCount()).willReturn(warehouseCount);
		return summary;
	}
}