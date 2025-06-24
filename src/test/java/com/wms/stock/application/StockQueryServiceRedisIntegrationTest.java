package com.wms.stock.application;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("StockQueryService Redis 통합 테스트")
class StockQueryServiceRedisIntegrationTest {

	@Autowired
	private StockQueryService stockQueryService;

	@Autowired
	private RedisTemplate<String, Object> redisTemplate;

	private static final String TEST_KEY_PREFIX = "test_current_stock:";

	@BeforeEach
	void setUp() {
		// 기존 테스트 키들 정리
		cleanupTestKeys();

		// 테스트용 캐시 데이터 설정
		redisTemplate.opsForValue().set("current_stock:1:10", 100); // 창고1, 물품10, 수량100
		redisTemplate.opsForValue().set("current_stock:1:20", 50);  // 창고1, 물품20, 수량50
		redisTemplate.opsForValue().set("current_stock:2:10", 75);  // 창고2, 물품10, 수량75
		redisTemplate.opsForValue().set("current_stock:2:30", 25);  // 창고2, 물품30, 수량25
	}

	@AfterEach
	void tearDown() {
		// 테스트 후 캐시 정리
		cleanupTestKeys();
	}

	private void cleanupTestKeys() {
		// 테스트용 키들 정리
		Set<String> keys = redisTemplate.keys("current_stock:*");
		if (keys != null && !keys.isEmpty()) {
			redisTemplate.delete(keys);
		}

		// 추가 테스트 키들도 정리
		Set<String> testKeys = redisTemplate.keys(TEST_KEY_PREFIX + "*");
		if (testKeys != null && !testKeys.isEmpty()) {
			redisTemplate.delete(testKeys);
		}
	}

	@Test
	@DisplayName("Redis SCAN - 전체 재고 캐시 조회")
	void getAllStocksFromCache_RealRedis() {
		// When
		List<StockQueryService.CachedStockInfo> result = stockQueryService.getAllStocksFromCache();

		// Then
		assertThat(result).hasSize(4);

		// 창고1 데이터 검증
		List<StockQueryService.CachedStockInfo> warehouse1Stocks = result.stream()
				.filter(stock -> stock.warehouseId().equals(1L))
				.toList();
		assertThat(warehouse1Stocks).hasSize(2);
		assertThat(warehouse1Stocks).extracting("quantity").containsExactlyInAnyOrder(100, 50);

		// 창고2 데이터 검증
		List<StockQueryService.CachedStockInfo> warehouse2Stocks = result.stream()
				.filter(stock -> stock.warehouseId().equals(2L))
				.toList();
		assertThat(warehouse2Stocks).hasSize(2);
		assertThat(warehouse2Stocks).extracting("quantity").containsExactlyInAnyOrder(75, 25);
	}

	@Test
	@DisplayName("Redis SCAN - 창고별 그룹핑")
	void getStocksGroupedByWarehouse_RealRedis() {
		// When
		Map<Long, List<StockQueryService.CachedStockInfo>> result =
				stockQueryService.getStocksGroupedByWarehouse();

		// Then
		assertThat(result).hasSize(2);
		assertThat(result).containsKeys(1L, 2L);

		// 창고1 검증
		List<StockQueryService.CachedStockInfo> warehouse1 = result.get(1L);
		assertThat(warehouse1).hasSize(2);
		assertThat(warehouse1).extracting("wareId").containsExactlyInAnyOrder(10L, 20L);

		// 창고2 검증
		List<StockQueryService.CachedStockInfo> warehouse2 = result.get(2L);
		assertThat(warehouse2).hasSize(2);
		assertThat(warehouse2).extracting("wareId").containsExactlyInAnyOrder(10L, 30L);
	}

	@Test
	@DisplayName("Redis SCAN - 물품별 그룹핑")
	void getStocksGroupedByWare_RealRedis() {
		// When
		Map<Long, List<StockQueryService.CachedStockInfo>> result =
				stockQueryService.getStocksGroupedByWare();

		// Then
		assertThat(result).hasSize(3);
		assertThat(result).containsKeys(10L, 20L, 30L);

		// 물품10 검증 (창고1, 창고2에 모두 있음)
		List<StockQueryService.CachedStockInfo> ware10 = result.get(10L);
		assertThat(ware10).hasSize(2);
		assertThat(ware10).extracting("warehouseId").containsExactlyInAnyOrder(1L, 2L);
		assertThat(ware10).extracting("quantity").containsExactlyInAnyOrder(100, 75);

		// 물품20 검증 (창고1에만 있음)
		List<StockQueryService.CachedStockInfo> ware20 = result.get(20L);
		assertThat(ware20).hasSize(1);
		assertThat(ware20.get(0).warehouseId()).isEqualTo(1L);
		assertThat(ware20.get(0).quantity()).isEqualTo(50);
	}

	@Test
	@DisplayName("Redis SCAN - 특정 창고 재고 조회")
	void scanWarehouseStocksFromCache_RealRedis() {
		// When
		Map<Long, Integer> warehouse1Stocks = stockQueryService.scanWarehouseStocksFromCache(1L);

		// Then
		assertThat(warehouse1Stocks).hasSize(2);
		assertThat(warehouse1Stocks).containsEntry(10L, 100);
		assertThat(warehouse1Stocks).containsEntry(20L, 50);
		assertThat(warehouse1Stocks).doesNotContainKey(30L); // 창고2에만 있음
	}

	@Test
	@DisplayName("Redis SCAN - 정확한 개수 테스트")
	void scanExactCount_SmallDataSet() {
		// Given - 기존 데이터 정리 후 새 데이터 생성
		cleanupTestKeys();

		int expectedCount = 5;
		for (int i = 1; i <= expectedCount; i++) {
			String key = String.format("current_stock:100:%d", i);
			redisTemplate.opsForValue().set(key, i * 10);
		}

		// When
		List<StockQueryService.CachedStockInfo> result = stockQueryService.getAllStocksFromCache();

		// Then
		assertThat(result).hasSize(expectedCount);
		assertThat(result).extracting("warehouseId").allMatch(id -> id.equals(100L));
		assertThat(result).extracting("quantity").allMatch(q -> (Integer) q > 0);
	}

	@Test
	@DisplayName("Redis SCAN - 중간 크기 데이터셋 성능 테스트")
	void scanMediumDataSet_Performance() {
		// Given - 기존 데이터 정리
		cleanupTestKeys();

		int warehouses = 5;
		int waresPerWarehouse = 20;
		int expectedTotal = warehouses * waresPerWarehouse;

		// 중간 크기 데이터 생성 (중복 없는 범위 사용)
		for (int warehouseId = 200; warehouseId < 200 + warehouses; warehouseId++) {
			for (int wareId = 1000; wareId < 1000 + waresPerWarehouse; wareId++) {
				String key = String.format("current_stock:%d:%d", warehouseId, wareId);
				int quantity = (int) (Math.random() * 100) + 1; // 1~100
				redisTemplate.opsForValue().set(key, quantity);
			}
		}

		// When
		long startTime = System.currentTimeMillis();
		List<StockQueryService.CachedStockInfo> result = stockQueryService.getAllStocksFromCache();
		long endTime = System.currentTimeMillis();

		// Then
		assertThat(result).hasSize(expectedTotal);
		assertThat(endTime - startTime).isLessThan(2000); // 2초 이내

		// 데이터 검증
		assertThat(result).allMatch(stock -> stock.quantity() > 0);
		assertThat(result).allMatch(stock -> stock.warehouseId() >= 200);
		assertThat(result).allMatch(stock -> stock.wareId() >= 1000);

		System.out.println("처리된 키 개수: " + result.size());
		System.out.println("처리 시간: " + (endTime - startTime) + "ms");
	}

	@Test
	@DisplayName("Redis SCAN - SCAN 커서 동작 검증")
	void scanCursorBehavior() {
		// Given
		cleanupTestKeys();

		// 테스트 데이터 생성
		for (int i = 1; i <= 15; i++) {
			String key = String.format("current_stock:300:%d", i);
			redisTemplate.opsForValue().set(key, i);
		}

		// When - 여러 번 호출하여 일관성 확인
		List<StockQueryService.CachedStockInfo> result1 = stockQueryService.getAllStocksFromCache();
		List<StockQueryService.CachedStockInfo> result2 = stockQueryService.getAllStocksFromCache();
		List<StockQueryService.CachedStockInfo> result3 = stockQueryService.getAllStocksFromCache();

		// Then - 일관된 결과 반환
		assertThat(result1).hasSize(15);
		assertThat(result2).hasSize(15);
		assertThat(result3).hasSize(15);

		// 내용도 동일한지 확인 (순서는 다를 수 있음)
		assertThat(result1).containsExactlyInAnyOrderElementsOf(result2);
		assertThat(result2).containsExactlyInAnyOrderElementsOf(result3);
	}

	@Test
	@DisplayName("Redis SCAN - 잘못된 키 패턴 필터링")
	void scanWithInvalidKeys_Filtering() {
		// Given - 잘못된 패턴의 키들 추가
		redisTemplate.opsForValue().set("wrong_pattern:1:10", 100);
		redisTemplate.opsForValue().set("current_stock:invalid:10", 50);
		redisTemplate.opsForValue().set("current_stock:1", 75); // 불완전한 키
		redisTemplate.opsForValue().set("current_stock:1:20:extra", 25); // 너무 긴 키

		// When
		List<StockQueryService.CachedStockInfo> result = stockQueryService.getAllStocksFromCache();

		// Then - 올바른 패턴의 키만 처리됨 (기존 4개만)
		assertThat(result).hasSize(4);
		assertThat(result).allMatch(stock ->
				stock.warehouseId() != null &&
						stock.wareId() != null &&
						stock.quantity() > 0
		);

		// 정리 - 잘못된 키들 삭제
		redisTemplate.delete("wrong_pattern:1:10");
		redisTemplate.delete("current_stock:invalid:10");
		redisTemplate.delete("current_stock:1");
		redisTemplate.delete("current_stock:1:20:extra");
	}

	@Test
	@DisplayName("Redis SCAN - 0 수량 필터링")
	void scanWithZeroQuantity_Filtering() {
		// Given - 0 수량 키들 추가
		redisTemplate.opsForValue().set("current_stock:999:1", 100);
		redisTemplate.opsForValue().set("current_stock:999:2", 0);   // 0 값
		redisTemplate.opsForValue().set("current_stock:999:3", null); // null 값

		// When
		List<StockQueryService.CachedStockInfo> result = stockQueryService.getAllStocksFromCache();

		// 999 창고의 결과만 필터링
		List<StockQueryService.CachedStockInfo> warehouse999 = result.stream()
				.filter(stock -> stock.warehouseId().equals(999L))
				.toList();

		// Then - 0과 null 값은 필터링됨
		assertThat(warehouse999).hasSize(1);
		assertThat(warehouse999.get(0).quantity()).isEqualTo(100);

		// 정리
		redisTemplate.delete("current_stock:999:1");
		redisTemplate.delete("current_stock:999:2");
		redisTemplate.delete("current_stock:999:3");
	}

	@Test
	@DisplayName("Redis SCAN - 빈 캐시에서의 동작")
	void scanEmptyCache() {
		// Given - 모든 캐시 삭제
		cleanupTestKeys();

		// When
		List<StockQueryService.CachedStockInfo> result = stockQueryService.getAllStocksFromCache();

		// Then
		assertThat(result).isEmpty();
	}

	@Test
	@DisplayName("Redis 키 패턴 디버깅")
	void debugRedisKeyPatterns() {
		// Given
		cleanupTestKeys();

		// 다양한 패턴의 키 생성
		redisTemplate.opsForValue().set("current_stock:1:1", 10);
		redisTemplate.opsForValue().set("current_stock:999:999", 20);
		redisTemplate.opsForValue().set("current_stock:1000:1000", 30);

		// Redis에 실제로 저장된 키들 확인
		Set<String> allKeys = redisTemplate.keys("current_stock:*");
		System.out.println("Redis에 존재하는 키들: " + allKeys);

		// 각 키의 값 확인
		allKeys.forEach(key -> {
			Object value = redisTemplate.opsForValue().get(key);
			System.out.println(key + " = " + value);
		});

		// When
		List<StockQueryService.CachedStockInfo> result = stockQueryService.getAllStocksFromCache();

		// Then
		System.out.println("SCAN 결과: " + result);
		assertThat(result).hasSize(3);
		assertThat(result).extracting("quantity").containsExactlyInAnyOrder(10, 20, 30);
	}

	@Test
	@DisplayName("Redis SCAN - 스케일링 테스트")
	void scanScalingTest() {
		// Given
		cleanupTestKeys();

		// 단계별로 데이터 증가시키며 테스트
		int[] dataSizes = {10, 50, 100};

		for (int dataSize : dataSizes) {
			// 이전 데이터 정리
			cleanupTestKeys();

			// 새로운 크기의 데이터 생성
			for (int i = 1; i <= dataSize; i++) {
				String key = String.format("current_stock:500:%d", i);
				redisTemplate.opsForValue().set(key, i);
			}

			// 성능 측정
			long startTime = System.currentTimeMillis();
			List<StockQueryService.CachedStockInfo> result = stockQueryService.getAllStocksFromCache();
			long endTime = System.currentTimeMillis();

			// 검증
			assertThat(result).hasSize(dataSize);
			System.out.println("데이터 크기: " + dataSize + ", 처리 시간: " + (endTime - startTime) + "ms");

			// 성능이 선형적으로 증가하는지 확인 (너무 느려지지 않는지)
			assertThat(endTime - startTime).isLessThan(dataSize * 10); // 항목당 10ms 이내
		}
	}
}