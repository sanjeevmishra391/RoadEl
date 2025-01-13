package SystemDesign.Examples.ParkingLot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import SystemDesign.Examples.ParkingLot.ParkingLotManager.Pair;

public class ParkingLot {

    private final ParkingLotManager manager;
    private final Scanner sc;
    private HashMap<String, Spot> allocatedSpaceMap;


    public ParkingLot(ParkingLotManager manager, Scanner sc) {
        this.manager = manager;
        this.sc = sc;
        allocatedSpaceMap = new HashMap<>();
    }

    public void displayMenu() {
        System.out.println("\nChoose any option");
        System.out.println("1. Check available parking spots");
        System.out.println("2. Park your vehicle");
        System.out.println("3. Take away");
        System.out.println("4. Check vehicle parking spot");
        System.out.println("5. Exit");
        System.out.print(":: ");
    }

    public VehicleType getUserVehicleType() {
        System.out.println("\nChoose the vehicle type");
        System.out.print("1. Car\n2. Bike\n3. Truck\n:: ");
        int type = sc.nextInt();
        return switch (type) {
            case 1 -> VehicleType.CAR;
            case 2 -> VehicleType.BIKE;
            case 3 -> VehicleType.TRUCK;
            default -> throw new IllegalArgumentException("Invalid vehicle type");
        };
    }

    public void start() {
        int option;
        do {
            displayMenu();
            option = sc.nextInt();
            try {
                switch (option) {
                    case 1 -> checkAvailableSpots();
                    case 2 -> parkVehicle();
                    case 3 -> releaseVehicle();
                    case 4 -> checkParkingSpot();
                    case 5 -> System.out.println("Exiting...");
                    default -> System.out.println("Invalid option");
                }
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
            }
        } while (option != 5);
    }

    private void checkAvailableSpots() {
        VehicleType type = getUserVehicleType();
        Map<Level, List<Spot>> availableSpots = manager.getEmptySpots(type);
        if (availableSpots.isEmpty()) {
            System.out.println("No spots available for " + type);
        } else {
            System.out.println("Available spots for " + type + ":");
            availableSpots.forEach((level, spots) -> {
                System.out.println(level);
                spots.forEach(System.out::println);
            });
        }
    }

    private void parkVehicle() throws Exception {
        VehicleType type = getUserVehicleType();
        System.out.print("Enter vehicle number: ");
        sc.nextLine(); // Consume newline
        String number = sc.nextLine();
        Vehicle vehicle = new Vehicle(number, type);
        Pair allocatedSpot = manager.bookSpot(vehicle);
        allocatedSpaceMap.put(vehicle.getNumber(), allocatedSpot.spot);
        System.out.println("Vehicle allocated at " + allocatedSpot.level + ", Spot: " + allocatedSpot.spot);
    }

    private void releaseVehicle() {
        System.out.println("Enter the vehicle number to release");
        sc.nextLine();
        String vehicleNumber = sc.nextLine();
        if(!allocatedSpaceMap.containsKey(vehicleNumber)) {
            System.out.println("This vehicle is not parked.");
            return;
        }

        Spot allocatedSpace = allocatedSpaceMap.get(vehicleNumber);
        allocatedSpace.setFilled(false);
        System.out.println("Vehicle " + vehicleNumber + " released from Spot " + allocatedSpace.getId());
    }

    private void checkParkingSpot() {
        System.out.println("Enter the vehicle number to find the spot");
        sc.nextLine();
        String vehicleNumber = sc.nextLine();
        if(!allocatedSpaceMap.containsKey(vehicleNumber)) {
            System.out.println("This vehicle is not parked.");
            return;
        }

        System.out.println("Vehicle " + vehicleNumber + " is parked at spot " + allocatedSpaceMap.get(vehicleNumber).getId());
    }
    static List<Level> initialise(int totalLevels, int spotsInEachLevel[], VehicleType vehicleTypeAllowedInSpots[][]) {
        List<Level> levels = new ArrayList<>();
        for(int i=0; i<totalLevels; i++) {
            String levelId = "l" + i;
            Level level = new Level(levelId);
            for(int s =0; s<spotsInEachLevel[i]; s++) {
                String spotId = levelId + "s" + s;
                level.addSpot(new Spot(spotId, vehicleTypeAllowedInSpots[i][s], false));
            }
            levels.add(level);
        }

        return levels;
    }
    
    public static void main(String[] args) {

        List<Level> levels = initialise(3, 
                            new int[]{2, 5, 4}, 
                            new VehicleType[][] {
                                    { VehicleType.CAR, VehicleType.BIKE},
                                    { VehicleType.TRUCK, VehicleType.CAR, VehicleType.CAR, VehicleType.BIKE, VehicleType.TRUCK},
                                    { VehicleType.CAR, VehicleType.BIKE, VehicleType.BIKE, VehicleType.CAR}
                                });
        
        ParkingLot pl = new ParkingLot(new ParkingLotManager(levels), new Scanner(System.in));

        pl.start();
    }
}
