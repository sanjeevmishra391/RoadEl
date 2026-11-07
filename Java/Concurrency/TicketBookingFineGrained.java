package Java.Concurrency;

import java.util.concurrent.ConcurrentHashMap;

class TicketBookingFineGrained {

    /**
     * Maintains a separate lock object for each seat.
     *
     * Example:
     * A1 -> Lock Object 1
     * A2 -> Lock Object 2
     *
     * This allows different seats to be booked concurrently.
     */
    private final ConcurrentHashMap<String, Object> seatLocks =
            new ConcurrentHashMap<>();

    /**
     * Stores which visitor owns which seat.
     *
     * Example:
     * A1 -> visitor123
     * A2 -> visitor456
     */
    private final ConcurrentHashMap<String, String> seatOwners =
            new ConcurrentHashMap<>();

    /**
     * Returns the lock associated with a particular seat.
     *
     * computeIfAbsent() ensures that only one lock object is
     * created for a particular seat.
     *
     * If the lock already exists, the existing lock is returned.
     */
    private Object getLock(String seatId) {
        return seatLocks.computeIfAbsent(
                seatId,
                k -> new Object()
        );
    }

    /**
     * Attempts to book a seat for a visitor.
     *
     * The lock is specific to the seat being booked.
     * Therefore:
     *
     * Thread 1 -> books A1 -> locks A1
     * Thread 2 -> books A2 -> locks A2
     *
     * Both operations can execute concurrently.
     *
     * But:
     *
     * Thread 1 -> books A1
     * Thread 2 -> books A1
     *
     * Only one thread can enter the synchronized block at a time.
     */
    public boolean bookSeat(String seatId, String visitorId) {

        // Acquire the lock specific to this seat.
        synchronized (getLock(seatId)) {

            // Check whether the seat has already been booked.
            if (seatOwners.containsKey(seatId)) {
                return false;
            }

            // Seat is available, so assign it to the visitor.
            seatOwners.put(seatId, visitorId);

            return true;
        }
        // Lock is automatically released when leaving the synchronized block.
    }
}