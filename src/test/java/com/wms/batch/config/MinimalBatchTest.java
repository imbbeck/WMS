package com.wms.batch.config;

import com.wms.applicationInfra.config.QuerydslConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 가장 기본적인 Spring Boot 테스트
 * ApplicationContext만 로딩되는지 확인
 */
@SpringBootTest
@Import(QuerydslConfig.class)
@TestPropertySource(properties = {
		"spring.batch.job.enabled=false",
		"spring.cache.type=none",
		"spring.data.redis.repositories.enabled=false",
		"spring.datasource.url=jdbc:h2:mem:testdb",
		"spring.jpa.hibernate.ddl-auto=create-drop"
})
class MinimalBatchTest {

	@Test
	@DisplayName("Spring Boot 애플리케이션 컨텍스트 로딩 테스트")
	void contextLoads() {
		// ApplicationContext가 정상적으로 로딩되면 성공
		assertThat(true).isTrue();
	}
}