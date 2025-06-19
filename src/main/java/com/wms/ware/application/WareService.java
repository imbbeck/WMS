package com.wms.ware.application;

import com.wms.location.domain.event.LocationCreatedEvent;
import com.wms.location.domain.event.LocationDeletedEvent;
import com.wms.location.domain.event.LocationUpdatedEvent;
import com.wms.ware.domain.model.Ware;
import com.wms.ware.domain.repository.WareRepository;
import com.wms.ware.dto.WareDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WareService {

    private final WareRepository wareRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public Ware createWare(WareDTO.CreateReq request) {
        if (wareRepository.existsByName(request. getName())) {
            throw  new ResponseStatusException(HttpStatus.CONFLICT);
        }

        Ware ware = request.toEntity();

        Ware saved = wareRepository.save(ware);

        // 생성 이벤트 발행
        // ReferenceDataCacheManager.handleLocationCreated에서 구독. 캐시 생성
        eventPublisher.publishEvent(new LocationCreatedEvent(saved.getId(), saved.getName()));

        return saved;
    }



    public Ware getWare(Long id) {
        return wareRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 물품입니다: " + id));
    }

    public List<Ware> getWares() {
        return wareRepository.findAll();
    }

    public List<Ware> getWaresByType(String type) {
        return wareRepository.findAllByType(type);
    }

    @Transactional
    public Ware updateWare(Long id, WareDTO.UpdateReq request) {
        Ware ware = getWare(id);
        ware.update(request.getName(), request.getType(), request.getPaletteUnit());

        // 수정 이벤트 발행
        // ReferenceDataCacheManager.handleLocationUpdated에서 구독. 캐시 갱신
        eventPublisher.publishEvent(new LocationUpdatedEvent(ware.getId(), ware.getName()));

        return ware;
    }

    @Transactional
    public void deleteWare(Long id) {
        Ware ware = getWare(id);

        // 삭제 이벤트 발행
        // LocationConnectionService.onLocationDeleted에서 구독. 연결된 Connection들 자동 삭제
        // ReferenceDataCacheManager.handleLocationDeleted에서 구독. 캐시 삭제
        eventPublisher.publishEvent(new LocationDeletedEvent(id));

        wareRepository.delete(ware);
    }
} 