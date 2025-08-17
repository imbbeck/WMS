package com.wms.applicationInfra.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
	// CORS 설정은 Security 설정에서 처리하므로 제거
	// 필요시 다른 웹 관련 설정 추가
}
