package com.wms.logisticTemplate.application;

import com.wms.location.domain.exception.LocationException;
import com.wms.location.domain.model.LocationConnection;
import com.wms.location.domain.model.LocationType;
import com.wms.location.domain.repository.LocationConnectionRepository;
import com.wms.logisticTemplate.domain.exception.LogisticTemplateException;
import com.wms.logisticTemplate.domain.model.LogisticTemplate;
import com.wms.logisticTemplate.domain.model.LogisticType;
import com.wms.logisticTemplate.domain.repository.LogisticTemplateRepository;
import com.wms.logisticTemplate.dto.LogisticTemplateDTO;
import com.wms.location.domain.model.Location;
import com.wms.location.domain.repository.LocationRepository;
import com.wms.ware.domain.exception.WareException;
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
	private final LocationConnectionRepository locationConnectionRepository;

	@Transactional
	public LogisticTemplate create(LogisticTemplateDTO.CreateReq req) {
		Ware ware = wareRepository.findById(req.getWareId())
				.orElseThrow(() -> WareException.notFound(req.getWareId()));

		Location from = locationRepository.findById(req.getFromLocationId())
				.orElseThrow(() -> LocationException.notFound(req.getFromLocationId()));

		Location to = locationRepository.findById(req.getToLocationId())
				.orElseThrow(() -> LocationException.notFound(req.getToLocationId()));

		// 타입, from, to의 유효성 검사
		LogisticType type = req.getType();
		if (!type.isValidLocationTypes(from.getType(), to.getType())) {
			throw LogisticTemplateException.notMatchedLocationWithType(type.getValidationMessage());
		}

		// 평균 소요시간 가져오기
		Integer trt = locationConnectionRepository.findByLocationAIdAndLocationBId(Math.min(from.getId(), to.getId()), Math.max(from.getId(), to.getId()))
				.map(LocationConnection::getTrt)
				.orElse(null);

		LogisticTemplate template = req.toEntity(ware, from, to, trt);
		return logisticTemplateRepository.save(template);
	}

	@Transactional
	public LogisticTemplate update(Long id, LogisticTemplateDTO.UpdateReq req) {
		LogisticTemplate template = logisticTemplateRepository.findById(id)
				.orElseThrow(() -> LogisticTemplateException.notFound(id));

		template.update(req.getName(), req.getTrt(), req.getStandardQuantity());
		return template;
	}


	@Transactional(readOnly = true)
	public LogisticTemplate findById(Long id) {
		return logisticTemplateRepository.findWithEntityGraphById(id)
				.orElseThrow(() -> LogisticTemplateException.notFound(id)); // 상세조회용 findWithEntityGraphById
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
				.orElseThrow(() -> LogisticTemplateException.notFound(id));
		logisticTemplateRepository.delete(template);
	}
}
