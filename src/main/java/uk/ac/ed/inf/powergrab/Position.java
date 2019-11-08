package uk.ac.ed.inf.powergrab;

public class Position {
	public double latitude;
	public double longitude;

	public Position(double latitude, double longitude) {
		this.latitude = latitude;
		this.longitude = longitude;
	}

	public Position nextPosition(Direction direction) {
		double updatedLongitude = this.longitude +  direction.longitude();
		double updatedLatitude = this.latitude + direction.latitude();
		return new Position(updatedLatitude, updatedLongitude);
	}

	public boolean inPlayArea() {
		if (latitude > 55.942617 && latitude < 55.946233 && longitude < -3.184319 && longitude > -3.192473) {
			return true;
		}
		return false;
	}
}

