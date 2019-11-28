package uk.ac.ed.inf.powergrab;

import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.Geometry;
import com.mapbox.geojson.Point;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.*;

/**
 * <h1>Stateful Drone - "smart" version of the drone</h1>
 * <p>
 *     Stateful drone by definition has no limitations, it is able to
 *     remember the whole map of play area and any of its previous moves.
 *
 *     It creates a plan before moving and then follows it.
 *     Path consists of green stations only, it avoids red ones while moving.
 *
 *     StatefulDrone extends Drone class which stores additional fields:
 *     coins, power, position and moves
 * </p>

 */
public class StatefulDrone extends Drone {
    /**
     * The whole GameStateMap created by Game.
     */
    GameStateMap gameStateMap;
    /**
     * Drone's game strategy = queue of green stations which StatefulDrone follows.
     */
    Queue<Feature> planToFollow;
    /**
     * Previous positions of the drone, which are later added to the GameStateMap.
     */
    ArrayList<Position> previousPositions;
    /**
     * Last direction that the drone moved.
     */
    Direction lastDirectionUsed;
    /**
     * Writer used to write Drone's moves to the .txt file.
     */
    PrintWriter pathTxtWriter;


    public StatefulDrone(Position initialPosition, GameStateMap gameStateMap, String file) throws FileNotFoundException, UnsupportedEncodingException {
        super(initialPosition);
        this.planToFollow = new LinkedList<>();
        this.gameStateMap = gameStateMap;
        previousPositions = new ArrayList<Position>();
        lastDirectionUsed = null;
        findGamePlan();
        this.pathTxtWriter = new PrintWriter(file, "UTF-8");
    }

    /**
     * <p>
     *     Method used to move the drone -> to play the game with this drone.
     *     It is called in a loop until the drone make 250 moves or run out of power.
     *     Each call of a move method means move to the next station in the queue plan,
     *     so move() runs until the next green station is reached.
     * </p>
     */

    public void move() {
        if(planToFollow.size() == 0){
            moveRandomly();
            return;
        }
        Feature target = planToFollow.peek();
        Point p = (Point) target.geometry();
        Position targetPosition = new Position(p.coordinates().get(1), p.coordinates().get(0));
        double distance = gameStateMap.calculateDistance(position, targetPosition);

        while(distance > 0.00025){
            Direction dir = getDirectionToTarget(target);
            boolean isSafe = checkSafetyOfDirection(dir);
            if(!isSafe) {
                moveToAvoidRedStation(target);
            }
//            if(power<=1.25) break;
            if(!position.nextPosition(dir).inPlayArea()){
                double lessSteepAngle = dir.toAnticlockwiseAngle() - 30;
                dir = gameStateMap.getDirectionFromAngle(lessSteepAngle);
            }
            Position prev = position;
            power -= 1.25;
            lastDirectionUsed = dir;
            writeMoveToFile(prev);
            position = position.nextPosition(dir);
            previousPositions.add(position);
            distance = gameStateMap.calculateDistance(position, targetPosition);
            moves++;

        }

        coins += target.getProperty("coins").getAsDouble();
        power += target.getProperty("power").getAsDouble();

//        take all coins/energy from it
        gameStateMap.updateStation(target.getProperty("id").getAsString(), 0, 0);
        planToFollow.remove();
    }

    /**
     * <p>
     *     This is a method used to check if moving in a given direction
     *     is safe for the drone on not.
     *     Safe means that there isn't any red station reachable at the next
     *     position after moving in the given direction.
     * </p>
     * @param dir the direction which drone wants to know if is safe to move
     * @return boolean which says if the direction is safe or not
     */
    private boolean checkSafetyOfDirection(Direction dir){
        Position nextPosition = position.nextPosition(dir);
        Feature redStationInRange = null;
        double redSmallestDistance = 10;
        double greenSmallestDistance = 10;
        for(Feature f : gameStateMap.features.features()){
            if(!f.geometry().type().equals("Point")) continue;
            Point p = (Point) f.geometry();
            Position stationPosition = new Position(p.coordinates().get(1), p.coordinates().get(0));
            double distance = gameStateMap.calculateDistance(nextPosition, stationPosition);
            if(distance <= 0.00025){
//            get closest red and green
                if(f.getProperty("marker-symbol").getAsString().equals("danger")){
                    if(distance < redSmallestDistance) {
                        redSmallestDistance = distance;
                        redStationInRange = f;
                    }
                }
                else {
                    if (distance < greenSmallestDistance) {
                        greenSmallestDistance = distance;
                    }
                }
            }
        }
        if(redStationInRange == null || greenSmallestDistance < redSmallestDistance){
            return true;
        }
        return false;
    }

    /**
     * <p>
     *     This method is used when the drone can't move in the desired direction.
     *     It is used to avoid the red station that lies in the direction to target station.
     *     To avoid this obstacle, drone moves left or right of the desired direction.
     * </p>
     * @param target Feature corresponding to drone's target = current head of a queue plan
     * @return boolean which says if the direction is safe or not
     */
    private void moveToAvoidRedStation(Feature target){
//        want to go in d direction - but its not safe - avoid by going to left/right
        Direction d = getDirectionToTarget(target);

        if(moveToSidesToAvoid(d,90)) return; // try right direction

        if(moveToSidesToAvoid(d,-90)) return; // try left then

//      go back if previous don't work
        double reverseAngle = (d.toAnticlockwiseAngle() - 180) % 360;
        Direction moveBackwardsDirection = gameStateMap.getDirectionFromAngle(reverseAngle);
        Position previousPosition = position;
        moves++;
        power -= 1.25;
        writeMoveToFile(previousPosition);
        position = position.nextPosition(moveBackwardsDirection);
        previousPositions.add(position);
        moveToAvoidRedStation(target);

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
//            double reverseAngle = (d.to_anticlock_angle() - 180) % 360;
//            Direction moveBackwards = gameStateMap.get_direction_from_angle(reverseAngle);
//            avoid_these_directions.add(d);
//            move_to_avoid_red(target, avoid_these_directions);
//        }
    }

    /**
     * <p>
     *     A method called from move_to_avoid_red method. This method moves
     *     the drone in a direction of a given angle. It is always either right (90)
     *     or left (-90) angle of the desired direction (direction to head of plan)
     *     This method is called recursively until it is safe to move or until the
     *     edge of a play area is reached.
     * </p>
     * @param target desired Direction which drone wants to move, but is not safe
     * @param angle int angle that drone uses to avoid an obstacle, always 90 or -90
     * @return boolean false if this method failed to avoid an obstacle by moving in
     *                 direction of a given angle
     */
    public boolean moveToSidesToAvoid(Direction target, int angle){
        double perpAngleToDesiredAngle = (target.toAnticlockwiseAngle() + angle) % 360;
        Direction moveToPerpSide = gameStateMap.getDirectionFromAngle(perpAngleToDesiredAngle);

        if(!position.nextPosition(moveToPerpSide).inPlayArea() || !checkSafetyOfDirection(moveToPerpSide)){
            return false;
        }

        Position previousPosition = position;
        moves++;
        power -= 1.25;
        position = position.nextPosition(moveToPerpSide);
        writeMoveToFile(previousPosition);
        previousPositions.add(position);
        checkGreenStationsNearby();

        if(checkSafetyOfDirection(target)) return true;
        return moveToSidesToAvoid(target, angle);
    }

    /**
     * <p>
     *     A method used to check if the drone is near some other green stations
     *     to which it moved by avoiding obstacles (red stations) while moving towards
     *     the current head of a plan queue.
     *     If there is such a green station, remove it from the plan queue and collect
     *     its coins and power.
     * </p>
     */
    public void checkGreenStationsNearby(){
        Feature closestGreenStation = findClosestGreenStation(gameStateMap.features, position);
        Point p = (Point) closestGreenStation.geometry();
        Position stationPosition = new Position(p.coordinates().get(1), p.coordinates().get(0));
        double distance = gameStateMap.calculateDistance(position, stationPosition);
        if(distance < 0.00025){
            coins += closestGreenStation.getProperty("coins").getAsDouble();
            power += closestGreenStation.getProperty("power").getAsDouble();
            gameStateMap.updateStation(closestGreenStation.getProperty("id").getAsString(),0,0);
            planToFollow.remove(stationPosition);
        }
    }

    /**
     * <p>
     *     Method used when initializing StatefulDrone.
     *     Current strategy is to firstly, find a closest station to the current
     *     position of a drone, add it to the queue (head of a queue) and after that,
     *     find the closest station to the previously found station.
     *     This creates a queue of Feature that represent the stations
     *     which drone is going to follow when play() is called.
     * </p>
     */
    public void findGamePlan(){
        List<Feature> featureArrayList = new ArrayList<>(gameStateMap.features.features());
        FeatureCollection stations = FeatureCollection.fromFeatures(featureArrayList);
        Position position = this.position;

        while(true){
            Feature closestGreenStation = findClosestGreenStation(stations, position);
            if(closestGreenStation == null){
                break;
            }
            planToFollow.add(closestGreenStation);
            stations.features().remove(closestGreenStation);
            Point p = (Point) closestGreenStation.geometry();
            position = new Position(p.coordinates().get(1), p.coordinates().get(0));
        }
    }

    /**
     * <p>
     *     Method that is called to find the closest green station to the position
     *     given, from the FeatureCollection given as parameters.
     *     It iterates through all the features and simply stores the closest one.
     * </p>
     * @param features FeatureCollection from which it finds the closest one
     * @param pos Position that is used to calculate distances
     * @return Feature that represent the closest green station to the given Position
     */
    private Feature findClosestGreenStation(FeatureCollection features, Position pos){
        Feature closestGreenStation = null;
        double closestDistance = 10000;
        for (Feature feature: features.features()) { //
            Geometry g = feature.geometry();
            if (g.type().equals("Point") && feature.getProperty("marker-symbol").getAsString().equals("lighthouse")){
                Point p = (Point) g;
                Position stationPosition = new Position(p.coordinates().get(1), p.coordinates().get(0));
                double distance = gameStateMap.calculateDistance(pos, stationPosition);
                if(distance < closestDistance){
                    closestDistance = distance;
                    closestGreenStation = feature;
                }
            }
        }
        return closestGreenStation;
    }

    /**
     * <p>
     *     This method is used to find the Direction which leads to the target position.
     *     It uses atan2 in order to calculate angle and from angle if uses GamestateMap
     *     method getDirectionFromAngle which calculates Direction corresponding
     *     to the given angle.
     * </p>
     * @param target Feature that drone wants to find the Direction to
     * @return dir Direction which leads to the target position
     */
    private Direction getDirectionToTarget(Feature target){
        Point p = (Point) target.geometry();
        double targetLongitude = p.coordinates().get(0);
        double targetLatitude = p.coordinates().get(1);
        double angle = Math.toDegrees(Math.atan2(targetLatitude - position.latitude, targetLongitude - position.longitude));
        return gameStateMap.getDirectionFromAngle(angle);
    }

    /**
     * <p>
     *     Method used to send the path of a drone to the GameStateMap.
     *     GameStateMap's addFlightPath method adds the path to its
     *     FeatureCollection features which represent the game map.
     * </p>
     */
    public void addPathToMap(){
        Position[] positions = new Position[previousPositions.size()];
        gameStateMap.addFlightPath(previousPositions.toArray(positions));
    }

    /**
     * <p>
     *     This method is called after each move of a drone.
     *     It uses the fied writer to write its move to the .txt file.
     * </p>
     * @param prev previous Position of a drone which is needed in order
     *             to write required information to the file
     */
    public void writeMoveToFile(Position prev){
        if(this.moves == 250) return; //System.exit(0);
        pathTxtWriter.format("%f, %f, %s, %f, %f, %f, %f \n", prev.latitude, prev.longitude, lastDirectionUsed.name(), position.latitude, position.longitude, coins, power);
        System.out.printf("Current location: (%f,%f), Coins: %f, Power: %f, moved here by going: %s \n", position.latitude, position.longitude, coins, power, lastDirectionUsed.name());
    }

    /**
     * <p>
     *     This method is used when drone empties is plan queue.
     *     Meaning that it reached all the green stations and now
     *     all the is left is to either move until it has moved 250
     *     times or until is runs out of energy.
     *     Goal is to move in random directions, but avoid red stations.
     * </p>
     */
    private void moveRandomly(){
        if(moves > 250 || power < 1.25) return;
        HashMap<Direction, Position> possibleNextPositions = gameStateMap.getPossiblePositions(position);
        Direction randomDirection = gameStateMap.getRandomDirection(possibleNextPositions.keySet().size(), possibleNextPositions.keySet().toArray(new Direction[possibleNextPositions.keySet().size()]));
        if(checkSafetyOfDirection(randomDirection)){
            Position previousPosition = position;
            moves++;
            power -= 1.25;
            position = position.nextPosition(randomDirection);
            writeMoveToFile(previousPosition);
            previousPositions.add(position);
        }
        moveRandomly();
    }
}
