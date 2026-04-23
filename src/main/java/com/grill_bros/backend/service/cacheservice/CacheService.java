package com.grill_bros.backend.service.cacheservice;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class CacheService {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;


    public <T> void set(String key, T value, long ttlSeconds) {
        try {
            String json = objectMapper.writeValueAsString(value);
            redisTemplate.opsForValue().set(key, json, ttlSeconds, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("Cache SET failed for key={}: {}", key, e.getMessage());
        }
    }

    public void setRaw(String key, String value, long ttlSeconds) {
        try {
            redisTemplate.opsForValue().set(key, value, ttlSeconds, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("Cache SET_RAW failed for key={}: {}", key, e.getMessage());
        }
    }

    public <T> Optional<T> get(String key, Class<T> type) {
        try {
            String json = redisTemplate.opsForValue().get(key);
            if (json == null) return Optional.empty();
            return Optional.of(objectMapper.readValue(json, type));
        } catch (Exception e) {
            log.warn("Cache GET failed for key={}: {}", key, e.getMessage());
            return Optional.empty();
        }
    }

    public <T> Optional<T> get(String key, TypeReference<T> typeRef) {
        try {
            String json = redisTemplate.opsForValue().get(key);
            if (json == null) return Optional.empty();
            return Optional.of(objectMapper.readValue(json, typeRef));
        } catch (Exception e) {
            log.warn("Cache GET failed for key={}: {}", key, e.getMessage());
            return Optional.empty();
        }
    }

    public Optional<String> getRaw(String key) {
        try {
            return Optional.ofNullable(redisTemplate.opsForValue().get(key));
        } catch (Exception e) {
            log.warn("Cache GET_RAW failed for key={}: {}", key, e.getMessage());
            return Optional.empty();
        }
    }

    public boolean exists(String key) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (Exception e) {
            log.warn("Cache EXISTS failed for key={}: {}", key, e.getMessage());
            return false;
        }
    }

    public void evict(String key) {
        try {
            redisTemplate.delete(key);
        } catch (Exception e) {
            log.warn("Cache EVICT failed for key={}: {}", key, e.getMessage());
        }
    }

    public void evictByPattern(String pattern) {
        try {
            var keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.debug("Evicted {} keys matching pattern={}", keys.size(), pattern);
            }
        } catch (Exception e) {
            log.warn("Cache EVICT_PATTERN failed for pattern={}: {}", pattern, e.getMessage());
        }
    }

    public long increment(String key, long ttlSeconds) {
        try {
            Long count = redisTemplate.opsForValue().increment(key);
            if (count != null && count == 1) {
                redisTemplate.expire(key, Duration.ofSeconds(ttlSeconds));
            }
            return count != null ? count : 0L;
        } catch (Exception e) {
            log.warn("Cache INCREMENT failed for key={}: {}", key, e.getMessage());
            return 0L;
        }
    }

    public boolean acquireLock(String key, long ttlSeconds) {
        try {
            Boolean acquired = redisTemplate.opsForValue()
                    .setIfAbsent(key, "LOCKED", Duration.ofSeconds(ttlSeconds));
            return Boolean.TRUE.equals(acquired);
        } catch (Exception e) {
            log.warn("Cache ACQUIRE_LOCK failed for key={}: {}", key, e.getMessage());
            return false;
        }
    }

    public void releaseLock(String key) {
        evict(key);
    }

    public void pushToList(String key, String value, long ttlSeconds) {
        try {
            redisTemplate.opsForList().leftPush(key, value);
            redisTemplate.expire(key, Duration.ofSeconds(ttlSeconds));
            // cap the list at 50 items
            redisTemplate.opsForList().trim(key, 0, 49);
        } catch (Exception e) {
            log.warn("Cache PUSH_LIST failed for key={}: {}", key, e.getMessage());
        }
    }

    public java.util.List<String> popAllFromList(String key) {
        try {
            var items = redisTemplate.opsForList().range(key, 0, -1);
            redisTemplate.delete(key);
            return items != null ? items : java.util.List.of();
        } catch (Exception e) {
            log.warn("Cache POP_ALL failed for key={}: {}", key, e.getMessage());
            return java.util.List.of();
        }
    }
}
