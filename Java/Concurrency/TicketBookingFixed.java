package Java.Concurrency;

import java.util.concurrent.ConcurrentHashMap;

public class TicketBookingFixed {

    /**
     * Stores seat ownership.
     *
     * Example:
     * A1 -> user1
     * A2 -> user2
     */
    private final ConcurrentHashMap<String, String> seatOwners = new ConcurrentHashMap<>();

    /**
     * One lock object per seat.
     *
     * Example:
     * A1 -> Lock Object 1
     * A2 -> Lock Object 2
     */
    private final ConcurrentHashMap<String, Object> seatLocks = new ConcurrentHashMap<>();

    /**
     * Returns the lock associated with a seat.
     * Creates it only once if it doesn't already exist.
     */
    private Object getLock(String seatId) {
        return seatLocks.computeIfAbsent(seatId, k -> new Object());
    }

    /**
     * Books a seat for a visitor.
     */
    public boolean bookSeat(String seatId, String visitorId) {

        synchronized (getLock(seatId)) {

            // Seat already booked
            if (seatOwners.containsKey(seatId)) {
                return false;
            }

            seatOwners.put(seatId, visitorId);
            return true;
        }
    }

    /**
     * Swaps seats between two visitors.
     *
     * Problem:
     * Thread 1:
     *   lock(A1) -> waits for A2
     *
     * Thread 2:
     *   lock(A2) -> waits for A1
     *
     * This creates a deadlock.
     *
     * Solution:
     * Always acquire locks in a globally consistent order.
     */
    public boolean swapSeats(String visitorId1, String seat1, String visitorId2, String seat2) {

        // Same seat, nothing to swap
        if (seat1.equals(seat2)) {
            return false;
        }

        /**
         * Determine lock order.
         *
         * Example:
         * A1, A5 -> lock A1 then A5
         * A5, A1 -> lock A1 then A5
         *
         * Every thread follows the same order.
         */
        String first = seat1.compareTo(seat2) < 0 ? seat1 : seat2;

        String second = seat1.compareTo(seat2) < 0 ? seat2 : seat1;

        // Acquire first lock
        synchronized (getLock(first)) {

            // Acquire second lock
            synchronized (getLock(second)) {

                // Verify ownership before swap
                if (!visitorId1.equals(seatOwners.get(seat1))) {
                    return false;
                }

                if (!visitorId2.equals(seatOwners.get(seat2))) {
                    return false;
                }

                // Perform swap
                seatOwners.put(seat1, visitorId2);
                seatOwners.put(seat2, visitorId1);

                return true;
            }
        }
    }

    /**
     * Utility method for testing.
     */
    public String getOwner(String seatId) {
        return seatOwners.get(seatId);
    }
}