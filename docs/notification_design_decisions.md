# 알림 시스템 설계 결정사항

## 1. 전체 아키텍처 개요

### 목적
- 재고 동기화 전용 SSE를 범용 알림 시스템으로 확장
- 실시간 알림 전송 + 추후 DB 저장 기능 지원

### 전체 구조
```
notification/
├── domain/
│   ├── Notification (애그리거트 루트) (추후 구현)
│   ├── NotificationStatus (전략 패턴 인터페이스)
│   ├── StockSyncStatus, SystemNotificationStatus, TaskNotificationStatus (구현체)
│   └── NotificationRepository (도메인 인터페이스)
├── application/
│   └── NotificationSseService (SSE 전송 응용 서비스)
└── infrastructure/
    └── NotificationJpaRepository (추후 구현)
```

## 2. 핵심 설계 결정

### 2.1 애그리거트 분리 결정
**결정**: 알림 도메인을 별도 애그리거트로 분리

**근거**:
- 알림 타입별 다른 상태 관리 필요
- 전체공지/개인알림 구분 로직
- 추후 읽음상태, 보관기간 등 복잡한 도메인 규칙 예상
- SSE 전송 로직과 도메인 로직 분리 필요

### 2.2 Status 설계 - 전략 패턴 적용
**결정**: `NotificationStatus` 인터페이스를 각 알림 타입별 enum이 구현

**구조**:
```java
public interface NotificationStatus {
    String getDisplayName();
    boolean isTerminal();
    NotificationType getNotificationType();
}

// 구현체들
StockSyncStatus implements NotificationStatus
SystemNotificationStatus implements NotificationStatus  
TaskNotificationStatus implements NotificationStatus
```

**장점**:
- 타입별 고유 상태값 사용 가능
- 상태별 메타데이터 자동 관리
- 새로운 알림 타입 추가 시 기존 코드 수정 불필요
- 다형성을 통한 일관된 처리

### 2.3 전체 공지 저장 방식
**결정**: `user_id = -1L`을 전체 공지 식별자로 사용

**테이블 구조**:
```sql
notifications
├── id
├── user_id (-1L = 전체공지, 실제값 = 개인알림)
├── type
├── title
├── message
└── created_at
```

**장점**:
- 테이블 구조 단순
- 쿼리 로직 간단
- 개발/유지보수 용이

**제한사항**:
- 전체 공지의 읽음 상태는 1단계에서 관리하지 않음

### 2.4 읽음 상태 관리 - 단계별 접근
**1단계** (현재): 읽음 관리 없음
- 실시간 SSE 전송에 집중
- 단순한 알림 로깅만 수행

**2단계** (추후): 읽음 상태 테이블 분리
```sql
notification_read_status
├── user_id
├── notification_id  
└── read_at
```

### 2.5 저장 전략 - 실시간 우선
**결정**: 실시간 SSE 전송 → 비동기 DB 저장

**근거**:
- 사용자 경험 최우선 (실시간성)
- DB 저장 실패가 실시간 알림에 영향 없음
- 성능 최적화

**구현**:
```java
public void sendNotificationToUser(...) {
    // 1. 즉시 SSE 전송
    sendToSubscribers(event);
    
    // 2. 비동기 DB 저장 (추후)
    // saveNotificationAsync(event);
}
```

## 3. SSE 연결 관리

### 3.1 구독자 관리
- **사용자별 구독**: `Map<Long, Set<SseEmitter>> userSubscribers`
- **토픽별 구독**: `Map<String, Set<SseEmitter>> topicSubscribers`

### 3.2 연결 종료 처리
**결정**: 서버 주도 연결 종료

**터미널 상태 처리**:
```java
if (status.isTerminal()) {
    // 최종 상태 전송 후 5초 대기
    CompletableFuture.delayedExecutor(5, TimeUnit.SECONDS)
        .execute(() -> {
            // 명시적 연결 종료
            subscribers.forEach(emitter -> emitter.complete());
            subscribers.remove(taskId);
        });
}
```

### 3.3 재연결 방지
**방법**: 클라이언트에서 `isTerminal` 플래그 확인
```javascript
if (data.isTerminal) {
    shouldReconnect = false;
    // 재연결 시도 하지 않음
}
```

## 4. 확장 계획

### 4.1 알림 타입 확장
- 현재: 재고동기화, 시스템알림, 작업알림
- 추후: 주문알림, 배송알림, 사용자알림 등
- 각 타입별 고유 Status enum 추가

### 4.2 기능 확장
- [ ] DB 저장 로직 구현
- [ ] 읽음 상태 관리
- [ ] 알림 목록 조회 API
- [ ] 알림 설정 관리 (타입별 on/off)
- [ ] 푸시 알림 연동

### 4.3 성능 최적화
- [ ] 배치 DB 저장 (bulk insert)
- [ ] Redis 캐싱 도입
- [ ] 메시지 큐 도입 (Kafka, RabbitMQ)

## 5. 기술적 고려사항

### 5.1 JSON 응답 구조
```json
{
  "userId": 123,
  "type": "TASK",
  "title": "재고 동기화", 
  "message": "완료되었습니다",
  "timestamp": "2025-09-03T10:30:00",
  "statusCode": "COMPLETED",
  "statusDisplay": "완료",
  "isTerminal": true
}
```

### 5.2 패턴 적용
- **전략 패턴**: NotificationStatus 인터페이스
- **애그리거트 패턴**: 도메인 모델 캡슐화
- **비동기 패턴**: SSE + 추후 DB 저장

### 5.3 트레이드오프
**단순함 vs 확장성**: 애그리거트 분리로 초기 복잡도 증가하지만 장기 유지보수성 향상
**성능 vs 일관성**: 비동기 저장으로 성능 우선, 일관성은 eventual consistency
**저장공간 vs 쿼리성능**: user_id=-1L 방식으로 단순함 선택, 읽음상태는 추후 확장

## 6. 구현 우선순위

1. **1차**: NotificationStatus 전략 패턴 구현
2. **2차**: 애그리거트 분리 및 도메인 모델 정리  
3. **3차**: SSE 서비스 리팩토링
4. **4차**: DB 저장 로직 구현
5. **5차**: 읽음 상태 관리 기능 추가