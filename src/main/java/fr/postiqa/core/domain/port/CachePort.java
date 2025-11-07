package fr.postiqa.core.domain.port;

import java.time.Duration;
import java.util.Optional;

/**
 * Port for caching scraped data.
 * <p>
 * Provides a simple key-value cache abstraction to avoid re-scraping
 * frequently accessed data.
 * <p>
 * Implementation can be in-memory (Spring Cache) or distributed (Redis).
 */
public interface CachePort {

    /**
     * Retrieve a cached value by key.
     *
     * @param key  the cache key
     * @param type the expected value type
     * @return Optional containing the cached value, or empty if not found
     */
    <T> Optional<T> get(String key, Class<T> type);

    /**
     * Store a value in cache with a TTL.
     *
     * @param key   the cache key
     * @param value the value to cache
     * @param ttl   time-to-live duration
     */
    <T> void put(String key, T value, Duration ttl);

    /**
     * Store a value in cache with default TTL.
     *
     * @param key   the cache key
     * @param value the value to cache
     */
    <T> void put(String key, T value);

    /**
     * Remove a specific key from cache.
     *
     * @param key the cache key to evict
     */
    void evict(String key);

    /**
     * Clear all cached values.
     */
    void clear();

    /**
     * Check if a key exists in cache.
     *
     * @param key the cache key
     * @return true if key exists, false otherwise
     */
    boolean contains(String key);
}
