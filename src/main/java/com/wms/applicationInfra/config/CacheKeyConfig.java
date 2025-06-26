package com.wms.applicationInfra.config;

import java.util.Arrays;
import java.util.stream.Collectors;

import com.wms.stock.domain.model.StockKey;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CacheKeyConfig {

	@Bean("locationCacheKeyGenerator")
	public KeyGenerator locationCacheKeyGenerator() {
		return (target, method, params) -> {

			if (method.getName().contains("WarehouseCapacity") && params.length > 0) {
				Long warehouseId = (Long) params[0];
				return String.format("warehouse:%d", warehouseId) + ":capacity";
			}

			// fallback
			return Arrays.stream(params)
					.map(String::valueOf)
					.collect(Collectors.joining(":"));
		};
	}

	@Bean("stockCacheKeyGenerator")
	public KeyGenerator stockCacheKeyGenerator() {
		return (target, method, params) -> {

			if (method.getName().contains("Inventory") && params.length >= 1) {
				// 첫 번째 파라미터는 항상 StockKey
				StockKey stockKey = (StockKey) params[0];
				return stockKey.toCacheKey(); // "current_stock:warehouseId:wareId"
			}

			if (method.getName().contains("WarehouseCurrentSum") && params.length > 0) {
				Long warehouseId = (Long) params[0];
				return String.format("warehouse:%d:currentSum", warehouseId);
			}

			// fallback
			return Arrays.stream(params)
					.map(String::valueOf)
					.collect(Collectors.joining(":"));
		};
	}
}
