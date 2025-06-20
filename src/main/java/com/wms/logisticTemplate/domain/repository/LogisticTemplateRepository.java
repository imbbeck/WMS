package com.wms.logisticTemplate.domain.repositoty;

import java.util.Optional;

import com.wms.logisticTemplate.domain.model.LogisticTemplate;
import com.wms.logisticTemplate.domain.model.LogisticType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LogisticTemplateRepository extends JpaRepository<LogisticTemplate, Long> {

	@EntityGraph(attributePaths = {"ware", "fromLocation", "toLocation"})
	Optional<LogisticTemplate> findWithEntityGraphById(Long id); // fetch join

	@EntityGraph(attributePaths = {"ware", "fromLocation", "toLocation"})
	Page<LogisticTemplate> findAllWithPagination(Pageable pageable);

	@EntityGraph(attributePaths = {"ware", "fromLocation", "toLocation"})
	Page<LogisticTemplate> findAllWithPaginationByType(LogisticType type, Pageable pageable);

	@EntityGraph(attributePaths = {"ware", "fromLocation", "toLocation"})
	Page<LogisticTemplate> findAllWithPaginationByWareId(Long wareId, Pageable pageable);
}
