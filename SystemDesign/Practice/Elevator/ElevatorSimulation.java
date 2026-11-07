package SystemDesign.Practice.Elevator;

import SystemDesign.Practice.Elevator.types.RequestType;

public class ElevatorSimulation {

    public static void main(String[] args) {

        ElevatorController controller = new ElevatorController();

        System.out.println("=== Simulation Started ===");

        // User at floor 3 wants to go UP
        controller.requestElevator(3, RequestType.PICKUP_UP);

        for (int tick = 1; tick <= 25; tick++) {

            System.out.println("\n===== TICK " + tick + " =====");

            /*
             * Simulate passenger entering elevator and
             * pressing destination button.
             */

            if (tick == 4) {
                Elevator elevator = controller.getElevator(0);
                elevator.selectFloor(8);

                System.out.println("Passenger entered Elevator-0 and selected floor 8");
            }

            if (tick == 8) {
                controller.requestElevator(6, RequestType.PICKUP_DOWN);
            }

            if (tick == 12) {
                Elevator elevator = controller.getElevator(1);
                elevator.selectFloor(1);

                System.out.println("Passenger entered Elevator-1 and selected floor 1");
            }

            controller.step();

            controller.printStatus();
        }
    }
}