package com.example.wms.moveorder.service;

import com.example.wms.inventory.repository.InventoryRepository;
import com.example.wms.inventory.service.VirtualInventoryService;
import com.example.wms.location.domain.Location;
import com.example.wms.location.domain.LocationType;
import com.example.wms.location.repository.LocationRepository;
import com.example.wms.moveorder.domain.MoveOrder;
import com.example.wms.moveorder.dto.CreateMoveOrderRequest;
import com.example.wms.moveorder.repository.MoveOrderRepository;
import com.example.wms.ware.domain.Ware;
import com.example.wms.ware.repository.WareRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import java.time.LocalDate;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class MoveOrderServiceTest {

    @Autowired
    private MoveOrderService moveOrderService;
    @Autowired
    private MoveOrderRepository moveOrderRepository;
    @Autowired
    private LocationRepository locationRepository;
    @Autowired
    private WareRepository wareRepository;
    @Autowired
    private VirtualInventoryService virtualInventoryService;
    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private Location fromLocation;
    private Location toLocation;
    private Ware ware;

    @BeforeEach
    void setUp() {
        ware = wareRepository.save(new Ware("테스트 상품", "전자제품", 10L));
        fromLocation = locationRepository.save(new Location("출발지", LocationType.YARD, 100L, ""));
        toLocation = locationRepository.save(new Location("도착지", LocationType.YARD, 50L, "")); // 용량 50으로 설정
    }

    @AfterEach
    void tearDown() {
        moveOrderRepository.deleteAllInBatch();
        wareRepository.deleteAllInBatch();
        locationRepository.deleteAllInBatch();
        redisTemplate.getConnectionFactory().getConnection().flushAll();
    }

    @Test
    @DisplayName("이동 주문 생성에 성공한다.")
    void createOrder_success() {
        // given
        virtualInventoryService.increase(fromLocation.getId(), ware.getId(), 20L);
        CreateMoveOrderRequest request = createRequest(10L);

        // when
        moveOrderService.createOrder(request);

        // then
        assertThat(moveOrderRepository.count()).isEqualTo(1);
        assertThat(virtualInventoryService.getQuantity(fromLocation.getId(), ware.getId())).isEqualTo(10L);
        assertThat(virtualInventoryService.getQuantity(toLocation.getId(), ware.getId())).isEqualTo(10L);
    }

    @Test
    @DisplayName("출발지의 재고가 부족하면 주문 생성에 실패한다.")
    void createOrder_fail_insufficient_stock() {
        // given
        virtualInventoryService.increase(fromLocation.getId(), ware.getId(), 5L);
        CreateMoveOrderRequest request = createRequest(10L);

        // when & then
        assertThatThrownBy(() -> moveOrderService.createOrder(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("출발지의 재고가 부족합니다.");
    }
    
    @Test
    @DisplayName("도착지의 수용량을 초과하면 주문 생성에 실패한다.")
    void createOrder_fail_insufficient_capacity() {
        // given
        virtualInventoryService.increase(fromLocation.getId(), ware.getId(), 60L);
        virtualInventoryService.increase(toLocation.getId(), ware.getId(), 45L); // 도착지 용량 50, 현재고 45
        CreateMoveOrderRequest request = createRequest(10L); // 10개 이동 시도

        // when & then
        assertThatThrownBy(() -> moveOrderService.createOrder(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("도착지의 용량이 부족합니다.");
    }

    @Test
    @DisplayName("주문 취소에 성공한다.")
    void cancelOrder_success() {
        // given
        virtualInventoryService.increase(fromLocation.getId(), ware.getId(), 20L);
        moveOrderService.createOrder(createRequest(10L));
        MoveOrder savedOrder = moveOrderRepository.findAll().get(0);
        
        // when
        moveOrderService.cancelOrder(savedOrder.getId());
        
        // then
        MoveOrder cancelledOrder = moveOrderRepository.findById(savedOrder.getId()).get();
        assertThat(cancelledOrder.getStatus()).isEqualTo(com.example.wms.moveorder.domain.MoveOrderStatus.CANCELLED);
        // 예측 재고 원복 확인
        assertThat(virtualInventoryService.getQuantity(fromLocation.getId(), ware.getId())).isEqualTo(20L);
        assertThat(virtualInventoryService.getQuantity(toLocation.getId(), ware.getId())).isZero();
    }

    private CreateMoveOrderRequest createRequest(Long quantity) {
        CreateMoveOrderRequest request = new CreateMoveOrderRequest();
        request.setName("테스트 주문");
        request.setType(com.example.wms.moveorder.domain.MoveOrderType.TRANSFER);
        request.setFromLocationId(fromLocation.getId());
        request.setToLocationId(toLocation.getId());
        request.setScheduledDate(LocalDate.now());
        request.setWareId(ware.getId());
        request.setQuantity(quantity);
        return request;
    }
} 