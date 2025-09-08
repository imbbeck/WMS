# LogisticTask 상태 전환 API 문서

## 개요
물류 작업(LogisticTask)의 상태 전환 API와 규칙을 정의한 문서입니다. 각 상태에서 어떤 상태로 전환이 가능한지, 그리고 어떤 API를 통해 전환할 수 있는지를 명시합니다.

## 상태별 전환 가능 API 매트릭스

| 현재 상태 | 전환 가능 상태 | API 엔드포인트 | HTTP 메서드 | 권한 | 설명 |
|-----------|---------------|---------------|------------|------|------|
| **PENDING** | INITIATED | `/logistic-tasks/{taskId}/initiate` | POST | ADMIN, 배정된 WORKER | 작업 시작 |
| | INITIATE_DELAYED | `/logistic-tasks/{taskId}/delay/initiation` | POST | ADMIN, 배정된 WORKER | 시작 지연 처리 |
| | CANCELLED | `/logistic-tasks/{taskId}/cancel` | POST | ADMIN, 배정된 WORKER | 작업 취소 |
| **INITIATE_DELAYED** | INITIATED | `/logistic-tasks/{taskId}/initiate` | POST | ADMIN, 배정된 WORKER | 지연 후 작업 시작 |
| | CANCELLED | `/logistic-tasks/{taskId}/cancel` | POST | ADMIN, 배정된 WORKER | 작업 취소 |
| **INITIATED** | COMPLETED | `/logistic-tasks/{taskId}/complete` | POST | ADMIN, 배정된 WORKER | 작업 완료 |
| | COMPLETE_DELAYED | `/logistic-tasks/{taskId}/delay/completion` | POST | ADMIN, 배정된 WORKER | 완료 지연 처리 |
| | FAILED | `/logistic-tasks/{taskId}/fail` | POST | ADMIN, 배정된 WORKER | 작업 실패 |
| **COMPLETE_DELAYED** | COMPLETED | `/logistic-tasks/{taskId}/complete` | POST | ADMIN, 배정된 WORKER | 지연 후 작업 완료 |
| **COMPLETED** | - | - | - | - | **최종 상태** (변경 불가) |
| **CANCELLED** | - | - | - | - | **최종 상태** (변경 불가) |
| **FAILED** | - | - | - | - | **최종 상태** (변경 불가) |

## API 엔드포인트 상세

### 1. 작업 상태 변경 API

#### 🟢 작업 시작
- **URL**: `POST /logistic-tasks/{taskId}/initiate`
- **권한**: `ADMIN` 또는 배정된 `WORKER`
- **가능한 현재 상태**: `PENDING`, `INITIATE_DELAYED`
- **결과 상태**: `INITIATED`
- **응답**: `LogisticTaskDTO.ActionRes`

#### 🔴 작업 완료
- **URL**: `POST /logistic-tasks/{taskId}/complete`
- **권한**: `ADMIN` 또는 배정된 `WORKER`
- **가능한 현재 상태**: `INITIATED`, `COMPLETE_DELAYED`
- **결과 상태**: `COMPLETED`
- **응답**: `LogisticTaskDTO.ActionRes`

#### ⏸️ 작업 취소
- **URL**: `POST /logistic-tasks/{taskId}/cancel`
- **권한**: `ADMIN` 또는 배정된 `WORKER`
- **가능한 현재 상태**: `PENDING`, `INITIATE_DELAYED`
- **결과 상태**: `CANCELLED`
- **응답**: `LogisticTaskDTO.ActionRes`

#### ❌ 작업 실패
- **URL**: `POST /logistic-tasks/{taskId}/fail`
- **권한**: `ADMIN` 또는 배정된 `WORKER`
- **가능한 현재 상태**: `INITIATED`
- **결과 상태**: `FAILED`
- **응답**: `LogisticTaskDTO.ActionRes`

#### ⏰ 시작 지연
- **URL**: `POST /logistic-tasks/{taskId}/delay/initiation`
- **권한**: `ADMIN` 또는 배정된 `WORKER`
- **가능한 현재 상태**: `PENDING`
- **결과 상태**: `INITIATE_DELAYED`
- **응답**: `LogisticTaskDTO.ActionRes`

#### ⏰ 완료 지연
- **URL**: `POST /logistic-tasks/{taskId}/delay/completion`
- **권한**: `ADMIN` 또는 배정된 `WORKER`
- **가능한 현재 상태**: `INITIATED`
- **결과 상태**: `COMPLETE_DELAYED`
- **응답**: `LogisticTaskDTO.ActionRes`

### 2. 작업 수정 API

#### 📝 전체 수정
- **URL**: `PUT /logistic-tasks/{taskId}`
- **권한**: `ADMIN`
- **가능한 현재 상태**: `PENDING`, `INITIATE_DELAYED`
- **결과 상태**: `PENDING` (항상 초기화)
- **응답**: `LogisticTaskDTO.Res`

#### 📝 부분 수정
- **URL**: `PATCH /logistic-tasks/{taskId}`
- **권한**: `ADMIN`
- **가능한 현재 상태**: `PENDING`, `INITIATE_DELAYED`
- **결과 상태**: `PENDING` (항상 초기화)
- **응답**: `LogisticTaskDTO.Res`

### 3. 작업 삭제 API

#### 🗑️ 작업 삭제
- **URL**: `DELETE /logistic-tasks/{taskId}`
- **권한**: `ADMIN`
- **가능한 현재 상태**: `PENDING`, `INITIATE_DELAYED`
- **응답**: `204 No Content`

## 상태별 허용 작업 요약

### PENDING (대기)
- ✅ **시작**: `/initiate`
- ✅ **지연**: `/delay/initiation`
- ✅ **취소**: `/cancel`
- ✅ **수정**: `PUT`, `PATCH`
- ✅ **삭제**: `DELETE`

### INITIATE_DELAYED (시작 지연)
- ✅ **시작**: `/initiate`
- ✅ **취소**: `/cancel`
- ✅ **수정**: `PUT`, `PATCH`
- ✅ **삭제**: `DELETE`

### INITIATED (진행 중)
- ✅ **완료**: `/complete`
- ✅ **지연**: `/delay/completion`
- ✅ **실패**: `/fail`
- ❌ **수정/삭제 불가**

### COMPLETE_DELAYED (완료 지연)
- ✅ **완료**: `/complete`
- ❌ **기타 모든 작업 불가**

### 최종 상태 (COMPLETED, CANCELLED, FAILED)
- ❌ **모든 변경 불가**
