package com.wms.stock.application;

import com.wms.applicationInfra.domain.FieldEnum;
import com.wms.location.application.LocationCacheService;
import com.wms.location.domain.exception.LocationException;
import com.wms.location.domain.model.Location;
import com.wms.location.domain.model.LocationType;
import com.wms.location.domain.repository.LocationRepository;
import com.wms.stock.domain.event.StockCreatedEvent;
import com.wms.stock.domain.event.StockDeletedEvent;
import com.wms.stock.domain.event.StockUpdatedEvent;
import com.wms.stock.domain.exception.StockException;
import com.wms.stock.domain.model.Stock;
import com.wms.stock.domain.repository.StockRepository;
import com.wms.stock.dto.StockDTO;
import com.wms.ware.domain.exception.WareException;
import com.wms.ware.domain.repository.WareRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("StockCtrlService 테스트")
class StockCtrlServiceTest {

	@InjectMocks
	private StockCtrlService stockCtrlService;

	@Mock
	private StockRepository stockRepository;

	@Mock
	private LocationRepository locationRepository;

	@Mock
	private WareRepository wareRepository;

	@Mock
	private ApplicationEventPublisher eventPublisher;

	@Mock
	private LocationCacheService locationCacheService;

	@Mock
	private StockCacheService stockCacheService;

	// ========== create() 메서드 테스트 ==========

	@Test
	@DisplayName("재고 생성 성공")
	void createStock_Success() {
		// Given
		Long wareId = 1L;
		Long warehouseId = 2L;
		Integer quantity = 100;

		StockDTO.CreateReq request = StockDTO.CreateReq.builder()
				.wareId(wareId)
				.warehouseId(warehouseId)
				.quantity(quantity)
				.build();

		Location warehouse = Location.builder()
				.name("테스트 창고")
				.type(LocationType.WAREHOUSE)
				.capacity(500)
				.coordinateX(0)
				.coordinateY(0)
				.build();

		Stock expectedStock = Stock.builder()
				.wareId(wareId)
				.warehouseId(warehouseId)
				.quantity(quantity)
				.build();

		given(wareRepository.existsById(wareId)).willReturn(true);
		given(locationRepository.findById(warehouseId)).willReturn(Optional.of(warehouse));
		given(stockRepository.findByWarehouseIdAndWareId(wareId, warehouseId)).willReturn(Optional.empty());
		given(stockRepository.getTotalPaletteCountByWarehouseId(warehouseId)).willReturn(200);
		given(stockRepository.save(any(Stock.class))).willReturn(expectedStock);

		// When
		Stock result = stockCtrlService.create(request);

		// Then
		assertThat(result).isNotNull();
		assertThat(result.getWareId()).isEqualTo(wareId);
		assertThat(result.getWarehouseId()).isEqualTo(warehouseId);
		assertThat(result.getQuantity()).isEqualTo(quantity);

		// 이벤트 발행 확인
		verify(eventPublisher).publishEvent(any(StockCreatedEvent.class));
	}

	@Test
	@DisplayName("재고 생성 실패 - 존재하지 않는 물품")
	void createStock_WareNotFound() {
		// Given
		StockDTO.CreateReq request = StockDTO.CreateReq.builder()
				.wareId(999L)
				.warehouseId(1L)
				.quantity(100)
				.build();

		given(wareRepository.existsById(999L)).willReturn(false);

		// When & Then
		assertThatThrownBy(() -> stockCtrlService.create(request))
				.isInstanceOf(WareException.NotFoundEx.class);

		// 이벤트 발행되지 않음 확인
		verify(eventPublisher, never()).publishEvent(any());
	}

	@Test
	@DisplayName("재고 생성 실패 - 존재하지 않는 창고")
	void createStock_WarehouseNotFound() {
		// Given
		StockDTO.CreateReq request = StockDTO.CreateReq.builder()
				.wareId(1L)
				.warehouseId(999L)
				.quantity(100)
				.build();

		given(wareRepository.existsById(1L)).willReturn(true);
		given(locationRepository.findById(999L)).willReturn(Optional.empty());

		// When & Then
		assertThatThrownBy(() -> stockCtrlService.create(request))
				.isInstanceOf(LocationException.NotFoundEx.class);

		verify(eventPublisher, never()).publishEvent(any());
	}

	@Test
	@DisplayName("재고 생성 실패 - 창고가 아닌 장소")
	void createStock_NotWarehouse() {
		// Given
		StockDTO.CreateReq request = StockDTO.CreateReq.builder()
				.wareId(1L)
				.warehouseId(2L)
				.quantity(100)
				.build();

		Location inboundLocation = Location.builder()
				.name("입고처")
				.type(LocationType.INBOUND)
				.capacity(null)
				.coordinateX(0)
				.coordinateY(0)
				.build();

		given(wareRepository.existsById(1L)).willReturn(true);
		given(locationRepository.findById(2L)).willReturn(Optional.of(inboundLocation));

		// When & Then
		assertThatThrownBy(() -> stockCtrlService.create(request))
				.isInstanceOf(LocationException.NotWarehouseEx.class);

		verify(eventPublisher, never()).publishEvent(any());
	}

	@Test
	@DisplayName("재고 생성 실패 - 중복 재고")
	void createStock_Duplicate() {
		// Given
		StockDTO.CreateReq request = StockDTO.CreateReq.builder()
				.wareId(1L)
				.warehouseId(2L)  // warehouseId가 2L
				.quantity(100)
				.build();

		Location warehouse = Location.builder()
				.name("테스트 창고")
				.type(LocationType.WAREHOUSE)
				.capacity(500)
				.coordinateX(0)
				.coordinateY(0)
				.build();

		Stock existingStock = Stock.builder()
				.wareId(1L)
				.warehouseId(2L)
				.quantity(50)
				.build();

		given(wareRepository.existsById(1L)).willReturn(true);
		given(locationRepository.findById(2L)).willReturn(Optional.of(warehouse));  // 2L로 수정
		given(stockRepository.findByWarehouseIdAndWareId(1L, 2L)).willReturn(Optional.of(existingStock));

		// When & Then
		assertThatThrownBy(() -> stockCtrlService.create(request))
				.isInstanceOf(StockException.ConflictEx.class);  // 올바른 예외 타입으로 수정

		verify(eventPublisher, never()).publishEvent(any());
	}

	@Test
	@DisplayName("재고 생성 실패 - 창고 용량 초과")
	void createStock_CapacityExceeded() {
		// Given
		StockDTO.CreateReq request = StockDTO.CreateReq.builder()
				.wareId(1L)
				.warehouseId(2L)
				.quantity(200) // 초과 수량
				.build();

		Location warehouse = Location.builder()
				.name("테스트 창고")
				.type(LocationType.WAREHOUSE)
				.capacity(300) // 용량 300
				.coordinateX(0)
				.coordinateY(0)
				.build();

		given(wareRepository.existsById(1L)).willReturn(true);
		given(locationRepository.findById(2L)).willReturn(Optional.of(warehouse));
		given(stockRepository.findByWarehouseIdAndWareId(1L, 2L)).willReturn(Optional.empty());
		given(stockRepository.getTotalPaletteCountByWarehouseId(2L)).willReturn(250); // 현재 250 사용중

		// When & Then
		assertThatThrownBy(() -> stockCtrlService.create(request))
				.isInstanceOf(LocationException.WarehouseCapacityExceededEx.class);

		verify(eventPublisher, never()).publishEvent(any());
	}

	// ========== update() 메서드 테스트 ==========

	@Test
	@DisplayName("재고 수정 성공")
	void updateStock_Success() {
		// Given
		Long wareId = 1L;
		Long warehouseId = 2L;
		Integer newQuantity = 150;

		StockDTO.UpdateReq request = StockDTO.UpdateReq.builder()
				.quantity(newQuantity)
				.build();

		Stock existingStock = Stock.builder()
				.wareId(wareId)
				.warehouseId(warehouseId)
				.quantity(100) // 기존 수량
				.build();

		Location warehouse = Location.builder()
				.name("테스트 창고")
				.type(LocationType.WAREHOUSE)
				.capacity(500)
				.coordinateX(0)
				.coordinateY(0)
				.build();

		given(stockRepository.findByWarehouseIdAndWareId(wareId, warehouseId))
				.willReturn(Optional.of(existingStock));
		given(locationRepository.findById(warehouseId)).willReturn(Optional.of(warehouse));
		given(stockRepository.getTotalPaletteCountByWarehouseId(warehouseId)).willReturn(200);

		// When
		Stock result = stockCtrlService.update(wareId, warehouseId, request);

		// Then
		assertThat(result.getQuantity()).isEqualTo(newQuantity);

		// 이벤트 발행 확인
		ArgumentCaptor<StockUpdatedEvent> eventCaptor = ArgumentCaptor.forClass(StockUpdatedEvent.class);
		verify(eventPublisher).publishEvent(eventCaptor.capture());
		StockUpdatedEvent capturedEvent = eventCaptor.getValue();

		// StockUpdatedEvent의 실제 필드들로 검증
		assertThat(capturedEvent.getWareId()).isEqualTo(wareId);
		assertThat(capturedEvent.getWarehouseId()).isEqualTo(warehouseId);
		assertThat(capturedEvent.getNewQuantity()).isEqualTo(newQuantity);
		assertThat(capturedEvent.getOldQuantity()).isEqualTo(100);
	}

	@Test
	@DisplayName("재고 수정 성공 - 수량 0으로 변경 시 삭제")
	void updateStock_DeleteWhenZero() {
		// Given
		Long wareId = 1L;
		Long warehouseId = 2L;

		StockDTO.UpdateReq request = StockDTO.UpdateReq.builder()
				.quantity(0) // 0으로 변경
				.build();

		Stock existingStock = Stock.builder()
				.wareId(wareId)
				.warehouseId(warehouseId)
				.quantity(100)
				.build();

		Location warehouse = Location.builder()
				.name("테스트 창고")
				.type(LocationType.WAREHOUSE)
				.capacity(500)
				.coordinateX(0)
				.coordinateY(0)
				.build();

		given(stockRepository.findByWarehouseIdAndWareId(wareId, warehouseId))
				.willReturn(Optional.of(existingStock));
		given(locationRepository.findById(warehouseId)).willReturn(Optional.of(warehouse));
		given(stockRepository.getTotalPaletteCountByWarehouseId(warehouseId)).willReturn(100);

		// When
		Stock result = stockCtrlService.update(wareId, warehouseId, request);

		// Then
		assertThat(result.getQuantity()).isEqualTo(0);

		// 삭제 이벤트 발행 확인
		verify(stockRepository).delete(existingStock);
		verify(eventPublisher).publishEvent(any(StockDeletedEvent.class));
	}

	@Test
	@DisplayName("재고 수정 실패 - 재고 없음")
	void updateStock_NotFound() {
		// Given
		StockDTO.UpdateReq request = StockDTO.UpdateReq.builder()
				.quantity(150)
				.build();

		given(stockRepository.findByWarehouseIdAndWareId(1L, 2L))
				.willReturn(Optional.empty());

		// When & Then
		assertThatThrownBy(() -> stockCtrlService.update(1L, 2L, request))
				.isInstanceOf(StockException.NotFoundEx.class);

		verify(eventPublisher, never()).publishEvent(any());
	}

	@Test
	@DisplayName("재고 수정 실패 - 용량 초과")
	void updateStock_CapacityExceeded() {
		// Given
		Long wareId = 1L;
		Long warehouseId = 2L;

		StockDTO.UpdateReq request = StockDTO.UpdateReq.builder()
				.quantity(400) // 초과 수량
				.build();

		Stock existingStock = Stock.builder()
				.wareId(wareId)
				.warehouseId(warehouseId)
				.quantity(100) // 기존 수량
				.build();

		Location warehouse = Location.builder()
				.name("테스트 창고")
				.type(LocationType.WAREHOUSE)
				.capacity(499)
				.coordinateX(0)
				.coordinateY(0)
				.build();

		given(stockRepository.findByWarehouseIdAndWareId(wareId, warehouseId))
				.willReturn(Optional.of(existingStock));
		given(locationRepository.findById(warehouseId)).willReturn(Optional.of(warehouse));
		given(stockRepository.getTotalPaletteCountByWarehouseId(warehouseId)).willReturn(200);
		// 200 - 100(기존) + 400(새로운) = 500으로 용량 초과

		// When & Then
		assertThatThrownBy(() -> stockCtrlService.update(wareId, warehouseId, request))
				.isInstanceOf(LocationException.WarehouseCapacityExceededEx.class);

		verify(eventPublisher, never()).publishEvent(any());
	}

	// ========== delete() 메서드 테스트 ==========

	@Test
	@DisplayName("재고 삭제 성공")
	void deleteStock_Success() {
		// Given
		Long wareId = 1L;
		Long warehouseId = 2L;

		Stock existingStock = Stock.builder()
				.wareId(wareId)
				.warehouseId(warehouseId)
				.quantity(100)
				.build();

		given(stockRepository.findByWarehouseIdAndWareId(wareId, warehouseId))
				.willReturn(Optional.of(existingStock));

		// When
		stockCtrlService.delete(wareId, warehouseId);

		// Then
		verify(stockRepository).delete(existingStock);
		verify(eventPublisher).publishEvent(any(StockDeletedEvent.class));
	}

	@Test
	@DisplayName("재고 삭제 실패 - 재고 없음")
	void deleteStock_NotFound() {
		// Given
		given(stockRepository.findByWarehouseIdAndWareId(1L, 2L))
				.willReturn(Optional.empty());

		// When & Then
		assertThatThrownBy(() -> stockCtrlService.delete(1L, 2L))
				.isInstanceOf(StockException.NotFoundEx.class);

		verify(stockRepository, never()).delete(any());
		verify(eventPublisher, never()).publishEvent(any());
	}

	// ========== 이벤트 발행 검증 테스트 ==========

	@Test
	@DisplayName("이벤트 발행 세부 검증 - StockCreatedEvent")
	void verifyStockCreatedEvent() {
		// Given
		StockDTO.CreateReq request = StockDTO.CreateReq.builder()
				.wareId(1L)
				.warehouseId(2L)
				.quantity(100)
				.build();

		Location warehouse = Location.builder()
				.name("테스트 창고")
				.type(LocationType.WAREHOUSE)
				.capacity(500)
				.coordinateX(0)
				.coordinateY(0)
				.build();

		Stock savedStock = Stock.builder()
				.wareId(1L)
				.warehouseId(2L)
				.quantity(100)
				.build();

		given(wareRepository.existsById(1L)).willReturn(true);
		given(locationRepository.findById(2L)).willReturn(Optional.of(warehouse));
		given(stockRepository.findByWarehouseIdAndWareId(1L, 2L)).willReturn(Optional.empty());
		given(stockRepository.getTotalPaletteCountByWarehouseId(2L)).willReturn(100);
		given(stockRepository.save(any(Stock.class))).willReturn(savedStock);

		// When
		stockCtrlService.create(request);

		// Then
		ArgumentCaptor<StockCreatedEvent> eventCaptor = ArgumentCaptor.forClass(StockCreatedEvent.class);
		verify(eventPublisher).publishEvent(eventCaptor.capture());

		StockCreatedEvent capturedEvent = eventCaptor.getValue();
		// StockCreatedEvent의 실제 필드들로 검증
		assertThat(capturedEvent.getWareId()).isEqualTo(1L);
		assertThat(capturedEvent.getWarehouseId()).isEqualTo(2L);
		assertThat(capturedEvent.getQuantity()).isEqualTo(100);
	}
}