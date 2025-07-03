# 물류 관리 시스템 (WMS - Warehouse Management System)

> **Spring Boot 3.x + DDD + Clean Architecture 기반의 엔터프라이즈급 물류 창고 관리 시스템**

## 📋 프로젝트 개요

입고, 출고, 창고간 이동을 통한 물류 이동 관리 시스템으로, **시뮬레이션 기반 정합성 검증**과 **Redis Stream 기반 비동기 재고 관리**를 통해 고성능과 데이터 일관성을 보장합니다.

### 🎯 핵심 기능

#### 📦 **물류 작업 관리**
- **시뮬레이션 기반 검증**: 작업 생성/수정 시 시간순 이벤트 시뮬레이션으로 재고 부족 및 창고 용량 초과 사전 탐지
- **실시간 작업 상태 관리**: 7가지 상태로 세밀한 작업 진행 상황 추적
- **작업자 스케줄 관리**: 이동 소요시간을 고려한 스케줄 충돌 방지
- **템플릿 기반 계획**: 반복 작업의 효율적 관리

#### 📊 **재고 관리**
- **Redis 캐시 기반 고성능 조회**: 창고별/물품별 실시간 재고 현황
- **Redis Stream 기반 비동기 처리**: 물류 작업 상태 변경 시 재고 자동 업데이트
- **낙관적 락 + 지수적 백오프**: 동시성 충돌 해결
- **SSE 기반 실시간 알림**: 재고 변경 처리 상태를 클라이언트에 실시간 전달

#### 🏢 **장소 및 경로 관리**
- **무방향 그래프 구조**: 장소간 연결 및 이동 소요시간 관리
- **창고 타입별 용량 관리**: INBOUND/OUTBOUND/WAREHOUSE 구분
- **실시간 용량 모니터링**: 창고별 현재 사용량 추적

#### 👥 **사용자 및 권한 관리**
- **JWT 기반 인증**: 토큰 기반 보안
- **역할별 권한 제어**: ADMIN/WORKER 권한 분리
- **BCrypt 패스워드 암호화**: 보안 강화

#### ⚡ **배치 처리**
- **매일 자정 배치**: 재고 스냅샷 생성 및 히스토리 이동
- **Spring Batch**: 대용량 데이터 처리 최적화
- **데이터 정합성 검증**: 배치 처리 시 실제 재고와 캐시 일치성 검사

## 🏗️ 시스템 아키텍처

### DDD + Clean Architecture 구조

```
┌─────────────────────────────────────────────────────────────────────┐
│                        📱 Presentation Layer                        │
├─────────────────┬─────────────────┬─────────────────┬───────────────┤
│ 관리자 대시보드  │ 작업자 대시보드  │ API 컨트롤러     │ SSE 스트림    │
│ • 전체 현황     │ • 개인 작업     │ • REST API      │ • 실시간 알림 │
│ • 작업 계획     │ • 상태 변경     │ • Swagger UI    │ • 상태 변경   │
└─────────────────┴─────────────────┴─────────────────┴───────────────┘
                                │
┌─────────────────────────────────────────────────────────────────────┐
│                        🎯 Application Layer                         │
├─────────────────┬─────────────────┬─────────────────┬───────────────┤
│ LogisticTask    │ Stock           │ Location        │ UserInfo      │
│ • 작업 관리     │ • 재고 관리     │ • 장소 관리     │ • 사용자 관리 │
│ • 검증 서비스   │ • 캐시 서비스   │ • 연결 관리     │ • 인증 서비스 │
│ • 시뮬레이션    │ • 스트림 처리   │ • 용량 관리     │ • JWT        │
└─────────────────┴─────────────────┴─────────────────┴───────────────┘
                                │
┌─────────────────────────────────────────────────────────────────────┐
│                          💎 Domain Layer                            │
├─────────────────┬─────────────────┬─────────────────┬───────────────┤
│ LogisticTask    │ Stock           │ Location        │ UserInfo      │
│ Aggregate       │ Aggregate       │ Aggregate       │ Aggregate     │
│ • 상태 관리     │ • 재고 변경     │ • 타입 검증     │ • 패스워드    │
│ • 비즈니스 규칙 │ • 용량 검증     │ • 연결 관리     │ • 권한 관리   │
│ • 이벤트 발행   │ • 이벤트 발행   │ • 이벤트 발행   │ • 이벤트 발행 │
└─────────────────┴─────────────────┴─────────────────┴───────────────┘
                                │
┌─────────────────────────────────────────────────────────────────────┐
│                      🔧 Infrastructure Layer                        │
├─────────────────┬─────────────────┬─────────────────┬───────────────┤
│ JPA Repository  │ Redis Cache     │ Redis Streams   │ Spring Batch  │
│ • QueryDSL      │ • 재고 캐시     │ • 이벤트 스트림 │ • 배치 처리   │
│ • MySQL 연동    │ • 용량 캐시     │ • 비동기 처리   │ • 스냅샷 생성 │
│ • 트랜잭션      │ • 고성능 조회   │ • 실시간 동기화 │ • 히스토리    │
└─────────────────┴─────────────────┴─────────────────┴───────────────┘
```

### 이벤트 드리븐 아키텍처

```
물류작업 상태변경 → 도메인 이벤트 발행 → Redis Stream 발행 → 비동기 재고 처리
                                                              ↓
재고 변경 실패 ← 낙관적 락 재시도 ← 지수적 백오프 ← 재고 캐시 업데이트
     ↓                                               ↓
최종 실패 시 SSE 알림                               SSE 상태 알림
```

## 🚀 기술 스택

### **Backend Core**
- **Java 17** - 최신 LTS 버전
- **Spring Boot 3.2.3** - 최신 스프링 부트
- **Spring Data JPA** - 데이터 접근 계층
- **QueryDSL 5.0.0** - 타입 안전 쿼리
- **Spring Security + JWT** - 인증/인가
- **Spring Batch** - 배치 처리

### **Database & Cache**
- **MySQL 8.0** - 메인 데이터베이스
- **Redis** - 캐시 및 스트림 처리
- **Redis Streams** - 이벤트 스트리밍

### **Architecture & Patterns**
- **DDD (Domain Driven Design)** - 도메인 중심 설계
- **Clean Architecture** - 계층형 아키텍처
- **Event-Driven Architecture** - 이벤트 기반 아키텍처
- **CQRS Pattern** - 명령/조회 분리

### **Testing & Quality**
- **JUnit 5** - 단위 테스트
- **Spring Boot Test** - 통합 테스트
- **JaCoCo** - 테스트 커버리지
- **H2 Database** - 테스트용 인메모리 DB

### **Documentation & API**
- **Swagger/OpenAPI 3** - API 문서화
- **Gradle 8.14** - 빌드 도구

## 📦 프로젝트 구조

```
WMS/
├── src/main/java/com/wms/
│   ├── 🏗️ applicationInfra/          # 공통 인프라스트럭처
│   │   ├── config/                   # 설정 (Redis, Security, Batch 등)
│   │   ├── exception/                # 글로벌 예외 처리
│   │   ├── idnameMapCashing/         # ID-Name 매핑 캐시
│   │   └── util/                     # 유틸리티 (JSON, 낙관적 락 등)
│   │
│   ├── 📦 logisticTask/              # 물류작업 애그리거트
│   │   ├── application/              # 작업 서비스, 검증 서비스
│   │   ├── domain/
│   │   │   ├── model/               # LogisticTask, Status, SimulationEvent
│   │   │   ├── repository/          # 데이터 접근 인터페이스
│   │   │   ├── event/               # 도메인 이벤트
│   │   │   └── exception/           # 도메인 예외
│   │   ├── interfaces/              # REST 컨트롤러
│   │   └── dto/                     # 데이터 전송 객체
│   │
│   ├── 📊 stock/                     # 재고 애그리거트
│   │   ├── application/              # 재고 서비스, 캐시 서비스, 스트림 처리
│   │   ├── domain/
│   │   │   ├── model/               # Stock, StockKey, DailySnapshot
│   │   │   ├── repository/          # 데이터 접근 인터페이스
│   │   │   ├── event/               # 재고 변경 이벤트
│   │   │   └── exception/           # 도메인 예외
│   │   ├── batch/                   # 스냅샷 배치 처리
│   │   ├── interfaces/              # REST 컨트롤러, SSE 컨트롤러
│   │   └── dto/                     # 데이터 전송 객체
│   │
│   ├── 🏢 location/                  # 장소 애그리거트
│   │   ├── application/              # 장소 서비스, 연결 서비스
│   │   ├── domain/
│   │   │   ├── model/               # Location, LocationConnection, LocationType
│   │   │   ├── repository/          # 데이터 접근 인터페이스
│   │   │   ├── service/             # 도메인 서비스
│   │   │   ├── event/               # 도메인 이벤트
│   │   │   └── exception/           # 도메인 예외
│   │   ├── interfaces/              # REST 컨트롤러
│   │   └── dto/                     # 데이터 전송 객체
│   │
│   ├── 👥 userInfo/                  # 사용자 애그리거트
│   │   ├── application/              # 사용자 서비스, 인증 서비스, JWT
│   │   ├── domain/
│   │   │   ├── model/               # UserInfo, Password (Value Object)
│   │   │   ├── repository/          # 데이터 접근 인터페이스
│   │   │   ├── event/               # 도메인 이벤트
│   │   │   └── exception/           # 도메인 예외
│   │   ├── interfaces/              # REST 컨트롤러
│   │   └── dto/                     # 데이터 전송 객체
│   │
│   ├── 📋 logisticTemplate/          # 작업 템플릿 애그리거트
│   ├── 📦 ware/                      # 물품 애그리거트
│   ├── ⚙️ batch/                     # 배치 처리 (스케줄러, 파티셔너)
│   └── WmsApplication.java           # 메인 애플리케이션
│
├── src/test/                         # 테스트 코드
├── scripts/                          # DB 스크립트
├── docs/                             # 문서
│   ├── DESIGN.md                     # 상세 설계서
│   ├── LogisticTask_DESIGN.md        # 물류작업 애그리거트 설계
│   └── stock_cache_strategy.md       # 재고 캐시 전략
└── build.gradle                      # 빌드 설정
```

## 🔧 설치 및 실행

### 필수 조건
- **Java 17+**
- **MySQL 8.0+**
- **Redis 6.0+**

### 환경 설정

1. **데이터베이스 설정**
```sql
-- MySQL 데이터베이스 생성
CREATE DATABASE wms_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'wms_user'@'localhost' IDENTIFIED BY 'wms_password';
GRANT ALL PRIVILEGES ON wms_db.* TO 'wms_user'@'localhost';
```

2. **Redis 설정**
```bash
# Redis 실행 (포트 6379)
redis-server
```

3. **애플리케이션 설정**
```yaml
# application.yml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/wms_db
    username: wms_user
    password: wms_password
  
  data:
    redis:
      host: localhost
      port: 6379
```

### 실행 방법

```bash
# 프로젝트 클론
git clone [repository-url]
cd WMS

# 데이터베이스 스키마 생성
mysql -u wms_user -p wms_db < scripts/schema-mysql.sql

# 초기 데이터 삽입
mysql -u wms_user -p wms_db < scripts/dml_wms.sql

# 애플리케이션 실행
./gradlew bootRun

# 또는 JAR 빌드 후 실행
./gradlew build
java -jar build/libs/WMS-1.0.0-SNAPSHOT.jar
```

### API 문서 접근
- **Swagger UI**: http://localhost:8080/swagger-ui/index.html
- **OpenAPI JSON**: http://localhost:8080/v3/api-docs

## 📊 핵심 비즈니스 로직

### 🎯 **시뮬레이션 기반 정합성 검증**

물류 작업 생성/수정 시 **우선순위 큐 기반 시간순 시뮬레이션**을 실행하여 다음을 사전 검증:

1. **재고 부족 방지**: 출발 창고의 재고 충분성 검사
2. **용량 초과 방지**: 도착 창고의 수용 가능 용량 검사
3. **스케줄 충돌 방지**: 작업자별 이동 소요시간 고려한 스케줄 검증
4. **시간순 의존성**: 전체 작업들의 시간순 실행 가능성 검증

```java
// 시뮬레이션 예시
PriorityQueue<SimulationEvent> eventQueue = new PriorityQueue<>();
// 모든 작업을 시작/완료 이벤트로 변환
// 시간순으로 이벤트 처리하면서 재고/용량 상태 추적
```

### ⚡ **Redis Stream 기반 비동기 재고 처리**

물류 작업 상태 변경 시 **이벤트 스트림**을 통한 재고 동기화:

```
Client → API: 작업 시작 요청
API → Stream: 재고 감소 이벤트 발행
API → Client: 202 Accepted

Processor → Stream: 이벤트 수신
Processor → Cache: 재고 업데이트
Processor → SSE: 처리 상태 알림
SSE → Client: 실시간 상태 업데이트
```

### 📈 **물류 작업 상태 관리**

```
[시작] → PENDING: 작업 생성

PENDING → INITIATED: 작업 시작
PENDING → INITIATE_DELAYED: 시작 지연
PENDING → CANCELLED: 작업 취소

INITIATED → COMPLETED: 작업 완료
INITIATED → COMPLETE_DELAYED: 완료 지연
INITIATED → FAILED: 작업 실패

INITIATE_DELAYED → PENDING: 수정을 통한 복원
INITIATE_DELAYED → CANCELLED: 취소

모든 완료 상태 → [히스토리]: 자정 배치로 히스토리 이동
```

### 🔄 **동시성 제어 전략**

1. **낙관적 락**: 재고 엔티티에 `@Version` 필드 적용
2. **지수적 백오프**: 충돌 시 재시도 간격 점진적 증가
3. **Redis Stream**: 순차 처리를 통한 동시성 문제 완화
4. **SSE 알림**: 비동기 처리 상태를 실시간으로 클라이언트에 전달

## 🧪 테스트 전략

### 테스트 실행
```bash
# 전체 테스트
./gradlew test

# 통합 테스트
./gradlew integrationTest

# 테스트 커버리지 보고서
./gradlew jacocoTestReport
# 결과: build/reports/jacoco/test/html/index.html
```

### 테스트 구조
- **단위 테스트**: 도메인 로직 및 비즈니스 규칙 검증
- **통합 테스트**: API 엔드포인트 및 데이터베이스 연동 테스트
- **시나리오 테스트**: 전체 물류 플로우 테스트

## 📈 성능 최적화

### 🚀 **캐시 전략**
- **재고 캐시**: `current_stock:{warehouseId}:{wareId}` 패턴
- **창고 용량**: `warehouse:{warehouseId}:capacity` 패턴
- **창고 사용량**: `warehouse:{warehouseId}:currentSum` 패턴
- **Write-Through 캐싱**: DB 업데이트와 동시에 캐시 갱신

### ⚡ **조회 최적화**
- **창고 중심 조회**: Redis SCAN 패턴 매칭 활용
- **물품 중심 조회**: DB 직접 조회 (키 패턴 제약)
- **QueryDSL**: 타입 안전 동적 쿼리

### 📊 **배치 최적화**
- **Spring Batch**: 대용량 데이터 처리
- **파티셔닝**: ID 기반 병렬 처리
- **청크 단위 처리**: 메모리 효율성

## 🔒 보안

- **JWT 토큰**: Stateless 인증
- **BCrypt**: 패스워드 해싱
- **역할 기반 권한**: ADMIN/WORKER 구분
- **API 인증**: 모든 API 엔드포인트 보호

## 📖 문서

- **[📋 시스템 설계서](docs/DESIGN.md)** - 전체 시스템 아키텍처 및 비즈니스 규칙
- **[🎯 물류작업 설계서](docs/LogisticTask_DESIGN.md)** - 물류작업 애그리거트 상세 설계
- **[💾 재고 캐시 전략](docs/stock_cache_strategy.md)** - Redis 캐시 전략 및 성능 최적화
- **[🔌 API 문서](docs/API.md)** - REST API 명세서

## 🚀 향후 개선 계획

### Phase 2
- [ ] 프론트엔드 구현 (React/Vue.js)
- [ ] 최적 경로 추천 시스템
- [ ] 한 물류이동 당 여러 물품 지원
- [ ] 실시간 알림 시스템 확장

### Phase 3
- [ ] 마이크로서비스 아키텍처 전환
- [ ] Kafka 기반 이벤트 스트리밍
- [ ] AI/ML 기반 수요 예측
- [ ] 모바일 앱 개발

---

> **🎯 목표**: 고성능, 확장 가능하며 유지보수가 용이한 엔터프라이즈급 물류 관리 시스템 구축