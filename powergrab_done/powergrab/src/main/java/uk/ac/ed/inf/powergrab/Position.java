package uk.ac.ed.inf.powergrab;

/**
 * <h1>
 * Position represents (latitude, longitude) double which is used
 * to locate drone and stations on the map.
 * </h1>
 */
public class Position {
    public double latitude;
    public double longitude;

    public Position(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    /**
     * <p>
     * This method calculates the position of the drone after it
     * moves in a given Direction.
     * It uses the enum Direction in order to avoid calculating
     * changes in latitude/longitude each time it is called.
     * </p>
     *
     * @param direction Direction of a drone's move for which method
     *                  return corresponding change in latitude and
     *                  longitude
     * @return Position which is a result of moving in a given Direction
     */
    public Position nextPosition(Direction direction) {
        double updatedLongitude = this.longitude + direction.changeOfLongitude();
        double updatedLatitude = this.latitude + direction.changeOfLatitude();
        return new Position(updatedLatitude, updatedLongitude);
    }

    /**
     * <p>
     * This method checks if a Position is in a predefined play area
     * </p>
     *
     * @return boolean where True means that positions lies in play area
     */
    public boolean inPlayArea() {
        return (latitude > 55.942617 && latitude < 55.946233 && longitude < -3.184319 && longitude > -3.192473);
    }
}
