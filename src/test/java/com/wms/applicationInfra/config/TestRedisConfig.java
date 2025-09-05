package com.wms.applicationInfra.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;

@TestConfiguration
@Profile("test")
public class TestRedisConfig {

	@Container
	static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
			.withExposedPorts(6379)
			.withReuse(true); // 테스트 간 컨테이너 재사용

	@Bean
	@Primary
	public RedisConnectionFactory testRedisConnectionFactory() {
		redis.start();
		LettuceConnectionFactory factory = new LettuceConnectionFactory(
				redis.getHost(),
				redis.getMappedPort(6379)
		);
		return factory;
	}
}