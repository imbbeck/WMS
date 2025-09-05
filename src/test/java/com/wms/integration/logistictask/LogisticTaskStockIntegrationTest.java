package com.wms.integration.logistictask;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import com.wms.location.domain.model.Location;
import com.wms.location.domain.model.LocationType;
import com.wms.location.domain.repository.LocationRepository;
import com.wms.logisticTask.application.LogisticTaskService;
import com.wms.logisticTask.domain.model.LogisticTask;
import com.wms.logisticTask.domain.repository.LogisticTaskRepository;
import com.wms.logisticTask.dto.LogisticTaskDTO;
import com.wms.logisticTemplate.domain.model.LogisticType;
import com.wms.stock.application.StockCtrlService;
import com.wms.stock.application.StockEventStreamProcessor;
import com.wms.stock.application.StockQueryService;
import com.wms.notification.application.StockSyncNotificationAdapter;
import com.wms.notification.domain.model.StockSyncStatus;
import com.wms.stock.domain.model.Stock;
import com.wms.stock.domain.repository.StockRepository;
import com.wms.stock.dto.StockDTO;
import com.wms.userInfo.domain.model.Password;
import com.wms.userInfo.domain.model.UserInfo;
import com.wms.userInfo.domain.model.UserType;
import com.wms.userInfo.domain.repository.UserInfoRepository;
import com.wms.ware.domain.model.Ware;
import com.wms.ware.domain.repository.WareRepository;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@Slf4j
@SpringBootTest
@Disabled("오래걸려서 비활성화")
@DisplayName("LogisticTask-Stock Redis Stream 통합 테스트2")
public class LogisticTaskStockIntegrationTest {
	@Autowired
	private LogisticTaskService logisticTaskService;

	@Autowired
	private StockCtrlService stockCtrlService;

	@Autowired
	private StockQueryService stockQueryService;

	@Autowired
	private StockEventStreamProcessor streamProcessor;  // 추가

	@Autowired
	private RedisTemplate<String, Object> redisTemplate;

	@Autowired
	private LogisticTaskRepository logisticTaskRepository;
	@Autowired
	private StockRepository stockRepository;
	@Autowired
	private UserInfoRepository userInfoRepository;
	@Autowired
	private WareRepository wareRepository;
	@Autowired
	private LocationRepository locationRepository;

	@SpyBean
	private StockSyncNotificationAdapter stockSyncEventService;



	// 기존 필드들...
	private UserInfo worker;
	private Ware laptop;
	private Location warehouseA;
	private Location warehouseB;
	private Location inbound;
	private Location outbound;


	@BeforeEach
	void setUp() throws InterruptedException {
		streamProcessor.stopStreamConsumers();
		Thread.sleep(2000); // 2초 대기
		cleanupAllData();
		streamProcessor.forceStartStreamConsumers();
		Thread.sleep(2000); // 2초 대기
		setupTestData();

		log.info("테스트 설정 완료");
	}

	@AfterEach
	void tearDown() {
		// 1. Consumer 중지
		streamProcessor.stopStreamConsumers();

		// 2. 모든 데이터 정리
		cleanupAllData();

		// 3. SecurityContext 정리
		SecurityContextHolder.clearContext();

		log.info("테스트 정리 완료");
	}

	private void cleanupAllData() {
		// DB 정리
		cleanupDatabase();

		// Redis 정리
		cleanupRedis();
	}

	private void cleanupDatabase() {
		try {
			logisticTaskRepository.deleteAll();
			stockRepository.deleteAll();
			userInfoRepository.deleteAll();
			wareRepository.deleteAll();
			locationRepository.deleteAll();
		} catch (Exception e) {
			log.warn("DB 정리 중 오류", e);
		}
	}

	private void cleanupRedis() {
		try {
			// 1. 스트림 정리
			var streamKeys = redisTemplate.keys("stock:events:*");
			if (streamKeys != null && !streamKeys.isEmpty()) {
				redisTemplate.delete(streamKeys);
			}

			// 2. 캐시 정리
			var cacheKeys = redisTemplate.keys("current_stock:*");
			if (cacheKeys != null && !cacheKeys.isEmpty()) {
				redisTemplate.delete(cacheKeys);
			}

			var warehouseKeys = redisTemplate.keys("warehouse:*");
			if (warehouseKeys != null && !warehouseKeys.isEmpty()) {
				redisTemplate.delete(warehouseKeys);
			}
		} catch (Exception e) {
			log.warn("Redis 정리 중 오류", e);
		}
	}

	private void setupTestData() {
		// 사용자 생성
		worker = UserInfo.builder()
				.username("worker_test")
				.name("테스트작업자")
				.email("worker@test.com")
				.password(new Password("password"))
				.type(UserType.WORKER)
				.build();
		worker = userInfoRepository.save(worker);

		// 물품 생성
		laptop = Ware.builder()
				.name("노트북")
				.type("전자제품")
				.paletteUnit(20)
				.build();
		laptop = wareRepository.save(laptop);

		// 장소 생성
		warehouseA = Location.builder()
				.name("창고A")
				.type(LocationType.WAREHOUSE)
				.capacity(1000)
				.coordinateX(100)
				.coordinateY(100)
				.build();
		warehouseA = locationRepository.save(warehouseA);

		warehouseB = Location.builder()
				.name("창고B")
				.type(LocationType.WAREHOUSE)
				.capacity(800)
				.coordinateX(200)
				.coordinateY(100)
				.build();
		warehouseB = locationRepository.save(warehouseB);

		inbound = Location.builder()
				.name("입고처")
				.type(LocationType.INBOUND)
				.coordinateX(50)
				.coordinateY(50)
				.build();
		inbound = locationRepository.save(inbound);

		outbound = Location.builder()
				.name("출고처")
				.type(LocationType.OUTBOUND)
				.coordinateX(250)
				.coordinateY(150)
				.build();
		outbound = locationRepository.save(outbound);

		// 초기 재고 설정
		stockCtrlService.create(StockDTO.CreateReq.builder()
				.warehouseId(warehouseA.getId())
				.wareId(laptop.getId())
				.quantity(30)
				.build());

		// 인증 설정
		SecurityContextHolder.getContext().setAuthentication(
				new UsernamePasswordAuthenticationToken(worker, null, null));
	}

	@Test
	@DisplayName("입고 작업 시나리오 - 입고처에서 창고로 물품 이동")
	void inboundTask_Scenario() {
		// Given: 입고 작업 생성
		LogisticTaskDTO.CreateReq inboundRequest = LogisticTaskDTO.CreateReq.builder()
				.name("노트북 입고 작업")
				.type(LogisticType.INBOUND)
				.workerId(worker.getId())
				.wareId(laptop.getId())
				.fromLocationId(inbound.getId())
				.toLocationId(warehouseA.getId())
				.quantity(20)
				.scheduledDate(LocalDate.now().plusDays(1)) // 내일로 설정
				.etd(LocalTime.of(9, 0))
				.eta(LocalTime.of(10, 0))
				.templateIdSnapshot(1)
				.build();

		// 초기 재고 확인
		Stock initialStock = stockQueryService.getStockByWarehouseAndWare(
				warehouseA.getId(), laptop.getId());
		int initialQuantity = initialStock.getQuantity();

		// When: 작업 생성 및 시작
		LogisticTask inboundTask = logisticTaskService.create(inboundRequest);
		logisticTaskService.initiateTask(inboundTask.getId());

		// Then: 입고 시작 시 재고 변화 없음 (입고처는 재고 관리 대상이 아님)
		Stock stockAfterInitiate = stockQueryService.getStockByWarehouseAndWare(
				warehouseA.getId(), laptop.getId());
		assertThat(stockAfterInitiate.getQuantity()).isEqualTo(initialQuantity);

		// When: 작업 완료
		logisticTaskService.completeTask(inboundTask.getId());

		// Then: 비동기 재고 증가 처리 대기 및 검증
		Awaitility.await()
				.atMost(60, TimeUnit.SECONDS)
				.pollInterval(2, TimeUnit.SECONDS)
				.ignoreExceptions()
				.untilAsserted(() -> {
					log.info("재고 상태 확인 중... 초기: {}", initialQuantity);
					Stock finalStock = stockQueryService.getStockByWarehouseAndWare(
							warehouseA.getId(), laptop.getId());
					log.info("현재 재고: {}, 예상 재고: {}", finalStock.getQuantity(), initialQuantity + 20);
					assertThat(finalStock.getQuantity()).isEqualTo(initialQuantity + 20);
				});

		// SSE 이벤트 발행 검증
		verify(stockSyncEventService, timeout(5000).atLeastOnce())
				.broadcastStockSyncStatus(eq(inboundTask.getId()), eq(StockSyncStatus.COMPLETED), anyString());

		log.info("입고 작업 완료 - 초기재고: {}, 최종재고: {}",
				initialQuantity, initialQuantity + 20);
	}

	@Test
	@DisplayName("출고 작업 시나리오 - 창고에서 출고처로 물품 이동")
	void outboundTask_Scenario() {
		// Given: 출고 작업 생성
		LogisticTaskDTO.CreateReq outboundRequest = LogisticTaskDTO.CreateReq.builder()
				.name("노트북 출고 작업")
				.type(LogisticType.OUTBOUND)
				.workerId(worker.getId())
				.wareId(laptop.getId())
				.fromLocationId(warehouseA.getId())
				.toLocationId(outbound.getId())
				.quantity(15)
				.scheduledDate(LocalDate.now().plusDays(1)) // 내일로 설정
				.etd(LocalTime.of(11, 0))
				.eta(LocalTime.of(12, 0))
				.templateIdSnapshot(2)
				.build();

		// 초기 재고 확인
		Stock initialStock = stockQueryService.getStockByWarehouseAndWare(
				warehouseA.getId(), laptop.getId());
		int initialQuantity = initialStock.getQuantity();

		// When: 작업 생성 및 시작
		LogisticTask outboundTask = logisticTaskService.create(outboundRequest);
		logisticTaskService.initiateTask(outboundTask.getId());

		// Then: 비동기 재고 감소 처리 대기 및 검증
		Awaitility.await()
				.atMost(60, TimeUnit.SECONDS)
				.pollInterval(2, TimeUnit.SECONDS)
				.ignoreExceptions()
				.untilAsserted(() -> {
					Stock stockAfterInitiate = stockQueryService.getStockByWarehouseAndWare(
							warehouseA.getId(), laptop.getId());
					assertThat(stockAfterInitiate.getQuantity()).isEqualTo(initialQuantity - 15);
				});

		// When: 작업 완료
		logisticTaskService.completeTask(outboundTask.getId());

		// Then: 출고 완료 시 재고 변화 없음 (출고처는 재고 관리 대상이 아님)
		Stock finalStock = stockQueryService.getStockByWarehouseAndWare(
				warehouseA.getId(), laptop.getId());
		assertThat(finalStock.getQuantity()).isEqualTo(initialQuantity - 15);

		// SSE 이벤트 발행 검증
		verify(stockSyncEventService, timeout(5000).atLeastOnce())
				.broadcastStockSyncStatus(eq(outboundTask.getId()), eq(StockSyncStatus.COMPLETED), anyString());

		log.info("출고 작업 완료 - 초기재고: {}, 최종재고: {}",
				initialQuantity, initialQuantity - 15);
	}

	@Test
	@DisplayName("내부 이동 작업 시나리오 - 창고A에서 창고B로 물품 이동")
	void innerMovementTask_Scenario() {
		// Given: 내부 이동 작업 생성
		LogisticTaskDTO.CreateReq innerRequest = LogisticTaskDTO.CreateReq.builder()
				.name("창고A → 창고B 노트북 이동")
				.type(LogisticType.INNER)
				.workerId(worker.getId())
				.wareId(laptop.getId())
				.fromLocationId(warehouseA.getId())
				.toLocationId(warehouseB.getId())
				.quantity(25)
				.scheduledDate(LocalDate.now().plusDays(1)) // 내일로 설정
				.etd(LocalTime.of(14, 0))
				.eta(LocalTime.of(15, 0))
				.templateIdSnapshot(3)
				.build();

		// 초기 재고 확인
		Stock warehouseAStock = stockQueryService.getStockByWarehouseAndWare(
				warehouseA.getId(), laptop.getId());
		int initialQuantityA = warehouseAStock.getQuantity();

		int initialQuantityB = 0;
		// 창고B 초기 재고 (없을 수 있음)
		try {
			Stock warehouseBStock = stockQueryService.getStockByWarehouseAndWare(
					warehouseB.getId(), laptop.getId());
			if (warehouseBStock != null) {
				initialQuantityB = warehouseBStock.getQuantity();
			}
		} catch (Exception e) {
			log.warn("창고B 재고 조회 실패, 초기 재고 0으로 설정", e);
		}

		// When: 작업 생성 및 시작
		LogisticTask innerTask = logisticTaskService.create(innerRequest);
		logisticTaskService.initiateTask(innerTask.getId());

		// Then: 창고A 재고 감소 확인
		Awaitility.await()
				.atMost(60, TimeUnit.SECONDS)
				.pollInterval(2, TimeUnit.SECONDS)
				.ignoreExceptions()
				.untilAsserted(() -> {
					Stock stockAfterInitiate = stockQueryService.getStockByWarehouseAndWare(
							warehouseA.getId(), laptop.getId());
					assertThat(stockAfterInitiate.getQuantity()).isEqualTo(initialQuantityA - 25);
				});

		// When: 작업 완료
		logisticTaskService.completeTask(innerTask.getId());

		// Then: 창고B 재고 증가 확인
		int finalInitialQuantityB = initialQuantityB;
		Awaitility.await()
				.atMost(60, TimeUnit.SECONDS)
				.pollInterval(2, TimeUnit.SECONDS)
				.ignoreExceptions()
				.untilAsserted(() -> {
					Stock finalStockB = stockQueryService.getStockByWarehouseAndWare(
							warehouseB.getId(), laptop.getId());
					assertThat(finalStockB.getQuantity()).isEqualTo(finalInitialQuantityB + 25);
				});

		// 최종 재고 검증
		Stock finalStockA = stockQueryService.getStockByWarehouseAndWare(
				warehouseA.getId(), laptop.getId());
		assertThat(finalStockA.getQuantity()).isEqualTo(initialQuantityA - 25);

		// SSE 이벤트 발행 검증
		verify(stockSyncEventService, timeout(5000).atLeastOnce())
				.broadcastStockSyncStatus(eq(innerTask.getId()), eq(StockSyncStatus.COMPLETED), anyString());

		log.info("내부 이동 완료 - 창고A: {} → {}, 창고B: {} → {}",
				initialQuantityA, initialQuantityA - 25,
				initialQuantityB, initialQuantityB + 25);
	}

	@Test
	@DisplayName("재고 부족 시나리오 - 출고 수량이 재고보다 많은 경우")
	void insufficientStock_Scenario() {
		// Given: 현재 재고보다 많은 수량으로 출고 작업 생성
		Stock currentStock = stockQueryService.getStockByWarehouseAndWare(
				warehouseA.getId(), laptop.getId());
		int availableQuantity = currentStock.getQuantity();
		int requestedQuantity = availableQuantity + 10; // 재고보다 10개 더 요청

		LogisticTaskDTO.CreateReq outboundRequest = LogisticTaskDTO.CreateReq.builder()
				.name("재고 부족 출고 테스트")
				.type(LogisticType.OUTBOUND)
				.workerId(worker.getId())
				.wareId(laptop.getId())
				.fromLocationId(warehouseA.getId())
				.toLocationId(outbound.getId())
				.quantity(requestedQuantity)
				.scheduledDate(LocalDate.now().plusDays(1)) // 내일로 설정
				.etd(LocalTime.of(16, 0))
				.eta(LocalTime.of(17, 0))
				.templateIdSnapshot(4)
				.build();

		// When: 작업 생성을 위해 임시로 재고 증가
		stockCtrlService.increaseStock(warehouseA.getId(), laptop.getId(), 20);

		LogisticTask outboundTask = logisticTaskService.create(outboundRequest);

		// 재고를 다시 원래대로 감소 (서비스 메서드 사용)
		stockCtrlService.decreaseStock(warehouseA.getId(), laptop.getId(), 20);

		logisticTaskService.initiateTask(outboundTask.getId());

		// Then: 재고 부족으로 인한 실패 처리 확인
		Awaitility.await()
				.atMost(60, TimeUnit.SECONDS)
				.pollInterval(2, TimeUnit.SECONDS)
				.ignoreExceptions()
				.untilAsserted(() -> verify(stockSyncEventService, atLeastOnce())
						.broadcastStockSyncStatus(eq(outboundTask.getId()), eq(StockSyncStatus.FAILED), anyString()));

		// 재고가 변경되지 않았는지 확인
		Stock finalStock = stockQueryService.getStockByWarehouseAndWare(
				warehouseA.getId(), laptop.getId());
		assertThat(finalStock.getQuantity()).isEqualTo(availableQuantity);

		log.info("재고 부족 시나리오 완료 - 요청수량: {}, 가용재고: {}",
				requestedQuantity, availableQuantity);
	}

	@Test
	@DisplayName("동시성 시나리오 - 여러 작업의 동시 실행")
	void concurrentTasks_Scenario() throws InterruptedException {
		// Given: 추가 작업자들 생성
		UserInfo worker2 = createWorker("worker_test2", "테스트작업자2", "worker2@test.com");
		UserInfo worker3 = createWorker("worker_test3", "테스트작업자3", "worker3@test.com");

		Stock initialStock = stockQueryService.getStockByWarehouseAndWare(
				warehouseA.getId(), laptop.getId());
		int initialQuantity = initialStock.getQuantity();

		// 창고B 초기 재고도 확인
		int initialQuantityB = getStockQuantitySafely(warehouseB.getId(), laptop.getId());

		// 각각 다른 작업자에게 배정
		LogisticTaskDTO.CreateReq task1 = createTaskRequestForWorker("동시작업1", 5, LocalTime.now().plusHours(1), worker.getId());
		LogisticTaskDTO.CreateReq task2 = createTaskRequestForWorker("동시작업2", 8, LocalTime.now().plusHours(1), worker2.getId());
		LogisticTaskDTO.CreateReq task3 = createTaskRequestForWorker("동시작업3", 12, LocalTime.now().plusHours(1), worker3.getId());

		// When: 동시에 작업 생성 및 실행
		LogisticTask logisticTask1 = logisticTaskService.create(task1);
		LogisticTask logisticTask2 = logisticTaskService.create(task2);
		LogisticTask logisticTask3 = logisticTaskService.create(task3);

		// 동시 실행
		Thread t1 = new Thread(() -> {
			setSecurityContext(worker);
			executeTaskWithAuth(logisticTask1.getId());
		});

		Thread t2 = new Thread(() -> {
			setSecurityContext(worker2);
			executeTaskWithAuth(logisticTask2.getId());
		});

		Thread t3 = new Thread(() -> {
			setSecurityContext(worker3);
			executeTaskWithAuth(logisticTask3.getId());
		});

		t1.start();
		Thread.sleep(100);
		t2.start();
		Thread.sleep(100);
		t3.start();

		// 모든 스레드 완료 대기
		t1.join();
		t2.join();
		t3.join();

		// Then: 모든 작업 완료 후 최종 재고 확인
		Awaitility.await()
				.atMost(60, TimeUnit.SECONDS) // 시간 늘림
				.pollInterval(2, TimeUnit.SECONDS) // 폴링 간격도 늘림
				.ignoreExceptions()
				.untilAsserted(() -> {
					Stock finalStock = stockQueryService.getStockByWarehouseAndWare(
							warehouseA.getId(), laptop.getId());
					int finalQuantityB = getStockQuantitySafely(warehouseB.getId(), laptop.getId());

					// 총 이동량: 5 + 8 + 12 = 25
					assertThat(finalStock.getQuantity()).isEqualTo(initialQuantity - 25);
					assertThat(finalQuantityB).isEqualTo(initialQuantityB + 25);
				});

		// SSE 이벤트 검증
		verify(stockSyncEventService, timeout(10000).atLeast(3))
				.broadcastStockSyncStatus(anyLong(), eq(StockSyncStatus.COMPLETED), anyString());

		log.info("동시성 시나리오 완료 - 초기 창고A: {}, 최종 창고A: {}, 초기 창고B: {}, 최종 창고B: {}",
				initialQuantity, initialQuantity - 25, initialQuantityB, initialQuantityB + 25);
	}

	@Test
	@DisplayName("SSE 이벤트 순서 검증 - 재고 동기화 상태 변화")
	void sseEventOrder_Verification() {
		// Given
		LogisticTaskDTO.CreateReq request = createTaskRequest("SSE 테스트", 5, LocalTime.now().plusHours(1));

		// When
		LogisticTask task = logisticTaskService.create(request);
		logisticTaskService.initiateTask(task.getId());
		logisticTaskService.completeTask(task.getId());

		// Then: SSE 이벤트 순서 검증
		ArgumentCaptor<StockSyncStatus> statusCaptor = ArgumentCaptor.forClass(StockSyncStatus.class);
		ArgumentCaptor<Long> taskIdCaptor = ArgumentCaptor.forClass(Long.class);

		Awaitility.await()
				.atMost(60, TimeUnit.SECONDS)
				.pollInterval(2, TimeUnit.SECONDS)
				.ignoreExceptions()
				.untilAsserted(() -> {
					verify(stockSyncEventService, atLeast(2))
							.broadcastStockSyncStatus(taskIdCaptor.capture(), statusCaptor.capture(), anyString());

					// 캡처된 상태들 검증
					assertThat(statusCaptor.getAllValues()).contains(
							StockSyncStatus.PROCESSING,
							StockSyncStatus.COMPLETED
					);

					assertThat(taskIdCaptor.getAllValues()).allMatch(id -> id.equals(task.getId()));
				});

		log.info("SSE 이벤트 순서 검증 완료");
	}

	// 헬퍼 메소드들
	private LogisticTaskDTO.CreateReq createTaskRequest(String name, int quantity, LocalTime etd) {
		return LogisticTaskDTO.CreateReq.builder()
				.name(name)
				.type(LogisticType.INNER)
				.workerId(worker.getId())
				.wareId(laptop.getId())
				.fromLocationId(warehouseA.getId())
				.toLocationId(warehouseB.getId())
				.quantity(quantity)
				.scheduledDate(LocalDate.now().plusDays(1)) // 내일로 설정
				.etd(etd)
				.eta(etd.plusHours(1))
				.templateIdSnapshot(1)
				.build();
	}

	private int getStockQuantitySafely(Long warehouseId, Long wareId) {
		try {
			Stock stock = stockQueryService.getStockByWarehouseAndWare(warehouseId, wareId);
			return stock.getQuantity();
		} catch (Exception e) {
			return 0; // 재고가 없으면 0 반환
		}
	}

	private void setSecurityContext(UserInfo user) {
		UsernamePasswordAuthenticationToken auth =
				new UsernamePasswordAuthenticationToken(user, null, null);
		SecurityContextHolder.getContext().setAuthentication(auth);
	}

	private void executeTaskWithAuth(Long taskId) {
		try {
			logisticTaskService.initiateTask(taskId);
			logisticTaskService.completeTask(taskId);
		} catch (Exception e) {
			log.error("Task execution failed for taskId: {}", taskId, e);
		} finally {
			SecurityContextHolder.clearContext(); // 정리
		}
	}

	private UserInfo createWorker(String username, String name, String email) {
		UserInfo worker = UserInfo.builder()
				.username(username)
				.name(name)
				.email(email)
				.password(new Password("password"))
				.type(UserType.WORKER)
				.build();
		return userInfoRepository.save(worker);
	}

	private LogisticTaskDTO.CreateReq createTaskRequestForWorker(String name, int quantity, LocalTime etd, Long workerId) {
		return LogisticTaskDTO.CreateReq.builder()
				.name(name)
				.type(LogisticType.INNER)
				.workerId(workerId)
				.wareId(laptop.getId())
				.fromLocationId(warehouseA.getId())
				.toLocationId(warehouseB.getId())
				.quantity(quantity)
				.scheduledDate(LocalDate.now().plusDays(1))
				.etd(etd)
				.eta(etd.plusHours(1))
				.templateIdSnapshot(1)
				.build();
	}
}
