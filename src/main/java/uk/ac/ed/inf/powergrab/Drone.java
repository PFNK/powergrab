package uk.ac.ed.inf.powergrab;

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;

import com.mapbox.geojson.Feature;
import com.mapbox.geojson.Geometry;
import com.mapbox.geojson.Point;


//1. Initialise the drone state (position, seed, type);
//2. Repeat
//	2.1 Inspect the current state of the map, and current position;
//	2.2 Calculate allowable moves of the drone;
//	2.3 Decide in which direction to move;
//	2.4 Move to your next position, update your position;
//	2.5 Charge from the nearest changing station (if in range).
//Until 250 moves, or insufficient energy to move.

public class Drone {
	double coins;
	double power;
	Position position;
	MapController map;
	int moves;
	// stateful with have previous move here as well ? or moves
	// stateless is memoryless - it only knows about stations that are within the range
	
	public Drone(Position initial_position, MapController m) {
		this.position = initial_position;
		this.coins = 0.0;
		this.power = 250.0;
		this.map = m;
		this.moves = 0;
	}
	
	public void move() {
		Direction dir = map.get_direction();
		position = position.nextPosition(dir);
		map.position = position;
		map.path.add(position);
		coins += map.closest_coins;
		power += map.closest_power;
		moves++;
		power -= 1.25;
	}
	
}
