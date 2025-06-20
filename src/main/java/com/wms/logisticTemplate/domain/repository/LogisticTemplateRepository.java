package com.wms.logisticTemplate.domain.repository;

import java.util.Optional;

import com.wms.logisticTemplate.domain.model.LogisticTemplate;
import com.wms.logisticTemplate.domain.model.LogisticType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.NonNull;

public interface LogisticTemplateRepository extends JpaRepository<LogisticTemplate, Long> {

	@EntityGraph(attributePaths = {"ware", "fromLocation", "toLocation"})
	Optional<LogisticTemplate> findWithEntityGraphById(Long id); // fetch join

	@EntityGraph(attributePaths = {"ware", "fromLocation", "toLocation"})
	Page<LogisticTemplate> findAll(@NonNull Pageable pageable);

	@EntityGraph(attributePaths = {"ware", "fromLocation", "toLocation"})
	Page<LogisticTemplate> findAllByType(LogisticType type, Pageable pageable);

	@EntityGraph(attributePaths = {"ware", "fromLocation", "toLocation"})
	Page<LogisticTemplate> findAllByWareId(Long wareId, Pageable pageable);
}
