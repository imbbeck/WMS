package com.wms.integration.logistictask;

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
import com.wms.stock.application.StockSyncEventService;
import com.wms.stock.domain.event.StockSyncStatus;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Slf4j
@SpringBootTest
@ActiveProfiles("test")
@Disabled("Redis Stream Consumer 재시작으로 인한 다른 테스트 간섭 방지 - 개별 테스트로만 실행")
@DisplayName("LogisticTask-Stock Redis Stream 장애 복원력 테스트")
class LogisticTaskStockStreamResilenceTest {

    @Autowired
    private LogisticTaskService logisticTaskService;

    @Autowired
    private StockCtrlService stockCtrlService;

    @Autowired
    private StockQueryService stockQueryService;

    @Autowired
    private UserInfoRepository userInfoRepository;

    @Autowired
    private WareRepository wareRepository;

    @Autowired
    private LocationRepository locationRepository;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @SpyBean
    private StockSyncEventService stockSyncEventService;

    @SpyBean
    private StockEventStreamProcessor streamProcessor;

    private UserInfo worker;
    private Ware testWare;
    private Location warehouseA;
    private Location warehouseB;
    private Location outbound;
    @Autowired
    private LogisticTaskRepository logisticTaskRepository;
    @Autowired
    private StockRepository stockRepository;

    @BeforeEach
    void setUp() {
        // Consumer Group 정리 추가
        cleanupConsumerGroups();

        // 테스트 데이터 준비
        worker = userInfoRepository.save(UserInfo.builder()
                .username("resilience_worker")
                .name("복원력테스트작업자")
                .email("resilience@test.com")
                .password(new Password("password"))
                .type(UserType.WORKER)
                .build());

        testWare = wareRepository.save(Ware.builder()
                .name("복원력테스트물품")
                .type("테스트")
                .paletteUnit(10)
                .build());

        warehouseA = locationRepository.save(Location.builder()
                .name("복원력창고A")
                .type(LocationType.WAREHOUSE)
                .capacity(1000)
                .coordinateX(100)
                .coordinateY(100)
                .build());

        warehouseB = locationRepository.save(Location.builder()
                .name("복원력창고B")
                .type(LocationType.WAREHOUSE)
                .capacity(800)
                .coordinateX(200)
                .coordinateY(100)
                .build());

        outbound = locationRepository.save(Location.builder()
                .name("복원력출고처")
                .type(LocationType.OUTBOUND)
                .coordinateX(300)
                .coordinateY(100)
                .build());

        // 초기 재고 설정
        stockCtrlService.create(StockDTO.CreateReq.builder().warehouseId(warehouseA.getId()).wareId(testWare.getId()).quantity(100).build());

        // 인증 설정
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(worker, null, null));

        log.info("복원력 테스트 데이터 준비 완료");
    }

    @AfterEach
    void tearDown() {
        // DB 데이터 정리 (역순으로 삭제)
        logisticTaskRepository.deleteAll();
        stockRepository.deleteAll();
        userInfoRepository.deleteAll();
        wareRepository.deleteAll();
        locationRepository.deleteAll();

        // Redis 캐시 정리
        cleanupCaches();

        // Redis 스트림 정리
        cleanupRedisStreams();

        // SecurityContext 정리
        SecurityContextHolder.clearContext();

        log.info("테스트 데이터 정리 완료");
    }

    @Test
    @DisplayName("스트림 메시지 처리 지연 시나리오")
    void streamProcessingDelay_Scenario() {
        // Given: 스트림 프로세서 지연 시뮬레이션
        doAnswer(invocation -> {
            Thread.sleep(3000); // 3초 지연
            return invocation.callRealMethod();
        }).when(streamProcessor).consumeStreamEvents(anyString());

        LogisticTaskDTO.CreateReq request = createOutboundRequest(15);
        Stock initialStock = stockQueryService.getStockByWarehouseAndWare(
                warehouseA.getId(), testWare.getId());

        // When: 작업 시작
        LogisticTask task = logisticTaskService.create(request);
        logisticTaskService.initiateTask(task.getId());

        // Then: 처리 중 상태 확인
        verify(stockSyncEventService, timeout(2000))
                .broadcastSyncStatus(eq(task.getId()), eq(StockSyncStatus.QUEUED), anyString());

        // 최종 완료 대기
        Awaitility.await()
                .atMost(120, TimeUnit.SECONDS)
                .pollInterval(2, TimeUnit.SECONDS)
                .ignoreExceptions()
                .untilAsserted(() -> {
                    Stock finalStock = stockQueryService.getStockByWarehouseAndWare(
                            warehouseA.getId(), testWare.getId());
                    assertThat(finalStock.getQuantity()).isEqualTo(initialStock.getQuantity() - 15);
                });

        verify(stockSyncEventService, timeout(10000))
                .broadcastSyncStatus(eq(task.getId()), eq(StockSyncStatus.COMPLETED), anyString());

        log.info("스트림 처리 지연 시나리오 완료");
    }

    @Test
    @Disabled("멱등성 구현 후 활성화 예정")
    @DisplayName("중복 메시지 처리 시나리오 - 동일한 이벤트 중복 발생")
    void duplicateMessageHandling_Scenario() {
        // Given: 수동으로 중복 스트림 메시지 생성
        LogisticTaskDTO.CreateReq request = createInnerMovementRequest(20);
        LogisticTask task = logisticTaskService.create(request);

        String streamKey = "stock:events:" + testWare.getId() + ":" + warehouseA.getId();

        // 동일한 감소 이벤트를 여러 번 발행
        Map<String, Object> eventData = Map.of(
                "taskId", task.getId(),
                "wareId", testWare.getId(),
                "locationId", warehouseA.getId(),
                "changeType", "DECREASE",
                "quantity", 20,
                "timestamp", LocalTime.now().toString(),
                "eventJson", "{\"taskId\":" + task.getId() + ",\"changeType\":\"DECREASE\",\"quantity\":20}"
        );

        // When: 중복 메시지 발행
        RecordId record1 = redisTemplate.opsForStream().add(streamKey, eventData);
        RecordId record2 = redisTemplate.opsForStream().add(streamKey, eventData);
        RecordId record3 = redisTemplate.opsForStream().add(streamKey, eventData);

        // Then: 중복 처리로 인한 재고 과도 감소가 발생하지 않아야 함
        Awaitility.await()
                .atMost(60, TimeUnit.SECONDS)
                .pollInterval(2, TimeUnit.SECONDS)
                .ignoreExceptions()
                .untilAsserted(() -> {
                    // 처리 완료 대기
                    verify(streamProcessor, atLeast(3)).processStreamRecord(eq(streamKey), any());
                });

        log.info("중복 메시지 처리 시나리오 완료 - records: {}, {}, {}", record1, record2, record3);
    }

    @Test
    @DisplayName("스트림 소비자 재시작 시나리오")
    void streamConsumerRestart_Scenario() {
        // Given
        LogisticTaskDTO.CreateReq request = createOutboundRequest(5);
        Stock initialStock = stockQueryService.getStockByWarehouseAndWare(
                warehouseA.getId(), testWare.getId());

        // When: 작업 시작
        LogisticTask task = logisticTaskService.create(request);
        logisticTaskService.initiateTask(task.getId());

        // 스트림 프로세서 중지 시뮬레이션
        streamProcessor.stopStreamConsumers();

        // 짧은 대기 후 재시작
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        streamProcessor.startStreamConsumers();

        // Then: 재시작 후에도 정상 처리되어야 함
        Awaitility.await()
                .atMost(60, TimeUnit.SECONDS)
                .pollInterval(2, TimeUnit.SECONDS)
                .ignoreExceptions()
                .untilAsserted(() -> {
                    Stock finalStock = stockQueryService.getStockByWarehouseAndWare(
                            warehouseA.getId(), testWare.getId());
                    assertThat(finalStock.getQuantity()).isEqualTo(initialStock.getQuantity() - 5);
                });

        log.info("스트림 소비자 재시작 시나리오 완료");
    }

    @Test
    @DisplayName("대용량 메시지 처리 시나리오")
    void highVolumeMessages_Scenario() throws InterruptedException {
        // Given: 다수의 작업을 동시에 생성하고 여러 작업자 준비
        int taskCount = 20;
        int quantityPerTask = 2;

        // 여러 작업자 생성
        List<UserInfo> workers = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            UserInfo newWorker = UserInfo.builder()
                    .username("volume_worker_" + i)
                    .name("대용량작업자" + i)
                    .email("volume" + i + "@test.com")
                    .password(new Password("password"))
                    .type(UserType.WORKER)
                    .build();
            workers.add(userInfoRepository.save(newWorker));
        }

        Stock initialStock = stockQueryService.getStockByWarehouseAndWare(
                warehouseA.getId(), testWare.getId());

        // When: 대량 작업을 하루 단위로 분산 생성
        LocalTime baseTime = LocalTime.of(9, 0); // 오전 9시 시작
        LocalDate baseDate = LocalDate.now().plusDays(1); // 내일부터 시작

        for (int i = 0; i < taskCount; i++) {
            UserInfo selectedWorker = workers.get(i % workers.size()); // 작업자를 순환하여 배정

            // 하루 단위로 날짜 분산 (하루에 최대 4개 작업)
            int dayOffset = i / 4; // 4개씩 하루에 배치
            int timeSlot = i % 4;   // 하루 내에서 시간대 분산

            LocalDate scheduledDate = baseDate.plusDays(dayOffset);
            LocalTime etd = baseTime.plusHours(timeSlot * 2); // 2시간 간격 (9시, 11시, 13시, 15시)
            LocalTime eta = etd.plusHours(1); // 1시간 후 완료

            LogisticTaskDTO.CreateReq request = LogisticTaskDTO.CreateReq.builder()
                    .name("대용량테스트" + i)
                    .type(LogisticType.OUTBOUND)
                    .workerId(selectedWorker.getId())
                    .wareId(testWare.getId())
                    .fromLocationId(warehouseA.getId())
                    .toLocationId(outbound.getId())
                    .quantity(quantityPerTask)
                    .scheduledDate(scheduledDate) // 날짜 분산
                    .etd(etd) // 시간 분산
                    .eta(eta)
                    .templateIdSnapshot(i)
                    .build();

            LogisticTask task = logisticTaskService.create(request);

            // 비동기 실행
            final UserInfo workerForAuth = selectedWorker;
            new Thread(() -> {
                try {
                    SecurityContextHolder.getContext().setAuthentication(
                            new UsernamePasswordAuthenticationToken(workerForAuth, null, null));
                    logisticTaskService.initiateTask(task.getId());
                    Thread.sleep(100); // 약간의 지연
                    logisticTaskService.completeTask(task.getId());
                } catch (Exception e) {
                    log.error("작업 실행 중 오류: {}", task.getId(), e);
                }
            }).start();
        }

        // Then: 모든 작업 완료 후 재고 확인
        Awaitility.await()
                .atMost(60, TimeUnit.SECONDS)
                .pollInterval(2, TimeUnit.SECONDS)
                .ignoreExceptions()
                .untilAsserted(() -> {
                    Stock finalStock = stockQueryService.getStockByWarehouseAndWare(
                            warehouseA.getId(), testWare.getId());

                    // 예상 최종 재고: 초기재고 - (작업수 × 작업당수량)
                    int expectedFinalQuantity = initialStock.getQuantity() - (taskCount * quantityPerTask);
                    assertThat(finalStock.getQuantity()).isEqualTo(expectedFinalQuantity);
                    log.info("예상 최종 재고{}, 최종 재고: {}", expectedFinalQuantity, finalStock.getQuantity());
                });

        // 성공 이벤트가 충분히 발생했는지 확인
        verify(stockSyncEventService, timeout(20000).atLeast(taskCount))
                .broadcastSyncStatus(anyLong(), eq(StockSyncStatus.COMPLETED), anyString());

        log.info("대용량 메시지 처리 시나리오 완료 - {} 개 작업 처리, {} 일에 걸쳐 분산.",
                taskCount, (taskCount - 1) / 4 + 1);
    }

    private LogisticTaskDTO.CreateReq createOutboundRequest(int quantity) {
        LocalTime futureTime = LocalTime.MIDNIGHT; //
        return LogisticTaskDTO.CreateReq.builder()
                .name("복원력테스트_출고_" + quantity)
                .type(LogisticType.OUTBOUND)
                .workerId(worker.getId())
                .wareId(testWare.getId())
                .fromLocationId(warehouseA.getId())
                .toLocationId(outbound.getId())
                .quantity(quantity)
                .scheduledDate(LocalDate.now().plusDays(1))
                .etd(futureTime)
                .eta(futureTime.plusHours(1))
                .templateIdSnapshot(1)
                .build();
    }

    private LogisticTaskDTO.CreateReq createInnerMovementRequest(int quantity) {
        LocalTime futureTime = LocalTime.now().plusHours(3);
        return LogisticTaskDTO.CreateReq.builder()
                .name("복원력테스트_내부이동_" + quantity)
                .type(LogisticType.INNER)
                .workerId(worker.getId())
                .wareId(testWare.getId())
                .fromLocationId(warehouseA.getId())
                .toLocationId(warehouseB.getId())
                .quantity(quantity)
                .scheduledDate(LocalDate.now().plusDays(1))
                .etd(futureTime)
                .eta(futureTime.plusHours(1))
                .templateIdSnapshot(2)
                .build();
    }

    private void cleanupRedisStreams() {
        try {
            var keys = redisTemplate.keys("stock:events:*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.info("Redis 스트림 정리 완료: {} 개", keys.size());
            }
        } catch (Exception e) {
            log.warn("Redis 스트림 정리 중 오류", e);
        }
    }

    private void cleanupCaches() {
        try {
            // 재고 캐시 정리
            var stockKeys = redisTemplate.keys("current_stock:*");
            if (stockKeys != null && !stockKeys.isEmpty()) {
                redisTemplate.delete(stockKeys);
                log.info("재고 캐시 정리 완료: {} 개", stockKeys.size());
            }

            // 창고 캐시 정리
            var warehouseKeys = redisTemplate.keys("warehouse:*");
            if (warehouseKeys != null && !warehouseKeys.isEmpty()) {
                redisTemplate.delete(warehouseKeys);
                log.info("창고 캐시 정리 완료: {} 개", warehouseKeys.size());
            }
        } catch (Exception e) {
            log.warn("캐시 정리 중 오류", e);
        }
    }

    private void cleanupConsumerGroups() {
        try {
            String consumerGroup = "stock-processors";
            var streamKeys = redisTemplate.keys("stock:events:*");
            if (streamKeys != null) {
                for (String streamKey : streamKeys) {
                    try {
                        redisTemplate.opsForStream().destroyGroup(streamKey, consumerGroup);
                        log.debug("Consumer Group 정리: {}", streamKey);
                    } catch (Exception e) {
                        // Consumer Group이 없으면 무시
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Consumer Group 정리 중 오류", e);
        }
    }
}