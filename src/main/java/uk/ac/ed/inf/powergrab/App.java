package uk.ac.ed.inf.powergrab;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Random;


public class App 
{
    public static void main(String[] args) throws IOException {
    	String[] date = {args[0], args[1], args[2]};
    	Position initial_position = new Position(Double.parseDouble(args[3]),Double.parseDouble(args[4]));		
    	int seed = Integer.parseInt(args[5]);
    	Random rnd = new Random(seed);
    	String type = args[6];
		GameMap gameMap = new GameMap(date,rnd);
		String file = String.format("%s-%s-%s-%s.txt", type, date[0], date[1], date[2]);
		String file_json = String.format("%s-%s-%s-%s.geojson", type, date[0], date[1], date[2]);

		PrintWriter writer = new PrintWriter(file, "UTF-8");
		if(type.equals("stateless")) {
			StatelessMapController mapC = new StatelessMapController(gameMap, initial_position);
	    	StatelessDrone drone = new StatelessDrone(initial_position,mapC);
	    	while(drone.power > 1.25 && drone.moves < 250) {
	    		Position prev = drone.position;
	    		drone.move();
	    		writer.format("%f, %f, %s, %f, %f, %f, %f \n", prev.latitude, prev.longitude, mapC.last_dir.name(), drone.position.latitude, drone.position.longitude, drone.coins, drone.power);
	    		System.out.printf("Current location: (%f,%f), Coins: %f, Power: %f, moved here by going: %s \n", drone.position.latitude, drone.position.longitude, drone.coins, drone.power, mapC.last_dir.name());
	    	}
			mapC.add_path();
			String json = gameMap.features.toJson();
			FileWriter path_writer = new FileWriter(file_json);
			path_writer.write(json);
			path_writer.flush();
			writer.close();
		}


		if(type.equals("stateful")) {
			StatefulDrone drone = new StatefulDrone(initial_position, gameMap, file);
			while(drone.power > 0 && drone.moves < 250) {
				if(drone.plan.size() == 0) break;
				Position prev = drone.position;
				drone.move();
			}
			drone.add_path();
			String json = gameMap.features.toJson();
			FileWriter path_writer = new FileWriter(file_json);
			path_writer.write(json);
			path_writer.flush();
			drone.writer.close();
		}
    }
}
