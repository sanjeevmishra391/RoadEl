package SystemDesign.Examples.ParkingLot;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Level {
    private String id;
    List<Spot> spots;
    
    Level(String id) {
        this.id = id;
        spots = new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    boolean addSpot(Spot spot) {
        try {
            spots.add(spot);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return false;
        }
        return true;
    }

    List<Spot> getEmptySpots() {
        return spots.stream().filter(s -> s.isFilled() == false).collect(Collectors.toList());
    }

    List<Spot> getEmSpotsForSpecificVehicleType(VehicleType type) {
        return spots.stream().filter(s -> s.isFilled() == false).filter(s -> s.getAllowedVehicleType().equals(type)).collect(Collectors.toList());
    }

    @Override
    public String toString() {
        return "Level Id :"+ id;
    }
}
