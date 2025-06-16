package com.wms.location.application;

import com.wms.location.domain.model.Location;
import com.wms.location.domain.model.TransferDuration;
import com.wms.location.domain.repository.LocationRepository;
import com.wms.location.domain.repository.TransferDurationRepository;
import com.wms.location.dto.TransferDurationRequest;
import com.wms.location.dto.TransferDurationUpdateRequest;
import com.wms.location.mapper.TransferDurationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TransferDurationService {

    private final TransferDurationRepository transferDurationRepository;
    private final LocationRepository locationRepository;
    private final TransferDurationMapper transferDurationMapper;
    private final LocationService locationService;

    @Transactional
    public TransferDuration createTransferDuration(TransferDurationRequest request) {
        Location fromLocation = locationService.getLocation(request.getFromLocationId());
        Location toLocation = locationService.getLocation(request.getToLocationId());

        if (transferDurationRepository.findByFromLocationAndToLocation(fromLocation, toLocation).isPresent()) {
            throw new IllegalArgumentException("이미 존재하는 이동 경로입니다.");
        }

        TransferDuration transferDuration = TransferDuration.builder()
                .fromLocation(fromLocation)
                .toLocation(toLocation)
                .estimatedDuration(request.getEstimatedDuration())
                .build();

        transferDuration.validateLocations();
        return transferDurationRepository.save(transferDuration);
    }

    public TransferDuration getTransferDuration(Long id) {
        return transferDurationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 이동 시간 정보입니다."));
    }

    public TransferDuration getTransferDurationByLocations(Long fromLocationId, Long toLocationId) {
        return transferDurationRepository.findByFromLocationIdAndToLocationId(fromLocationId, toLocationId)
                .orElseThrow(() -> new IllegalArgumentException(
                    String.format("Transfer duration not found between locations: %d -> %d", 
                        fromLocationId, toLocationId)));
    }

    @Transactional
    public TransferDuration updateTransferDuration(Long id, TransferDurationUpdateRequest request) {
        TransferDuration transferDuration = getTransferDuration(id);
        Location fromLocation = transferDuration.getFromLocation();
        Location toLocation = transferDuration.getToLocation();
        
        transferDuration.update(fromLocation, toLocation, request.getEstimatedDuration ());
        return transferDuration;
    }

    @Transactional
    public void deleteTransferDuration(Long id) {
        TransferDuration transferDuration = getTransferDuration(id);
        transferDurationRepository.delete(transferDuration);
    }
} 