package Java.Concurrency;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class ConnectionPool {

    // Holds all currently available connections
    private final BlockingQueue<Connection> availableConnections;

    public ConnectionPool(int poolSize) {

        // Bounded queue limits total connections in the pool
        availableConnections = new LinkedBlockingQueue<>(poolSize);

        // Pre-create connections
        for (int i = 0; i < poolSize; i++) {
            availableConnections.add(createNewConnection());
        }
    }

    public Connection acquire() throws InterruptedException {

        // Blocks if no connection is available
        return availableConnections.take();
    }

    public void release(Connection conn) throws InterruptedException {

        // Returns connection back to pool
        availableConnections.put(conn);
    }

    public void executeQuery(String query) throws InterruptedException {

        Connection conn = acquire();

        try {
            conn.execute(query);
        } finally {
            // Always return connection to avoid leaks
            release(conn);
        }
    }

    private Connection createNewConnection() {
        return new Connection();
    }
}

class Connection {

    public void execute(String query) {
        System.out.println(Thread.currentThread().getName() + " executing: " + query);
    }
}