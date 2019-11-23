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
    GameStateMap gameStateMap;
    Queue<Feature> plan;
    ArrayList<Position> path;
    Direction last_dir;
    PrintWriter writer;


    public StatefulDrone(Position initial_position, GameStateMap gameStateMap, String file) throws FileNotFoundException, UnsupportedEncodingException {
        super(initial_position);
        this.plan = new LinkedList<>();
        this.gameStateMap = gameStateMap;
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
        double distance = gameStateMap.calculate_distance(position, target_pos);

        while(distance > 0.00025){
            Direction dir = get_direction_to_target(target);
            boolean is_safe = check_safety(dir);
            if(!is_safe) {
                move_to_avoid_red(target, new ArrayList<Direction>());
            }
//            if(power<=1.25) break;
            if(!position.nextPosition(dir).inPlayArea()){
                double less_steeper_angle = dir.to_anticlock_angle() - 30;
                dir = gameStateMap.get_direction_from_angle(less_steeper_angle);
            }
            Position prev = position;
            power -= 1.25;
            last_dir = dir;
            write_move(prev);
            position = position.nextPosition(dir);
            path.add(position);
            distance = gameStateMap.calculate_distance(position, target_pos);
            moves++;

        }

        coins += target.getProperty("coins").getAsDouble();
        power += target.getProperty("power").getAsDouble();

//        take all coins/energy from it
        gameStateMap.update_station(target.getProperty("id").getAsString(), 0, 0);
        plan.remove();
    }

    private boolean check_safety(Direction d){
        Position next_pos = position.nextPosition(d);
        Feature red_in_range = null;
        Feature green_in_range = null;
        double red_dist = 10;
        double green_dist = 10;
        for(Feature f : gameStateMap.features.features()){
            if(!f.geometry().type().equals("Point")) continue;
            Point p = (Point) f.geometry();
            Position station_pos = new Position(p.coordinates().get(1), p.coordinates().get(0));
            double distance = gameStateMap.calculate_distance(next_pos, station_pos);
            if(distance <= 0.00025){
//            get closest red and green
                if(f.getProperty("marker-symbol").getAsString().equals("danger")){
                    if(distance < red_dist) {
                        red_dist = distance;
                        red_in_range = f;
                    }
                }
                else {
                    if (distance < green_dist) {
                        green_dist = distance;
                        green_in_range = f;
                    }
                }
            }
        }
        if(red_in_range == null || green_dist < red_dist){
            return true;
        }
        return false;
    }

    private void move_to_avoid_red(Feature target, ArrayList<Direction> avoid_these_directions){
//        want to go in d direction - but its not safe - avoid by going to left/right
        Direction d = get_direction_to_target(target);

        if(avoid_red(d,90)) return; // try right direction

        if(avoid_red(d,-90)) return; // try left then

//      go back if previous don't work
        double backward_angle = (d.to_anticlock_angle() - 180) % 360;
        Direction move_backwards = gameStateMap.get_direction_from_angle(backward_angle);
        Position prev = position;
        moves++;
        power -= 1.25;
        write_move(prev);
        position = position.nextPosition(move_backwards);
        path.add(position);
        move_to_avoid_red(target, avoid_these_directions);

//
////        if(directions_to_move != null) {
////            for (Direction next_dir : directions_to_move) {
////                final_next_position = final_next_position.nextPosition(next_dir);
////            }
////        }
//
//        if(right_side && position.nextPosition(move_to_r_side).inPlayArea() && !avoid_these_directions.contains(move_to_r_side)){
//            Position prev = position;
//            moves++;
//            power -= 1.25;
//            write_move(prev);
//            position = position.nextPosition(move_to_r_side);
//            path.add(position);
//            check_greens();
//            Direction desired = get_direction_to_target(target);
//            if(check_safety(desired)) return;
//            avoid_these_directions.add(gameStateMap.get_direction_from_angle((move_to_r_side.to_anticlock_angle() - 180) % 360 ));
//            move_to_avoid_red(target, avoid_these_directions);
//        }
//
//        boolean left_side = check_safety(move_to_l_side);
//        if(left_side && position.nextPosition(move_to_l_side).inPlayArea() && !avoid_these_directions.contains(move_to_l_side)){
//            Position prev = position;
//            moves++;
//            power -= 1.25;
//            write_move(prev);
//            position = position.nextPosition(move_to_l_side);
//            path.add(position);
//            check_greens();
//            Direction desired = get_direction_to_target(target);
//            if(check_safety(desired)) return;
//            avoid_these_directions.add(gameStateMap.get_direction_from_angle((move_to_l_side.to_anticlock_angle() - 180) % 360 ));
//            move_to_avoid_red(target, avoid_these_directions);
//        }

//        if(position.nextPosition(move_to_r_side).inPlayArea()){
//            directions_to_move.add(move_to_r_side);
//            move_to_avoid_red(d, directions_to_move);
//        }
//        else if(position.nextPosition(move_to_l_side).inPlayArea()){
//            directions_to_move.add(move_to_l_side);
//            move_to_avoid_red(d,directions_to_move);
//        }
////        go back
//        else{
//            double backward_angle = (d.to_anticlock_angle() - 180) % 360;
//            Direction move_backwards = gameStateMap.get_direction_from_angle(backward_angle);
//            avoid_these_directions.add(d);
//            move_to_avoid_red(target, avoid_these_directions);
//        }
    }

    public boolean avoid_red(Direction target, int angle){
        double right_side_angle = (target.to_anticlock_angle() + angle) % 360;
        Direction move_to_r_side = gameStateMap.get_direction_from_angle(right_side_angle);

//         if out of area of not safe, move left
        if(!position.nextPosition(move_to_r_side).inPlayArea() || !check_safety(move_to_r_side)){
            return false;
        }

        Position prev = position;
        moves++;
        power -= 1.25;
        write_move(prev);
        position = position.nextPosition(move_to_r_side);
        path.add(position);
        check_greens();

        if(check_safety(target)) return true;

        return avoid_red(target, angle);
    }


    public void check_greens(){
        Feature closest_green = find_closest_green_station(gameStateMap.features, position);
        Point p = (Point) closest_green.geometry();
        Position station = new Position(p.coordinates().get(1), p.coordinates().get(0));
        double distance = gameStateMap.calculate_distance(position, station);
        if(distance < 0.00025){
            coins += closest_green.getProperty("coins").getAsDouble();
            power += closest_green.getProperty("power").getAsDouble();
            gameStateMap.update_station(closest_green.getProperty("id").getAsString(),0,0);
            plan.remove(station);
        }
    }


    public void find_plan(){
//        FeatureCollection stations = FeatureCollection.fromFeatures(gameMap.features.features());
        List<Feature> b = new ArrayList<Feature>(gameStateMap.features.features());
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
                double distance = gameStateMap.calculate_distance(pos, station);
                if(distance < closest_distance){
                    closest_distance = distance;
                    closest_green_station = f;
                }
            }
        }
        return closest_green_station;
    }

    private Direction get_direction_to_target(Feature target){
        Point p = (Point) target.geometry();
        double target_x = p.coordinates().get(0); //longitude = x
        double target_y = p.coordinates().get(1); //latitude = y
        double angle = Math.toDegrees(Math.atan2(target_y - position.latitude, target_x - position.longitude));
        Direction dir = gameStateMap.get_direction_from_angle(angle);
        return dir;
    }


    public void add_path(){
        Position[] arr = new Position[path.size()];
        gameStateMap.add_flight_path(path.toArray(arr));
    }

    public void write_move(Position prev){
        if(this.moves == 250) return; //System.exit(0);
        writer.format("%f, %f, %s, %f, %f, %f, %f \n", prev.latitude, prev.longitude, last_dir.name(), position.latitude, position.longitude, coins, power);
        System.out.printf("Current location: (%f,%f), Coins: %f, Power: %f, moved here by going: %s \n", position.latitude, position.longitude, coins, power, last_dir.name());
    }

    private void move_randomly(){
        if(moves > 250 || power < 1.25) return;
        HashMap<Direction, Position> next_positions = gameStateMap.get_possible_positions(position);
        Direction rand_dir = gameStateMap.get_random_direction(next_positions.keySet().size(), next_positions.keySet().toArray(new Direction[next_positions.keySet().size()]));
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
