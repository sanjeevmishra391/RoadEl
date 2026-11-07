package SystemDesign.Practice.Elevator;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import SystemDesign.Practice.Elevator.types.Direction;
import SystemDesign.Practice.Elevator.types.RequestType;

public class Elevator {
    // it maintains limits - validations
    // it manages requests where it need to stop
    // track position, direction, floors to visit
    Set<Request> requests;
    private int currentFloor;
    private Direction currentDirection;
    private final int FLOOR_LIMIT;

    Elevator(int floorLimit) {
        requests = new HashSet<>();
        this.currentFloor = 0;
        this.currentDirection = Direction.IDLE;
        this.FLOOR_LIMIT = floorLimit;
    }

    public boolean addRequest(Request request) {
        if (request.getFloor() < 0 || request.getFloor() > this.FLOOR_LIMIT) {
            return false;
        }
        if (request.getFloor() == currentFloor) {
            return true;
        }
        if (requests.contains(request)) {
            return false;
        }
        return requests.add(request);
    }

    public void step() {
        // check for requests, reversal, idle or moving, pick and execute

        if (requests.isEmpty()) {
            this.currentDirection = Direction.IDLE;
            return;
        }

        if(currentDirection == Direction.IDLE) {
            this.currentDirection = directionTowardsNearestRequest();
        }

        RequestType type = currentDirection == Direction.UP ? RequestType.PICKUP_UP : RequestType.PICKUP_DOWN;
        Request hallPick = new Request(currentFloor, type);
        Request destination = new Request(currentFloor, RequestType.DESTINATION);
        
        if (requests.contains(hallPick) || requests.contains(destination)) {
            requests.remove(destination);
            requests.remove(hallPick);

            if(requests.isEmpty()) {
                currentDirection = Direction.IDLE;
            }
            return;
        }

        if(!hasRequestsAhead(currentDirection)) {
            this.currentDirection = currentDirection == Direction.UP ? Direction.DOWN : Direction.UP;
        }

        if(this.currentDirection == Direction.UP) {
            currentFloor++;
        } else {
            currentFloor--;
        }
    }

    public int getCurrentFloor() {
        return this.currentFloor;
    }

    public Direction getCurrentDirection() {
        return this.currentDirection;
    }

    public boolean hasRequestsBeyond(int floor, Direction direction) {
        if(direction != this.currentDirection)
            return false;

        for(Request request: requests) {
            if(direction == Direction.UP && request.getFloor() >= floor) {
                if(request.getRequestType() == RequestType.PICKUP_UP || request.getRequestType() == RequestType.DESTINATION) {
                    return true;
                }
            } else if(direction == Direction.DOWN && request.getFloor() <= floor) {
                if(request.getRequestType() == RequestType.PICKUP_DOWN || request.getRequestType() == RequestType.DESTINATION) {
                    return true;
                }
            }
        }

        return false;
    }

    public boolean selectFloor(int floor) {
        Request request = new Request(floor, RequestType.DESTINATION);

        return addRequest(request);
    }

    private Direction directionTowardsNearestRequest() {
        Optional<Request> nearestReq = requests.stream()
            .min(Comparator.comparingInt(r -> Math.abs(r.getFloor() - this.currentFloor)));

        if(nearestReq.isPresent()) {
            return nearestReq.get().getFloor() > this.currentFloor ? Direction.UP : Direction.DOWN;
        }

        return Direction.UP;
    }

    private boolean hasRequestsAhead(Direction direction) {
        for (Request request : requests) {
            if (direction == Direction.UP && request.getFloor() > currentFloor) {
                return true;
            }
            if (direction == Direction.DOWN && request.getFloor() < currentFloor) {
                return true;
            }
        }
        return false;
    }
}
