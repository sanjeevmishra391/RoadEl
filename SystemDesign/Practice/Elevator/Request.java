package SystemDesign.Practice.Elevator;

import java.util.Objects;

import SystemDesign.Practice.Elevator.types.RequestType;

public class Request {

    private int floor;
    private RequestType type;

    public Request(int floor, RequestType requestType) {
        this.floor = floor;
        this.type = requestType;
    }

    public int getFloor() {
        return this.floor;
    }

    public RequestType getRequestType() {
        return this.type;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof Request)) {
            return false;
        }

        Request other = (Request) obj;

        return floor == other.floor && type == other.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(floor, type);
    }

    @Override
    public String toString() {
        return "Request{" + "floor=" + floor + ", type=" + type +'}';
    }
}
