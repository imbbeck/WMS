-- Location (장소)
INSERT INTO location (name, type, capacity, remark) VALUES ('입고장', 'INBOUND', 0, '외부 상품이 입고되는 장소');
INSERT INTO location (name, type, capacity, remark) VALUES ('출고장', 'OUTBOUND', 0, '외부로 상품이 출고되는 장소');
INSERT INTO location (name, type, capacity, remark) VALUES ('A-야적장', 'YARD', 1000, 'A 구역 야외 야적장');
INSERT INTO location (name, type, capacity, remark) VALUES ('B-야적장', 'YARD', 1000, 'B 구역 야외 야적장');
INSERT INTO location (name, type, capacity, remark) VALUES ('1번-창고', 'WAREHOUSE', 500, '1번 실내 창고');
INSERT INTO location (name, type, capacity, remark) VALUES ('2번-창고', 'WAREHOUSE', 800, '2번 실내 창고');

-- Ware (물품)
INSERT INTO ware (name, type, palette_unit) VALUES ('TV-100', '가전', 10);
INSERT INTO ware (name, type, palette_unit) VALUES ('냉장고-S', '가전', 5);
INSERT INTO ware (name, type, palette_unit) VALUES ('노트북-G', 'IT', 50);
INSERT INTO ware (name, type, palette_unit) VALUES ('생수-2L', '식료품', 200);

-- Inventory (재고) - 초기 재고 설정
INSERT INTO inventory (location_id, ware_id, quantity) VALUES (3, 1, 100); -- A-야적장, TV-100, 100개
INSERT INTO inventory (location_id, ware_id, quantity) VALUES (3, 4, 500); -- A-야적장, 생수-2L, 500개
INSERT INTO inventory (location_id, ware_id, quantity) VALUES (5, 3, 200); -- 1번-창고, 노트북-G, 200개
INSERT INTO inventory (location_id, ware_id, quantity) VALUES (6, 2, 50);  -- 2번-창고, 냉장고-S, 50개

-- MoveOrder (이동오더)
-- PENDING 상태의 오더 (Redis 예측 재고 계산에 사용될 데이터)
INSERT INTO move_order (name, type, from_location_id, to_location_id, scheduled_date, status, ware_id, quantity)
VALUES ('입고-240610-001', 'INBOUND', 1, 3, '2025-06-10', 'PENDING', 3, 50); -- 입고장 -> A-야적장, 노트북-G, 50개
INSERT INTO move_order (name, type, from_location_id, to_location_id, scheduled_date, status, ware_id, quantity)
VALUES ('내부이동-240610-001', 'TRANSFER', 3, 6, '2025-06-10', 'PENDING', 1, 20); -- A-야적장 -> 2번-창고, TV-100, 20개

-- 완료/취소된 과거 오더
INSERT INTO move_order (name, type, from_location_id, to_location_id, scheduled_date, status, ware_id, quantity)
VALUES ('출고-240609-001', 'OUTBOUND', 3, 2, '2025-06-09', 'COMPLETED', 4, 100); -- A-야적장 -> 출고장, 생수-2L, 100개
INSERT INTO move_order (name, type, from_location_id, to_location_id, scheduled_date, status, ware_id, quantity)
VALUES ('내부이동-240609-002', 'TRANSFER', 5, 6, '2025-06-09', 'CANCELLED', 3, 30); -- 1번-창고 -> 2번-창고, 노트북-G, 30개

-- LocationTransferTime (장소 간 이동 시간)
INSERT INTO location_transfer_time (from_location_id, to_location_id, duration_min) VALUES (1, 3, 10); -- 입고장 -> A-야적장
INSERT INTO location_transfer_time (from_location_id, to_location_id, duration_min) VALUES (3, 5, 15); -- A-야적장 -> 1번-창고
INSERT INTO location_transfer_time (from_location_id, to_location_id, duration_min) VALUES (3, 4, 5);  -- A-야적장 -> B-야적장
INSERT INTO location_transfer_time (from_location_id, to_location_id, duration_min) VALUES (5, 6, 3);  -- 1번-창고 -> 2번-창고
INSERT INTO location_transfer_time (from_location_id, to_location_id, duration_min) VALUES (3, 2, 10); -- A-야적장 -> 출고장 