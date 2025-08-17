package com.wms.applicationInfra.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";
    private static final String COOKIE_SCHEME_NAME = "cookieAuth";

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("WMS API Documentation")
                        .description("Warehouse Management System - Complete API Documentation\n\n" +
                                "### 인증 방법:\n" +
                                "1. **JWT Bearer Token**: Authorization 헤더에 Bearer 토큰 사용\n" +
                                "2. **Cookie 인증**: 로그인 후 자동으로 설정되는 쿠키 사용\n\n" +
                                "### 사용법:\n" +
                                "1. `api/auth/login` API로 로그인\n" +
                                "2. 응답에서 받은 `accessToken`을 Bearer 토큰으로 사용\n" +
                                "3. 또는 쿠키가 자동으로 설정되어 인증됨")
                        .version("1.0.0"))
                .addSecurityItem(new SecurityRequirement()
                        .addList(SECURITY_SCHEME_NAME)
                        .addList(COOKIE_SCHEME_NAME))
                .components(new Components()
                        // JWT Bearer Token 스키마
                        .addSecuritySchemes(SECURITY_SCHEME_NAME, new SecurityScheme()
                                .name(SECURITY_SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT Bearer Token을 입력하세요. 'Bearer ' 접두사는 자동으로 추가됩니다."))
                        // Cookie 인증 스키마
                        .addSecuritySchemes(COOKIE_SCHEME_NAME, new SecurityScheme()
                                .name("accessToken")
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.COOKIE)
                                .description("로그인 후 자동으로 설정되는 쿠키 인증")));
    }
}