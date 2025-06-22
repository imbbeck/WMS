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

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@Import(QuerydslConfig.class)
class StockRepositoryTest {

	@Autowired
	private StockRepository stockRepository;

	@Autowired
	private EntityManager em;

	private Location loc1;
	private Location loc2;
	private Ware ware1;
	private Ware ware2;

	@BeforeEach
	void setup() {
		// Location 세팅
		loc1 = Location.builder()
				.name("Warehouse A")
				.type(LocationType.WAREHOUSE)
				.capacity(1000)
				.coordinateX(0)
				.coordinateY(0)
				.build();
		loc2 = Location.builder()
				.name("Warehouse B")
				.type(LocationType.WAREHOUSE)
				.capacity(500)
				.coordinateX(1)
				.coordinateY(1)
				.build();
		em.persist(loc1);
		em.persist(loc2);

		// Ware 세팅
		ware1 = Ware.builder()
				.name("Item A")
				.type("Type1")
				.paletteUnit(10)
				.build();
		ware2 = Ware.builder()
				.name("Item B")
				.type("Type2")
				.paletteUnit(20)
				.build();
		em.persist(ware1);
		em.persist(ware2);

		// Stock 세팅
		Stock stock1 = Stock.builder()
				.warehouseId(loc1.getId())
				.wareId(ware1.getId())
				.quantity(100)
				.build();
		Stock stock2 = Stock.builder()
				.warehouseId(loc1.getId())
				.wareId(ware2.getId())
				.quantity(50)
				.build();
		Stock stock3 = Stock.builder()
				.warehouseId(loc2.getId())
				.wareId(ware1.getId())
				.quantity(30)
				.build();
		em.persist(stock1);
		em.persist(stock2);
		em.persist(stock3);

		em.flush();
		em.clear();  // 캐시 초기화 -> DB 조회 시 쿼리 발생 확인 가능
	}

	//TODO: 유일성 보장 되는지 테스트 필요
	@Test
	@DisplayName("findByWarehouse_IdAndWare_Id: 특정 창고, 물품 재고 조회")
	void testFindByWarehouseIdAndWareId() {
		Optional<Stock> result = stockRepository.findByWarehouseIdAndWareId(loc1.getId(), ware1.getId());
		assertThat(result).isPresent();
		assertThat(result.get().getQuantity()).isEqualTo(100);
	}

	@Test
	@DisplayName("findAllByWarehouse_Id: 특정 창고의 모든 재고 조회")
	void testFindAllByWarehouseId() {
		List<Stock> stocks = stockRepository.findAllByWarehouseId(loc1.getId());

		assertThat(stocks).hasSize(2); // stock1, stock2

		for (Stock stock : stocks) {
			assertThat(stock.getWarehouseId()).isEqualTo(loc1.getId());
			assertThat(stock.getWareId()).isIn(ware1.getId(), ware2.getId());
			assertThat(stock.getQuantity()).isPositive();
		}
	}

	@Test
	@DisplayName("findAllByWare_Id: 특정 물품의 모든 창고 재고 조회")
	void testFindAllByWareId() {
		List<Stock> stocks = stockRepository.findAllByWareId(ware1.getId());

		assertThat(stocks).hasSize(2); // stock1, stock3

		for (Stock stock : stocks) {
			assertThat(stock.getWareId()).isEqualTo(ware1.getId());
			assertThat(stock.getWarehouseId()).isIn(loc1.getId(), loc2.getId());
			assertThat(stock.getQuantity()).isPositive();
		}
	}

	@Test
	@DisplayName("findAllByWare_IdAndQuantityGreaterThan: 특정 물품 재고 있고 수량 초과하는 재고 조회")
	void testFindAllByWareIdAndQuantityGreaterThan() {
		List<Stock> stocks = stockRepository.findAllByWareIdAndQuantityGreaterThan(ware1.getId(), 20);
		assertThat(stocks).hasSizeGreaterThan(0);
		for (Stock s : stocks) {
			assertThat(s.getQuantity()).isGreaterThan(20);
		}
	}

	@Test
	@DisplayName("findAllByQuantityLessThanEqual: 재고 부족 상태 항목 조회")
	void testFindAllByQuantityLessThanEqual() {
		List<Stock> stocks = stockRepository.findAllByQuantityLessThanEqual(50);
		for (Stock s : stocks) {
			assertThat(s.getQuantity()).isLessThanOrEqualTo(50);
		}
	}

	@Test
	@DisplayName("getTotalQuantityByWarehouse: 특정 창고의 총 재고 수량 조회")
	void testGetTotalQuantityByWarehouse() {
		Long total = stockRepository.getTotalQuantityByWarehouse(loc1.getId());
		assertThat(total).isEqualTo(150L);  // 100 + 50
	}

	@Test
	@DisplayName("getTotalQuantityByWare: 특정 물품의 전체 재고 수량 조회")
	void testGetTotalQuantityByWare() {
		Long total = stockRepository.getTotalQuantityByWare(ware1.getId());
		assertThat(total).isEqualTo(130L);  // 100 + 30
	}

	@Test
	@DisplayName("findAllByQuantity: 재고가 0인 항목 조회")
	void testFindAllByQuantity() {
		List<Stock> stocks = stockRepository.findAllByQuantity(0);
		assertThat(stocks).isEmpty();
	}

	@Test
	@DisplayName("findAllByWarehouse_IdAndQuantity: 특정 창고의 재고 0인 항목 조회")
	void testFindAllByWarehouseIdAndQuantity() {
		List<Stock> stocks = stockRepository.findAllByWarehouseIdAndQuantity(loc1.getId(), 0);
		assertThat(stocks).isEmpty();
	}

	// N+1 문제 직접 확인용(로그로 쿼리 확인)
	// 기본적으로 @EntityGraph 적용된 메서드는 N+1 방지됨
	// 여기서는 명시적으로 @EntityGraph 미적용 메서드도 테스트용으로 호출해 비교 가능
	// 예를 들어, 직접 커스텀 JPQL 없이 findAllByWarehouse_IdAndWare_Id 메서드를 여러 개 호출해본다든지

}
