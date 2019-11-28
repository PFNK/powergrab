package uk.ac.ed.inf.powergrab;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import com.mapbox.geojson.*;

/**
 * <h1>
 *     Map of a Game represented by FeatureCollection.
 * </h1>
 * <p>
 *     Provides access to FeatureCollection that includes
 *     all stations on the map, where drone in moving.
 *     It also provides usefull methods that are used to
 *     manipulate the features or extract some information
 *     from them.
 * </p>
 */
public class GameStateMap {
	String[] date;
	URL mapUrl;
	FeatureCollection features;
	Random random;
	
	public GameStateMap(String[] date, Random rnd) throws IOException {
		this.date = date;
		this.random = rnd;
		String mapString = String.format("http://homepages.inf.ed.ac.uk/stg/powergrab/%s/%s/%s/powergrabmap.geojson", date[2], date[1], date[0]);
		try {
	        this.mapUrl = new URL(mapString);
	    } catch (IOException e) {
	        throw new RuntimeException(e);
	    }
		HttpURLConnection conn = (HttpURLConnection) this.mapUrl.openConnection();
		conn.setReadTimeout(10000);
		conn.setConnectTimeout(15000);
		conn.setRequestMethod("GET");
		conn.setDoInput(true);
		conn.connect();
		String mapSource = new BufferedReader(new InputStreamReader(conn.getInputStream())).lines().collect(Collectors.joining("\n"));
		this.features = FeatureCollection.fromJson(mapSource);
	}

	/**
	 * <p>
	 *     This method is used after the drone finishes its movement,
	 *     when the game is finished.
	 *     It is called to add the previous positions (path) that
	 *     drone has been to, to the FeatureCollection features, so
	 *     for example, it is possible to use GEOJSON visualiser
	 *     to see actual path of a drone.
	 * </p>
	 *
	 * @param path array of drone's previous positions
	 */
	public void addFlightPath(Position[] path) {
		List<Point> coords = new ArrayList<Point>();
		for(int i=0;i<path.length;i++) {
			Point p = Point.fromLngLat(path[i].longitude, path[i].latitude);
			coords.add(p);
		}
		LineString line = LineString.fromLngLats(coords);
		Feature feature = Feature.fromGeometry(line);
		this.features.features().add(feature);
	}

	/**
	 * <p>
	 *     Simple method used to calculate the distance between
	 *     two positions
	 * </p>
	 * @param p1 first Position
	 * @param p2 second Position
	 * @return distance double between given two Position parameters
	 */
	public double calculateDistance(Position p1, Position p2) {
		return Math.hypot(p1.latitude-p2.latitude, p1.longitude-p2.longitude);
	}

	/**
	 * <p>
	 *     This method return a random Direction from the given array of
	 *     Directions using only first <code> count </code> elements.
	 * </p>
	 * @param count number of elements of given directions to choose random Direction from
	 * @param directions array of Directions from which to choose the random one
	 * @return the random Direction from given
	 */
	public Direction getRandomDirection(int count, Direction[] directions) {
		int index = random.nextInt(count);
		return directions[index];
	}

	/**
	 * <p>
	 *     A method that finds all the posible Positions where the drone is
	 *     able to move to, checking only if it is in the play area of a map.
	 * </p>
	 * @param position the position from which the drone moves
	 * @return HashMap where key is the Direction which drone can move to and
	 * 		   the corresponding value is the Position where move will be if
	 * 		   it moves using its key (Direction).
	 */
	public HashMap<Direction, Position> getPossiblePositions(Position position) {
		HashMap<Direction, Position> possiblePositions = new HashMap<Direction,Position>();
		for (Direction d : Direction.values()) { 
			if(position.nextPosition(d).inPlayArea()) {
				possiblePositions.put(d, position.nextPosition(d));
			}
		}
		return possiblePositions;
	}

	/**
	 * <p>
	 *     This method updates fields of Feature in FeatureCollection features
	 *     which represent the map of current play area.
	 *     Method updates coins and power which drone takes or gives to the station.
	 * </p>
	 * @param stationId id of a station that was in range of a drone
	 *                   Each Feature that corresponds to the station is
	 *                   uniquely represented by its id
	 * @param coins current amount of coins that drone has, in case of station
	 *              being red, the coins are given to the station
	 * @param power current amount of power of the drone, used for same reason as coins
	 */
	public void updateStation(String stationId, double coins, double power){
		for(Feature f : features.features()){
			if(f.getProperty("id").getAsString().equals(stationId)){
				//take all positive coins/power
				if(f.getProperty("marker-symbol").getAsString().equals("lighthouse")) {
					f.addNumberProperty("coins", 0);
					f.addNumberProperty("power", 0);
					break;
				}
				//add drone's coins/power to negative ones
				f.addNumberProperty("coins", f.getProperty("coins").getAsDouble() + coins);
				f.addNumberProperty("power", f.getProperty("power").getAsDouble() + power);
				break;
			}
		}
	}

	/**
	 * <p>
	 *     This method transforms a Direction to the angle which it represents.
	 *     Starting at East which is equal to 0 angle but since there are only
	 *     16 different directions, each Direction has its own range of angles
	 *     which all belongs to the same Direction.
	 *     Since there is 360 degrees and 16 directions, each direction covers
	 *     22.5 degrees and so each range is 22.5 located around the "true"
	 *     degree of that direction.
	 *     For example East corresponds to 0 angle, however because of the
	 *     range East lies between 0 + 11.25 and 0 - 11.25, which means
	 *     11.25 and 348.75 degrees.
	 * </p>
	 * @param angle angle which lies in a range of some Direction
	 * @return Direction that represents given angle
	 */
	public Direction getDirectionFromAngle(double angle){
		if(angle < 0){
			angle += 360;
		}

	    if(angle <= 11.25 || angle > 348.75){
	        return Direction.E;
        }
        if(angle <= 33.75 && angle > 11.25){
            return Direction.ENE;
        }
        if(angle <= 56.25 && angle > 33.75){
            return Direction.NE;
        }
        if(angle <= 78.75 && angle > 56.25){
            return Direction.NNE;
        }
        if(angle <= 101.25 && angle > 78.75){
            return Direction.N;
        }
        if(angle <= 123.75 && angle > 101.25){
            return Direction.NNW;
        }
        if(angle <= 146.25 && angle > 123.75){
            return Direction.NW;
        }
        if(angle <= 168.75 && angle > 146.25){
            return Direction.WNW;
        }
        if(angle <= 191.25 && angle > 168.75){
            return Direction.W;
        }
        if(angle <= 213.75 && angle > 191.25){
            return Direction.WSW;
        }
        if(angle <= 236.25 && angle > 213.75){
            return Direction.SW;
        }
        if(angle <= 258.75 && angle > 236.25){
            return Direction.SSW;
        }
        if(angle <= 281.25 && angle > 258.75){
            return Direction.S;
        }
        if(angle <= 303.75 && angle > 281.25){
            return Direction.SSE;
        }
        if(angle <= 326.25 && angle > 303.75){
            return Direction.SE;
        }
        if(angle <= 348.75 && angle > 326.25) {
            return Direction.ESE;
        }
        else return null;
    }
}
