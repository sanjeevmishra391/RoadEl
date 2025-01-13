package SystemDesign.Examples.ParkingLot;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ParkingLotManager {

    class Pair {
        Level level;
        Spot spot;
        Pair(Level level, Spot spot) {
            this.level = level;
            this.spot = spot;
        }
    }

    private List<Level> levels;

    public ParkingLotManager(List<Level> levels) {
        this.levels = levels;
    }
    

    public Map<Level, List<Spot>> getEmptySpots(VehicleType vehicleType) {
        Map<Level, List<Spot>> availableSpotsMap = new HashMap<>();
        for(Level level : levels) {
            List<Spot> spotsAvaList = level.getEmSpotsForSpecificVehicleType(vehicleType);
            if(spotsAvaList.size() !=0)
                availableSpotsMap.put(level, spotsAvaList);
        }
        return availableSpotsMap;
    }

    public Pair bookSpot(Vehicle vehicle) throws Exception {
        Map<Level, List<Spot>> availableSpotsMap = getEmptySpots(vehicle.getType());

        if (availableSpotsMap.isEmpty()) {
            throw new Exception("No spots available for " + vehicle.getType());
        }

        for (Map.Entry<Level, List<Spot>> entry : availableSpotsMap.entrySet()) {
            Level allocatedLevel = entry.getKey();
            Spot allocatedSpot = entry.getValue().get(0);
            allocatedSpot.setFilled(true);
            return new Pair(allocatedLevel, allocatedSpot);
        }

        throw new Exception("Unable to allocate spot for " + vehicle.getNumber());
    }

    public void releaseSpot(Level level, Spot spot) {
        spot.setFilled(false);
    }
}
