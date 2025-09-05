package com.wms.applicationInfra.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.boot.web.servlet.FilterRegistrationBean;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    
    /**
     * SSE 응답 헤더 최적화 필터
     * 
     * 문제점들:
     * 1. Connection: close → keep-alive로 변경 필요
     * 2. Content-Encoding: gzip → SSE에서는 압축 비활성화 필요
     * 3. 기타 SSE 최적화 헤더 추가
     */
    @Bean
    public FilterRegistrationBean<SseResponseHeaderFilter> sseHeaderFilter() {
        FilterRegistrationBean<SseResponseHeaderFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new SseResponseHeaderFilter());
        registration.addUrlPatterns("/api/notifications/*");  // SSE 엔드포인트에만 적용
        registration.setOrder(1);  // 높은 우선순위
        return registration;
    }
    
    /**
     * SSE 전용 응답 헤더 필터
     */
    public static class SseResponseHeaderFilter implements Filter {
        
        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                throws IOException, ServletException {
            
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            HttpServletResponse httpResponse = (HttpServletResponse) response;
            
            // SSE 엔드포인트인지 확인
            String requestURI = httpRequest.getRequestURI();
            if (requestURI.contains("/notifications/subscribe")) {
                
                // SSE 필수 헤더 설정
                httpResponse.setContentType("text/event-stream");
                httpResponse.setCharacterEncoding("UTF-8");
                
                // 캐시 비활성화 (더 강화)
                httpResponse.setHeader("Cache-Control", "no-cache, no-store, max-age=0, must-revalidate");
                httpResponse.setHeader("Pragma", "no-cache");
                httpResponse.setHeader("Expires", "0");
                
                // 연결 유지 (가장 중요!)
                httpResponse.setHeader("Connection", "keep-alive");
                
                // 압축 비활성화 (gzip 방지)
                httpResponse.setHeader("Content-Encoding", "identity");
                
                // SSE 최적화 헤더들
                httpResponse.setHeader("X-Accel-Buffering", "no");  // Nginx 버퍼링 비활성화
                httpResponse.setHeader("X-Content-Type-Options", "nosniff");
                
                // CORS 헤더 (필요시)
                httpResponse.setHeader("Access-Control-Allow-Origin", "*");
                httpResponse.setHeader("Access-Control-Allow-Methods", "GET, OPTIONS");
                httpResponse.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
                httpResponse.setHeader("Access-Control-Expose-Headers", "Content-Type");
            }
            
            chain.doFilter(request, response);
        }
    }
}
