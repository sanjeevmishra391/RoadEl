package Java.Concurrency;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Thread-safe cache implementation using ReadWriteLock.
 *
 * Why ReadWriteLock?
 * - Multiple threads can read simultaneously.
 * - Only one thread can write at a time.
 * - While a write is in progress, no reads are allowed.
 *
 * This improves performance for read-heavy workloads compared
 * to synchronizing every method.
 */
public class ReadWriteLockCache {

    /**
     * ReentrantReadWriteLock provides:
     * - readLock()  -> shared lock for readers
     * - writeLock() -> exclusive lock for writers
     */
    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();

    /**
     * Underlying cache storage.
     * HashMap itself is NOT thread-safe, so access is protected
     * using the read-write lock.
     */
    private final Map<String, String> cache = new HashMap<>();

    /**
     * Retrieves a value from the cache.
     *
     * Multiple threads can execute this method concurrently
     * because they acquire the shared read lock.
     */
    public String get(String key) {

        // Acquire shared read lock
        readWriteLock.readLock().lock();

        try {
            // Safe read operation
            return cache.get(key);
        } finally {
            // Always release lock to avoid deadlocks
            readWriteLock.readLock().unlock();
        }
    }

    /**
     * Inserts or updates a value in the cache.
     *
     * Only one thread can execute this section at a time because
     * the write lock is exclusive.
     */
    public void put(String key, String value) {

        // Acquire exclusive write lock
        readWriteLock.writeLock().lock();

        try {
            // Safe write operation
            cache.put(key, value);
        } finally {
            // Always release lock
            readWriteLock.writeLock().unlock();
        }
    }
}

/*

1. Why use ReadWriteLock instead of synchronized?
synchronized allows only one thread at a time.
ReadWriteLock allows multiple concurrent readers.

2. Can a reader acquire the lock while a writer is holding it?
No. Writers are exclusive.

3. Can multiple writers acquire the lock together?
No. Only one writer is allowed.

4. What is lock starvation in ReadWriteLock?
Continuous readers may prevent a writer from getting the lock.
new ReentrantReadWriteLock(true) enables fairness.

5. What does "Reentrant" mean?
The same thread can acquire the same lock multiple times without deadlocking.

6. Can a thread upgrade from read lock to write lock?
Generally no. This can lead to deadlock.
Releasing the read lock first is usually required.

7. When is ReadWriteLock beneficial?
Read-heavy systems:
- Caches
- Configuration stores
- Product catalogs
- Leaderboards
- In-memory reference data
*/