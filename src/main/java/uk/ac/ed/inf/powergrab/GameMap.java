package uk.ac.ed.inf.powergrab;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import com.google.gson.JsonPrimitive;
import com.mapbox.geojson.*;
import org.json.simple.JSONObject;

public class GameMap {
	String[] date;
	URL mapUrl;
	FeatureCollection features;
	Random rnd;
	
	public GameMap(String[] date, Random rnd) throws IOException {
		this.date = date;
		this.rnd = rnd;
		String mapString = String.format("http://homepages.inf.ed.ac.uk/stg/powergrab/%s/%s/%s/powergrabmap.geojson", date[2], date[1], date[0]);
		try {
	        this.mapUrl = new URL(mapString);
	    } catch (IOException e) {
	        throw new RuntimeException(e);
	    }
		HttpURLConnection conn = (HttpURLConnection) this.mapUrl.openConnection();
		conn.setReadTimeout(10000); // milliseconds
		conn.setConnectTimeout(15000); // milliseconds
		conn.setRequestMethod("GET");
		conn.setDoInput(true);
		conn.connect();
		String mapSource = new BufferedReader(new InputStreamReader(conn.getInputStream())).lines().collect(Collectors.joining("\n"));
		this.features = FeatureCollection.fromJson(mapSource);
	}
	
	public void add_flight_path(Position[] path) {
		List<Point> coords = new ArrayList<Point>();
		for(int i=0;i<path.length;i++) {
			Point p = Point.fromLngLat(path[i].longitude, path[i].latitude);
			coords.add(p);
		}
		LineString line = LineString.fromLngLats(coords);
		Feature feature = Feature.fromGeometry(line);
		this.features.features().add(feature);
	}
	
	public double calculate_distance(Position p1, Position p2) {
		return Math.hypot(p1.latitude-p2.latitude, p1.longitude-p2.longitude);
	}
	
	public Direction get_random_direction(int count, Direction[] directions) {
		int index = rnd.nextInt(count);
		return directions[index];
	}
	
	public HashMap<Direction, Position> get_possible_positions(Position position) {
		HashMap<Direction, Position> positions = new HashMap<Direction,Position>();
		for (Direction d : Direction.values()) { 
			if(position.nextPosition(d).inPlayArea()) {
				positions.put(d, position.nextPosition(d));
			}
		}
		return positions;
	}
	
	public void update_station(String station_id, double coins, double power){
		for(Feature f : features.features()){
			if(f.getProperty("id").getAsString().equals(station_id)){
				f.addNumberProperty("coins", f.getProperty("coins").getAsDouble() - coins);
				f.addNumberProperty("power", f.getProperty("power").getAsDouble() - power);
				break;
			}
		}
	}

	public Direction get_direction_from_angle(double angle){
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
