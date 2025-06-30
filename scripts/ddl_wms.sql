-- ============================================
-- 물류 시스템 데이터베이스 DDL
-- ============================================

CREATE DATABASE IF NOT EXISTS wms
	CHARACTER SET utf8mb4
	COLLATE utf8mb4_unicode_ci;

USE wms;

-- ============================================
-- 1. 사용자 정보 테이블
-- ============================================
CREATE TABLE user_info (
	                       id BIGINT AUTO_INCREMENT PRIMARY KEY,
	                       username VARCHAR(20) NOT NULL UNIQUE,
	                       name VARCHAR(100) NOT NULL,
	                       email VARCHAR(255) NOT NULL UNIQUE,
	                       password VARCHAR(255) NOT NULL,
	                       type ENUM('ADMIN', 'WORKER') NOT NULL,
	                       created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
	                       updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

	                       INDEX idx_username (username),
	                       INDEX idx_type (type)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- 2. 장소 테이블
-- ============================================
CREATE TABLE location (
	                      id BIGINT AUTO_INCREMENT PRIMARY KEY,
	                      name VARCHAR(100) NOT NULL UNIQUE,
	                      type ENUM('INBOUND', 'OUTBOUND', 'WAREHOUSE') NOT NULL,
	                      capacity INT NULL,
	                      coordinate_x INT NOT NULL,
	                      coordinate_y INT NOT NULL,
	                      created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
	                      updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

	                      INDEX idx_name (name),
	                      INDEX idx_type (type),

	                      CONSTRAINT chk_warehouse_capacity CHECK (
		                      (type = 'WAREHOUSE' AND capacity IS NOT NULL AND capacity > 0) OR
		                      (type IN ('INBOUND', 'OUTBOUND') AND capacity IS NULL)
		                      )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- 3. 장소 간 연결 테이블
-- ============================================
CREATE TABLE location_connection (
	                                 id BIGINT AUTO_INCREMENT PRIMARY KEY,
	                                 location_a_id BIGINT NOT NULL,
	                                 location_b_id BIGINT NOT NULL,
	                                 trt INT NOT NULL,
	                                 created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
	                                 updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

	                                 UNIQUE KEY uk_location_connection (location_a_id, location_b_id),

	                                 FOREIGN KEY (location_a_id) REFERENCES location(id) ON DELETE CASCADE,
	                                 FOREIGN KEY (location_b_id) REFERENCES location(id) ON DELETE CASCADE,

	                                 CONSTRAINT chk_location_order CHECK (location_a_id < location_b_id),
	                                 CONSTRAINT chk_trt_positive CHECK (trt > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- 4. 물품 테이블
-- ============================================
CREATE TABLE ware (
	                  id BIGINT AUTO_INCREMENT PRIMARY KEY,
	                  name VARCHAR(100) NOT NULL UNIQUE,
	                  type VARCHAR(100) NOT NULL,
	                  palette_unit INT NOT NULL,
	                  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
	                  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

	                  INDEX idx_name (name),
	                  INDEX idx_type (type),

	                  CONSTRAINT chk_palette_unit_positive CHECK (palette_unit > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- 5. 재고 테이블
-- ============================================
CREATE TABLE stocks (
	                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
	                    ware_id BIGINT NOT NULL,
	                    location_id BIGINT NOT NULL,
	                    quantity INT NOT NULL DEFAULT 0,
	                    version BIGINT NOT NULL DEFAULT 0,
	                    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
	                    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

	                    UNIQUE KEY uk_stock_ware_location (ware_id, location_id),
	                    INDEX idx_ware_id (ware_id),
	                    INDEX idx_location_id (location_id),

	                    CONSTRAINT chk_quantity_non_negative CHECK (quantity >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- 6. 재고 일일 스냅샷 테이블
-- ============================================
CREATE TABLE stock_daily_snapshot (
	                                  id BIGINT AUTO_INCREMENT PRIMARY KEY,
	                                  ware_id BIGINT NOT NULL,
	                                  location_id BIGINT NOT NULL,
	                                  snapshot_date DATE NOT NULL,
	                                  quantity INT NOT NULL,
	                                  change_from_yesterday INT NULL,

	                                  UNIQUE KEY uk_snapshot_key (ware_id, location_id, snapshot_date),
	                                  INDEX idx_ware_id (ware_id),
	                                  INDEX idx_snapshot_date_desc (snapshot_date DESC),
	                                  INDEX idx_location_id (location_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- 7. 물류이동 템플릿 테이블
-- ============================================
CREATE TABLE logistic_template (
	                               id BIGINT AUTO_INCREMENT PRIMARY KEY,
	                               name VARCHAR(255) NOT NULL,
	                               type ENUM('INBOUND', 'OUTBOUND', 'INNER') NOT NULL,
	                               ware_id BIGINT NOT NULL,
	                               from_location_id BIGINT NOT NULL,
	                               to_location_id BIGINT NOT NULL,
	                               trt INT NULL,
	                               standard_quantity INT NOT NULL,
	                               created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
	                               updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

	                               INDEX idx_name (name),
	                               INDEX idx_type (type),
	                               INDEX idx_ware_id (ware_id),
	                               INDEX idx_from_location_id(from_location_id),

	                               FOREIGN KEY (ware_id) REFERENCES ware(id),
	                               FOREIGN KEY (from_location_id) REFERENCES location(id),
	                               FOREIGN KEY (to_location_id) REFERENCES location(id),

	                               CONSTRAINT chk_standard_quantity_positive CHECK (standard_quantity > 0),
	                               CONSTRAINT chk_different_locations CHECK (from_location_id != to_location_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- 8. 물류이동 작업 테이블 (당일만 보관)
-- ============================================
CREATE TABLE logistic_task (
	                           id BIGINT AUTO_INCREMENT PRIMARY KEY,
	                           name VARCHAR(255) NOT NULL,
	                           type ENUM('INBOUND', 'OUTBOUND', 'INNER') NOT NULL,
	                           worker_id BIGINT NOT NULL,
	                           ware_id BIGINT NOT NULL,
	                           from_location_id BIGINT NOT NULL,
	                           to_location_id BIGINT NOT NULL,
	                           quantity INT NOT NULL,
	                           scheduled_date DATE NOT NULL,
	                           etd TIME NOT NULL,
	                           eta TIME NOT NULL,
	                           atd TIME NULL,
	                           ata TIME NULL,
	                           status ENUM('PENDING', 'INITIATED', 'INITIATE_DELAYED', 'COMPLETED', 'COMPLETE_DELAYED', 'CANCELLED', 'FAILED') NOT NULL DEFAULT 'PENDING',
	                           template_id_snapshot INT NULL,
	                           created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
	                           updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

	                           INDEX idx_scheduled_date_desc (scheduled_date DESC),
	                           INDEX idx_status (status),
	                           INDEX idx_worker_schedule (worker_id, scheduled_date, status),
	                           INDEX idx_dashboard_query (scheduled_date, worker_id, etd),

	                           FOREIGN KEY (worker_id) REFERENCES user_info(id),
	                           FOREIGN KEY (ware_id) REFERENCES ware(id),
	                           FOREIGN KEY (from_location_id) REFERENCES location(id),
	                           FOREIGN KEY (to_location_id) REFERENCES location(id),

	                           CONSTRAINT chk_quantity_positive CHECK (quantity > 0),
	                           CONSTRAINT chk_different_task_locations CHECK (from_location_id != to_location_id),
	                           CONSTRAINT chk_eta_after_etd CHECK (eta >= etd)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- 9. 물류이동 작업 히스토리 테이블
-- ============================================
CREATE TABLE logistic_task_history (
	                                   id BIGINT AUTO_INCREMENT PRIMARY KEY,
	                                   original_task_id BIGINT NOT NULL,
	                                   name VARCHAR(255) NOT NULL,
	                                   type ENUM('INBOUND', 'OUTBOUND', 'INNER') NOT NULL,
	                                   worker_id BIGINT NOT NULL,
	                                   worker_name VARCHAR(100) NOT NULL,
	                                   ware_id BIGINT NOT NULL,
	                                   ware_name VARCHAR(100) NOT NULL,
	                                   from_location_id BIGINT NOT NULL,
	                                   from_location_name VARCHAR(100) NOT NULL,
	                                   to_location_id BIGINT NOT NULL,
	                                   to_location_name VARCHAR(100) NOT NULL,
	                                   quantity INT NOT NULL,
	                                   scheduled_date DATE NOT NULL,
	                                   etd TIME NOT NULL,
	                                   eta TIME NOT NULL,
	                                   atd TIME NULL,
	                                   ata TIME NULL,
	                                   final_status ENUM('COMPLETED', 'COMPLETE_DELAYED', 'CANCELLED', 'FAILED', 'EXPIRED') NOT NULL,
	                                   template_id_snapshot INT NULL,
	                                   completed_at DATETIME(6) NULL,
	                                   created_at DATETIME(6) NOT NULL,
	                                   archived_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

	                                   INDEX idx_scheduled_date_desc (scheduled_date DESC),
	                                   INDEX idx_worker_id (worker_id),
	                                   INDEX idx_final_status (final_status),
	                                   INDEX idx_type (type),
	                                   INDEX idx_archived_at (archived_at),

	                                   CONSTRAINT chk_hist_quantity_positive CHECK (quantity > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
