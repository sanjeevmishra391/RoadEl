package Java.Concurrency;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Demonstrates Optimistic Locking using Compare-And-Set (CAS).
 *
 * What is Optimistic Locking?
 * ---------------------------
 * Optimistic locking assumes that conflicts between threads are rare.
 * Instead of acquiring a lock before modifying data, a thread:
 *
 * 1. Reads the current value.
 * 2. Computes the new value.
 * 3. Attempts to update the value only if it has not changed.
 * 4. If another thread modified the value meanwhile, the update fails
 *    and the thread retries.
 *
 * This approach avoids blocking threads and often performs better
 * than traditional locking when contention is low.
 *
 * CAS (Compare-And-Set) is the JVM's lock-free primitive that enables
 * optimistic concurrency.
 *
 * Conceptually:
 *
 * CAS(expectedValue, newValue)
 *
 * means:
 *
 * "Update the value to newValue only if the current value
 *  is still equal to expectedValue."
 *
 * Similar to database optimistic locking:
 *
 * UPDATE account
 * SET balance = ?, version = version + 1
 * WHERE id = ?
 *   AND version = expectedVersion;
 *
 * If another transaction has already updated the row,
 * the version won't match and the update fails.
 */
public class ConcurrencyTracker {

    /**
     * Stores the highest concurrent count observed.
     *
     * AtomicInteger provides atomic operations implemented
     * using CPU-level CAS instructions.
     */
    private final AtomicInteger maxConcurrent = new AtomicInteger(0);

    /**
     * Updates the maximum concurrent value if the supplied
     * value is greater than the current maximum.
     *
     * This method uses optimistic locking via CAS.
     *
     * Example:
     *
     * Current max = 10
     *
     * updateMaxConcurrent(15) -> updates to 15
     * updateMaxConcurrent(8)  -> ignored
     */
    public void updateMaxConcurrent(int current) {

        /**
         * Retry loop.
         *
         * CAS can fail if another thread modifies the value
         * after we read it but before we update it.
         *
         * In that case we retry using the latest value.
         */
        while (true) {

            /**
             * Step 1: Read current value.
             *
             * This is similar to reading a database row along
             * with its version number.
             */
            int observed = maxConcurrent.get();

            /**
             * Step 2: No update required if current value
             * is not greater than the maximum.
             */
            if (current <= observed) {
                return;
            }

            /**
             * Step 3: Optimistic update attempt.
             *
             * Update maxConcurrent to current only if its value
             * is still equal to observed.
             *
             * compareAndSet(expected, newValue)
             */
            if (maxConcurrent.compareAndSet(observed, current)) {

                /**
                 * CAS succeeded.
                 *
                 * This means no other thread changed the value
                 * between our read and write operations.
                 */
                return;
            }

            /**
             * CAS failed.
             *
             * Another thread modified the value before our update.
             *
             * Example:
             *
             * Thread A reads 10
             * Thread B updates 10 -> 15
             * Thread A attempts CAS(10, 12)
             *
             * CAS fails because value is now 15.
             *
             * Thread A retries by reading the latest value.
             */
        }
    }

    /**
     * Returns the maximum concurrent value observed.
     */
    public int getMaxConcurrent() {
        return maxConcurrent.get();
    }

    public static void main(String[] args) throws InterruptedException {

        ConcurrencyTracker tracker = new ConcurrencyTracker();

        Runnable task1 = () -> tracker.updateMaxConcurrent(10);
        Runnable task2 = () -> tracker.updateMaxConcurrent(20);
        Runnable task3 = () -> tracker.updateMaxConcurrent(15);

        Thread t1 = new Thread(task1);
        Thread t2 = new Thread(task2);
        Thread t3 = new Thread(task3);

        t1.start();
        t2.start();
        t3.start();

        t1.join();
        t2.join();
        t3.join();

        System.out.println("Maximum concurrent value = " + tracker.getMaxConcurrent());
    }
}