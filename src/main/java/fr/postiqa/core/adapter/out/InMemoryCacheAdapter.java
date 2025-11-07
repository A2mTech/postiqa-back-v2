package fr.postiqa.core.adapter.out;

import fr.postiqa.core.domain.port.CachePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

/**
 * In-memory cache adapter using Spring Cache
 * <p>
 * Provides simple in-memory caching for scraped data.
 * Uses Spring's CacheManager with concurrent maps.
 */
@Component
public class InMemoryCacheAdapter implements CachePort {

    private static final Logger log = LoggerFactory.getLogger(InMemoryCacheAdapter.class);
    private static final String DEFAULT_CACHE_NAME = "socialPosts";
    private static final Duration DEFAULT_TTL = Duration.ofMinutes(30);

    private final CacheManager cacheManager;

    public InMemoryCacheAdapter(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @Override
    public <T> Optional<T> get(String key, Class<T> type) {
        log.debug("Cache GET: {}", key);
        Cache cache = getCache();
        T value = cache.get(key, type);
        if (value != null) {
            log.debug("Cache HIT: {}", key);
        } else {
            log.debug("Cache MISS: {}", key);
        }
        return Optional.ofNullable(value);
    }

    @Override
    public <T> void put(String key, T value, Duration ttl) {
        log.debug("Cache PUT: {} (TTL: {})", key, ttl);
        Cache cache = getCache();
        // Note: Spring's simple ConcurrentMapCache doesn't support TTL per entry
        // For TTL support, consider using Caffeine or Redis
        cache.put(key, value);
    }

    @Override
    public <T> void put(String key, T value) {
        put(key, value, DEFAULT_TTL);
    }

    @Override
    public void evict(String key) {
        log.debug("Cache EVICT: {}", key);
        Cache cache = getCache();
        cache.evict(key);
    }

    @Override
    public void clear() {
        log.info("Cache CLEAR ALL");
        Cache cache = getCache();
        cache.clear();
    }

    @Override
    public boolean contains(String key) {
        Cache cache = getCache();
        Cache.ValueWrapper wrapper = cache.get(key);
        return wrapper != null;
    }

    private Cache getCache() {
        return Objects.requireNonNull(cacheManager.getCache(DEFAULT_CACHE_NAME));
    }
}
