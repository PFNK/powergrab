package uk.ac.ed.inf.powergrab;


//1. Initialise the drone state (position, seed, type);
//2. Repeat
//	2.1 Inspect the current state of the map, and current position;
//	2.2 Calculate allowable moves of the drone;
//	2.3 Decide in which direction to move;
//	2.4 Move to your next position, update your position;
//	2.5 Charge from the nearest changing station (if in range).
//Until 250 moves, or insufficient energy to move.

public class StatelessDrone extends Drone {
	StatelessMapController map;
	// stateless is memoryless - it only knows about stations that are within the range
	
	public StatelessDrone(Position initial_position, StatelessMapController m) {
		super(initial_position);
		this.map = m;
	}
	
	public void move() {
		Direction next_dir = map.get_direction();
		position = position.nextPosition(next_dir);
		map.position = position;
		map.path.add(position);

		coins += map.closest_coins;
		power += map.closest_power;

		moves++;
		power -= 1.25;

		map.drone_energy = power;
		map.drone_coins = coins;
	}

	
}
