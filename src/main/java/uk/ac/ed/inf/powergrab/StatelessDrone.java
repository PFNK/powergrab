package uk.ac.ed.inf.powergrab;

/**
 * <h1>Stateless Drone - limited, memoryless version of the drone</h1>
 * <p1>
 *     Stateless drone only knows is current position on a map and can
 *     only look one step ahead without remembering its previous moves.
 *
 *     Its decision of the next move can only be based on information
 *     about the charging stations which are within range of the
 *     sixteen positions where the drone can be after one move.
 *
 *     Drone is trying to move towards charging stations with positive
 *     value, while avoiding charging stations with negative value
 *     if possible.
 * </p1>
 */
public class StatelessDrone extends Drone {
	StatelessMapController mapController;

	public StatelessDrone(Position initialPosition, StatelessMapController mapController) {
		super(initialPosition);
		this.mapController = mapController;
	}

	/**
	 * <p>
	 *     This method is called for each move of a drone.
	 *     Firstly it gets the optimal direction from
	 *     the its StatelessMapController which scans
	 *     the map for the drone.
	 *     After the move, drone updates its fields,
	 *     as well as its fields that are stored in
	 *     StatelessMapController.
	 * </p>
	 */
	public void move() {
		Direction directionToMove = mapController.getDirectionToMove();
		position = position.nextPosition(directionToMove);
		mapController.position = position;
		mapController.previousPositions.add(position);

		coins += mapController.lastCollectedCoins;
		power += mapController.lastCollectedPower;
		movesCount++;
		power -= 1.25;

		mapController.dronesPower = power;
		mapController.dronesCoins = coins;
	}
}
