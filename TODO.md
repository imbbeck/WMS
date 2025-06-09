# 내부 물류 시스템 구조 설계
- try to make best practice for OOP, DDD

## 목표
- 입고, 출고, 내부이동 오더를 통해 물류 이동을 관리, 각 오더 생성 시 정합성을 엄격히 검증하여 시스템 전체의 재고 일관성을 보장.
- more: 최선의 물류이동 루트를 알고리즘으로 제시 
---

## 제한 조건
- 물품 수량은 파레트 단위로
- 카파도 물품 종류에 상관없이 파레트 단위

## 주요 테이블 및 도메인

### 1. 장소 (Location)
- `id`: PK
- `name`: 장소 이름
- `type`: [INBOUND, OUTBOUND, YARD, WAREHOUSE] (이넘)
- `capacity`: 정수 (YARD, WAREHOUSE인 경우만 유효)
- `remark`: 비고

### 2. 재고 (Inventory) - 장소:물품 연결 테이블, (fk,fk)유일성있어야함 
- `id`: PK
- `location_id`: FK → 장소
- `ware_id`: FK → 물품
- `quantity`: 정수

### 3. 물품 (Ware)
- `id`: PK
- `name`: 문자열
- `type`: 문자열 (예: 가전제품, 나사 등)
- `palette_unit` : 파레트 당 물품 개수 (예: 휴지곽은 100개/1파레트) <- 나중에 오더 수량이 물품개수로 들어올 경우 감안.

### 4. 이동오더 (MoveOrder)
- `id`: PK
- `name`: 문자열
- `type`: [INBOUND, OUTBOUND, TRANSFER] (이넘)
- `from_location_id`: FK
- `to_location_id`: FK
- `scheduled_date`: 실행 예정일
- `status`: [PENDING, COMPLETED, CANCELLED, FAILED] (이넘)
- `ware_id`: FK
- `quantity`: 정수

#### 오더 타입별 유효 경로 제약

- INBOUND  : 입고처 -> 야적장 
- OUTBOUND : 야적장 -> 출고처
- TRANSFER : 야적장/창고 -> 야적장/창고

### 5. 장소 간 이동 시간 (LocationTransferTime)
- `id`: PK
- `from_location_id`: FK
- `to_location_id`: FK
- `duration_min`: 이동 시간 (분)

---
## 추가 고려 테이블

### 작업자 (Worker)
- `id`: PK
- `name`: 문자열
- `...` (기타 속성)

### 실행 계획 (ExecutionPlan)
- `id`: PK
- `worker_id`: FK
- `order_id`: FK
- `date`: 날짜
- `sequence`: 실행 순서

---

## 재고 정합성 검증

### 오더 생성 시 정합성 원칙
- **예측 재고**: 최근 입력된 오더의 아직 수행되지 않은 이동 결과를 캐싱하여 다음 오더 입력시 정합성 판단에 사용
- **출발지의 예측 재고 ≥ 오더 수량**
- **도착지의 예측 재고 + 오더 수량 ≤ 장소 용량** (YARD, WAREHOUSE만 해당)
- 모든 검증은 **“예측 재고”** 기준으로 판단

---

## Redis 기반 예측 재고 관리

### Redis와 DB 역할 분리
- 목적 : 빠른 정합성 판단 | 진짜 재고 관리
- 갱신 시점 : 오더 생성/취소 | 오더 실행 완료

### Redis Key 구조
- virtual_inventory(가칭):{locationId}:{wareId} = 수량 (정수)

### 업데이트 흐름

- 오더 생성 성공 시:
  - 출발지: `decrBy` 수량
  - 도착지: `incrBy` 수량
- 오더 취소 시:
  - 출발지: `incrBy` 수량
  - 도착지: `decrBy` 수량

## Redis 초기화 전략

- 시스템 기동 시:
  1. Inventory 테이블 조회 → Redis 초기화
  2. PENDING 오더 조회 → 각 장소/물품별 수량 증감 반영
- 이후 오더 생성/취소 시 Redis 실시간 반영

## 기타 고려사항

- Redis와 DB 간 **동기화 보장**: 오더 생성 트랜잭션과 Redis 갱신은 반드시 함께 처리
- **TTL 적용하지 않음**: 예측 재고는 수동으로만 갱신되며 만료되면 안 됨
- 장애 시 **Fallback 로직 필요**: Redis 장애 시 DB 기반 재계산 처리 필요

---

##  향후 확장 가능 항목

- 오더 우선순위, 작업자 배정 알고리즘
- 병렬 실행 계획에 따른 deadlock 방지
- 이벤트 기반 재고 동기화 (Kafka, Redis Stream 등)
- 배치로 매일자정 전체 완료 이동오더와 재고 상황 일치 여부 검사


