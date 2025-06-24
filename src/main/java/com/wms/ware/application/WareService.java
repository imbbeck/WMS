package com.wms.ware.application;

import com.wms.applicationInfra.domain.FieldEnum;
import com.wms.ware.domain.event.WareCreatedEvent;
import com.wms.ware.domain.event.WareDeletedEvent;
import com.wms.ware.domain.event.WareUpdatedEvent;
import com.wms.ware.domain.exception.WareException;
import com.wms.ware.domain.model.Ware;
import com.wms.ware.domain.repository.WareRepository;
import com.wms.ware.dto.WareDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WareService {

    private final WareRepository wareRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public Ware createWare(WareDTO.CreateReq request) {
        if (wareRepository.existsByName(request.getName())) {
            throw WareException.duplicate(FieldEnum.NAME, request.getName());
        }
        Ware ware = request.toEntity();
        Ware saved = wareRepository.save(ware);

        // 생성 이벤트 발행
        // ReferenceDataCacheManager.handleLocationCreated에서 구독. 캐시 생성
        eventPublisher.publishEvent(new WareCreatedEvent(saved.getId(), saved.getName()));

        return saved;
    }

    public Ware getWareById(Long id) {
        return wareRepository.findById(id)
                .orElseThrow(() -> WareException.notFound(id));
    }

    public List<Ware> getAllWares() {
        return wareRepository.findAll();
    }

    public List<Ware> getWaresByType(String type) {
        return wareRepository.findAllByType(type);
    }

    @Transactional
    public Ware updateWare(Long id, WareDTO.UpdateReq request) {
        Ware ware = getWareById(id);
        ware.update(request.getName(), request.getType(), request.getPaletteUnit());

        // 수정 이벤트 발행
        // ReferenceDataCacheManager.handleLocationUpdated에서 구독. 캐시 갱신
        eventPublisher.publishEvent(new WareUpdatedEvent(ware.getId(), ware.getName()));

        return ware;
    }

    @Transactional
    public void deleteWare(Long id) {
        Ware ware = getWareById(id);

        // 삭제 이벤트 발행
        // LocationConnectionService.onLocationDeleted에서 구독. 연결된 Connection들 자동 삭제
        // ReferenceDataCacheManager.handleLocationDeleted에서 구독. 캐시 삭제
        eventPublisher.publishEvent(new WareDeletedEvent(id));

        wareRepository.delete(ware);
    }
} 