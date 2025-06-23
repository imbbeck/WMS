# 물류 시스템 구조 설계

-   try to make best practice for OOP(DDD), clean architecture

---

## 목표

-   입고, 출고, 내부 이동을 통해 물류이동을 관리, 물류이동 계획 생성 시 정합성을 엄격히 검증하여 시스템 전체의 재고 일관성을 보장.
-   재고 등 캐싱을 활용한 성능 개선
-   배치로 매일자정 전체 완료 실행된 작업 결과와 재고 상황 일치 여부 검사
-   별도의 배치 서버에서 작업자별 쓰레드에서 스케줄에 맞춰 메인 서버의 api를 호출하는 방식으로 통합테스트

### 향후 확장 가능 항목

-   한 물류이동 당 한 물품 -> 한 물류이동 당 여러 물품
-   물류이동 장애(CANCEL, FAILED, DELAYED 상황) 발생시 대처 시나리오.
-   로깅 및 장애 알림 mq 적용
-   최선의 물류이동 루트를 제시하는 시스템

---

### 필요 페이지

-   로그인 페이지
-   유저관리 페이지
-   물류이동계획 crud 페이지
-   물류이동 crud 페이지
-   전체 물류이동 상황 대시보드
-   작업자별 물류이동계획 대시보드 - 배정받은 물류이동계획 조회 및 물류이동 시작/완료 처리 페이지

---

## 제한 조건

-   물품 수량은 파레트 단위로
-   카파도 물품 종류에 상관없이 파레트 단위

---

## 주요 테이블 및 도메인

### 사용자 (UserInfo)

-   `id`: Long, PK (자동생성)
-   `username`: 로그인 ID (String, 유니크, 최대 20자, 소문자/숫자/언더스코어만)
-   `name`: 이름 (String)
-   `email`: 이메일 (String, 유니크)
-   `password`: 암호 - Password Value Object로 분리
-   `type`: [ADMIN, WORKER] (이넘)
-   `createdAt`: 생성일시 (LocalDateTime, 자동생성)
-   `updatedAt`: 수정일시 (LocalDateTime, 자동갱신)

#### Password Value Object
-   `value`: 암호화된 패스워드 (BCrypt 사용)

-   **비즈니스 규칙**:
    -   Worker는 자신의 물류이동계획 대시보드 페이지에만 접근가능
    -   username은 소문자, 숫자, 언더스코어만 허용
    -   패스워드는 BCrypt로 암호화하여 저장

### 장소 (Location)

-   `id`: Long, PK (자동생성)
-   `name`: 장소 이름 (String, 유니크)
-   `type`: [INBOUND, OUTBOUND, WAREHOUSE] (이넘)
-   `capacity`: 정수 (WAREHOUSE인 경우만 필수, 다른 타입은 null)
-   `coordinateX`: 장소 페이지 내 x좌표 (Integer)
-   `coordinateY`: 장소 페이지 내 y좌표 (Integer)
-   `createdAt`: 생성일시 (LocalDateTime, 자동생성)
-   `updatedAt`: 수정일시 (LocalDateTime, 자동갱신)

-   **비즈니스 규칙**:
    -   창고의 capacity 초과 불가
    -   WAREHOUSE 타입은 capacity가 0보다 커야 함
    -   INBOUND/OUTBOUND 타입은 capacity를 가질 수 없음

### 장소 간 연결 (LocationConnection)

-   `id`: Long, PK (자동생성)
-   `locationAId`: 항상 더 작은 ID (Long)
-   `locationBId`: 항상 더 큰 ID (Long)
-   `trt`: total required time - 이동 소요시간 (Integer)
-   `createdAt`: 생성일시 (LocalDateTime, 자동생성)
-   `updatedAt`: 수정일시 (LocalDateTime, 자동갱신)
-   **유니크 제약조건**: (`locationAId`, `locationBId`)

-   **비즈니스 규칙**:
    -   무방향 그래프 구조 (A-B 연결 시 B-A는 중복 생성되지 않음)
    -   자기 자신과의 연결 불가
    -   소요시간은 0보다 커야 함

### 물품 (Ware)

-   `id`: Long, PK (자동생성)
-   `name`: 문자열 (String, 유니크)
-   `type`: 문자열 (예: 가전제품, 나사 등)
-   `paletteUnit`: 파레트 당 물품 개수 (Integer, 0보다 커야 함)
-   `createdAt`: 생성일시 (LocalDateTime, 자동생성)
-   `updatedAt`: 수정일시 (LocalDateTime, 자동갱신)

-   **비즈니스 규칙**:
    -   파레트 당 물품 개수는 0보다 커야 함

### 재고 (Stock) - 장소:물품 연결 테이블

-   `id`: Long, PK (자동생성)
-   `wareId`: FK → 물품
-   `warehouseId`: FK → 장소 (창고)
-   `quantity`: 정수 (파레트 단위)
-   `version`: Long (낙관적 락용)
-   `createdAt`: 생성일시 (LocalDateTime, 자동생성)
-   `updatedAt`: 수정일시 (LocalDateTime, 자동갱신)
-   **유니크 제약조건**: (`wareId`, `warehouseId`)

-   **비즈니스 규칙**:
    -   재고는 음수가 될 수 없음
    -   재고 CUD 정책: 물류이동을 통해서만 생성, 수정, 삭제됨
        -   재고변화: 물류이동 상태변화 시 이벤트 발행 → 재고 쪽에서 해당 이벤트 구독 처리
        -   재고상황 캐싱(inventoryStatus:{locationId}:{wareId} quantity, wareHouseStatus:{locationId} sum of quantity)
        -   비동기로 재고변화 처리
    -   창고 용량 초과 검증
    -   재고 부족 검증

### 재고 일일 스냅샷 (StockDailySnapshot)

-   `id`: Long, PK (자동생성)
-   **Embedded Key (StockSnapshotKey)**:
    -   `wareId`: FK → 물품
    -   `warehouseId`: FK → 장소 (창고)
    -   `snapshotDate`: 스냅샷 날짜 (LocalDate)
-   `quantity`: 해당 날짜의 재고 수량 (Integer)
-   `changeFromYesterday`: 전일 대비 변화량 (Integer)
-   **유니크 제약조건**: (`wareId`, `warehouseId`, `snapshotDate`)

-   **비즈니스 규칙**:
    -   매일 자정 배치 프로세스로 생성
    -   전일 대비 증감량 자동 계산

### 물류이동 템플릿 (LogisticTemplate)

-   `id`: Long, PK (자동생성)
-   `name`: 문자열 (String, 최대 255자)
-   `type`: [INBOUND, OUTBOUND, INNER] (이넘)
-   `wareId`: FK → 물품
-   `fromLocationId`: FK → 출발 장소
-   `toLocationId`: FK → 도착 장소
-   `standardQuantity`: 표준 수량 (Integer, 0보다 커야 함)
-   `createdAt`: 생성일시 (LocalDateTime, 자동생성)
-   `updatedAt`: 수정일시 (LocalDateTime, 자동갱신)

-   **비즈니스 규칙**:
    -   INBOUND: 입고처 → 창고
    -   OUTBOUND: 창고 → 출고처
    -   INNER: 창고 → 창고

### 물류이동 작업 (LogisticTask)

-   `id`: Long, PK (자동생성)
-   `name`: 작업명 (String, 최대 255자)
-   `type`: [INBOUND, OUTBOUND, INNER] (템플릿에서 복사)
-   `workerId`: FK → 작업자
-   `wareId`: FK → 물품 (템플릿에서 복사)
-   `fromLocationId`: FK → 출발지 (템플릿에서 복사)
-   `toLocationId`: FK → 도착지 (템플릿에서 복사)
-   `quantity`: 수량 (Integer)
-   `scheduledDate`: 예정 날짜 (LocalDate)
-   `etd`: Estimated Time of Departure (LocalTime)
-   `eta`: Estimated Time of Arrival (LocalTime)
-   `atd`: Actual Time of Departure (LocalTime, nullable)
-   `ata`: Actual Time of Arrival (LocalTime, nullable)
-   `status`: [PENDING, INITIATED, INITIATE_DELAYED, COMPLETED, COMPLETE_DELAYED, CANCELLED, FAILED] (이넘)
-   `templateIdSnapshot`: 참조한 템플릿 ID (Integer, 추적용, FK 아님)
-   `createdAt`: 생성일시 (LocalDateTime, 자동생성)
-   `updatedAt`: 수정일시 (LocalDateTime, 자동갱신)

-   **비즈니스 규칙**:
    -   PENDING/INITIATE_DELAYED 상태일 때만 수정/취소 가능
    -   INITIATED 상태일 때만 실패 처리 가능
    -   INITIATED로 상태변경 시 LogisticTaskInitiated 이벤트 발행
        -   이벤트 구독자가 재고 변경을 수행 (비동기 처리)
    -   COMPLETED로 상태변경 시 LogisticTaskCompleted 이벤트 발행
        -   이벤트 구독자가 재고 변경을 수행 (비동기 처리)

#### 상태 전환 규칙:
- `PENDING` → `INITIATED` (작업 시작)
- `PENDING` → `INITIATE_DELAYED` (시작 지연)
- `PENDING` → `CANCELLED` (취소)
- `INITIATED` → `COMPLETED` (완료)
- `INITIATED` → `COMPLETE_DELAYED` (완료 지연)
- `INITIATED` → `FAILED` (실패)
- `INITIATE_DELAYED` → `PENDING` (수정을 통한 복원)
- `INITIATE_DELAYED` → `CANCELLED` (취소)

---

#### 날짜별 작업자별 전체 물류이동 작업 조회 UI/UX

x축에 커서 올리면 창고별 재고 정보 호버

| 작업자 | 08:00         | 08:30         | 09:00         | ... |
| ------ | ------------- | ------------- | ------------- | --- |
| 김봉남 | movement_aa01 |               | movement_aa02 | ... |
| 박춘봉 |               | movement_ab01 | movement_ab02 | ... |

---

## 물류이동 계획 무결성 검사 - 물류이동 계획 생성/수정/삭제 시 재고/카파 정합성 검증

### 목적

물류이동 작업 생성/수정/삭제 시도 시마다 지금까지 수립된 물류이동 계획 + 대상 물류이동 작업으로 시뮬레이션을 수행하여 재고 변경에 문제 없는지 검사하여 실행 가능 여부를 사전 탐지

### 1. 구성요소

#### 1.1 시뮬레이션 입력

-   **작업계획**: 이동 그룹 + 이동 작업들
-   **작업자 작업 시작 시간**
-   **장소 간 이동시간 그래프**
-   **기초 재고 상태 (기준 시간 기준 캐싱)**

### 2. 주요 처리 절차

#### 2.1 시뮬레이션 트리거

-   UI에서 물류이동 작업 생성/수정/삭제 시도
-   백엔드 큐(메모리 내 우선순위 큐 등)에 요청 추가
-   배치 프로세스 또는 스레드가 순차 실행

#### 2.2 시뮬레이션 흐름

1. **작업별 예상 시작/완료시간 계산**
    - 이동시간 + 버퍼 고려
2. **시간 순으로 재고 변화 예상**
    - 시간 흐름에 따라 작업 실행 시뮬레이션
3. **예상 시점별 재고/카파 상태 캐싱**
    - sim_inventoryStatus:{locationId}:{wareId} quantity, sim_wareHouseStatus:{locationId} sum of quantity
    - 시뮬 시작시 inventoryStatus, wareHouseStatus를 복사, 종료시 캐시 삭제
4. **각 작업 실행 가능 여부 확인**
    - 선행 작업 완료 여부
    - 출발지 재고 부족 여부
    - 도착지 창고 카파 초과 여부

### 3. 검증 실패 처리

#### 3.1 실패 단계

-   작업 등록 → 시뮬 실행 전 단계
-   시뮬 중간 작업 처리 중

#### 3.2 실패 사유

-   재고 부족
-   창고 카파 초과
-   선행 작업 미완료
-   예상 시간 계산 오류

### 4. 구현 전략

#### 4.1 큐 기반 처리

-   우선순위 큐 형태로 요청 정렬
-   단일 스레드 또는 제한된 풀에서 순차 실행

#### 4.2 병렬성 고려

-   시뮬 내에서는 병렬성 없음 (시간 압축 시뮬)
-   실제 작업에서는 비관적 락 등 고려

#### 4.3 결과 전달

-   클라이언트에는 시뮬 완료 후 실패 정보 리스트 제공
-   실시간 스트리밍은 실제 수행 단계에서 적용

---

## 실제 운영 시 동시성 이슈 대응 전략

### 예상 시나리오

여러 작업자가 동시에 같은 재고나 물류이동 건에 대해 `시작 처리` 또는 `완료 처리`를 시도할 경우, DB 업데이트 충돌이 발생할 수 있음.

예:

-   재고 감소 시 낙관적 락 실패
-   동시에 같은 Movement에 완료 처리 요청 발생
-   병렬 처리 환경에서 트랜잭션 충돌 발생

### 대응 전략: 낙관적 락 + 지수적 백오프 + 사용자 재시도 유도

#### 1. 낙관적 락 (Optimistic Locking)

-   `@Version` 필드로 엔티티 버전 관리 (Stock 엔티티에 구현됨)
-   트랜잭션 커밋 시 버전 mismatch 발생 시 예외 throw
-   충돌 시 다른 트랜잭션 우선 처리됨

#### 2. 지수적 백오프 재시도

-   충돌 발생 시 자동 재시도 로직 수행
-   재시도 간격을 점진적으로 늘림 (exponential backoff)

예:  
1회 실패 → 100ms  
2회 실패 → 300ms  
3회 실패 → 700ms  
4회 실패 → 1500ms  
5회 실패 → 사용자에게 메시지 전달

> **최대 5회까지만 자동 재시도**

#### 3. 사용자 메시지 예시

> "다른 사용자가 먼저 작업을 완료했습니다. 화면을 새로고침한 후 다시 시도해주세요."

-   시스템이 처리 불가능한 경우, 명시적인 UI 메시지로 사용자 재입력 유도
-   사용자는 수동으로 다시 완료 처리 시도

#### 구현 예시

```java
int retry = 0;
long[] delays = {100, 300, 700, 1500, 3000};

while (retry < 5) {
    try {
        logisticTaskService.completeTask(id);
        return;
    } catch (OptimisticLockingFailureException e) {
        Thread.sleep(delays[retry]);
        retry++;
    }
}
throw new ConflictException("다른 사용자가 먼저 작업을 완료했습니다. 다시 시도해주세요.");
```

---

## StockDailySnapshot 매일 자정 결산 배치 프로세스

매일 자정에 실행되는 Spring Batch 기반 스냅샷 생성 프로세스

### 배치 구성요소

- **StockSnapshotReader**: Stock 엔티티를 ID 범위 기반으로 페이징 조회
- **StockSnapshotProcessor**: 어제 대비 수량 증감 계산 후 StockDailySnapshot 생성
- **StockSnapshotWriter**: 스냅샷 엔티티 DB에 저장
- **DynamicStockPartitioner**: id 기반 파티셔닝 (병렬 처리 지원)
- **StockSnapshotBatchConfig**: 전체 배치 작업(Job, Step, 병렬 처리 등) 설정
- **StockSnapshotScheduler**: 매일 자정 등 특정 시간에 배치 잡 실행

### 처리 흐름

1. **매일 자정 스케줄러 실행**
2. **현재 Stock 데이터 조회** (페이징 처리)
3. **전일 스냅샷과 비교하여 증감량 계산**
4. **StockDailySnapshot 엔티티 생성 및 저장**
5. **배치 실행 결과 로깅**

---

## 구현된 주요 비즈니스 로직

### Location 엔티티
- 창고 타입별 capacity 검증 (WAREHOUSE만 필수)
- 좌표 기반 UI 배치 지원

### LocationConnection 엔티티
- 무방향 그래프 구조로 중복 연결 방지
- 두 장소 간 연결 여부 및 상대방 ID 조회 메서드 제공

### LogisticTask 엔티티
- 상태별 수정/취소/실패 가능 여부 검증
- ETD/ETA 기반 일정 관리
- 템플릿 ID 스냅샷으로 추적성 확보

### Stock 엔티티
- 창고 용량 초과 검증
- 재고 부족 검증
- 낙관적 락을 통한 동시성 제어

### UserInfo 엔티티
- BCrypt 기반 패스워드 암호화
- 사용자명 패턴 검증 (소문자/숫자/언더스코어만)
- 역할별 권한 검증 메서드 제공

## 레디스 캐싱 전략
- Invalidation: write-througt and event-driven 캐싱
- ttl: Expiration 기능을 활용한 캐시 무효화