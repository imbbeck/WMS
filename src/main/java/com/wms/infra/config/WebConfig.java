package com.wms.infra.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
	@Override
	public void addCorsMappings(CorsRegistry registry) {
		registry.addMapping("/**") // 모든 경로에 대해
				.allowedOrigins("*") // 모든 도메인 허용 (보안상 필요 시 특정 도메인만)
				.allowedMethods("*") // GET, POST, PUT, DELETE 등 모두 허용
				.allowedHeaders("*"); // 모든 헤더 허용
	}
}
