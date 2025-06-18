package com.wms.location.application;

import com.wms.location.domain.model.Location;
import com.wms.location.domain.model.LocationConnection;
import com.wms.location.domain.model.LocationType;
import com.wms.location.domain.exception.LocationException.*;
import com.wms.location.domain.repository.LocationCacheManager;
import com.wms.location.domain.repository.LocationRepository;
import com.wms.location.dto.LocationDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LocationService {

    private final LocationRepository locationRepository;
    private final LocationCacheManager locationCache;


    @Transactional
    public Location createLocation(LocationDTO.createReq request) {
        if (locationRepository.existsByName(request.getName())) {
            throw new IllegalArgumentException("이미 존재하는 장소 이름입니다.");
        }

        Location location = Location.builder()
                .name(request.getName())
                .type(request.getType())
                .capacity(request.getCapacity())
                .build();

        Location saved = locationRepository.save(location);

        // Walk-through: 즉시 캐시 갱신
        locationCache.updateCache(saved.getId(), saved.getName(), saved.getType());

        return saved;
    }

    // Location 조회시 연결 그래프 정보도 함께
    public Location getLocationWithConnections(Long id) {
        return locationRepository.findByIdWithAllConnections(id)
                .orElseThrow(() -> new NotFoundException(id));
    }

    // 연결 그래프 정보 조회
    public List<LocationConnection> getConnectionInfo(Long locationId) {
        Location location = getLocationWithConnections(locationId);
        return location.getAllConnections();
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
    }
} 