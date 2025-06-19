package com.wms.ware.application;

import com.wms.ware.domain.model.Ware;
import com.wms.ware.domain.repository.WareRepository;
import com.wms.ware.dto.WareDTO;
import lombok.RequiredArgsConstructor;
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

    @Transactional
    public Ware createWare(WareDTO.createReq request) {
        if (wareRepository.existsByName(request. getName())) {
            throw  new ResponseStatusException(HttpStatus.CONFLICT);
        }

        Ware ware = request.toEntity();

        Ware saved = wareRepository.save(ware);

        // 캐시 추가 고민

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
    public Ware updateWare(Long id, WareDTO.updateReq request) {
        Ware ware = getWare(id);
        ware.update(request.getName(), request.getType(), request.getPaletteUnit());
        return ware;
    }

    @Transactional
    public void deleteWare(Long id) {
        Ware ware = getWare(id);
        wareRepository.delete(ware);
    }
} 