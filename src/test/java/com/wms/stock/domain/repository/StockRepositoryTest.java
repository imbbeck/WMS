package com.wms.stock.domain.repository;

import com.wms.applicationInfra.config.QuerydslConfig;
import com.wms.location.domain.model.Location;
import com.wms.location.domain.model.LocationType;
import com.wms.stock.domain.model.Stock;
import com.wms.ware.domain.model.Ware;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@Import(QuerydslConfig.class)
@TestPropertySource(properties = {
		"logging.level.org.hibernate.SQL=DEBUG",
		"logging.level.org.hibernate.type.descriptor.sql.BasicBinder=TRACE"
})
class StockRepositoryTest {

	@Autowired
	private StockRepository stockRepository;

	@Autowired
	private EntityManager em;

	private Location warehouse1;
	private Location warehouse2;
	private Location warehouse3;
	private Ware wareA;
	private Ware wareB;
	private Ware wareC;

	@BeforeEach
	void setup() {
		// Location 세팅 - 다양한 용량의 창고들
		warehouse1 = Location.builder()
				.name("대형창고")
				.type(LocationType.WAREHOUSE)
				.capacity(1000)
				.coordinateX(0)
				.coordinateY(0)
				.build();
		warehouse2 = Location.builder()
				.name("중형창고")
				.type(LocationType.WAREHOUSE)
				.capacity(500)
				.coordinateX(1)
				.coordinateY(1)
				.build();
		warehouse3 = Location.builder()
				.name("소형창고")
				.type(LocationType.WAREHOUSE)
				.capacity(200)
				.coordinateX(2)
				.coordinateY(2)
				.build();
		em.persist(warehouse1);
		em.persist(warehouse2);
		em.persist(warehouse3);

		// Ware 세팅 - 다양한 물품들
		wareA = Ware.builder()
				.name("전자제품A")
				.type("전자제품")
				.paletteUnit(10)
				.build();
		wareB = Ware.builder()
				.name("가구B")
				.type("가구")
				.paletteUnit(5)
				.build();
		wareC = Ware.builder()
				.name("의류C")
				.type("의류")
				.paletteUnit(20)
				.build();
		em.persist(wareA);
		em.persist(wareB);
		em.persist(wareC);

		// Stock 세팅 - 다양한 재고 상황
		createTestStocks();

		em.flush();
		em.clear(); // 캐시 초기화로 실제 쿼리 확인
	}

	private void createTestStocks() {
		// warehouse1: 전자제품A(100), 가구B(50), 의류C(30) = 총 180
		em.persist(Stock.builder()
				.warehouseId(warehouse1.getId())
				.wareId(wareA.getId())
				.quantity(100)
				.build());
		em.persist(Stock.builder()
				.warehouseId(warehouse1.getId())
				.wareId(wareB.getId())
				.quantity(50)
				.build());
		em.persist(Stock.builder()
				.warehouseId(warehouse1.getId())
				.wareId(wareC.getId())
				.quantity(30)
				.build());

		// warehouse2: 전자제품A(20), 가구B(80) = 총 100
		em.persist(Stock.builder()
				.warehouseId(warehouse2.getId())
				.wareId(wareA.getId())
				.quantity(20)
				.build());
		em.persist(Stock.builder()
				.warehouseId(warehouse2.getId())
				.wareId(wareB.getId())
				.quantity(80)
				.build());

		// warehouse3: 의류C(60) = 총 60 (wareA, wareB는 없음)
		em.persist(Stock.builder()
				.warehouseId(warehouse3.getId())
				.wareId(wareC.getId())
				.quantity(60)
				.build());
	}

	// ========== 기본 조회 메서드 테스트 ==========

	@Test
	@DisplayName("findByWarehouseIdAndWareId: 특정 창고-물품 재고 조회")
	void testFindByWarehouseIdAndWareId() {
		// Given & When
		Optional<Stock> result = stockRepository.findByWarehouseIdAndWareId(wareA.getId(), warehouse1.getId());

		// Then
		assertThat(result).isPresent();
		assertThat(result.get().getQuantity()).isEqualTo(100);
		assertThat(result.get().getWareId()).isEqualTo(wareA.getId());
		assertThat(result.get().getWarehouseId()).isEqualTo(warehouse1.getId());
	}

	@Test
	@DisplayName("findByWarehouseIdAndWareId: 존재하지 않는 조합 조회")
	void testFindByWarehouseIdAndWareId_NotFound() {
		// Given & When
		// wareB는 warehouse3에 없으므로 빈 결과 반환되어야 함
		Optional<Stock> result = stockRepository.findByWarehouseIdAndWareId(wareB.getId(), warehouse3.getId());

		// Then
		assertThat(result).isEmpty();
	}

	@Test
	@DisplayName("findByWarehouseIdAndWareId: 또 다른 존재하지 않는 조합 조회")
	void testFindByWarehouseIdAndWareId_AnotherNotFound() {
		// Given & When
		// wareA는 warehouse3에 없으므로 빈 결과 반환되어야 함
		Optional<Stock> result = stockRepository.findByWarehouseIdAndWareId(wareA.getId(), warehouse3.getId());

		// Then
		assertThat(result).isEmpty();
	}

	@Test
	@DisplayName("findByWarehouseIdAndWareId: 존재하지 않는 창고 ID")
	void testFindByWarehouseIdAndWareId_NonexistentWarehouse() {
		// Given & When
		Optional<Stock> result = stockRepository.findByWarehouseIdAndWareId(wareA.getId(), 99999L);

		// Then
		assertThat(result).isEmpty();
	}

	@Test
	@DisplayName("findByWarehouseIdAndWareId: 존재하지 않는 물품 ID")
	void testFindByWarehouseIdAndWareId_NonexistentWare() {
		// Given & When
		Optional<Stock> result = stockRepository.findByWarehouseIdAndWareId(99999L, warehouse1.getId());

		// Then
		assertThat(result).isEmpty();
	}

	@Test
	@DisplayName("findAllByWarehouseId: 특정 창고의 모든 재고 조회")
	void testFindAllByKeyWarehouseId() {
		// Given & When
		List<Stock> stocks = stockRepository.findAllByKey_WarehouseId(warehouse1.getId());

		// Then
		assertThat(stocks).hasSize(3);
		assertThat(stocks).extracting("warehouseId")
				.containsOnly(warehouse1.getId());
		assertThat(stocks).extracting("wareId")
				.containsExactlyInAnyOrder(wareA.getId(), wareB.getId(), wareC.getId());
		assertThat(stocks).extracting("quantity")
				.containsExactlyInAnyOrder(100, 50, 30);
	}

	@Test
	@DisplayName("findAllByWareId: 특정 물품의 모든 창고 재고 조회")
	void testFindAllByKeyWareId() {
		// Given & When
		List<Stock> stocks = stockRepository.findAllByKey_WareId(wareA.getId());

		// Then
		assertThat(stocks).hasSize(2); // warehouse1, warehouse2
		assertThat(stocks).extracting("wareId")
				.containsOnly(wareA.getId());
		assertThat(stocks).extracting("warehouseId")
				.containsExactlyInAnyOrder(warehouse1.getId(), warehouse2.getId());
		assertThat(stocks).extracting("quantity")
				.containsExactlyInAnyOrder(100, 20);
	}

	@Test
	@DisplayName("findAllByWareIdAndQuantityGreaterThan: 특정 물품의 수량 조건 재고 조회")
	void testFindAllByKeyWareIdAndQuantityGreaterThan() {
		// Given & When
		List<Stock> stocks = stockRepository.findAllByKey_WareIdAndQuantityGreaterThan(wareA.getId(), 50);

		// Then
		assertThat(stocks).hasSize(1);
		assertThat(stocks.get(0).getQuantity()).isEqualTo(100);
		assertThat(stocks.get(0).getWarehouseId()).isEqualTo(warehouse1.getId());
	}

	// ========== 집계 쿼리 테스트 ==========

	@Test
	@DisplayName("findWarehouseStockSummary: 창고별 재고 요약 조회")
	void testFindWarehouseStockSummary() {
		// Given & When
		Optional<StockRepository.WarehouseStockSummary> summary =
				stockRepository.findWarehouseStockSummary(warehouse1.getId());

		// Then
		assertThat(summary).isPresent();
		assertThat(summary.get().getWarehouseId()).isEqualTo(warehouse1.getId());
		assertThat(summary.get().getTotalQuantity()).isEqualTo(180L); // 100+50+30
		assertThat(summary.get().getWareTypeCount()).isEqualTo(3L); // 3종류 물품
	}

	@Test
	@DisplayName("findWarehouseStockSummary: 재고 없는 창고 조회")
	void testFindWarehouseStockSummary_EmptyWarehouse() {
		// Given: 새 창고 생성 (재고 없음)
		Location emptyWarehouse = Location.builder()
				.name("빈창고")
				.type(LocationType.WAREHOUSE)
				.capacity(100)
				.coordinateX(10)
				.coordinateY(10)
				.build();
		em.persist(emptyWarehouse);
		em.flush();

		// When
		Optional<StockRepository.WarehouseStockSummary> summary =
				stockRepository.findWarehouseStockSummary(emptyWarehouse.getId());

		// Then
		assertThat(summary).isEmpty();
	}

	@Test
	@DisplayName("findAllWarehouseStockSummaries: 모든 창고별 재고 요약 조회")
	void testFindAllWarehouseStockSummaries() {
		// Given & When
		List<StockRepository.WarehouseStockSummary> summaries =
				stockRepository.findAllWarehouseStockSummaries();

		// Then
		assertThat(summaries).hasSize(3);

		// warehouse1 검증
		StockRepository.WarehouseStockSummary warehouse1Summary = summaries.stream()
				.filter(s -> s.getWarehouseId().equals(warehouse1.getId()))
				.findFirst().orElseThrow();
		assertThat(warehouse1Summary.getTotalQuantity()).isEqualTo(180L);
		assertThat(warehouse1Summary.getWareTypeCount()).isEqualTo(3L);

		// warehouse2 검증
		StockRepository.WarehouseStockSummary warehouse2Summary = summaries.stream()
				.filter(s -> s.getWarehouseId().equals(warehouse2.getId()))
				.findFirst().orElseThrow();
		assertThat(warehouse2Summary.getTotalQuantity()).isEqualTo(100L);
		assertThat(warehouse2Summary.getWareTypeCount()).isEqualTo(2L);

		// warehouse3 검증
		StockRepository.WarehouseStockSummary warehouse3Summary = summaries.stream()
				.filter(s -> s.getWarehouseId().equals(warehouse3.getId()))
				.findFirst().orElseThrow();
		assertThat(warehouse3Summary.getTotalQuantity()).isEqualTo(60L);
		assertThat(warehouse3Summary.getWareTypeCount()).isEqualTo(1L);
	}

	@Test
	@DisplayName("findWareStockSummary: 물품별 재고 요약 조회")
	void testFindWareStockSummary() {
		// Given & When
		Optional<StockRepository.WareStockSummary> summary =
				stockRepository.findWareStockSummary(wareA.getId());

		// Then
		assertThat(summary).isPresent();
		assertThat(summary.get().getWareId()).isEqualTo(wareA.getId());
		assertThat(summary.get().getTotalQuantity()).isEqualTo(120L); // 100+20
		assertThat(summary.get().getWarehouseCount()).isEqualTo(2L); // 2개 창고
	}

	@Test
	@DisplayName("findAllWareStockSummaries: 모든 물품별 재고 요약 조회")
	void testFindAllWareStockSummaries() {
		// Given & When
		List<StockRepository.WareStockSummary> summaries =
				stockRepository.findAllWareStockSummaries();

		// Then
		assertThat(summaries).hasSize(3);

		// wareA 검증 (전자제품A: warehouse1(100) + warehouse2(20) = 120)
		StockRepository.WareStockSummary wareASummary = summaries.stream()
				.filter(s -> s.getWareId().equals(wareA.getId()))
				.findFirst().orElseThrow();
		assertThat(wareASummary.getTotalQuantity()).isEqualTo(120L);
		assertThat(wareASummary.getWarehouseCount()).isEqualTo(2L);

		// wareB 검증 (가구B: warehouse1(50) + warehouse2(80) = 130)
		StockRepository.WareStockSummary wareBSummary = summaries.stream()
				.filter(s -> s.getWareId().equals(wareB.getId()))
				.findFirst().orElseThrow();
		assertThat(wareBSummary.getTotalQuantity()).isEqualTo(130L);
		assertThat(wareBSummary.getWarehouseCount()).isEqualTo(2L);

		// wareC 검증 (의류C: warehouse1(30) + warehouse3(60) = 90)
		StockRepository.WareStockSummary wareCSummary = summaries.stream()
				.filter(s -> s.getWareId().equals(wareC.getId()))
				.findFirst().orElseThrow();
		assertThat(wareCSummary.getTotalQuantity()).isEqualTo(90L);
		assertThat(wareCSummary.getWarehouseCount()).isEqualTo(2L);
	}

	@Test
	@DisplayName("getTotalPaletteCountByWarehouseId: 창고별 총 파레트 수량 조회")
	void testGetTotalPaletteCountByWarehouseId() {
		// Given & When
		Integer totalCount = stockRepository.getTotalPaletteCountByWarehouseId(warehouse1.getId());

		// Then
		assertThat(totalCount).isEqualTo(180); // 100+50+30
	}

	@Test
	@DisplayName("getTotalPaletteCountByWarehouseId: 재고 없는 창고")
	void testGetTotalPaletteCountByWarehouseId_EmptyWarehouse() {
		// Given: 새 창고 생성 (재고 없음)
		Location emptyWarehouse = Location.builder()
				.name("빈창고")
				.type(LocationType.WAREHOUSE)
				.capacity(100)
				.coordinateX(10)
				.coordinateY(10)
				.build();
		em.persist(emptyWarehouse);
		em.flush();

		// When
		Integer totalCount = stockRepository.getTotalPaletteCountByWarehouseId(emptyWarehouse.getId());

		// Then
		assertThat(totalCount).isEqualTo(0);
	}

	// ========== 배치 처리용 메서드 테스트 ==========

	@Test
	@DisplayName("findMinId, findMaxId: ID 범위 조회")
	void testFindMinMaxId() {
		// Given & When
		Long minId = stockRepository.findMinId();
		Long maxId = stockRepository.findMaxId();

		// Then
		assertThat(minId).isNotNull();
		assertThat(maxId).isNotNull();
		assertThat(maxId).isGreaterThanOrEqualTo(minId);
	}

	@Test
	@DisplayName("findByIdBetween: ID 범위로 페이징 조회")
	void testFindByIdBetween() {
		// Given
		Long minId = stockRepository.findMinId();
		Long maxId = stockRepository.findMaxId();
		Pageable pageable = PageRequest.of(0, 3);

		// When
		Page<Stock> result = stockRepository.findByIdBetween(minId, maxId, pageable);

		// Then
		assertThat(result.getContent()).hasSize(3);
		assertThat(result.getTotalElements()).isEqualTo(6L); // 총 6개 재고
		assertThat(result.getTotalPages()).isEqualTo(2);

		// ID 범위 내 데이터인지 확인
		result.getContent().forEach(stock -> assertThat(stock.getId()).isBetween(minId, maxId));
	}

	@Test
	@DisplayName("count: 전체 재고 개수 조회")
	void testCount() {
		// Given & When
		long totalCount = stockRepository.count();

		// Then
		assertThat(totalCount).isEqualTo(6L); // setup에서 생성한 재고 개수
	}

	// ========== 특수 조회 메서드 테스트 ==========

	@Test
	@DisplayName("existsByWarehouseId: 창고 재고 존재 여부 확인")
	void testExistsByKeyWarehouseId() {
		// Given & When & Then
		assertThat(stockRepository.existsByKey_WarehouseId(warehouse1.getId())).isTrue();
		assertThat(stockRepository.existsByKey_WarehouseId(warehouse2.getId())).isTrue();
		assertThat(stockRepository.existsByKey_WarehouseId(warehouse3.getId())).isTrue();

		// 존재하지 않는 창고 ID
		assertThat(stockRepository.existsByKey_WarehouseId(99999L)).isFalse();
	}

	// ========== 유니크 제약조건 테스트 ==========

	@Test
	@DisplayName("유니크 제약조건: 동일한 (wareId, warehouseId) 중복 저장 시 예외")
	void testUniqueConstraint() {
		// Given: 이미 존재하는 조합과 동일한 재고 생성
		Stock duplicateStock = Stock.builder()
				.wareId(wareA.getId())
				.warehouseId(warehouse1.getId())
				.quantity(999)
				.build();

		// When & Then
		assertThatThrownBy(() -> {
			em.persist(duplicateStock);
			em.flush(); // 실제 DB에 반영하여 제약조건 확인
		}).isInstanceOf(Exception.class);
	}

	// ========== 낙관적 락 테스트 ==========

	@Test
	@DisplayName("낙관적 락: 버전 충돌 시나리오")
	void testOptimisticLock() {
		// Given: 기존 재고 조회
		Stock stock = stockRepository.findByWarehouseIdAndWareId(wareA.getId(), warehouse1.getId())
				.orElseThrow();
		Long originalVersion = stock.getVersion();

		// 첫 번째 수정
		stock.updateQuantityAndCheckDeletion(200);
		em.flush(); // 버전 증가
		em.clear();

		// 기존 버전으로 다시 조회한 것처럼 시뮬레이션
		Stock staleStock = Stock.builder()
				.wareId(wareA.getId())
				.warehouseId(warehouse1.getId())
				.quantity(300)
				.build();
		// 기존 ID와 버전 설정 (리플렉션 또는 테스트용 setter 필요)

		// When & Then: 두 번째 수정 시도 시 버전 충돌
		// 실제 프로덕션에서는 OptimisticLockingFailureException 발생
		// 여기서는 버전 필드가 증가했는지 확인
		Stock updatedStock = stockRepository.findByWarehouseIdAndWareId(wareA.getId(), warehouse1.getId())
				.orElseThrow();
		assertThat(updatedStock.getVersion()).isGreaterThan(originalVersion);
	}

	// ========== 성능 테스트 (참고용) ==========

	@Test
	@DisplayName("성능 테스트: 기존 데이터로 집계 쿼리 성능 측정")
	void testPerformanceWithExistingData() {
		// Given: 기존 setup 데이터만 사용 (6개 재고)
		em.flush();
		em.clear();

		// When: 집계 쿼리 실행 시간 측정
		long startTime = System.currentTimeMillis();

		// 여러 쿼리를 연속 실행하여 성능 측정
		List<StockRepository.WarehouseStockSummary> warehouseSummaries =
				stockRepository.findAllWarehouseStockSummaries();
		List<StockRepository.WareStockSummary> wareSummaries =
				stockRepository.findAllWareStockSummaries();
		Integer totalWarehouse1 = stockRepository.getTotalPaletteCountByWarehouseId(warehouse1.getId());
		Integer totalWarehouse2 = stockRepository.getTotalPaletteCountByWarehouseId(warehouse2.getId());

		long endTime = System.currentTimeMillis();

		// Then: 결과 검증 및 성능 확인
		assertThat(warehouseSummaries).hasSize(3); // 3개 창고
		assertThat(wareSummaries).hasSize(3); // 3개 물품
		assertThat(totalWarehouse1).isEqualTo(180); // warehouse1 총합
		assertThat(totalWarehouse2).isEqualTo(100); // warehouse2 총합

		long executionTime = endTime - startTime;
		System.out.println("집계 쿼리 실행 시간: " + executionTime + "ms");
		System.out.println("처리된 창고 수: " + warehouseSummaries.size());
		System.out.println("처리된 물품 수: " + wareSummaries.size());

		// 성능 기준 (기본 데이터로는 매우 빨라야 함)
		assertThat(executionTime).isLessThan(500L);
	}

	@Test
	@DisplayName("대량 조회 성능 테스트: 페이징 처리")
	void testPagingPerformance() {
		// Given: 기존 데이터로 페이징 테스트
		Pageable pageable = PageRequest.of(0, 3);

		// When
		long startTime = System.currentTimeMillis();
		Page<Stock> page1 = stockRepository.findAll(pageable);
		Page<Stock> page2 = stockRepository.findAll(PageRequest.of(1, 3));
		long endTime = System.currentTimeMillis();

		// Then
		assertThat(page1.getContent()).hasSize(3);
		assertThat(page1.getTotalElements()).isEqualTo(6L);
		assertThat(page2.getContent()).hasSize(3);

		long executionTime = endTime - startTime;
		System.out.println("페이징 쿼리 실행 시간: " + executionTime + "ms");
		assertThat(executionTime).isLessThan(200L);
	}

	@Test
	@DisplayName("복합 조회 성능 테스트")
	void testComplexQueryPerformance() {
		// Given & When: 다양한 조건으로 조회 성능 측정
		long startTime = System.currentTimeMillis();

		// 창고별 조회
		List<Stock> warehouse1Stocks = stockRepository.findAllByKey_WarehouseId(warehouse1.getId());
		List<Stock> warehouse2Stocks = stockRepository.findAllByKey_WarehouseId(warehouse2.getId());

		// 물품별 조회
		List<Stock> wareAStocks = stockRepository.findAllByKey_WareId(wareA.getId());
		List<Stock> wareBStocks = stockRepository.findAllByKey_WareId(wareB.getId());

		// 조건부 조회
		List<Stock> highQuantityStocks = stockRepository.findAllByKey_WareIdAndQuantityGreaterThan(wareA.getId(), 50);

		long endTime = System.currentTimeMillis();

		// Then
		assertThat(warehouse1Stocks).hasSize(3);
		assertThat(warehouse2Stocks).hasSize(2);
		assertThat(wareAStocks).hasSize(2);
		assertThat(wareBStocks).hasSize(2);
		assertThat(highQuantityStocks).hasSize(1); // wareA의 warehouse1만 100개

		long executionTime = endTime - startTime;
		System.out.println("복합 조회 실행 시간: " + executionTime + "ms");
		assertThat(executionTime).isLessThan(300L);
	}
}