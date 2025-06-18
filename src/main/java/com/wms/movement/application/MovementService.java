package com.wms.movement.application;

import com.wms.location.domain.exception.LocationException;
import com.wms.location.domain.model.Location;
import com.wms.location.domain.model.LocationType;
import com.wms.location.domain.repository.LocationRepository;
import com.wms.movement.domain.event.MovementCompletedEvent;
import com.wms.movement.domain.event.MovementStartedEvent;
import com.wms.movement.domain.exception.MovementException;
import com.wms.movement.domain.model.Movement;
import com.wms.movement.domain.model.MovementStatus;
import com.wms.movement.domain.repository.MovementRepository;
import com.wms.movement.dto.MovementRequest;
import com.wms.ware.domain.exception.WareException;
import com.wms.ware.domain.model.Ware;
import com.wms.ware.domain.repository.WareRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MovementService {

    private final MovementRepository movementRepository;
    private final WareRepository wareRepository;
    private final LocationRepository locationRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public Movement createMovement(MovementRequest request) {
        log.info("물류이동 생성 요청: {}", request);
        
        Ware ware = wareRepository.findById(request.wareId())
                .orElseThrow(() -> new WareException.NotFoundException(request.wareId()));
        
        Location fromLocation = locationRepository.findById(request.fromLocationId())
                .orElseThrow(() -> new LocationException.NotFoundException(request.fromLocationId()));
        
        Location toLocation = locationRepository.findById(request.toLocationId())
                .orElseThrow(() -> new LocationException.NotFoundException(request.toLocationId()));
        
        Movement movement = Movement.create(ware, fromLocation, toLocation, request.quantity());
        log.info("물류이동 생성 완료: {}", movement);
        
        return movementRepository.save(movement);
    }

    public Movement getMovement(Long id) {
        log.debug("물류이동 조회: {}", id);
        return movementRepository.findById(id)
                .orElseThrow(() -> new MovementException.NotFoundException(id));
    }

    public List<Movement> getAllMovements() {
        log.debug("전체 물류이동 목록 조회");
        return movementRepository.findAll();
    }

    public List<Movement> getMovementsByStatus(String status) {
        log.debug("상태별 물류이동 목록 조회: {}", status);
        return movementRepository.findAllByStatus(MovementStatus.valueOf(status));
    }

    public List<Movement> getMovementsByWare(Long wareId) {
        log.debug("물품별 물류이동 목록 조회: {}", wareId);
        return movementRepository.findAllByWareId(wareId);
    }

    public List<Movement> getMovementsByFromLocation(Long fromLocationId) {
        log.debug("출발지별 물류이동 목록 조회: {}", fromLocationId);
        return movementRepository.findByFromLocation(fromLocationId);
    }

    public List<Movement> getMovementsByToLocation(Long toLocationId) {
        log.debug("도착지별 물류이동 목록 조회: {}", toLocationId);
        return movementRepository.findByToLocation(toLocationId);
    }

    @Transactional
    public Movement startMovement(Long id) {
        log.info("물류이동 시작 요청: {}", id);
        
        Movement movement = getMovement(id);
        movement.start();
        movement = movementRepository.save(movement);

        if (movement.getFromLocation() != null &&
                !LocationType.INBOUND.equals(movement.getFromLocation().getType())) {
            log.info("물류이동 시작 이벤트 발행: {}", movement);
            eventPublisher.publishEvent(new MovementStartedEvent(movement));
        }

        return movement;
    }

    @Transactional
    public Movement completeMovement(Long id) {
        log.info("물류이동 완료 요청: {}", id);
        
        Movement movement = getMovement(id);
        movement.complete();
        movement = movementRepository.save(movement);

        if (movement.getToLocation() != null &&
                !LocationType.OUTBOUND.equals(movement.getToLocation().getType())) {
            log.info("물류이동 완료 이벤트 발행: {}", movement);
            eventPublisher.publishEvent(new MovementCompletedEvent(movement));
        }
        
        return movement;
    }

    @Transactional
    public Movement cancelMovement(Long id) {
        log.info("물류이동 취소 요청: {}", id);
        
        Movement movement = getMovement(id);
        movement.cancel();
        return movementRepository.save(movement);
    }
} 