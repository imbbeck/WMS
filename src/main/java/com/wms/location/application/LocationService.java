package com.wms.location.application;

import com.wms.applicationInfra.idnameMapCashing.DomainCacheManager;
import com.wms.location.domain.event.LocationCreatedEvent;
import com.wms.location.domain.event.LocationDeletedEvent;
import com.wms.location.domain.event.LocationUpdatedEvent;
import com.wms.location.domain.model.Location;
import com.wms.location.domain.model.LocationType;
import com.wms.location.domain.exception.LocationException.*;
import com.wms.location.domain.repository.LocationRepository;
import com.wms.location.dto.LocationDTO;
import com.wms.location.dto.LocationWithConnectionsDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LocationService {

    private final LocationRepository locationRepository;
    private final DomainCacheManager<Long, String> locationCacheManager;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public Location createLocation(LocationDTO.CreateReq request) {
        if (locationRepository.existsByName(request.getName())) {
            throw new IllegalArgumentException("이미 존재하는 장소 이름입니다.");
        }

        //  엔티티 생성 책임을 DTO에 위임
        Location location = request.toEntity();

        Location saved = locationRepository.save(location);

        // 생성 이벤트 발행
        // ReferenceDataCacheManager.handleLocationCreated에서 구독. 캐시 생성
        eventPublisher.publishEvent(new LocationCreatedEvent(saved.getId(), saved.getName()));

        return saved;
    }

    // Location 조회시 연결 그래프 정보도 함께.
    public LocationWithConnectionsDTO getLocationWithConnections(Long id) {
        return locationRepository.findLocationWithConnections(id)
                .orElseThrow(() -> new NotFoundException(id));
    }

    // 단순 조회: 그래프 불필요
    public Location getLocation(Long id) {
        return locationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(id));
    }

    public List<Location> getLocations() {
        return locationRepository.findAll();
    }

    public List<Location> getLocationsByType(LocationType type) {
        return locationRepository.findByType(type);
    }

    @Transactional
    public Location updateLocation(Long id, LocationDTO.UpdateReq request) {
        Location location = getLocation(id);
        location.update(request.getName(), request.getCapacity(), request.getCoordinateX(), request.getCoordinateY());

        // 수정 이벤트 발행
        // ReferenceDataCacheManager.handleLocationUpdated에서 구독. 캐시 갱신
        eventPublisher.publishEvent(new LocationUpdatedEvent(location.getId(), location.getName()));

        return location;
    }

    @Transactional
    public void deleteLocation(Long id) {
        Location location = getLocation(id);

        // 삭제 이벤트 발행
        // LocationConnectionService.onLocationDeleted에서 구독. 연결된 Connection들 자동 삭제
        // ReferenceDataCacheManager.handleLocationDeleted에서 구독. 캐시 삭제
        eventPublisher.publishEvent(new LocationDeletedEvent(id));

        locationRepository.delete(location);

    }
} 