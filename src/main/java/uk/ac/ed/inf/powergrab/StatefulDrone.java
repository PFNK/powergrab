package uk.ac.ed.inf.powergrab;

import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.Geometry;
import com.mapbox.geojson.Point;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.*;


public class StatefulDrone extends Drone {
    GameMap gameMap;
    Queue<Feature> plan;
    ArrayList<Position> path;
    Direction last_dir;
    PrintWriter writer;


    public StatefulDrone(Position initial_position, GameMap gameMap, String file) throws FileNotFoundException, UnsupportedEncodingException {
        super(initial_position);
        this.plan = new LinkedList<>();
        this.gameMap = gameMap;
        path = new ArrayList<Position>();
        last_dir = null;
        find_plan();
        this.writer = new PrintWriter(file, "UTF-8");
    }

    public void move() {
        if(plan.size() == 0){
            move_randomly();
            return;
        }
        Feature target = plan.peek();
        Point p = (Point) target.geometry();
        Position target_pos = new Position(p.coordinates().get(1), p.coordinates().get(0));
        double distance = gameMap.calculate_distance(position, target_pos);

        while(distance > 0.00025){
            Direction dir = get_direction(target);
            boolean is_safe = check_safety(dir);
            while (!is_safe) {
                if(power<=1.25) break;
                move_to_avoid_red(dir);
                is_safe = check_safety(dir);
            }
            if(power<=1.25) break;
            Position prev = position;
            power -= 1.25;
            last_dir = dir;
            write_move(prev);
            position = position.nextPosition(dir);
            path.add(position);
            distance = gameMap.calculate_distance(position, target_pos);
            moves++;

        }

        gameMap.update_station(target.getProperty("id").getAsString(), coins, power);
        coins += target.getProperty("coins").getAsDouble();
        power += target.getProperty("power").getAsDouble();
        plan.remove();
    }

    private boolean check_safety(Direction d){
        Position next_pos = position.nextPosition(d);
        Feature in_range = null;
        for(Feature f : gameMap.features.features()){
            if(!f.type().equals("Point")) continue;
            Point p = (Point) f.geometry();
            Position station_pos = new Position(p.coordinates().get(1), p.coordinates().get(0));
            double distance = gameMap.calculate_distance(next_pos, station_pos);
            if(distance <= 0.00025 && f.getProperty("marker-symbol").getAsString().equals("danger")){
                in_range = f;
            }
        }
        if(in_range == null){
            return true;
        }
        return false;
    }

    private void move_to_avoid_red(Direction d){
        //want to go in d direction - but its not safe - avoid by going to left/right
        if(power<=1.25) return;
        double right_side_angle = d.to_anticlock_angle() + 90;
        double left_side_angle = d.to_anticlock_angle() - 90;
        Direction move_to_r_side = gameMap.get_direction_from_angle(right_side_angle);
        Direction move_to_l_side = gameMap.get_direction_from_angle(left_side_angle);

        boolean right_side = check_safety(move_to_r_side);
        if(right_side){
            if(position.nextPosition(move_to_r_side).inPlayArea()) {
                Position prev = position;
                moves++;
                power -= 1.25;
                write_move(prev);
                position = position.nextPosition(move_to_r_side);
                path.add(position);
                //check for potential greens here?
                return;
            }
        }
        boolean left_side = check_safety(move_to_l_side);
        if(left_side){
            if(position.nextPosition(move_to_r_side).inPlayArea()) {
                Position prev = position;
                moves++;
                power -= 1.25;
                write_move(prev);
                position = position.nextPosition(move_to_l_side);
                path.add(position);
                //check for potential greens here?
                return;
            }
        }
        move_to_avoid_red(move_to_r_side);
    }

    public void find_plan(){
//        FeatureCollection stations = FeatureCollection.fromFeatures(gameMap.features.features());
        List<Feature> b = new ArrayList<Feature>(gameMap.features.features());
        FeatureCollection stations = FeatureCollection.fromFeatures(b);

        Position pos = position;
        while(true){
            Feature closest_station = find_closest_green_station(stations, pos);
            if(closest_station == null){
                break;
            }
            plan.add(closest_station);
            stations.features().remove(closest_station);
            Point p = (Point) closest_station.geometry();
            pos = new Position(p.coordinates().get(1), p.coordinates().get(0));


        }
    }

    private Feature find_closest_green_station(FeatureCollection features, Position pos){
        Feature closest_green_station = null;
        double closest_distance = 100000;
        for (Feature f: features.features()) { //
            Geometry g = f.geometry();
            if (g.type().equals("Point") && f.getProperty("marker-symbol").getAsString().equals("lighthouse")){
                Point p = (Point) g;
                Position station = new Position(p.coordinates().get(1), p.coordinates().get(0));
                double distance = gameMap.calculate_distance(pos, station);
                if(distance < closest_distance){
                    closest_distance = distance;
                    closest_green_station = f;
                }
            }
        }
        return closest_green_station;
    }

    private Direction get_direction(Feature target){
        Point p = (Point) target.geometry();
        double target_x = p.coordinates().get(0); //longitude = x
        double target_y = p.coordinates().get(1); //latitude = y
        double angle = Math.toDegrees(Math.atan2(target_y - position.latitude, target_x - position.longitude));
        Direction dir = gameMap.get_direction_from_angle(angle);
        return dir;
    }


    public void add_path(){
        Position[] arr = new Position[path.size()];
        gameMap.add_flight_path(path.toArray(arr));
    }

    public void write_move(Position prev){
        writer.format("%f, %f, %s, %f, %f, %f, %f \n", prev.latitude, prev.longitude, last_dir.name(), position.latitude, position.longitude, coins, power);
        System.out.printf("Current location: (%f,%f), Coins: %f, Power: %f, moved here by going: %s \n", position.latitude, position.longitude, coins, power, last_dir.name());
    }

    private void move_randomly(){
        if(moves > 250 || power < 1.25) return;
        HashMap<Direction, Position> next_positions = gameMap.get_possible_positions(position);
        Direction rand_dir = gameMap.get_random_direction(next_positions.keySet().size(),next_positions.keySet().toArray(new Direction[next_positions.keySet().size()]));
        if(check_safety(rand_dir)){
            Position prev = position;
            moves++;
            power -= 1.25;
            write_move(prev);
            position = position.nextPosition(rand_dir);
            path.add(position);
        }
        move_randomly();
    }
}
