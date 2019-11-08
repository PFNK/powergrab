package uk.ac.ed.inf.powergrab;

public enum Direction {
	N(0, 0.0003), 
	NNE(0.0003 * Math.cos(Math.toRadians(22.5)), 0.0003 * Math.sin(Math.toRadians(22.5))), 
	NE(0.0003 * Math.cos(Math.toRadians(45)), 0.0003 * Math.sin(Math.toRadians(45))),
	ENE(0.0003 * Math.cos(Math.toRadians(67.5)), 0.0003 * Math.sin(Math.toRadians(67.5))), 
	E(0.0003,0),
	ESE(0.0003 * Math.cos(Math.toRadians(22.5)), -0.0003 * Math.sin(Math.toRadians(22.5))), 
	SE(0.0003 * Math.cos(Math.toRadians(45)), -0.0003 * Math.sin(Math.toRadians(45))), 
	SSE(0.0003 * Math.cos(Math.toRadians(67.5)), -0.0003 * Math.sin(Math.toRadians(67.5))), 
	S(0,-0.0003),
	SSW(-0.0003 * Math.cos(Math.toRadians(22.5)), -0.0003 * Math.sin(Math.toRadians(22.5))),
	SW(-0.0003 * Math.cos(Math.toRadians(45)), -0.0003 * Math.sin(Math.toRadians(45))), 
	WSW(-0.0003 * Math.cos(Math.toRadians(67.5)), -0.0003 * Math.sin(Math.toRadians(67.5))), 
	W(-0.0003, 0), 
	WNW(-0.0003 * Math.cos(Math.toRadians(22.5)), 0.0003 * Math.sin(Math.toRadians(22.5))), 
	NW(-0.0003 * Math.cos(Math.toRadians(45)), 0.0003 * Math.sin(Math.toRadians(45))), 
	NNW(-0.0003 * Math.cos(Math.toRadians(67.5)), 0.0003 * Math.sin(Math.toRadians(67.5)));
// use these when finding direction to move somewhere 
	private final double latitude;
	private final double longitude;

	Direction(double longitude,double latitude) {
		this.longitude = longitude;
		this.latitude = latitude;
	}

	public double latitude() {
		return latitude;
	}
	
	public double longitude() {
		return longitude;
	}
}