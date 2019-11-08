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

public class Map {
	String[] date;
	URL mapUrl;
	FeatureCollection features;
	Random rnd;
	
	public Map(String[] date, Random rnd) throws IOException {
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
	
//	FeatureCollection.fromJson(mapSource) returns a FeatureCollection.
//	• If fc is a FeatureCollection then fc.features() is a list of Feature objects.
//	• If f is a Feature then f.geometry() is a Geometry object.
//	• If g is a Geometry object, it may also be a Point.
//	• If p is a Point, then p.coordinates() is a list of double precision numbers.
//	• If f is a Feature with a property coins, then
//	f.getProperty(“coins”) is a JsonElement.
//	• We can convert a JsonElement using methods such as getAsString() and getAsFloat().
	
	
}
