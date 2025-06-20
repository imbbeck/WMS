# 물류 시스템 구조 설계

-   try to make best practice for OOP(DDD), clean arcitecture

---

## 목표

-   입고, 출고, 내부 이동를 통해 물류이동을 관리, 물류이동 계획 생성 시 정합성을 엄격히 검증하여 시스템 전체의 재고 일관성을 보장.
-   재고 등 캐싱을 활용한 성능 개선
-   배치로 매일자정 전체 완료 실행된 작업 결과와 재고 상황 일치 여부 검사
-   별도의 배치 서버에서 작업자별 쓰레드에서 스케줄에 맞춰 메인 서버의 api를 호출하는 방식으로 통합테스트

### 향후 확장 가능 항목

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

-   `id`: String, PK
-   `name`: 이름
-   `type`: [Admin, Worker] (이넘)

-   **비즈니스 규칙**:
    -   Worker는 자기자신의 물류이동계획 대시보드 페이지에만 접근가능

### 장소 (Location)

-   `id`: PK
-   `name`: 장소 이름
-   `type`: [INBOUND, OUTBOUND, WAREHOUSE] (이넘)
-   `capacity`: 정수 (WAREHOUSE인 경우만 유효)

-   **비즈니스 규칙**:
    -   창고의 capacity 초과 불가

### 장소 간 물류이동 시간 (TransferDuration)

-   `id`: PK
-   `from_location_id`: FK
-   `to_location_id`: FK
-   `duration_hour`: 물류이동 시간

### 물품 (Ware)

-   `id`: PK
-   `name`: 문자열
-   `type`: 문자열 (예: 가전제품, 나사 등)
-   `palette_unit` : 파레트 당 물품 개수 (예: 휴지곽은 100개/1파레트) <- 나중에 오더 수량이 물품개수로 들어올 경우 감안.

### 재고 (Stock) - 장소:물품 연결 테이블, (fk,fk)유일성있어야함

-   `id`: PK
-   `location_id`: FK → 장소
-   `ware_id`: FK → 물품
-   `quantity`: 정수

-   **비즈니스 규칙**:
    -   재고는 음수가 될 수 없음
    -   재고 CUD 정책: 물류이동을 통해서만 생성, 수정, 삭제됨
        -   재고변화: 물류이동 상태변화 시 이벤트 발행 → 재고 쪽에서 해당 이벤트 구독 처리
        -   재고상황 캐싱(inventoryStaus:{locationId}:{wareId} quantity, wareHouseStatus:{locationId} sum of quantity)
        -   비동기로 재고변화 처리되야 함.

### 물류이동 템플릿 (LogisticTemplate)

-   `id`: PK
-   `name`: 문자열
-   `type`: [INBOUND, OUTBOUND, INNER] (이넘)
-   `from_location_id`: FK
-   `to_location_id`: FK
-   `status`: [PENDING, STARTED, COMPLETED, DELAYED, CANCELLED, FAILED] (이넘)
-   `ware_id`: FK
-   `standard_quantity`: 표준 수량 (기본값, 실제 실행시 조정 가능)

-   **비즈니스 규칙**:
-   INBOUND : 입고처 -> 창고
-   OUTBOUND : 창고 -> 출고처
-   TRANSFER : 창고 -> 창고

### 물류이동 계획 (LogisticPlan)

-   `id`: PK
-   `worker_id`: FK
-   `scheduled_date`: 예정 실행날짜

### 물류이동 작업 (LogisticTask)

- `id`: PK
- `plan_id`: FK → TransferPlan
- `task_name`: 작업명 (템플릿에서 복사)
- `movement_type`: [INBOUND, OUTBOUND, INNER] (템플릿에서 복사)
- `from_location_id`: FK → 출발지 (템플릿에서 복사)
- `to_location_id`: FK → 도착지 (템플릿에서 복사)
- `ware_id`: FK → 물품 (템플릿에서 복사)
- `etd`: Estimated Time of Departure
- `eta`: Estimated Time of Arrival = etd + location_connection.trt
- `atd`: Actual Time of Departure
- `ata`: Actual Time of Arrival
- `quantity`: 수량
- `status`: [PENDING, INITIATED, COMPLETED, DELAYED, CANCELLED, FAILED] (이넘)
- `template_id_snapshot`: 참조한 템플릿 ID (추적용, FK 아님)

-   **비즈니스 규칙**:
    -   PENDING 상태일 때만 실행 가능
    -   INITIATED 로 상태변경 시 LogisticTaskInitiated 이벤트 발행
        -   이벤트 구독자가 재고 변경을 수행 (비동기 처리)
    -   COMPLETED 로 상태변경 시 LogisticTaskCompleted 이벤트 발행
        -   이벤트 구독자가 재고 변경을 수행 (비동기 처리)
    - `etd` 등은 다 24 * hour + min 으로

### 사용자 (UserInfo)

| 항목명   | 타입     | 제약조건              | 설명                                |
| -------- | -------- | --------------------- | --------------------------------- |
| id       | String   | PK, Not Null          | 사용자 식별자 (예: UUID, 사번 등) |
| name     | String   | Not Null              | 사용자 이름                       |
| role     | Enum     | Not Null              | 사용자 역할 (ADMIN, WORKER)       |
| worker   | Worker   | OneToOne (optional)   | 작업자 정보 (role이 WORKER일 경우 존재) |

### 작업자 (Worker)

| 항목명    | 타입       | 제약조건                   | 설명                          |
| --------- | ---------- | -------------------------- | ----------------------------- |
| id        | String     | PK, FK(UserInfo.id), Not Null | 사용자 ID와 동일 (1:1 매핑)  |
| department| String     | Nullable                   | 작업자 소속 부서명            |
| shift     | String     | Nullable                   | 작업자 근무조 정보            |
| hiredDate | LocalDate  | Nullable                   | 입사 일자                    |
| user      | UserInfo   | OneToOne                   | 소유자 UserInfo 엔티티 참조  |

#### 관계 및 특이사항

- `UserInfo`는 애그리거트 루트로 인증 및 권한 관리를 담당한다.
- `Worker`는 `UserInfo`에 종속된 하위 엔티티이며, `role`이 `WORKER`인 경우에만 생성.
- `Worker.id`는 `UserInfo.id`와 동일하며, `@MapsId`를 통해 매핑.
- `UserInfo` 삭제 시 연관된 `Worker`도 함께 삭제되어야 하며, 생명주기가 동일.
- `Worker` 정보 관리는 `UserInfo` 서비스 내에서 처리.

---

#### 물류이동 계획 생성시 ui/ux는 타임테이블 형태로. x축에 커서올리면 창고별 재고 정보 호버

| 08:10         | 08:30         | 09:00 |
| ------------- | ------------- | ----- |
| movement_aa01 | movement_aa02 |

#### 날짜별 전체 물류이동 계획 조회 ui/ux. x축에 커서올리면 창고별 재고 정보 호버

| 작업자 | 08:00         | 08:30         | 09:00         | ... |
| ------ | ------------- | ------------- | ------------- | --- |
| 김봉남 | movement_aa01 |               | movement_aa02 | ... |
| 박춘봉 |               | movement_ab01 | movement_ab02 | ... |


---

## 물류이동 계획/물류이동 작업 생성/수정/삭제 시 재고/카파 정합성 검증 검사 시뮬레이션 설계

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
    - 시뮬 시작시 inventoryStatus, wareHouseStatus 를 복사, 종료시 캐시 삭제
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

### 실제 운영 시 동시성 이슈 대응 전략

#### 예상 시나리오

여러 작업자가 동시에 같은 재고나 물류이동 건에 대해 `시작 처리` 또는 `완료 처리`를 시도할 경우, DB 업데이트 충돌이 발생할 수 있음.

예:

-   재고 감소 시 낙관적 락 실패
-   동시에 같은 Movement에 완료 처리 요청 발생
-   병렬 처리 환경에서 트랜잭션 충돌 발생

#### 대응 전략: 낙관적 락 + 지수적 백오프 + 사용자 재시도 유도

##### 1. 낙관적 락 (Optimistic Locking)

-   `@Version` 필드로 엔티티 버전 관리
-   트랜잭션 커밋 시 버전 mismatch 발생 시 예외 throw
-   충돌 시 다른 트랜잭션 우선 처리됨

##### 2. 지수적 백오프 재시도

-   충돌 발생 시 자동 재시도 로직 수행
-   재시도 간격을 점진적으로 늘림 (exponential backoff)

예:  
1회 실패 → 100ms  
2회 실패 → 300ms  
3회 실패 → 700ms  
4회 실패 → 1500ms  
5회 실패 → 사용자에게 메시지 전달

> **최대 5회까지만 자동 재시도**

##### 3. 사용자 메시지 예시

> "다른 사용자가 먼저 작업을 완료했습니다. 화면을 새로고침한 후 다시 시도해주세요."

-   시스템이 처리 불가능한 경우, 명시적인 UI 메시지로 사용자 재입력 유도
-   사용자는 수동으로 다시 완료 처리 시도

#### 구현 예시

```java
int retry = 0;
long[] delays = {100, 300, 700, 1500, 3000};

while (retry < 5) {
		try {
		movementService.completeMovement(id);
        return;
		        } catch (OptimisticLockingFailureException e) {
		Thread.sleep(delays[retry]);
retry++;
		}
		}
		throw new ConflictException("다른 사용자가 먼저 작업을 완료했습니다. 다시 시도해주세요.");

```
