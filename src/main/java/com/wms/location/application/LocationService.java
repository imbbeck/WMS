package com.wms.location.application;

import com.wms.location.domain.model.Location;
import com.wms.location.domain.model.LocationType;
import com.wms.location.domain.repository.LocationRepository;
import com.wms.location.dto.LocationRequest;
import com.wms.location.dto.LocationUpdateRequest;
import com.wms.location.mapper.LocationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LocationService {

    private final LocationRepository locationRepository;
    private final LocationMapper locationMapper;

    @Transactional
    public Location createLocation(LocationRequest request) {
        if (locationRepository.existsByName(request.getName())) {
            throw new IllegalArgumentException("이미 존재하는 장소 이름입니다.");
        }

        Location location = Location.builder()
                .name(request.getName())
                .type(request.getType())
                .capacity(request.getCapacity())
                .build();

        return locationRepository.save(location);
    }

    public Location getLocation(Long id) {
        return locationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Location not found with id: " + id));
    }

    public List<Location> getLocationsByType(LocationType type) {
        return locationRepository.findByType(type);
    }

    @Transactional
    public Location updateLocation(Long id, LocationUpdateRequest request) {
        Location location = getLocation(id);
        location.update(request.getName(), request.getType());
        return location;
    }

    @Transactional
    public void deleteLocation(Long id) {
        Location location = getLocation(id);
        locationRepository.delete(location);
    }
} 