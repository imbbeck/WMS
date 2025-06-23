package com.wms.logisticTemplate.domain.repository;

import java.util.List;
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

	//TODO: @EntityGraph + 쿼리 메서드 조합 변경. 단순한 조건일 때는 무난하지만, join 대상이 쿼리 조건에도 쓰이면 중복 join 발생 가능성 있음.
	@EntityGraph(attributePaths = {"ware", "fromLocation", "toLocation"})
	Page<LogisticTemplate> findAllByWareId(Long wareId, Pageable pageable);

	boolean existsByFromLocationIdOrToLocationId(Long locationId, Long locationId1);

	List<LogisticTemplate> findAllByFromLocationIdOrToLocationId(Long fromLocationId, Long fromLocationId1);
}
