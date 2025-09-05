package com.wms.notification.interfaces;

import com.wms.notification.application.NotificationSseService;
import com.wms.notification.domain.model.*;
import com.wms.userInfo.domain.model.UserInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;

/**
 * 범용 알림 SSE 컨트롤러
 * 사용자별 개인 알림을 실시간으로 전송
 * ADMIN 유저는 모든 알림을 수신할 수 있음
 */
@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Notifications", description = "실시간 알림 SSE API")
public class NotificationController {

    private final NotificationSseService notificationSseService;

    /**
     * 로그인한 사용자의 개인 알림 구독
     * ADMIN 유저의 경우 모든 사용자의 알림을 수신함
     */
    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(
        summary = "개인 알림 실시간 구독", 
        description = "로그인한 사용자의 개인 알림을 실시간으로 스트리밍합니다. ADMIN 유저는 모든 알림을 수신합니다."
    )
    @ApiResponse(responseCode = "200", description = "SSE 스트림 연결 성공")
    public SseEmitter subscribeToPersonalNotifications(
            @Parameter(hidden = true) @AuthenticationPrincipal UserInfo userInfo) {
        
        Long userId = userInfo.getId();
        log.debug("개인 알림 구독 요청: userId={}, username={}, type={}",
                userId, userInfo.getUsername(), userInfo.getType());
        
        return notificationSseService.subscribeToUserNotifications(userId);
    }

    /**
     * 특정 사용자 알림 구독 (관리자용)
     */
    @GetMapping(value = "/subscribe/{userId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(
        summary = "특정 사용자 알림 구독 (관리자용)", 
        description = "관리자가 특정 사용자의 알림을 실시간으로 모니터링합니다."
    )
    @ApiResponse(responseCode = "200", description = "SSE 스트림 연결 성공")
    @PreAuthorize("hasRole('ADMIN')")
    public SseEmitter subscribeToUserNotifications(
            @Parameter(description = "사용자 ID") @PathVariable Long userId,
            @Parameter(hidden = true) @AuthenticationPrincipal UserInfo adminUser) {
        
        log.debug("특정 사용자 알림 구독 요청: targetUserId={}, adminUserId={}", userId, adminUser.getId());
        
        return notificationSseService.subscribeToUserNotifications(userId);
    }

    /**
     * 개인 알림 수동 전송 (테스트용)
     */
    @PostMapping("/send/personal")
    @Operation(
        summary = "개인 알림 수동 전송", 
        description = "테스트 목적으로 특정 사용자에게 알림을 수동으로 전송합니다."
    )
    public ResponseEntity<String> sendPersonalNotification(
            @RequestBody SendPersonalNotificationRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UserInfo senderUser) {
        
        log.debug("개인 알림 수동 전송 요청: targetUserId={}, type={}, senderUserId={}",
                request.userId, request.type, senderUser.getId());
        
        NotificationStatus status = resolveNotificationStatus(request.type, request.statusCode);
        
        notificationSseService.sendNotificationToUser(
            request.userId,
            request.type,
            request.title,
            request.message,
            status,
            request.referenceId
        );
        
        return ResponseEntity.ok("알림이 전송되었습니다. (ADMIN 유저들도 자동으로 수신)");
    }

    /**
     * 내 알림 전송 (본인에게)
     */
    @PostMapping("/send/me")
    @Operation(
        summary = "본인에게 알림 전송", 
        description = "본인에게 테스트 알림을 전송합니다."
    )
    public ResponseEntity<String> sendNotificationToMe(
            @RequestBody SendMeNotificationRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UserInfo userInfo) {
        
        Long userId = userInfo.getId();
        log.debug("본인 알림 전송 요청: userId={}, type={}", userId, request.type);
        
        NotificationStatus status = resolveNotificationStatus(request.type, request.statusCode);
        
        notificationSseService.sendNotificationToUser(
            userId,
            request.type,
            request.title,
            request.message,
            status,
            request.referenceId
        );
        
        return ResponseEntity.ok("본인에게 알림이 전송되었습니다.");
    }

    /**
     * 구독 상태 조회 (모니터링용)
     */
    @GetMapping("/status")
    @Operation(
        summary = "구독 상태 조회", 
        description = "현재 알림 서비스 구독 상태를 조회합니다."
    )
    public ResponseEntity<Map<String, Object>> getSubscriptionStatus(
            @Parameter(hidden = true) @AuthenticationPrincipal UserInfo userInfo) {
        
        log.debug("구독 상태 조회 요청: userId={}", userInfo.getId());
        return ResponseEntity.ok(notificationSseService.getSubscriptionStatus());
    }

    /**
     * 헬스 체크
     */
    @GetMapping("/health")
    @Operation(summary = "알림 서비스 상태 확인", description = "알림 서비스의 상태를 확인합니다.")
    @ApiResponse(responseCode = "200", description = "서비스 정상")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Notification Service is running");
    }

    /**
     * 알림 타입과 상태 코드로 NotificationStatus 인스턴스 생성
     */
    private NotificationStatus resolveNotificationStatus(NotificationType type, String statusCode) {
        return switch (type) {
            case STOCK_SYNC -> StockSyncStatus.valueOf(statusCode);
            case SYSTEM -> SystemNotificationStatus.valueOf(statusCode);
            case TASK -> TaskNotificationStatus.valueOf(statusCode);
        };
    }

    /**
     * 개인 알림 전송 요청 DTO
     */
    public record SendPersonalNotificationRequest(
        Long userId,
        NotificationType type,
        String title,
        String message,
        String statusCode,
        Long referenceId
    ) {}

    /**
     * 본인 알림 전송 요청 DTO
     */
    public record SendMeNotificationRequest(
        NotificationType type,
        String title,
        String message,
        String statusCode,
        Long referenceId
    ) {}
}
