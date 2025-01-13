package SystemDesign.Examples.ParkingLot;

public class Spot {
    private String id;
    private boolean filled;
    private VehicleType allowedVehicleType;

    Spot(String id, VehicleType allowedVehicleType, boolean filled) {
        this.id = id;
        this.allowedVehicleType = allowedVehicleType;
        this.filled = filled;
    }

    public String getId() {
        return id;
    }

    public boolean isFilled() {
        return filled;
    }

    public void setFilled(boolean filled) {
        this.filled = filled;
    }

    public VehicleType getAllowedVehicleType() {
        return allowedVehicleType;
    }

    public void setAllowedVehicleType(VehicleType allowedVehicleType) {
        this.allowedVehicleType = allowedVehicleType;
    }

    @Override
    public String toString() {
        return "{ Id: " + id + " , Allowed Type : " + allowedVehicleType + " , Availability : " + (filled ? "Filled" : "Empty") + " }";
    }
}
