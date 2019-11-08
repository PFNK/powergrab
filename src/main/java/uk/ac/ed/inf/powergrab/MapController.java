package uk.ac.ed.inf.powergrab;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.Map.Entry;

import com.mapbox.geojson.Feature;
import com.mapbox.geojson.Geometry;
import com.mapbox.geojson.Point;

public class MapController {

	private Map map;
	Position position;
	ArrayList<Position> path;
	double closest_coins = 0.0;
	double closest_power = 0.0;
	Direction last_dir;

	
	public MapController(Map map, Position position) {
		this.map = map;
		this.position = position;
		path = new ArrayList<Position>();
		last_dir = null;
	}
	
	public Direction get_direction() { 
		// store only closest red, closest green and the rest 
		HashMap<Direction, Position> possible_positions = map.get_possible_positions(position);
		HashMap<String, String> closest_stations = new HashMap<String,String>();
		
		HashMap<String, ArrayList<Direction>> motions = new HashMap<String,ArrayList<Direction>>();
		double min_green_dist = 100000;
		double min_red_dist = 100000;
		Direction min_green_dir = null;
		Direction min_red_dir = null;
		ArrayList<Direction> safe_dir = new ArrayList<Direction>();
		HashMap<String,Double> coins_energy = new HashMap<String,Double>();
		
        // iterate through all possible directions
		for(Entry<Direction, Position> next_position: possible_positions.entrySet()) { // n<=16						
			// iterate though all features
			for (Feature f: map.features.features()){ // m=50
				Geometry g = f.geometry();
				if(g.type().equals("Point")){
					
					Point p = (Point) g;
					Position station = new Position(p.coordinates().get(1),p.coordinates().get(0));
					double distance = map.calculate_distance(next_position.getValue(), station);
				
					if(distance > 0.00025) { //skip unreachable ones - include bad directions
						if(!safe_dir.contains(next_position.getKey())){
							safe_dir.add(next_position.getKey());
						}
						continue;
					}
				
					if(f.getProperty("marker-symbol").getAsString().equals("lighthouse")) {
						
						if(distance < min_green_dist) {
							if(f.getProperty("coins").getAsDouble() < 5){
								continue;
							}
							min_green_dir = next_position.getKey();
							min_green_dist = distance;
							coins_energy.put("green_coins", f.getProperty("coins").getAsDouble());
							coins_energy.put("green_power", f.getProperty("power").getAsDouble());
							closest_stations.put("green_station",f.getProperty("id").getAsString());
						}
					}
					else{
						
						if(distance < min_red_dist) {
							min_red_dir = next_position.getKey();
							min_red_dist = distance;
							coins_energy.put("red_coins", f.getProperty("coins").getAsDouble());
							coins_energy.put("red_power", f.getProperty("power").getAsDouble());
							closest_stations.put("red_station",f.getProperty("id").getAsString());
						}
					}
						
				}
			}
		}
		
		ArrayList<Direction> gr = new ArrayList<Direction>();
		gr.add(min_green_dir);
		ArrayList<Direction> red = new ArrayList<Direction>();
		red.add(min_red_dir);
		if (min_red_dir != null){
			safe_dir.removeIf(min_red_dir::equals);
		}
		motions.put("green", gr);
		motions.put("red", red);
		motions.put("safe", safe_dir);

		Direction d = choose_closest_station(motions, coins_energy,closest_stations);
		last_dir = d;
		return d;
	}
	
	
	public Direction choose_closest_station(HashMap<String, ArrayList<Direction>> motions, HashMap<String,Double> coins_power, HashMap<String, String> closest_stations) {
		Direction d = null;
		if(motions.get("green").get(0) != null) {
			d = motions.get("green").get(0);
			closest_coins = coins_power.get("green_coins");
			closest_power = coins_power.get("green_power");
			map.update_station(closest_stations.get("green_station"),closest_coins,closest_power);
		}
		else if(motions.get("safe").get(0) != null) {
			d = get_rand_direction(motions.get("safe"));
			closest_coins = 0;
			closest_power = 0;
			if(d == null) {
				d = motions.get("red").get(0);
				closest_coins = coins_power.get("red_coins");
				closest_power = coins_power.get("red_power");
				map.update_station(closest_stations.get("red_station"),closest_coins,closest_power);
			}
		}
		else {
			d = motions.get("red").get(0);
			closest_coins = coins_power.get("red_coins");
			closest_power = coins_power.get("red_power");
			map.update_station(closest_stations.get("red_station"),closest_coins,closest_power);
		}
		return d;	
	}
	

	
	public Direction get_rand_direction(ArrayList<Direction> dirs) {
		if(dirs.isEmpty()) {
			return null;
		}
		Direction[] arr = new Direction[dirs.size()]; 
        arr = dirs.toArray(arr);
		Direction rand_dir = map.get_random_direction(arr.length,arr);
		
		Position next = position.nextPosition(rand_dir);
		if(next.inPlayArea()){
			return rand_dir;
		}
		else {
			//try different random direction, remove this one since we know it is invalid
			dirs.remove(rand_dir);
			return get_rand_direction(dirs);
		}
	}
		
	public void add_path() {
		Position[] arr = new Position[path.size()];
		map.add_flight_path(path.toArray(arr));
	}
}
