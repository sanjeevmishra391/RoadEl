# LLD Case Study: Elevator System

The Elevator System is among the most commonly asked LLD questions at Google. It tests your ability to model state machines, apply scheduling algorithms, and handle concurrency — all within a domain that is immediately understandable.

---

## 1. Problem Statement

*What the interviewer says:*

"Design an elevator control system for a building. The system should handle multiple elevators and multiple floors. Users can request an elevator from any floor by pressing Up or Down, and can request a destination floor from inside the elevator."

---

## 2. Clarifying Questions

Ask these before drawing anything. The answers constrain your design.

| Question | Expected Answer / What It Changes |
|---|---|
| How many floors does the building have? | Typical: 10–50. Affects floor representation and display. |
| How many elevators? | Typical: 2–8. Drives need for a scheduler/dispatcher. |
| Are there basement floors (negative floor numbers)? | Usually yes. Floor IDs should be integers, not 1-indexed. |
| What is the capacity of each elevator (persons)? | e.g., 10–15 people. You need to track load and reject entry at capacity. |
| Should the system handle direction priority? | Usually yes: an elevator going UP should pick up UP requests along the way before reversing. |
| How should we handle emergency situations? | Emergency button stops the elevator, opens door at next floor, disables normal operation. |
| Is there a maintenance mode? | Yes — remove elevator from service pool without disrupting others. |
| Are there accessibility requirements (e.g., audio cues, door hold button)? | Yes — mention but do not over-engineer unless pushed. |
| Should we handle express elevators (skip certain floors)? | No for now, but flag as an extension point. |
| Is this a real-time simulation or a logical model? | Logical model — we model transitions, not sleep timers. |

---

## 3. Functional Requirements

1. Users can call an elevator from any floor by pressing Up or Down (external request)
2. Users inside an elevator can press destination floor buttons (internal request)
3. The system dispatches the most appropriate elevator to each request
4. An elevator serves requests in an efficient order (not FCFS — see SCAN algorithm)
5. Elevators transition between Idle, Moving, and Door Open states
6. The system handles concurrent requests from multiple floors simultaneously
7. An elevator at capacity does not accept new passengers
8. Emergency stop halts all movement immediately

---

## 4. Non-Functional Requirements

1. **Correctness**: no request is permanently lost or starved
2. **Responsiveness**: minimize average wait time across all floor requests
3. **Thread safety**: multiple users can submit requests simultaneously
4. **Extensibility**: easy to swap the scheduling algorithm without changing elevator logic
5. **Observability**: system state (each elevator's floor, direction, queue) must be inspectable

---

## 5. Entities and Responsibilities

| Class / Interface | Single Responsibility |
|---|---|
| `Floor` | Represents a building floor; holds pending external requests (Up/Down) |
| `Request` | Value object: floor number, direction (for external), target floor (for internal) |
| `ElevatorState` | Interface/enum: defines the behavior contract for each elevator state |
| `Elevator` | State machine: current floor, current state, internal request queue, capacity |
| `ElevatorScheduler` | Interface: given a request and a list of elevators, returns the best elevator |
| `SCANScheduler` | Concrete scheduler implementing the LOOK/SCAN algorithm |
| `ElevatorController` | Coordinator: receives all requests, delegates to scheduler, drives elevator movement |
| `Direction` | Enum: UP, DOWN, IDLE |
| `DoorState` | Enum: OPEN, CLOSED |
| `ElevatorPanel` | Inside-elevator UI: buttons for each floor + emergency |
| `FloorPanel` | Outside-elevator UI: Up/Down call buttons per floor |

---

## 6. ASCII Class Diagram

```
+------------------+         +-------------------+
|  ElevatorController|<>----->| ElevatorScheduler |<<interface>>
|------------------|         +-------------------+
| elevators: List  |                   ^
| floors: List     |                   |
| scheduler        |         +-------------------+
| receiveRequest() |         |  SCANScheduler    |
| step()           |         | selectElevator()  |
+------------------+         +-------------------+
        |
        | manages
        v
+------------------+         +-------------------+
|    Elevator      |-------->| ElevatorState     |<<interface>>
|------------------|  state  +-------------------+
| id: int          |                   ^
| currentFloor: int|         +---------+---------+
| direction        |         |         |         |
| doorState        |       Idle   MovingUp  MovingDown
| internalRequests |         |         |         |
| capacity: int    |         +---------+---------+
| currentLoad: int |                   |
| state: Elevator  |              DoorOpen
|   State          |
| addInternalReq() |
| step()           |
| getScore()       |
+------------------+
        ^
        | 1..*
+------------------+         +------------------+
|     Request      |         |     Floor        |
|------------------|         |------------------|
| requestType:     |         | floorNumber: int |
|   INTERNAL|      |         | upRequests: Queue|
|   EXTERNAL       |         | downRequests:    |
| sourceFloor: int |         |   Queue          |
| targetFloor: int |         | hasUpRequest()   |
| direction:       |         | hasDownRequest() |
|   Direction      |         +------------------+
+------------------+
```

---

## 7. Design Patterns Used

### State Pattern (for Elevator state)

**Why:** An elevator's behavior depends heavily on its current state. "Accept a new floor request" means something different when the elevator is Idle (start moving) vs MovingUp (add to queue if in direction) vs DoorOpen (wait, can't move). Without the State pattern, you get a single `Elevator` class with cascading `if/else` or `switch` statements that grow with every new state.

The State pattern encapsulates each state's behavior in its own class. Adding a new state (e.g., Maintenance) means adding a new class, not modifying existing ones — Open/Closed Principle.

### Strategy Pattern (for scheduling algorithm)

**Why:** The algorithm that decides which elevator responds to a call is a separable concern from elevator operation. With `ElevatorScheduler` as an interface, you can swap SCAN for Nearest-Car, Zone-Based, or Machine-Learning-based scheduling without touching `ElevatorController` or `Elevator`. This also makes the scheduler independently testable.

### Observer Pattern (for floor request notification)

**Why:** When a user presses a call button on a floor, the `Floor` publishes an event. `ElevatorController` subscribes to floor events. This decouples the floor panel UI from the dispatch logic — `Floor` does not need to know about `ElevatorController`, and multiple subscribers (logging, metrics) can observe the same events without changing `Floor`.

---

## 8. Core Implementation

### 8.1 Direction and DoorState Enums

```java
public enum Direction {
    UP, DOWN, IDLE
}

public enum DoorState {
    OPEN, CLOSED
}
```

### 8.2 Request

```java
public class Request {

    public enum Type { INTERNAL, EXTERNAL }

    private final Type type;
    private final int sourceFloor;
    private final int targetFloor;     // Only meaningful for INTERNAL requests
    private final Direction direction; // Only meaningful for EXTERNAL requests

    // External request: user presses Up/Down on floor panel
    public static Request external(int sourceFloor, Direction direction) {
        return new Request(Type.EXTERNAL, sourceFloor, -1, direction);
    }

    // Internal request: user presses destination floor inside elevator
    public static Request internal(int sourceFloor, int targetFloor) {
        Direction dir = targetFloor > sourceFloor ? Direction.UP : Direction.DOWN;
        return new Request(Type.INTERNAL, sourceFloor, targetFloor, dir);
    }

    private Request(Type type, int sourceFloor, int targetFloor, Direction direction) {
        this.type = type;
        this.sourceFloor = sourceFloor;
        this.targetFloor = targetFloor;
        this.direction = direction;
    }

    public Type getType() { return type; }
    public int getSourceFloor() { return sourceFloor; }
    public int getTargetFloor() { return targetFloor; }
    public Direction getDirection() { return direction; }

    @Override
    public String toString() {
        return type == Type.EXTERNAL
            ? String.format("ExternalReq[floor=%d, dir=%s]", sourceFloor, direction)
            : String.format("InternalReq[from=%d, to=%d]", sourceFloor, targetFloor);
    }
}
```

### 8.3 ElevatorState Interface and Concrete States

```java
public interface ElevatorState {
    void handleInternalRequest(Elevator elevator, int targetFloor);
    void handleExternalRequest(Elevator elevator, Request request);
    void step(Elevator elevator); // Advance one "tick" of simulation
    String getStateName();
}

// ---- Idle State ----
public class IdleState implements ElevatorState {

    @Override
    public void handleInternalRequest(Elevator elevator, int targetFloor) {
        elevator.addDestination(targetFloor);
        Direction dir = targetFloor > elevator.getCurrentFloor()
            ? Direction.UP : Direction.DOWN;
        elevator.setDirection(dir);
        elevator.setState(dir == Direction.UP
            ? elevator.getMovingUpState() : elevator.getMovingDownState());
    }

    @Override
    public void handleExternalRequest(Elevator elevator, Request request) {
        elevator.addDestination(request.getSourceFloor());
        elevator.setDirection(request.getDirection() == Direction.UP
            ? Direction.UP : Direction.DOWN);
        elevator.setState(request.getSourceFloor() > elevator.getCurrentFloor()
            ? elevator.getMovingUpState() : elevator.getMovingDownState());
    }

    @Override
    public void step(Elevator elevator) {
        // Nothing to do while idle
    }

    @Override
    public String getStateName() { return "IDLE"; }
}

// ---- Moving Up State ----
public class MovingUpState implements ElevatorState {

    @Override
    public void handleInternalRequest(Elevator elevator, int targetFloor) {
        if (targetFloor > elevator.getCurrentFloor()) {
            elevator.addDestination(targetFloor); // In direction — add to queue
        } else {
            elevator.addDestination(targetFloor); // Will be served on downward pass
        }
    }

    @Override
    public void handleExternalRequest(Elevator elevator, Request request) {
        elevator.addDestination(request.getSourceFloor());
    }

    @Override
    public void step(Elevator elevator) {
        int next = elevator.peekNextDestination();
        if (next == -1) {
            elevator.setDirection(Direction.IDLE);
            elevator.setState(elevator.getIdleState());
            return;
        }

        if (next > elevator.getCurrentFloor()) {
            elevator.setCurrentFloor(elevator.getCurrentFloor() + 1);
        }

        if (elevator.getCurrentFloor() == next) {
            elevator.pollNextDestination();
            elevator.setState(elevator.getDoorOpenState());
            elevator.getDoorOpenState().onEnter(elevator);
        } else if (next < elevator.getCurrentFloor()) {
            elevator.setDirection(Direction.DOWN);
            elevator.setState(elevator.getMovingDownState());
        }
    }

    @Override
    public String getStateName() { return "MOVING_UP"; }
}

// ---- Moving Down State ---- (symmetric to MovingUp, omitted for brevity)

// ---- Door Open State ----
public class DoorOpenState implements ElevatorState {

    private int ticksRemaining = 3; // Simulate door open for 3 ticks

    public void onEnter(Elevator elevator) {
        ticksRemaining = 3;
        elevator.setDoorState(DoorState.OPEN);
        System.out.println("Elevator " + elevator.getId()
            + " door opened at floor " + elevator.getCurrentFloor());
    }

    @Override
    public void handleInternalRequest(Elevator elevator, int targetFloor) {
        elevator.addDestination(targetFloor); // Queue while door is open
    }

    @Override
    public void handleExternalRequest(Elevator elevator, Request request) {
        elevator.addDestination(request.getSourceFloor());
    }

    @Override
    public void step(Elevator elevator) {
        ticksRemaining--;
        if (ticksRemaining <= 0) {
            elevator.setDoorState(DoorState.CLOSED);
            System.out.println("Elevator " + elevator.getId()
                + " door closed at floor " + elevator.getCurrentFloor());

            int next = elevator.peekNextDestination();
            if (next == -1) {
                elevator.setDirection(Direction.IDLE);
                elevator.setState(elevator.getIdleState());
            } else if (next > elevator.getCurrentFloor()) {
                elevator.setDirection(Direction.UP);
                elevator.setState(elevator.getMovingUpState());
            } else {
                elevator.setDirection(Direction.DOWN);
                elevator.setState(elevator.getMovingDownState());
            }
        }
    }

    @Override
    public String getStateName() { return "DOOR_OPEN"; }
}
```

### 8.4 Elevator Class

```java
import java.util.*;
import java.util.concurrent.PriorityQueue;

public class Elevator {

    private final int id;
    private int currentFloor;
    private Direction direction;
    private DoorState doorState;
    private final int capacity;
    private int currentLoad;
    private boolean emergency;

    // Separate priority queues for up and down passes (SCAN algorithm)
    private final TreeSet<Integer> upQueue;    // Ascending: serve smallest-first going up
    private final TreeSet<Integer> downQueue;  // Descending: serve largest-first going down

    // State objects (created once, reused)
    private final ElevatorState idleState = new IdleState();
    private final ElevatorState movingUpState = new MovingUpState();
    private final ElevatorState movingDownState = new MovingDownState();
    private final DoorOpenState doorOpenState = new DoorOpenState();

    private ElevatorState currentState;

    public Elevator(int id, int initialFloor, int capacity) {
        this.id = id;
        this.currentFloor = initialFloor;
        this.direction = Direction.IDLE;
        this.doorState = DoorState.CLOSED;
        this.capacity = capacity;
        this.currentLoad = 0;
        this.upQueue = new TreeSet<>();
        this.downQueue = new TreeSet<>(Comparator.reverseOrder());
        this.currentState = idleState;
    }

    public synchronized void addDestination(int floor) {
        if (floor > currentFloor || direction == Direction.UP) {
            upQueue.add(floor);
        } else {
            downQueue.add(floor);
        }
    }

    public synchronized int peekNextDestination() {
        if (direction == Direction.UP || direction == Direction.IDLE) {
            if (!upQueue.isEmpty()) return upQueue.first();
            if (!downQueue.isEmpty()) return downQueue.first(); // Switch direction
        } else {
            if (!downQueue.isEmpty()) return downQueue.first();
            if (!upQueue.isEmpty()) return upQueue.first();   // Switch direction
        }
        return -1; // No destinations
    }

    public synchronized int pollNextDestination() {
        if (direction == Direction.UP || direction == Direction.IDLE) {
            if (!upQueue.isEmpty()) return upQueue.pollFirst();
            return downQueue.isEmpty() ? -1 : downQueue.pollFirst();
        } else {
            if (!downQueue.isEmpty()) return downQueue.pollFirst();
            return upQueue.isEmpty() ? -1 : upQueue.pollFirst();
        }
    }

    public synchronized void step() {
        if (emergency) return;
        currentState.step(this);
    }

    public synchronized void triggerEmergency() {
        this.emergency = true;
        this.doorState = DoorState.OPEN;
        System.out.println("EMERGENCY: Elevator " + id + " stopped at floor " + currentFloor);
    }

    // Score: used by scheduler to rank this elevator for a given request
    // Lower is better. In SCAN: distance to pickup, considering direction match
    public int score(Request request) {
        int distance = Math.abs(currentFloor - request.getSourceFloor());
        boolean directionMatch = direction == request.getDirection()
            || direction == Direction.IDLE;

        // Penalize if elevator is moving away from request
        boolean movingAway = (direction == Direction.UP
                              && request.getSourceFloor() < currentFloor)
                          || (direction == Direction.DOWN
                              && request.getSourceFloor() > currentFloor);

        if (emergency) return Integer.MAX_VALUE;
        if (currentLoad >= capacity) return Integer.MAX_VALUE;
        if (movingAway) return distance + 100; // High penalty for wrong direction
        if (directionMatch) return distance;
        return distance + 50;
    }

    // Getters and setters
    public int getId() { return id; }
    public int getCurrentFloor() { return currentFloor; }
    public void setCurrentFloor(int floor) { this.currentFloor = floor; }
    public Direction getDirection() { return direction; }
    public void setDirection(Direction direction) { this.direction = direction; }
    public DoorState getDoorState() { return doorState; }
    public void setDoorState(DoorState doorState) { this.doorState = doorState; }
    public ElevatorState getState() { return currentState; }
    public void setState(ElevatorState state) { this.currentState = state; }
    public ElevatorState getIdleState() { return idleState; }
    public ElevatorState getMovingUpState() { return movingUpState; }
    public ElevatorState getMovingDownState() { return movingDownState; }
    public DoorOpenState getDoorOpenState() { return doorOpenState; }
    public boolean isEmpty() {
        return upQueue.isEmpty() && downQueue.isEmpty()
               && currentState.getStateName().equals("IDLE");
    }

    @Override
    public String toString() {
        return String.format("Elevator[id=%d, floor=%d, dir=%s, state=%s, load=%d/%d]",
            id, currentFloor, direction, currentState.getStateName(), currentLoad, capacity);
    }
}
```

### 8.5 ElevatorScheduler Interface and SCAN Scheduler

```java
public interface ElevatorScheduler {
    Elevator selectElevator(List<Elevator> elevators, Request request);
}

public class SCANScheduler implements ElevatorScheduler {

    @Override
    public Elevator selectElevator(List<Elevator> elevators, Request request) {
        return elevators.stream()
            .min(Comparator.comparingInt(e -> e.score(request)))
            .orElseThrow(() -> new IllegalStateException("No elevators available"));
    }
}
```

### 8.6 ElevatorController

```java
import java.util.*;
import java.util.concurrent.*;

public class ElevatorController {

    private final List<Elevator> elevators;
    private final ElevatorScheduler scheduler;
    private final BlockingQueue<Request> pendingRequests;
    private volatile boolean running = true;

    public ElevatorController(List<Elevator> elevators, ElevatorScheduler scheduler) {
        this.elevators = elevators;
        this.scheduler = scheduler;
        this.pendingRequests = new LinkedBlockingQueue<>();
    }

    // Called by floor panels (external requests)
    public void requestElevator(int floor, Direction direction) {
        pendingRequests.offer(Request.external(floor, direction));
    }

    // Called by elevator panels (internal requests)
    public void requestFloor(Elevator elevator, int targetFloor) {
        elevator.addDestination(targetFloor);
    }

    // Main dispatch loop — runs on a dedicated thread
    public void dispatchLoop() {
        while (running) {
            try {
                Request request = pendingRequests.poll(100, TimeUnit.MILLISECONDS);
                if (request != null) {
                    Elevator chosen = scheduler.selectElevator(elevators, request);
                    chosen.addDestination(request.getSourceFloor());
                    System.out.println("Dispatched " + request + " to " + chosen);
                }

                // Advance simulation one tick for all elevators
                elevators.forEach(Elevator::step);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    public void triggerEmergency() {
        running = false;
        elevators.forEach(Elevator::triggerEmergency);
    }

    public void shutdown() {
        running = false;
    }

    public List<Elevator> getElevators() { return Collections.unmodifiableList(elevators); }
}
```

---

## 9. The Scheduling Algorithm in Detail

### Why FCFS (First-Come First-Served) is Wrong

Consider a building with 10 floors and 1 elevator at floor 5. If requests arrive in this order:
- Request: floor 3 UP
- Request: floor 7 UP
- Request: floor 1 UP
- Request: floor 9 UP

FCFS sends the elevator: 5 → 3 → 7 → 1 → 9. Total travel: 2 + 4 + 6 + 8 = **20 floors**.

### SCAN Algorithm (Elevator Algorithm)

SCAN operates like a scanner head:
1. Move in the current direction, stopping at every requested floor along the way
2. When there are no more requests in that direction, reverse direction
3. Sweep back, serving all requests in the new direction

Same requests with SCAN (elevator at floor 5, direction UP):
- Serve 7, then 9 (continue up)
- Reverse: serve 3, then 1

Travel: 5 → 7 → 9 → 3 → 1 = 4 + 2 + 6 + 2 = **14 floors**. 30% fewer floor traversals.

### LOOK Algorithm (Variant — Better in Practice)

LOOK is SCAN without the endpoint scan: the elevator only reverses direction when there are no more requests in the current direction, rather than at the top/bottom floor.

```
LOOK behavior:
- Going UP: serve all floors above current, then reverse (do not go to top floor if nothing is there)
- Going DOWN: serve all floors below current, then reverse
```

In the implementation above, `TreeSet<Integer> upQueue` and `downQueue` implement LOOK naturally: the elevator reverses when its current-direction queue empties, regardless of which floor that happens to be.

### Handling Concurrent Scheduling

The scheduler runs `selectElevator` for every new external request. Concurrent requests can both select the same elevator before it has had a chance to update its load. In a full implementation:
- Use the elevator's `score()` method which checks current load vs. capacity
- Optionally use optimistic assignment with a confirmation step

---

## 10. Edge Cases

### Two Requests at the Same Floor — One UP, One DOWN

The elevator arrives at floor 5. There is a person who wants to go UP and a person who wants to go DOWN. Standard behavior: the elevator opens its door, **both board**, but only serves the one going in the elevator's current direction. The other person boards but waits until the elevator reverses direction. Implementation: the elevator services the destination floors, grouped by direction, using the two-queue model.

### Elevator Stuck Between Floors

This is a hardware failure. At the software level:
- Set a `stuck` flag on the `Elevator` object
- Remove the elevator from the scheduler's pool (`score()` returns `MAX_VALUE`)
- Alert operations (log, send notification)
- Redistribute pending requests to other elevators

### Overweight / Door Won't Close

Detect via a weight sensor (simulated as `currentLoad >= capacity`). Behavior:
- Door stays open (`DoorOpenState` does not transition to closed)
- Audio alert triggered
- `step()` method does not advance to MovingUp/MovingDown until load drops below capacity

```java
// In DoorOpenState.step()
if (elevator.getCurrentLoad() >= elevator.getCapacity()) {
    triggerOverloadAlert(elevator);
    return; // Do not close door — stay in DoorOpenState
}
```

### Emergency Stop

`triggerEmergency()` is called on `ElevatorController` (could be from a fire panel, network signal, or in-elevator button):
1. Stop accepting new requests (`running = false` in dispatch loop)
2. All elevators stop movement immediately (set `emergency = true`)
3. Doors open at current floor (or next safe floor if between floors)
4. All elevators removed from service until manually reset

---

## 11. Extension Points

### Add Express Elevators

Add an `elevatorType: ElevatorType` field (STANDARD, EXPRESS, SERVICE). Update `ElevatorScheduler` to route requests to eligible elevators only. Express elevators have a non-contiguous floor set; modify `addDestination` to reject floors not in the allowed set.

### Add VIP Floors

Add `priority: RequestPriority` to `Request` (NORMAL, VIP). Update `SCANScheduler` to always assign the nearest idle elevator to VIP requests, bypassing the normal scoring. VIP floors could also have reserved elevators that only respond to VIP requests.

### Add Energy-Saving Mode

Add `EnergySavingScheduler implements ElevatorScheduler`. In this mode:
- When multiple elevators are idle, park them at optimal floors (evenly distributed) rather than leaving them at the last-used floor
- Prefer elevators already in motion over waking an idle elevator if the moving elevator can pick up the request with low additional cost
- Turn off lights and reduce ventilation when idle (modeled as a state transition to `StandbyState`)

---

## 12. Interview Follow-Up Questions

### Q1: "Your `ElevatorState` objects like `MovingUpState` appear to be stateless — could they be singletons?"

Yes. `MovingUpState` and `MovingDownState` carry no instance-level state (all state lives in `Elevator`). They can safely be shared across all `Elevator` instances as singletons. `DoorOpenState` is the exception — it holds `ticksRemaining`, which is per-elevator state. To make `DoorOpenState` a singleton, move `ticksRemaining` into `Elevator` and pass it to `step()`.

### Q2: "What happens if all elevators are at capacity when a request comes in?"

The scheduler returns the elevator with the lowest score that is not full. If all are full, the request stays in the `pendingRequests` queue. On the next dispatch tick, the scheduler retries. An escalation mechanism (wait timeout → alert) can be added: if a request has been waiting longer than N seconds, log an alert and raise priority.

### Q3: "How would you scale this to a real building management system where elevators communicate over a network?"

Replace `Elevator` with a network-aware proxy: the `ElevatorController` sends commands (target floor, emergency stop) over a message bus. Each physical elevator controller subscribes to its own command topic and publishes status events (current floor, load, door state). `ElevatorController` maintains a local shadow state updated by events. The `ElevatorScheduler` operates on the shadow state. This is the standard architecture for real elevator management systems (BACnet, Modbus over IP).

### Q4: "Why did you choose `TreeSet` over `PriorityQueue` for the floor queues?"

`TreeSet` supports both `first()` (minimum) and efficient removal of arbitrary elements (when a request is cancelled). `PriorityQueue` does not support O(log n) arbitrary removal. Since floor numbers are unique per direction in the LOOK algorithm, `TreeSet<Integer>` is a natural fit and eliminates duplicate requests for the same floor automatically.

### Q5: "How would you test the SCAN scheduler without running a full simulation?"

Unit test the scheduler in isolation by constructing `Elevator` objects with known state (current floor, direction, load) and asserting that `selectElevator` returns the correct elevator for a given request. Property-based tests: for any set of requests and elevator positions, verify that no request is permanently ignored and that average wait time with SCAN is not worse than FCFS across a random simulation. The `Strategy` pattern makes this straightforward — swap in a `NearestCarScheduler` and compare metrics.
