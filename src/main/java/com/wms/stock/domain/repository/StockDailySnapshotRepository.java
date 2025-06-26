package com.wms.stock.domain.repository;

import com.wms.stock.domain.model.StockDailySnapshot;
import com.wms.stock.domain.model.StockKey;
import com.wms.stock.domain.model.StockSnapshotKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StockDailySnapshotRepository extends JpaRepository<StockDailySnapshot, Long> {

	Optional<StockDailySnapshot> findByKey(StockSnapshotKey key);

	List<StockDailySnapshot> findByKey_StockKeyOrderByKey_SnapshotDateDesc(StockKey stockKey);
}

