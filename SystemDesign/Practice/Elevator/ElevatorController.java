package SystemDesign.Practice.Elevator;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import SystemDesign.Practice.Elevator.types.Direction;
import SystemDesign.Practice.Elevator.types.RequestType;

public class ElevatorController {
    // It will receive requests from user -> request (hall stops, up or down), destination (destination)
    // It manages elevators and allocates elevators for each requests
    // Request validations
    // Collections of elevators 

    // List<Elevators> elevators = new ArrayList<>();
    // requestElevator(floor, type) -> boolean
    
    private List<Elevator> elevators;
    private final int FLOORS_LIMIT;

    ElevatorController() {
        this.elevators = new ArrayList<>(3);
        this.FLOORS_LIMIT = 9;
        elevators.add(new Elevator(this.FLOORS_LIMIT));
        elevators.add(new Elevator(this.FLOORS_LIMIT));
        elevators.add(new Elevator(this.FLOORS_LIMIT));
    }

    public boolean requestElevator(int floor, RequestType type) {
        // validate floor
        if(floor<0 || floor > this.FLOORS_LIMIT) {
            return false;
        }
        // validate type
        if(type == RequestType.DESTINATION) {
            return false;
        }
        // create request
        Request request = new Request(floor, type);
        // pick the best elevator
        Elevator bestElevator = pickBestElevator(request);
        // add request to that elevator
        return bestElevator.addRequest(request);
    }

    public void step() {
        for(Elevator e: elevators) {
            e.step();
        }
    }

    private Elevator pickBestElevator(Request request) {
        // among all lifts, check for requests and the lift which is going in same direction and has floors beyond the curr request

        Direction direction = request.getRequestType() == RequestType.PICKUP_UP ? Direction.UP : Direction.DOWN;
        int floor = request.getFloor();

        // 1. Direction aware with requests queue analysis
        // find all elevators moving in same direction
        // check for lift which have requests so that it is going in same direction and has floors beyond the curr request
        // Optional<Elevator> best = pickDirectionAwareElevator(floor, direction);
        // if(best.isPresent()) {
        //     return best.get();
        // }

        // // 2. Pick nearest IDLE
        // best = pickNearestIdle(floor, direction);
        // if(best.isPresent()) {
        //     return best.get();
        // }
        
        // // 3. Pick nearest elevator
        // Elevator fallback = pickNearest(floor).orElseThrow(() -> new NoSuchElementException());
        // return fallback;

        return pickDirectionAwareElevator(floor, direction)
        .or(() -> pickNearestIdle(floor, direction))
        .or(() -> pickNearest(floor))
        .orElseThrow(NoSuchElementException::new);
    }

    private Optional<Elevator> pickDirectionAwareElevator(int floor, Direction direction) {

        Elevator nearestAlongDirection = null;
        int minDist = Integer.MAX_VALUE;

        for(Elevator elevator: elevators) {
            if (elevator.getCurrentDirection() != direction) {
                continue;
            }

            if((direction == Direction.UP && elevator.getCurrentFloor() > floor) || (direction == Direction.DOWN && elevator.getCurrentFloor() < floor)) {
                continue;
            }

            if(elevator.hasRequestsBeyond(floor, direction)) {
                int dist = Math.abs(elevator.getCurrentFloor() - floor);
                if(minDist > dist) {
                    nearestAlongDirection = elevator;
                    minDist = dist;
                }
            }
        }

        return Optional.ofNullable(nearestAlongDirection);
    }

    private Optional<Elevator> pickNearestIdle(int floor, Direction direction) {
        return elevators.stream()
            .filter(e -> e.getCurrentDirection() == Direction.IDLE)
            .min(Comparator.comparingInt(e -> Math.abs(e.getCurrentFloor() - floor)));
    }

    private Optional<Elevator> pickNearest(int floor) {
        return elevators.stream()
            .min(Comparator.comparingInt(e -> Math.abs(e.getCurrentFloor() - floor)));
    }

    public void printStatus() {
        for (int i = 0; i < elevators.size(); i++) {
            Elevator e = elevators.get(i);

            System.out.printf("Elevator-%d | Floor=%d | Direction=%s%n",
                    i + 1, e.getCurrentFloor(), e.getCurrentDirection());
        }
    }

    public Elevator getElevator(int index) {
        return elevators.get(index);
    }
}
