package com.example.wms.inventory.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class VirtualInventoryService {

    private final RedisTemplate<String, String> redisTemplate;
    private static final String VIRTUAL_INVENTORY_KEY_PREFIX = "virtual_inventory:";

    public void increase(Long locationId, Long wareId, Long quantity) {
        String key = generateKey(locationId, wareId);
        redisTemplate.opsForValue().increment(key, quantity);
    }

    public void decrease(Long locationId, Long wareId, Long quantity) {
        String key = generateKey(locationId, wareId);
        redisTemplate.opsForValue().decrement(key, quantity);
    }

    public Long getQuantity(Long locationId, Long wareId) {
        String key = generateKey(locationId, wareId);
        String value = redisTemplate.opsForValue().get(key);
        return value == null ? 0L : Long.parseLong(value);
    }

    private String generateKey(Long locationId, Long wareId) {
        return VIRTUAL_INVENTORY_KEY_PREFIX + locationId + ":" + wareId;
    }
} 