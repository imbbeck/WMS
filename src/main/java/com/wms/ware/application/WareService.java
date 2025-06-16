package com.wms.ware.application;

import com.wms.ware.domain.model.Ware;
import com.wms.ware.domain.repository.WareRepository;
import com.wms.ware.dto.WareRequest;
import com.wms.ware.dto.WareUpdateRequest;
import com.wms.ware.mapper.WareMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WareService {

    private final WareRepository wareRepository;
    private final WareMapper wareMapper;

    @Transactional
    public Ware createWare(WareRequest request) {
        return wareRepository.save(wareMapper.toEntity(request));
    }

    public Ware getWare(Long id) {
        return wareRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 물품입니다: " + id));
    }

    public List<Ware> getAllWares() {
        return wareRepository.findAll();
    }

    @Transactional
    public Ware updateWare(Long id, WareUpdateRequest request) {
        Ware ware = getWare(id);
        ware.update(
            request.name(),
            request.type(),
            request.paletteUnit()
        );
        return ware;
    }

    @Transactional
    public void deleteWare(Long id) {
        Ware ware = getWare(id);
        wareRepository.delete(ware);
    }
} 