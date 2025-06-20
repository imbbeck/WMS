package com.wms.logisticTemplate.application;

import com.wms.logisticTemplate.domain.model.LogisticTemplate;
import com.wms.logisticTemplate.domain.model.LogisticType;
import com.wms.logisticTemplate.domain.repository.LogisticTemplateRepository;
import com.wms.logisticTemplate.dto.LogisticTemplateDTO;
import com.wms.location.domain.model.Location;
import com.wms.location.domain.repository.LocationRepository;
import com.wms.ware.domain.model.Ware;
import com.wms.ware.domain.repository.WareRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LogisticTemplateService {
	private final LogisticTemplateRepository logisticTemplateRepository;
	private final WareRepository wareRepository;
	private final LocationRepository locationRepository;

	@Transactional
	public LogisticTemplate create(LogisticTemplateDTO.CreateReq req) {
		Ware ware = wareRepository.findById(req.getWareId())
				.orElseThrow(() -> new IllegalArgumentException("Ware not found"));

		Location from = locationRepository.findById(req.getFromLocationId())
				.orElseThrow(() -> new IllegalArgumentException("FromLocation not found"));

		Location to = locationRepository.findById(req.getToLocationId())
				.orElseThrow(() -> new IllegalArgumentException("ToLocation not found"));

		LogisticTemplate template = req.toEntity(ware, from, to);
		return logisticTemplateRepository.save(template);
	}

	@Transactional
	public LogisticTemplate update(Long id, LogisticTemplateDTO.UpdateReq req) {
		LogisticTemplate template = logisticTemplateRepository.findById(id)
				.orElseThrow(() -> new IllegalArgumentException("LogisticTemplate not found"));

		template.update(req.getName(), req.getType(), req.getStandardQuantity());
		return template;
	}

	@Transactional(readOnly = true)
	public LogisticTemplate findById(Long id) {
		return logisticTemplateRepository.findWithEntityGraphById(id)
				.orElseThrow(() -> new IllegalArgumentException("LogisticTemplate not found"));
	}

	// 전체 페이징 조회
	public Page<LogisticTemplate> getTemplates(Pageable pageable) {
		return logisticTemplateRepository.findAll(pageable);
	}

	// 타입별 페이징 조회
	public Page<LogisticTemplate> getTemplatesByType(LogisticType type, Pageable pageable) {
		return logisticTemplateRepository.findAllByType(type, pageable);
	}

	// wareId별 페이징 조회
	public Page<LogisticTemplate> getTemplatesByWareId(Long wareId, Pageable pageable) {
		return logisticTemplateRepository.findAllByWareId(wareId, pageable);
	}

	@Transactional
	public void delete(Long id) {
		LogisticTemplate template = logisticTemplateRepository.findById(id)
				.orElseThrow(() -> new IllegalArgumentException("LogisticTemplate not found"));
		logisticTemplateRepository.delete(template);
	}
}
