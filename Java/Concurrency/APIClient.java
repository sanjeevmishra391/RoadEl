package Java.Concurrency;

import java.util.concurrent.Semaphore;

/**
 * Example: Limiting concurrent API calls using Semaphore.
 *
 * Scenario:
 * We are calling an external API provider that allows only a limited
 * number of concurrent requests.
 *
 * Semaphore with 5 permits means:
 * - At most 5 threads can execute the API call simultaneously.
 * - The 6th thread will wait until one of the running requests completes.
 *
 * This is useful for:
 * - Protecting downstream services
 * - Preventing connection pool exhaustion
 * - Limiting resource usage
 * - Avoiding third-party API throttling
 */
public class APIClient {

    /**
     * Semaphore initialized with 5 permits.
     *
     * Available permits = 5 initially.
     *
     * acquire()  -> consumes a permit
     * release()  -> returns a permit
     */
    private final Semaphore requestPermits = new Semaphore(5);

    /**
     * Simulates making an HTTP request.
     */
    public Response makeRequest(String endpoint) throws InterruptedException {

        /*
         * Acquire a permit before making the request.
         *
         * If permits are available:
         *      continue immediately
         *
         * If all permits are exhausted:
         *      block until another thread releases one
         */
        requestPermits.acquire();

        try {

            System.out.println(Thread.currentThread().getName() + " acquired permit. Available permits = " + requestPermits.availablePermits());

            /*
             * Critical section.
             *
             * Only 5 threads can reach here simultaneously.
             */
            return HttpClient.get(endpoint);

        } finally {

            /*
             * Always release the permit.
             *
             * Putting release() inside finally guarantees
             * permit restoration even if:
             *  - exception occurs
             *  - API fails
             *  - timeout happens
             */
            requestPermits.release();

            System.out.println(Thread.currentThread().getName() + " released permit. Available permits = " + requestPermits.availablePermits());
        }
    }

    /**
     * Demo
     */
    public static void main(String[] args) {

        APIClient client = new APIClient();

        for (int i = 1; i <= 10; i++) {

            int requestId = i;

            new Thread(() -> {

                try {

                    Response response = client.makeRequest("/users/" + requestId);

                    System.out.println(Thread.currentThread().getName() + " got response: " + response.getBody());

                } catch (InterruptedException e) {

                    /*
                     * Restore interrupt status.
                     *
                     * InterruptedException clears the flag,
                     * so restore it before exiting.
                     */
                    Thread.currentThread().interrupt();

                    System.out.println(Thread.currentThread().getName() + " interrupted");
                }

            }, "Worker-" + i).start();
        }
    }
}

/**
 * Dummy HTTP client.
 */
class HttpClient {

    public static Response get(String endpoint) throws InterruptedException {

        System.out.println(Thread.currentThread().getName() + " calling " + endpoint);

        // Simulate network delay
        Thread.sleep(3000);

        return new Response("Success from " + endpoint);
    }
}

/**
 * Simple response class.
 */
class Response {

    private final String body;

    public Response(String body) {
        this.body = body;
    }

    public String getBody() {
        return body;
    }
}