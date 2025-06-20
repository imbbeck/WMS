-- Base data insert script for WMS system

-- Insert Locations
-- Inbound locations (입고처)
INSERT INTO location (name, type, capacity, coordinate_x, coordinate_y, created_at, updated_at) VALUES
('입고처A', 'INBOUND', NULL, 100, 100, NOW(), NOW()),
('입고처B', 'INBOUND', NULL, 100, 200, NOW(), NOW());

-- Outbound locations (출고처)
INSERT INTO location (name, type, capacity, coordinate_x, coordinate_y, created_at, updated_at) VALUES
('출고처A', 'OUTBOUND', NULL, 500, 100, NOW(), NOW()),
('출고처B', 'OUTBOUND', NULL, 500, 200, NOW(), NOW());

-- Warehouse locations (창고)
INSERT INTO location (name, type, capacity, coordinate_x, coordinate_y, created_at, updated_at) VALUES
('창고A', 'WAREHOUSE', 1000, 200, 150, NOW(), NOW()),
('창고B', 'WAREHOUSE', 800, 300, 150, NOW(), NOW()),
('창고C', 'WAREHOUSE', 1200, 400, 150, NOW(), NOW());

-- Insert Wares (물품)
INSERT INTO ware (name, type, palette_unit, created_at, updated_at) VALUES
('노트북', '전자제품', 20, NOW(), NOW()),
('모니터', '전자제품', 10, NOW(), NOW()),
('키보드', '전자제품', 50, NOW(), NOW()),
('마우스', '전자제품', 100, NOW(), NOW()),
('책상', '가구', 5, NOW(), NOW()),
('의자', '가구', 8, NOW(), NOW()),
('볼펜', '사무용품', 500, NOW(), NOW()),
('A4용지', '사무용품', 200, NOW(), NOW());

-- Insert Location Connections (위치 간 연결)
-- 입고장 -> 창고 연결
INSERT INTO location_connection (location_a_id, location_b_id, trt, created_at, updated_at) VALUES
(1, 5, 10, NOW(), NOW()), -- 입고장A -> 창고A (10분)
(1, 6, 15, NOW(), NOW()), -- 입고장A -> 창고B (15분)
(2, 6, 8, NOW(), NOW()),  -- 입고장B -> 창고B (8분)
(2, 7, 12, NOW(), NOW()); -- 입고장B -> 창고C (12분)

-- 창고 간 연결
INSERT INTO location_connection (location_a_id, location_b_id, trt, created_at, updated_at) VALUES
(5, 6, 5, NOW(), NOW()),  -- 창고A -> 창고B (5분)
(5, 7, 8, NOW(), NOW()),  -- 창고A -> 창고C (8분)
(6, 7, 6, NOW(), NOW());  -- 창고B -> 창고C (6분)

-- 창고 -> 출고장 연결
INSERT INTO location_connection (location_a_id, location_b_id, trt, created_at, updated_at) VALUES
(3, 5, 12, NOW(), NOW()), -- 출고장A -> 창고A (12분)
(3, 6, 10, NOW(), NOW()), -- 출고장A -> 창고B (10분)
(4, 6, 7, NOW(), NOW()),  -- 출고장B -> 창고B (7분)
(4, 7, 9, NOW(), NOW());  -- 출고장B -> 창고C (9분)

-- Insert Initial Stock Data (초기 재고)
INSERT INTO stocks (ware_id, location_id, quantity, version, created_at, updated_at) VALUES
-- 창고A 재고
(1, 5, 50, 0, NOW(), NOW()),  -- 노트북 50개
(2, 5, 30, 0, NOW(), NOW()),  -- 모니터 30개
(3, 5, 100, 0, NOW(), NOW()), -- 키보드 100개
(4, 5, 200, 0, NOW(), NOW()), -- 마우스 200개
(5, 5, 25, 0, NOW(), NOW()),  -- 책상 25개
(6, 5, 40, 0, NOW(), NOW()),  -- 의자 40개
-- 창고B 재고
(1, 6, 50, 0, NOW(), NOW()),  -- 노트북 50개
(2, 6, 30, 0, NOW(), NOW()),  -- 모니터 30개
(3, 6, 100, 0, NOW(), NOW()), -- 키보드 100개
(4, 6, 200, 0, NOW(), NOW()), -- 마우스 200개
(5, 6, 25, 0, NOW(), NOW()),  -- 책상 25개
(6, 6, 40, 0, NOW(), NOW()),  -- 의자 40개

-- 창고C 재고
(1, 7, 50, 0, NOW(), NOW()),  -- 노트북 50개
(2, 7, 30, 0, NOW(), NOW()),  -- 모니터 30개
(3, 7, 100, 0, NOW(), NOW()), -- 키보드 100개
(4, 7, 200, 0, NOW(), NOW()), -- 마우스 200개
(5, 7, 25, 0, NOW(), NOW()),  -- 책상 25개
(6, 7, 40, 0, NOW(), NOW());  -- 의자 40개