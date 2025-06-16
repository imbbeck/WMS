package com.wms.movement.integration;

import com.wms.location.domain.model.Location;
import com.wms.location.domain.model.LocationType;
import com.wms.location.domain.repository.LocationRepository;
import com.wms.movement.application.MovementService;
import com.wms.movement.domain.model.Movement;
import com.wms.movement.domain.model.MovementStatus;
import com.wms.movement.domain.repository.MovementRepository;
import com.wms.movement.dto.MovementRequest;
import com.wms.stock.application.StockService;
import com.wms.stock.domain.model.Stock;
import com.wms.stock.domain.repository.StockRepository;
import com.wms.ware.domain.model.Ware;
import com.wms.ware.domain.repository.WareRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class MovementStockIntegrationTest {

	@Autowired
	private MovementService movementService;

	@Autowired
	private StockService stockService;

	@Autowired
	private StockRepository stockRepository;

	@Autowired
	private WareRepository wareRepository;

	@Autowired
	private LocationRepository locationRepository;

	@Autowired
	private RedisTemplate<String, Object> redisTemplate;

	private Ware testWare;
	private Location sourceLocation;
	private Location targetLocation;
	private Location warehouseLocation;

	@BeforeEach
	void setUp() {
		// Redis 캐시 초기화
		redisTemplate.getConnectionFactory().getConnection().flushAll();

		// 테스트 데이터 생성
		testWare = wareRepository.save(Ware.builder()
				.name("테스트물품")
				.type("TEST")
				.paletteUnit(1)
				.build());

		sourceLocation = locationRepository.save(Location.builder()
				.name("입고처")
				.type(LocationType.INBOUND)
				.build());

		targetLocation = locationRepository.save(Location.builder()
				.name("출고처")
				.type(LocationType.OUTBOUND)
				.build());

		warehouseLocation = locationRepository.save(Location.builder()
				.name("창고")
				.type(LocationType.WAREHOUSE)
				.capacity(500)
				.build());

		// 초기 재고 생성
		Stock initialStock = Stock.create(testWare, sourceLocation, 100);
		stockRepository.save(initialStock);

		// 캐시 초기화
		stockService.initializeCache();
	}

	@Test
	@DisplayName("물류이동 생성부터 완료까지 전체 플로우 테스트")
	void testCompleteMovementFlow() {
		// Given
		MovementRequest request = new MovementRequest(
				testWare.getId(),
				sourceLocation.getId(),
				targetLocation.getId(),
				30
		);

		// When: 이동 생성
		Movement createdMovement = movementService.createMovement(request);

		// Then: 이동이 생성되었는지 확인
		assertThat(createdMovement).isNotNull();
		assertThat(createdMovement.getStatus()).isEqualTo(MovementStatus.PENDING);
		assertThat(createdMovement.getQuantity()).isEqualTo(30);

		// When: 이동 시작
		Movement startedMovement = movementService.startMovement(createdMovement.getId());

		// Then: 출발지 재고가 차감되고 상태가 변경되었는지 확인
		assertThat(startedMovement.getStatus()).isEqualTo(MovementStatus.IN_PROGRESS);
		assertThat(stockService.getStockQuantity(sourceLocation.getId(), testWare.getId())).isEqualTo(70);

		// When: 이동 완료
		Movement completedMovement = movementService.completeMovement(createdMovement.getId());

		// Then: 도착지에 재고가 추가되고 상태가 완료되었는지 확인
		assertThat(completedMovement.getStatus()).isEqualTo(MovementStatus.COMPLETED);
		assertThat(stockService.getStockQuantity(targetLocation.getId(), testWare.getId())).isEqualTo(30);
		assertThat(stockService.getStockQuantity(sourceLocation.getId(), testWare.getId())).isEqualTo(70);
	}

	@Test
	@DisplayName("창고 용량 초과 시 이동 실패 테스트")
	void testMovementFailsWhenWarehouseCapacityExceeded() {
		// Given: 창고에 이미 많은 재고가 있는 상황
		Stock warehouseStock = Stock.create(testWare, warehouseLocation, 480);
		stockRepository.save(warehouseStock);

		MovementRequest request = new MovementRequest(
				testWare.getId(),
				sourceLocation.getId(),
				warehouseLocation.getId(),
				30  // 창고 용량(500) 초과
		);

		// When & Then: 이동 생성은 성공하지만 완료 시 실패해야 함
		Movement movement = movementService.createMovement(request);
		movementService.startMovement(movement.getId());

		assertThatThrownBy(() -> movementService.completeMovement(movement.getId()))
				.hasMessageContaining("용량을 초과");
	}

	@Test
	@DisplayName("재고 부족 시 이동 시작 실패 테스트")
	void testMovementStartFailsWhenInsufficientStock() {
		// Given: 재고보다 많은 양을 이동하려는 요청
		MovementRequest request = new MovementRequest(
				testWare.getId(),
				sourceLocation.getId(),
				targetLocation.getId(),
				150  // 현재 재고(100)보다 많음
		);

		Movement movement = movementService.createMovement(request);

		// When & Then: 이동 시작 시 재고 부족으로 실패해야 함
		assertThatThrownBy(() -> movementService.startMovement(movement.getId()))
				.hasMessageContaining("재고가 부족");
	}

	@Test
	@DisplayName("캐시와 DB 일관성 테스트")
	void testCacheAndDatabaseConsistency() {
		// Given
		MovementRequest request = new MovementRequest(
				testWare.getId(),
				sourceLocation.getId(),
				targetLocation.getId(),
				20
		);

		// When: 완전한 이동 실행
		Movement movement = movementService.createMovement(request);
		movementService.startMovement(movement.getId());
		movementService.completeMovement(movement.getId());

		// Then: 캐시와 DB의 데이터가 일치하는지 확인
		Integer cacheQuantitySource = stockService.getStockQuantity(sourceLocation.getId(), testWare.getId());
		Integer cacheQuantityTarget = stockService.getStockQuantity(targetLocation.getId(), testWare.getId());

		Stock dbStockSource = stockRepository.findByWareIdAndLocationId(testWare.getId(), sourceLocation.getId()).orElse(null);
		Stock dbStockTarget = stockRepository.findByWareIdAndLocationId(testWare.getId(), targetLocation.getId()).orElse(null);

		assertThat(cacheQuantitySource).isEqualTo(80);
		assertThat(cacheQuantityTarget).isEqualTo(20);
		assertThat(dbStockSource.getQuantity()).isEqualTo(80);
		assertThat(dbStockTarget.getQuantity()).isEqualTo(20);
	}

	@Test
	@DisplayName("총 재고량 계산 테스트")
	void testTotalQuantityCalculation() {
		// Given: 여러 위치에 같은 물품 재고 생성
		Stock additionalStock1 = Stock.create(testWare, targetLocation, 50);
		Stock additionalStock2 = Stock.create(testWare, warehouseLocation, 30);
		stockRepository.save(additionalStock1);
		stockRepository.save(additionalStock2);

		// 캐시 재초기화
		stockService.initializeCache();

		// When: 물품별 총 재고량 조회
		Integer totalByWare = stockService.getTotalQuantityByWare(testWare.getId());
		Integer totalBySourceLocation = stockService.getTotalQuantityByLocation(sourceLocation.getId());
		Integer totalByTargetLocation = stockService.getTotalQuantityByLocation(targetLocation.getId());

		// Then: 총합이 정확한지 확인
		assertThat(totalByWare).isEqualTo(180); // 100 + 50 + 30
		assertThat(totalBySourceLocation).isEqualTo(100);
		assertThat(totalByTargetLocation).isEqualTo(50);
	}

	@Test
	@DisplayName("동시 이동 요청 처리 테스트")
	void testConcurrentMovements() {
		// Given: 두 개의 이동 요청
		MovementRequest request1 = new MovementRequest(
				testWare.getId(), sourceLocation.getId(), targetLocation.getId(), 30
		);
		MovementRequest request2 = new MovementRequest(
				testWare.getId(), sourceLocation.getId(), warehouseLocation.getId(), 40
		);

		// When: 두 이동 생성 및 실행
		Movement movement1 = movementService.createMovement(request1);
		Movement movement2 = movementService.createMovement(request2);

		movementService.startMovement(movement1.getId());
		movementService.startMovement(movement2.getId());

		movementService.completeMovement(movement1.getId());
		movementService.completeMovement(movement2.getId());

		// Then: 최종 재고 상태 확인
		assertThat(stockService.getStockQuantity(sourceLocation.getId(), testWare.getId())).isEqualTo(30); // 100 - 30 - 40
		assertThat(stockService.getStockQuantity(targetLocation.getId(), testWare.getId())).isEqualTo(30);
		assertThat(stockService.getStockQuantity(warehouseLocation.getId(), testWare.getId())).isEqualTo(40);
	}

	@Test
	@DisplayName("이동 취소 테스트")
	void testMovementCancellation() {
		// Given
		MovementRequest request = new MovementRequest(
				testWare.getId(), sourceLocation.getId(), targetLocation.getId(), 25
		);

		Movement movement = movementService.createMovement(request);

		// When: 이동 시작 후 취소 (이미 차감된 재고는 복구되지 않음)
		movementService.startMovement(movement.getId());
		Integer stockAfterStart = stockService.getStockQuantity(sourceLocation.getId(), testWare.getId());

		Movement cancelledMovement = movementService.cancelMovement(movement.getId());

		// Then: 상태만 취소로 변경 (재고는 복구되지 않음)
		assertThat(cancelledMovement.getStatus()).isEqualTo(MovementStatus.CANCELLED);
		assertThat(stockService.getStockQuantity(sourceLocation.getId(), testWare.getId())).isEqualTo(stockAfterStart);
	}

	@Test
	@DisplayName("새로운 위치로 이동 시 재고 생성 테스트")
	void testStockCreationForNewLocation() {
		// Given: 재고가 없는 새로운 위치
		Location newLocation = locationRepository.save(Location.builder()
				.name("새 창고")
				.type(LocationType.WAREHOUSE)
				.capacity(500)
				.build());

		MovementRequest request = new MovementRequest(
				testWare.getId(), sourceLocation.getId(), newLocation.getId(), 25
		);

		// When: 새 위치로 이동
		Movement movement = movementService.createMovement(request);
		movementService.startMovement(movement.getId());
		movementService.completeMovement(movement.getId());

		// Then: 새 위치에 재고가 생성되었는지 확인
		assertThat(stockService.getStockQuantity(newLocation.getId(), testWare.getId())).isEqualTo(25);

		Stock newStock = stockRepository.findByWareIdAndLocationId(testWare.getId(), newLocation.getId()).orElse(null);
		assertThat(newStock).isNotNull();
		assertThat(newStock.getQuantity()).isEqualTo(25);
	}
}
