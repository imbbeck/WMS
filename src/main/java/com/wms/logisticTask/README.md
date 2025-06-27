# LogisticTask (물류작업) 애그리거트

## 개요
물류 작업의 생성, 수정, 삭제 및 상태 관리를 담당하는 핵심 애그리거트입니다. 
입고, 출고, 내부 이동 작업을 통해 재고 이동을 관리하며, 실시간 정합성 검증과 상태 추적을 제공합니다.

## 주요 기능

### 1. 물류 작업 CRUD
- **생성**: 새로운 물류 작업 계획 수립 (정합성 검증 포함)
- **수정**: 기존 작업의 전체/부분 수정 (PENDING, INITIATE_DELAYED 상태에서만 가능)
- **삭제**: 작업 삭제 (취소 가능한 상태에서만)
- **조회**: 다양한 조건으로 작업 조회

### 2. 실시간 작업 상태 관리
- **PENDING** → **INITIATED** (작업 시작)
- **PENDING** → **INITIATE_DELAYED** (시작 지연)
- **INITIATED** → **COMPLETED** (작업 완료)
- **INITIATED** → **COMPLETE_DELAYED** (완료 지연)
- **INITIATED** → **FAILED** (작업 실패)
- **PENDING/INITIATE_DELAYED** → **CANCELLED** (작업 취소)

### 3. 정합성 검증 시스템
- 물류 작업 생성/수정 시 시뮬레이션 기반 검증
- 재고 부족 및 창고 용량 초과 사전 탐지
- 시간순 작업 의존성 검증

### 4. 대시보드 및 모니터링
- 날짜별 작업자별 스케줄 대시보드
- 작업자 개인 대시보드
- 상태별 통계 조회
- 실시간 작업 진행 상황 추적

## 아키텍처 구조

```
logisticTask/
├── application/           # 응용 서비스 계층
│   ├── LogisticTaskService.java            # 핵심 비즈니스 로직
│   └── LogisticTaskValidationService.java  # 정합성 검증 서비스
├── domain/               # 도메인 계층
│   ├── model/
│   │   ├── LogisticTask.java               # 물류작업 애그리거트 루트
│   │   ├── LogisticTaskStatus.java         # 작업 상태 열거형
│   │   └── SimulationEvent.java            # 시뮬레이션 이벤트
│   ├── repository/
│   │   └── LogisticTaskRepository.java     # 데이터 접근 인터페이스
│   ├── event/
│   │   ├── LogisticTaskInitiatedEvent.java # 작업 시작 이벤트
│   │   └── LogisticTaskCompletedEvent.java # 작업 완료 이벤트
│   └── exception/
│       └── LogisticTaskException.java      # 도메인 예외
├── interfaces/           # 표현 계층
│   └── LogisticTaskController.java         # REST API 컨트롤러
└── dto/                  # 데이터 전송 객체
    └── LogisticTaskDTO.java                # 요청/응답 DTO
```

## API 엔드포인트

### 기본 CRUD
- `POST /logistic-tasks` - 물류 작업 생성
- `PUT /logistic-tasks/{taskId}` - 물류 작업 전체 수정
- `PATCH /logistic-tasks/{taskId}` - 물류 작업 부분 수정
- `DELETE /logistic-tasks/{taskId}` - 물류 작업 삭제
- `GET /logistic-tasks/{taskId}` - 물류 작업 단건 조회
- `GET /logistic-tasks` - 모든 물류 작업 조회

### 작업 상태 변경
- `POST /logistic-tasks/{taskId}/initiate` - 작업 시작
- `POST /logistic-tasks/{taskId}/complete` - 작업 완료
- `POST /logistic-tasks/{taskId}/cancel` - 작업 취소
- `POST /logistic-tasks/{taskId}/fail` - 작업 실패 처리
- `POST /logistic-tasks/{taskId}/delay/initiation` - 시작 지연 처리
- `POST /logistic-tasks/{taskId}/delay/completion` - 완료 지연 처리

### 조회 및 필터링
- `GET /logistic-tasks/status/{status}` - 상태별 조회
- `GET /logistic-tasks/worker/{workerId}` - 작업자별 조회
- `GET /logistic-tasks/date/{date}` - 날짜별 조회
- `GET /logistic-tasks/date-range` - 날짜 범위별 조회
- `GET /logistic-tasks/location/{locationId}` - 장소별 조회
- `GET /logistic-tasks/ware/{wareId}` - 물품별 조회
- `GET /logistic-tasks/search` - 복합 조건 검색

### 대시보드 및 통계
- `GET /logistic-tasks/dashboard/daily` - 일별 대시보드
- `GET /logistic-tasks/dashboard/worker/{workerId}` - 작업자 대시보드
- `GET /logistic-tasks/statistics` - 전체 통계
- `GET /logistic-tasks/statistics/daily` - 일별 통계

## 권한 관리

### ADMIN 권한
- 모든 물류 작업 CRUD 가능
- 모든 작업자의 작업 상태 변경 가능
- 전체 대시보드 및 통계 조회 가능

### WORKER 권한
- 자신에게 배정된 작업의 상태 변경만 가능
- 자신의 작업 조회만 가능
- 자신의 개인 대시보드만 조회 가능

## 비즈니스 규칙

### 1. 상태 전환 규칙
- **수정 가능**: PENDING, INITIATE_DELAYED 상태에서만
- **취소 가능**: PENDING, INITIATE_DELAYED 상태에서만
- **시작 가능**: PENDING, INITIATE_DELAYED 상태에서만
- **완료 가능**: INITIATED, COMPLETE_DELAYED 상태에서만
- **실패 처리**: INITIATED 상태에서만

### 2. 정합성 검증
- 출발 예정시간 < 도착 예정시간
- 출발지 ≠ 도착지
- 수량 > 0
- 예정시간 > 현재시간
- 재고 부족 방지 (시뮬레이션)
- 창고 용량 초과 방지 (시뮬레이션)

### 3. 재고 연동
- **작업 시작 시**: 출발지가 창고인 경우 재고 감소
- **작업 완료 시**: 도착지가 창고인 경우 재고 증가
- **이벤트 기반**: 비동기 재고 변경 처리
- **실패 시**: 자동 롤백 (향후 구현)

## 핵심 특징

### 1. 시뮬레이션 기반 검증
```java
// 물류 작업 생성/수정 시 자동 실행
validationService.validateTaskCreation(newTask);
validationService.validateTaskModification(existingTask, modifiedTask);
```

### 2. 이벤트 드리븐 아키텍처
```java
// 작업 상태 변경 시 이벤트 발행
eventPublisher.publishEvent(LogisticTaskInitiatedEvent.builder()
    .taskId(task.getId())
    .wareId(task.getWare().getId())
    .fromLocationId(task.getFromLocation().getId())
    .quantity(task.getQuantity())
    .build());
```

### 3. 실시간 대시보드
- 30분 단위 시간대별 작업 스케줄 제공
- 작업자별 실시간 진행 상황 추적
- 상태별 통계 실시간 업데이트

## 확장 계획

### 1. 향후 기능 추가
- [ ] 장소간 이동소요시간을 이용한 validation 
  - 물류 작업 생성/수정 시 새로 생성/수정될 작업의 예정 시작시간 >= 이전 배정된 작업의 예정 완료시간 + 장소간 이동 소요시간 밸리데이션.
- [ ] 한 물류이동 당 여러 물품 지원
- [ ] 최적 경로 추천 시스템
- [ ] 작업 지연 시 자동 알림
- [ ] 작업 우선순위 관리
- [ ] 배치 작업 생성 기능

### 2. 성능 개선
- [ ] 대용량 작업 처리 최적화
- [ ] 캐시 활용 강화
- [ ] 데이터베이스 인덱스 튜닝
- [ ] 비동기 처리 확대

### 3. 모니터링 강화
- [ ] 메트릭스 수집
- [ ] 장애 알림 시스템
- [ ] 성능 대시보드
- [ ] 감사 로그 시스템

## 테스트 전략

### 1. 단위 테스트
- 도메인 로직 검증
- 상태 전환 규칙 테스트
- 비즈니스 규칙 검증

### 2. 통합 테스트
- API 엔드포인트 테스트
- 데이터베이스 연동 테스트
- 이벤트 발행/구독 테스트

### 3. 시나리오 테스트
- 전체 물류 플로우 테스트
- 동시성 처리 테스트
- 에러 상황 대응 테스트

이 애그리거트는 WMS 시스템의 핵심이며, 안정적이고 확장 가능한 물류 작업 관리를 제공합니다.
