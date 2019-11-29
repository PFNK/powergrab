package uk.ac.ed.inf.powergrab;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import com.mapbox.geojson.Feature;
import com.mapbox.geojson.Geometry;
import com.mapbox.geojson.Point;

/**
 * <h1>
 *     Controller used by StatelessDrone, because it can't access GameStateMap directly.
 * </h1>
 * <p>
 *     StatelessDrone needs to know where to move in order to avoid red stations and
 *     go to green stations if it can reach them in one move.
 *     However, stateless drone can't see the entire map and its features so this
 *     class is here to provide necessary information to the drone without giving
 *     it access to the whole GameStateMap.
 * </p>
 */
public class StatelessMapController {

	private GameStateMap gameStateMap;
	Position position;
	ArrayList<Position> path;
	double lastCollectedCoins;
	double lastCollectedPower;
	Direction lastDirectionUsed;
	double dronesCoins;
	double dronesPower;

	
	public StatelessMapController(GameStateMap gameStateMap, Position position) {
		this.gameStateMap = gameStateMap;
		this.position = position;
		path = new ArrayList<>();
		lastDirectionUsed = null;
		lastCollectedCoins = 0;
		lastCollectedPower = 0;
		dronesCoins = 0;
		dronesPower = 250;
	}

	/**
	 * <p>
	 *     This method is the directly tells StatelessDrone in which direction
	 *     it should move in order to collect most coins.
	 * </p>
	 * <p>
	 *     It goes through all possible moves in which the drone can currently
	 *     move and for each one it finds what are the reachable stations
	 *     from there. It stores only the closest green and red stations over
	 *     all possible move, and also all directions that doesn't lead to
	 *     any station. After that it uses another method to choose from these
	 *     found stations.
	 * </p>
	 * @return suitable Direction which drone uses to move so it goes to the
	 * 		   green stations nearby and avoid red stations nearby
	 */
	public Direction getDirectionToMove() {
		HashMap<Direction, Position> possiblePositions = gameStateMap.getPossiblePositions(position);
		HashMap<String, String> closestStations = new HashMap<>();
		
		HashMap<String, ArrayList<Direction>> directionsToClosestStations = new HashMap<>();
		double minGreenDist = 100000;
		double minRedDist = 100000;
		Direction minGreenDir = null;
		Direction minRedDir = null;
		ArrayList<Direction> safeDirections = new ArrayList<>();
		HashMap<String,Double> closestStationsCoinsPower = new HashMap<>();
		
		for(Entry<Direction, Position> nextPosition: possiblePositions.entrySet()) {
			for (Feature f: gameStateMap.features.features()){
				Geometry g = f.geometry();
				if(g.type().equals("Point")){
					
					Point stationPoint = (Point) g;
					Position stationPosition = new Position(stationPoint.coordinates().get(1),stationPoint.coordinates().get(0));
					double distance = gameStateMap.calculateDistance(nextPosition.getValue(), stationPosition);
				
					if(distance > 0.00025) {
						if(!safeDirections.contains(nextPosition.getKey())){
							safeDirections.add(nextPosition.getKey());
						}
						continue;
					}
				
					if(f.getProperty("marker-symbol").getAsString().equals("lighthouse")) {
						
						if(distance < minGreenDist) {
							if(f.getProperty("coins").getAsDouble() < 5){
								continue;
							}
							minGreenDir = nextPosition.getKey();
							minGreenDist = distance;
							closestStationsCoinsPower.put("greenCoins", f.getProperty("coins").getAsDouble());
							closestStationsCoinsPower.put("greenPower", f.getProperty("power").getAsDouble());
							closestStations.put("greenStation",f.getProperty("id").getAsString());
						}
					}

					else{
						
						if(distance < minRedDist) {
							minRedDir = nextPosition.getKey();
							minRedDist = distance;
							closestStationsCoinsPower.put("redCoins", f.getProperty("coins").getAsDouble());
							closestStationsCoinsPower.put("redPower", f.getProperty("power").getAsDouble());
							closestStations.put("redStation",f.getProperty("id").getAsString());
						}
					}
						
				}
			}
		}
		
		ArrayList<Direction> greenDirections = new ArrayList<>();
		greenDirections.add(minGreenDir);
		ArrayList<Direction> redDirections = new ArrayList<>();
		redDirections.add(minRedDir);
		if (minRedDir != null){
			safeDirections.removeIf(minRedDir::equals);
		}
		directionsToClosestStations.put("green", greenDirections);
		directionsToClosestStations.put("red", redDirections);
		directionsToClosestStations.put("safe", safeDirections);

		Direction optimalDirection = chooseClosestStation(directionsToClosestStations, closestStationsCoinsPower, closestStations);
		lastDirectionUsed = optimalDirection;
		return optimalDirection;
	}

	/**
	 * <p>
	 *     This method chooses which direction is the best one from the given ones.
	 *     If there is a Direction which leads to the green station, this direction
	 *     is always prioritizes this direction.
	 *     If there isn't a "green direction", the one which doesn't lead to any
	 *     station is chosen randomly, and if this fails (either there isn't such
	 *     direction or it leads out of play area), direction that leads to the
	 *     red station is chosen.
	 * </p>
	 * @param directionsToClosestStations HashMap where key is the type of a station/direction (green, safe, red)
	 *                                       and value is a list of directions which belongs to this type
	 *                                       green/red means that these directions leads to the green/red stations,
	 *                                       and safe means that there is no station.
	 * @param closestStationsCoinsPower HashMap where value is the amount of coins or power that each
	 *                                     station from the directionsToClosestStations hashmap has.
	 *                                     For example greenCoins key has the value of coins that green
	 *                                     station in directionsToClosestStations have.
	 * @param closestStations HashMap which stores the ids of the stations that are in
	 *                           the directionsToClosestStations.
	 * @return the best possible direction from the given directionsToClosestStations, depending on what
	 * 		   is in the directionsToClosestStations hashmap
	 */
	public Direction chooseClosestStation(HashMap<String, ArrayList<Direction>> directionsToClosestStations,
										  HashMap<String, Double> closestStationsCoinsPower,
										  HashMap<String, String> closestStations) {
		Direction d = null;
		if(directionsToClosestStations.get("green").get(0) != null) {
			d = directionsToClosestStations.get("green").get(0);
			lastCollectedCoins = closestStationsCoinsPower.get("greenCoins");
			lastCollectedPower = closestStationsCoinsPower.get("greenPower");
			gameStateMap.updateStation(closestStations.get("greenStation"), dronesCoins, dronesPower);
		}
		else if(directionsToClosestStations.get("safe").get(0) != null) {
			d = getRandDirection(directionsToClosestStations.get("safe"));
			lastCollectedCoins = 0;
			lastCollectedPower = 0;
			if(d == null) {
				d = directionsToClosestStations.get("red").get(0);
				lastCollectedCoins = closestStationsCoinsPower.get("redCoins");
				lastCollectedPower = closestStationsCoinsPower.get("redPower");
				gameStateMap.updateStation(closestStations.get("redStation"), dronesCoins, dronesPower);
			}
		}
		else {
			d = directionsToClosestStations.get("red").get(0);
			lastCollectedCoins = closestStationsCoinsPower.get("redCoins");
			lastCollectedPower = closestStationsCoinsPower.get("redPower");
			gameStateMap.updateStation(closestStations.get("redStation"), dronesCoins, dronesPower);
		}
		return d;
	}

	/**
	 * <p>
	 *     This method takes a list of directions and takes random
	 *     element from it using the GameStateMap's method which
	 *     uses the Random instance that was created using the
	 *     seed command-line argument.
	 * </p>
	 * @param directions list of direction from which method chooses random one
	 * @return random Direction from the given list
	 */
	public Direction getRandDirection(ArrayList<Direction> directions) {
		if(directions.isEmpty()) {
			return null;
		}
		Direction[] directionsArray = new Direction[directions.size()];
        directionsArray = directions.toArray(directionsArray);
		Direction randomDirection = gameStateMap.getRandomDirection(directionsArray.length, directionsArray);
		
		Position next = position.nextPosition(randomDirection);
		if(next.inPlayArea()){
			return randomDirection;
		}
		else {
			directions.remove(randomDirection);
			return getRandDirection(directions);
		}
	}

	/**
	 * <p>
	 *     This method is used to add the path of the drone to
	 *     the FeatureCollection represented by GameStateMap.
	 *     It is called when drone has done its last move and
	 *     game is about to finish.
	 * </p>
	 */
	public void addPath() {
		Position[] positions = new Position[path.size()];
		gameStateMap.addFlightPath(path.toArray(positions));
	}
}