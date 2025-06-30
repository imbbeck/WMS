package com.wms.logisticTask.dto;

import com.wms.location.domain.model.Location;
import com.wms.logisticTask.domain.model.LogisticTask;
import com.wms.logisticTask.domain.model.LogisticTaskStatus;
import com.wms.logisticTemplate.domain.model.LogisticType;
import com.wms.userInfo.domain.model.UserInfo;
import com.wms.ware.domain.model.Ware;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public class LogisticTaskDTO {

    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @Schema(name = "LogisticTaskCreateRequest", description = "물류 작업 생성 요청")
    public static class CreateReq {

        @NotBlank(message = "작업명은 필수입니다")
        @Schema(description = "작업명", example = "창고A → 창고B 노트북 이동")
        private String name;

        @NotNull(message = "물류 타입은 필수입니다")
        @Schema(description = "물류 타입", example = "INBOUND" , allowableValues = {"INBOUND", "OUTBOUND", "INNER"})
        private LogisticType type;

        @NotNull(message = "작업자 ID는 필수입니다")
        @Schema(description = "작업자 ID", example = "1")
        private Long workerId;

        @NotNull(message = "물품 ID는 필수입니다")
        @Schema(description = "물품 ID", example = "1")
        private Long wareId;

        @NotNull(message = "출발지 ID는 필수입니다")
        @Schema(description = "출발지 ID", example = "1")
        private Long fromLocationId;

        @NotNull(message = "도착지 ID는 필수입니다")
        @Schema(description = "도착지 ID", example = "2")
        private Long toLocationId;

        @NotNull(message = "수량은 필수입니다")
        @Positive(message = "수량은 자연수이어야 합니다")
        @Schema(description = "수량", example = "10")
        private Integer quantity;

        @NotNull(message = "예정 날짜는 필수입니다")
        @Schema(description = "예정 날짜", example = "2025-01-01")
        private LocalDate scheduledDate;

        @NotNull(message = "출발 예정시간은 필수입니다")
        @Schema(description = "출발 예정시간", example = "09:00")
        private LocalTime etd;

        @NotNull(message = "도착 예정시간은 필수입니다")
        @Schema(description = "도착 예정시간", example = "10:00")
        private LocalTime eta;

        @Schema(description = "템플릿 ID (참조용)", example = "1")
        private Integer templateIdSnapshot;

        @Builder
        public CreateReq(String name, LogisticType type, Long workerId, Long wareId,
                        Long fromLocationId, Long toLocationId, Integer quantity,
                        LocalDate scheduledDate, LocalTime etd, LocalTime eta,
                        Integer templateIdSnapshot) {
            this.name = name;
            this.type = type;
            this.workerId = workerId;
            this.wareId = wareId;
            this.fromLocationId = fromLocationId;
            this.toLocationId = toLocationId;
            this.quantity = quantity;
            this.scheduledDate = scheduledDate;
            this.etd = etd;
            this.eta = eta;
            this.templateIdSnapshot = templateIdSnapshot;
        }

        public LogisticTask toEntity(UserInfo worker, Ware ware, Location fromLocation, Location toLocation) {
            return LogisticTask.builder()
                    .name(name)
                    .type(type)
                    .worker(worker)
                    .ware(ware)
                    .fromLocation(fromLocation)
                    .toLocation(toLocation)
                    .quantity(quantity)
                    .scheduledDate(scheduledDate)
                    .etd(etd)
                    .eta(eta)
                    .templateIdSnapshot(templateIdSnapshot)
                    .build();
        }


        @AssertTrue(message = "작업 시작시간과 종료시간은 10분 단위여야 합니다.")
        public boolean isTimeSlotValid() {
            return etd != null && eta != null &&
                    etd.getMinute() % 10 == 0 &&
                    eta.getMinute() % 10 == 0;
        }

        @AssertTrue(message = "작업 시작시간은 종료시간보다 빨라야 합니다.")
        public boolean isEtdBeforeEta() {
            return etd != null && eta != null && etd.isBefore(eta);
        }

    }

    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @Schema(name = "LogisticTaskUpdateRequest", description = "물류 작업 수정 요청")
    public static class UpdateReq {

        @NotBlank(message = "작업명은 필수입니다")
        @Schema(description = "작업명", example = "창고A → 창고B 노트북 이동 (수정)")
        private String name;

        @NotNull(message = "작업자 ID는 필수입니다")
        @Schema(description = "작업자 ID", example = "2")
        private Long workerId;

        @NotNull(message = "수량은 필수입니다")
        @Positive(message = "수량은 자연수이어야 합니다")
        @Schema(description = "수량", example = "15")
        private Integer quantity;

        @NotNull(message = "출발 예정시간은 필수입니다")
        @Schema(description = "출발 예정시간", example = "10:00")
        private LocalTime etd;

        @NotNull(message = "도착 예정시간은 필수입니다")
        @Schema(description = "도착 예정시간", example = "11:00")
        private LocalTime eta;

        @Builder
        public UpdateReq(String name, Long workerId, Integer quantity,
                        LocalTime etd, LocalTime eta) {

            this.name = name;
            this.workerId = workerId;
            this.quantity = quantity;
            this.etd = etd;
            this.eta = eta;
        }

        @AssertTrue(message = "작업 시작시간과 종료시간은 10분 단위여야 합니다.")
        public boolean isTimeSlotValid() {
            return etd != null && eta != null &&
                    etd.getMinute() % 10 == 0 &&
                    eta.getMinute() % 10 == 0;
        }

        @AssertTrue(message = "작업 시작시간은 종료시간보다 빨라야 합니다.")
        public boolean isEtdBeforeEta() {
            return etd != null && eta != null && etd.isBefore(eta);
        }
    }

    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @Schema(name = "LogisticTaskPartialUpdateRequest", description = "물류 작업 부분 수정 요청 (작업자와 시간만)")
    public static class PartialUpdateReq {

        @NotNull(message = "작업자 ID는 필수입니다")
        @Schema(description = "작업자 ID", example = "3")
        private Long workerId;

        @NotNull(message = "출발 예정시간은 필수입니다")
        @Schema(description = "출발 예정시간", example = "14:00")
        private LocalTime etd;

        @NotNull(message = "도착 예정시간은 필수입니다")
        @Schema(description = "도착 예정시간", example = "15:00")
        private LocalTime eta;

        @Builder
        public PartialUpdateReq(Long workerId, LocalTime etd, LocalTime eta) {
            this.workerId = workerId;
            this.etd = etd;
            this.eta = eta;
        }

        @AssertTrue(message = "작업 시작시간과 종료시간은 10분 단위여야 합니다.")
        public boolean isTimeSlotValid() {
            return etd != null && eta != null &&
                    etd.getMinute() % 10 == 0 &&
                    eta.getMinute() % 10 == 0;
        }

        @AssertTrue(message = "작업 시작시간은 종료시간보다 빨라야 합니다.")
        public boolean isEtdBeforeEta() {
            return etd != null && eta != null && etd.isBefore(eta);
        }
    }

    @Getter
    @Builder
    @Schema(name = "LogisticTaskResponse", description = "물류 작업 응답")
    public static class Res {

        @Schema(description = "작업 ID", example = "1")
        private Long id;

        @Schema(description = "작업명", example = "창고A → 창고B 노트북 이동")
        private String name;

        @Schema(description = "물류 타입", example = "INNER")
        private LogisticType type;

        @Schema(description = "작업자 ID", example = "1")
        private Long workerId;

        @Schema(description = "작업자명", example = "김작업")
        private String workerName;

        @Schema(description = "물품 ID", example = "1")
        private Long wareId;

        @Schema(description = "물품명", example = "노트북")
        private String wareName;

        @Schema(description = "출발지 ID", example = "1")
        private Long fromLocationId;

        @Schema(description = "출발지명", example = "창고A")
        private String fromLocationName;

        @Schema(description = "도착지 ID", example = "2")
        private Long toLocationId;

        @Schema(description = "도착지명", example = "창고B")
        private String toLocationName;

        @Schema(description = "수량", example = "10")
        private Integer quantity;

        @Schema(description = "예정 날짜", example = "2025-01-01")
        private LocalDate scheduledDate;

        @Schema(description = "출발 예정시간", example = "09:00")
        private LocalTime etd;

        @Schema(description = "도착 예정시간", example = "10:00")
        private LocalTime eta;

        @Schema(description = "실제 출발시간", example = "09:05")
        private LocalTime atd;

        @Schema(description = "실제 도착시간", example = "10:03")
        private LocalTime ata;

        @Schema(description = "작업 상태", example = "PENDING")
        private LogisticTaskStatus status;

        @Schema(description = "템플릿 ID (참조용)", example = "1")
        private Integer templateIdSnapshot;

        @Schema(description = "생성일시", example = "2025-01-01T08:00:00")
        private LocalDateTime createdAt;

        @Schema(description = "수정일시", example = "2025-01-01T08:30:00")
        private LocalDateTime updatedAt;

        public static Res from(LogisticTask task, String workerName, String wareName,
                              String fromLocationName, String toLocationName) {
            return Res.builder()
                    .id(task.getId())
                    .name(task.getName())
                    .type(task.getType())
                    .workerId(task.getWorker().getId())
                    .workerName(workerName)
                    .wareId(task.getWare().getId())
                    .wareName(wareName)
                    .fromLocationId(task.getFromLocation().getId())
                    .fromLocationName(fromLocationName)
                    .toLocationId(task.getToLocation().getId())
                    .toLocationName(toLocationName)
                    .quantity(task.getQuantity())
                    .scheduledDate(task.getScheduledDate())
                    .etd(task.getEtd())
                    .eta(task.getEta())
                    .atd(task.getAtd())
                    .ata(task.getAta())
                    .status(task.getStatus())
                    .templateIdSnapshot(task.getTemplateIdSnapshot())
                    .createdAt(task.getCreatedAt())
                    .updatedAt(task.getUpdatedAt())
                    .build();
        }
    }

    @Getter
    @Builder
    @Schema(name = "LogisticTaskActionResponse", description = "물류 작업 상태 변경 응답")
    public static class ActionRes {

        @Schema(description = "작업 ID", example = "1")
        private Long id;

        @Schema(description = "작업명", example = "창고A → 창고B 노트북 이동")
        private String name;

        @Schema(description = "이전 상태", example = "PENDING")
        private LogisticTaskStatus previousStatus;

        @Schema(description = "현재 상태", example = "INITIATED")
        private LogisticTaskStatus currentStatus;

        @Schema(description = "처리 시간", example = "2025-01-01T09:05:00")
        private LocalDateTime processedAt;

        public static ActionRes from(LogisticTask task, LogisticTaskStatus previousStatus) {
            return ActionRes.builder()
                    .id(task.getId())
                    .name(task.getName())
                    .previousStatus(previousStatus)
                    .currentStatus(task.getStatus())
                    .processedAt(LocalDateTime.now())
                    .build();
        }
    }

    @Getter
    @Builder
    @Schema(name = "LogisticTaskDashboardResponse", description = "작업자별 일정을 포함한 일별 대시보드 응답")
    public static class DashboardRes {

        @Schema(description = "조회 날짜", example = "2025-01-15")
        private LocalDate date;

        @Schema(description = "작업자별 일정")
        private List<WorkerSchedule> workerSchedules;

        @Getter
        @Builder
        @Schema(name = "WorkerSchedule", description = "시간대별 작업을 포함한 작업자 일정")
        public static class WorkerSchedule {

            @Schema(description = "작업자 ID", example = "1")
            private Long workerId;

            @Schema(description = "작업자명", example = "김작업")
            private String workerName;

            @Schema(description = "시간대별 작업")
            private List<TimeSlot> timeSlots;
        }

        @Getter
        @Builder
        @Schema(name = "TimeSlot", description = "작업 정보를 포함한 시간대")
        public static class TimeSlot {

            @Schema(description = "시간", example = "09:00")
            private LocalTime time;

            @Schema(description = "해당 시간의 작업")
            private LogisticTaskDTO.Res task;

            @Schema(description = "작업 유형 (START, COMPLETE, EMPTY)", example = "START")
            private String type; // START, COMPLETE, EMPTY
        }
    }

    @Getter
    @Builder
    @Schema(name = "LogisticTaskSearchCriteria", description = "물류 작업 검색 조건")
    public static class SearchCriteria {

        @Schema(description = "작업명 (부분 검색)", example = "노트북")
        private String name;

        @Schema(description = "물류 타입", example = "INNER" , allowableValues = {"INBOUND", "OUTBOUND", "INNER"})
        private LogisticType type;

        @Schema(description = "작업자 ID", example = "1")
        private Long workerId;

        @Schema(description = "물품 ID", example = "1")
        private Long wareId;

        @Schema(description = "출발지 ID", example = "1")
        private Long fromLocationId;

        @Schema(description = "도착지 ID", example = "2")
        private Long toLocationId;

        @Schema(description = "작업 상태", example = "PENDING ", allowableValues = {"PENDING", "INITIATED", "INITIATE_DELAYED", "COMPLETED", "COMPLETE_DELAYED", "CANCELED", "FAILED", "EXPIRED"})
        private LogisticTaskStatus status;

        @Schema(description = "시작 날짜", example = "2025-01-01")
        private LocalDate startDate;

        @Schema(description = "종료 날짜", example = "2025-01-31")
        private LocalDate endDate;
    }
}
