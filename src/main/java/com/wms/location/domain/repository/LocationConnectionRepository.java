package com.wms.location.domain.repository;

import java.util.List;
import java.util.Optional;

import com.wms.location.domain.model.LocationConnection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LocationConnectionRepository extends JpaRepository<LocationConnection, Long> {

	// 특정 Location과 연결된 모든 Connection 조회
	@Query("SELECT lc FROM LocationConnection lc WHERE lc.locationAId = :locationId OR lc.locationBId = :locationId")
	List<LocationConnection> findAllByLocationId(@Param("locationId") Long locationId);

	// 두 Location 간 연결 존재 여부 확인
	@Query("SELECT lc FROM LocationConnection lc WHERE " +
			"(lc.locationAId = :id1 AND lc.locationBId = :id2) OR " +
			"(lc.locationAId = :id2 AND lc.locationBId = :id1)")
	Optional<LocationConnection> findByLocationIds(@Param("id1") Long id1, @Param("id2") Long id2);

	// 특정 Location이 포함된 연결 개수
	@Query("SELECT COUNT(lc) FROM LocationConnection lc WHERE lc.locationAId = :locationId OR lc.locationBId = :locationId")
	long countByLocationId(@Param("locationId") Long locationId);

	// 특정 Location들 중 하나라도 포함된 모든 연결
	@Query("SELECT lc FROM LocationConnection lc WHERE lc.locationAId IN :locationIds OR lc.locationBId IN :locationIds")
	List<LocationConnection> findAllByLocationIds(@Param("locationIds") List<Long> locationIds);

	@Modifying
	@Query("DELETE FROM LocationConnection lc WHERE lc.locationAId = :locationId OR lc.locationBId = :locationId")
	void deleteByLocationId(@Param("locationId") Long locationId);

	Optional<LocationConnection> findByLocationAIdAndLocationBId(Long locationAId, Long locationBId);
}

