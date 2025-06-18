package com.wms.location.application;

import com.wms.location.domain.event.LocationDeletedEvent;
import com.wms.location.domain.model.Location;
import com.wms.location.domain.model.LocationType;
import com.wms.location.domain.exception.LocationException.*;
import com.wms.location.domain.repository.LocationCacheManager;
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
    private final LocationCacheManager locationCache;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public Location createLocation(LocationDTO.createReq request) {
        if (locationRepository.existsByName(request.getName())) {
            throw new IllegalArgumentException("이미 존재하는 장소 이름입니다.");
        }

        //  엔티티 생성 책임을 DTO에 위임
        Location location = request.toEntity();


        Location saved = locationRepository.save(location);

        // Walk-through: 즉시 캐시 갱신
        locationCache.updateCache(saved.getId(), saved.getName(), saved.getType());

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
    public Location updateLocation(Long id, LocationDTO.updateReq request) {
        Location location = getLocation(id);
        location.update(request.getName(), request.getType(), request.getCapacity());

        // Walk-through: 즉시 캐시 갱신
        locationCache.updateCache(id, request.getName(), request.getType());

        return location;
    }

    @Transactional
    public void deleteLocation(Long id) {
        Location location = getLocation(id);
        // cascade 옵션으로 연결된 Connection들 자동 삭제
        locationRepository.delete(location);
        // Walk-through: 즉시 캐시에서 제거
        locationCache.removeFromCache(id);

        // 이벤트 발행
        eventPublisher.publishEvent(new LocationDeletedEvent(id));
    }
} 