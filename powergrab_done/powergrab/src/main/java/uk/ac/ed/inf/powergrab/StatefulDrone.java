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
 * Stateful drone by definition has no limitations, it is able to
 * remember the whole map of play area and any of its previous moves.
 * <p>
 * It creates a plan before moving and then follows it.
 * Path consists of green stations only, it avoids red ones while moving.
 * <p>
 * StatefulDrone extends Drone class which stores additional fields:
 * coins, power, position and moves
 * </p>
 */
public class StatefulDrone extends Drone {

    GameMap gameMap;
    Queue<Feature> planToFollow;
    ArrayList<Position> previousPositions;
    Direction lastDirectionUsed;
    PrintWriter pathTxtWriter;


    public StatefulDrone(Position initialPosition, GameMap gameMap, String file) throws FileNotFoundException, UnsupportedEncodingException {
        super(initialPosition);
        this.planToFollow = new LinkedList<>();
        this.gameMap = gameMap;
        previousPositions = new ArrayList<>();
        lastDirectionUsed = null;
        findGamePlan();
        this.pathTxtWriter = new PrintWriter(file, "UTF-8");
    }

    /**
     * <p>
     * Method used to move the drone -> to play the game with this drone.
     * It is called in a loop until the drone make 250 moves or run out of power.
     * Each call of a move method means move to the next station in the queue plan,
     * so move() runs until the next green station is reached.
     * </p>
     */

    public void move() {
        if (planToFollow.size() == 0) {
            moveRandomly();
            return;
        }
        Feature target = planToFollow.peek();
        Point p = (Point) target.geometry();
        Position targetPosition = new Position(p.coordinates().get(1), p.coordinates().get(0));
        double distance = gameMap.calculateDistance(position, targetPosition);

        while (distance > 0.00025) {
            Direction dir = getDirectionToTarget(target);
            boolean isSafe = checkSafetyOfDirection(dir);
            if (!isSafe) {
                moveToAvoidRedStation(target);
            }
            if (!position.nextPosition(dir).inPlayArea()) {
                double lessSteepAngle = dir.toAnticlockwiseAngle() - 30;
                dir = gameMap.getDirectionFromAngle(lessSteepAngle);
            }
            Position prev = position;
            power -= 1.25;
            lastDirectionUsed = dir;
            writeMoveToFile(prev);
            position = position.nextPosition(dir);
            previousPositions.add(position);
            distance = gameMap.calculateDistance(position, targetPosition);
            movesCount++;

        }

        coins += target.getProperty("coins").getAsDouble();
        power += target.getProperty("power").getAsDouble();
        gameMap.updateStation(target.getProperty("id").getAsString(), 0, 0);
        planToFollow.remove();
    }

    /**
     * <p>
     * This is a method used to check if moving in a given direction
     * is safe for the drone on not.
     * Safe means that there isn't any red station reachable at the next
     * position after moving in the given direction.
     * </p>
     *
     * @param dir the direction which drone wants to know if is safe to move
     * @return boolean which says if the direction is safe or not
     */
    private boolean checkSafetyOfDirection(Direction dir) {
        Position nextPosition = position.nextPosition(dir);
        Feature redStationInRange = null;
        double redSmallestDistance = 10;
        double greenSmallestDistance = 10;
        for (Feature f : gameMap.features.features()) {
            if (!f.geometry().type().equals("Point")) continue;
            Point p = (Point) f.geometry();
            Position stationPosition = new Position(p.coordinates().get(1), p.coordinates().get(0));
            double distance = gameMap.calculateDistance(nextPosition, stationPosition);
            if (distance <= 0.00025) {
                if (f.getProperty("marker-symbol").getAsString().equals("danger")) {
                    if (distance < redSmallestDistance) {
                        redSmallestDistance = distance;
                        redStationInRange = f;
                    }
                } else {
                    if (distance < greenSmallestDistance) {
                        greenSmallestDistance = distance;
                    }
                }
            }
        }
        if (redStationInRange == null || greenSmallestDistance < redSmallestDistance) {
            return true;
        }
        return false;
    }

    /**
     * <p>
     * This method is used when the drone can't move in the desired direction.
     * It is used to avoid the red station that lies in the direction to target station.
     * To avoid this obstacle, drone moves left or right of the desired direction.
     * </p>
     *
     * @param target Feature corresponding to drone's target = current head of a queue plan
     * @return boolean which says if the direction is safe or not
     */
    private void moveToAvoidRedStation(Feature target) {
        if (power < 1.25) return;
        Direction directionToTarget = getDirectionToTarget(target);

        if (moveToSidesToAvoid(directionToTarget, 45)) return;
        if (moveToSidesToAvoid(directionToTarget, 90)) return;
        if (moveToSidesToAvoid(directionToTarget, 130)) return;

        if (moveToSidesToAvoid(directionToTarget, -45)) return;
        if (moveToSidesToAvoid(directionToTarget, -90)) return;
        if (moveToSidesToAvoid(directionToTarget, -130)) return;

        double reverseAngle = (directionToTarget.toAnticlockwiseAngle() - 180) % 360;
        Direction moveBackwardsDirection = gameMap.getDirectionFromAngle(reverseAngle);
        if (!position.nextPosition(moveBackwardsDirection).inPlayArea()) {
            moveToRedStation(directionToTarget);
            return;
        }
        Position previousPosition = position;
        movesCount++;
        power -= 1.25;
        lastDirectionUsed = moveBackwardsDirection;
        writeMoveToFile(previousPosition);
        position = position.nextPosition(moveBackwardsDirection);
        previousPositions.add(position);
        if (power < 1.25) return;
        moveToAvoidRedStation(target);
    }

    /**
     * <p>
     * A method called from move_to_avoid_red method. This method moves
     * the drone in a direction of a given angle. It is always either right (90)
     * or left (-90) angle of the desired direction (direction to head of plan)
     * This method is called recursively until it is safe to move or until the
     * edge of a play area is reached.
     * </p>
     *
     * @param target desired Direction which drone wants to move, but is not safe
     * @param angle  int angle that drone uses to avoid an obstacle, always 90 or -90
     * @return boolean false if this method failed to avoid an obstacle by moving in
     * direction of a given angle
     */
    private boolean moveToSidesToAvoid(Direction target, int angle) {
        if (checkSafetyOfDirection(target)) return true;
        double perpAngleToDesiredAngle = (target.toAnticlockwiseAngle() + angle) % 360;
        Direction moveToPerpSide = gameMap.getDirectionFromAngle(perpAngleToDesiredAngle);
        if (power < 1.25) return true;
        if (!position.nextPosition(moveToPerpSide).inPlayArea() || !checkSafetyOfDirection(moveToPerpSide)) {
            return false;
        }

        Position previousPosition = position;
        movesCount++;
        power -= 1.25;
        position = position.nextPosition(moveToPerpSide);
        lastDirectionUsed = moveToPerpSide;
        writeMoveToFile(previousPosition);
        previousPositions.add(position);
        checkGreenStationsNearby();

        if (checkSafetyOfDirection(target)) return true;
        return moveToSidesToAvoid(target, angle);
    }

    /**
     * <p>
     * This method is used when drone can't move in desired direction,
     * direction left to desired, direction right to desired and direction
     * opposite to desired.
     * This means that drone is in a corner and in order to move towards
     * the target, it must pass through red station.
     * </p>
     *
     * @param target Direction to the target station
     */
    private void moveToRedStation(Direction target) {
        Position nextPos = position.nextPosition(target);
        Feature closestRedStation = findClosestStation(gameMap.features, nextPos, "red");
        Position previousPosition = position;
        lastDirectionUsed = target;
        movesCount++;
        power -= 1.25;
        position = nextPos;
        writeMoveToFile(previousPosition);
        previousPositions.add(position);

        double stationCoins = closestRedStation.getProperty("coins").getAsDouble();
        double stationPower = closestRedStation.getProperty("power").getAsDouble();
        gameMap.updateStation(closestRedStation.getProperty("id").getAsString(), coins, power);

        if (coins > stationCoins) coins = coins - stationCoins;
        else coins = 0;

        if (power > stationPower) power = power - stationPower;
        else power = 0;
    }

    /**
     * <p>
     * A method used to check if the drone is near some other green stations
     * to which it moved by avoiding obstacles (red stations) while moving towards
     * the current head of a plan queue.
     * If there is such a green station, remove it from the plan queue and collect
     * its coins and power.
     * </p>
     */
    private void checkGreenStationsNearby() {
        Feature closestGreenStation = findClosestStation(gameMap.features, position, "green");
        Point p = (Point) closestGreenStation.geometry();
        Position stationPosition = new Position(p.coordinates().get(1), p.coordinates().get(0));
        double distance = gameMap.calculateDistance(position, stationPosition);

        if (distance < 0.00025) {
            coins += closestGreenStation.getProperty("coins").getAsDouble();
            power += closestGreenStation.getProperty("power").getAsDouble();
            gameMap.updateStation(closestGreenStation.getProperty("id").getAsString(), 0, 0);
            planToFollow.remove(stationPosition);
        }
    }

    /**
     * <p>
     * Method used when initializing StatefulDrone.
     * Current strategy is to firstly, find a closest station to the current
     * position of a drone, add it to the queue (head of a queue) and after that,
     * find the closest station to the previously found station.
     * This creates a queue of Feature that represent the stations
     * which drone is going to follow when play() is called.
     * </p>
     */
    private void findGamePlan() {
        List<Feature> featureArrayList = new ArrayList<>(gameMap.features.features());
        FeatureCollection stations = FeatureCollection.fromFeatures(featureArrayList);
        Position position = this.position;

        while (true) {
            Feature closestGreenStation = findClosestStation(stations, position, "green");
            if (closestGreenStation == null) {
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
     * Method that is called to find the closest green station to the position
     * given, from the FeatureCollection given as parameters.
     * It iterates through all the features and simply stores the closest one.
     * </p>
     *
     * @param features FeatureCollection from which it finds the closest one
     * @param pos      Position that is used to calculate distances
     * @param type     type of station - either green or red
     * @return Feature that represent the closest green station to the given Position
     */
    private Feature findClosestStation(FeatureCollection features, Position pos, String type) {
        if (type == "green") type = "lighthouse";
        else type = "danger";
        Feature closestGreenStation = null;
        double closestDistance = 10000;
        for (Feature feature : features.features()) {
            Geometry g = feature.geometry();
            if (g.type().equals("Point") && feature.getProperty("marker-symbol").getAsString().equals(type)) {
                Point p = (Point) g;
                Position stationPosition = new Position(p.coordinates().get(1), p.coordinates().get(0));
                double distance = gameMap.calculateDistance(pos, stationPosition);
                if (distance < closestDistance) {
                    closestDistance = distance;
                    closestGreenStation = feature;
                }
            }
        }
        return closestGreenStation;
    }

    /**
     * <p>
     * This method is used to find the Direction which leads to the target position.
     * It uses atan2 in order to calculate angle and from angle if uses GameMap
     * method getDirectionFromAngle which calculates Direction corresponding
     * to the given angle.
     * </p>
     *
     * @param target Feature that drone wants to find the Direction to
     * @return dir Direction which leads to the target position
     */
    private Direction getDirectionToTarget(Feature target) {
        Point p = (Point) target.geometry();
        double targetLongitude = p.coordinates().get(0);
        double targetLatitude = p.coordinates().get(1);
        double angle = Math.toDegrees(Math.atan2(targetLatitude - position.latitude, targetLongitude - position.longitude));
        return gameMap.getDirectionFromAngle(angle);
    }

    /**
     * <p>
     * Method used to send the path of a drone to the GameMap.
     * GameMap's addFlightPath method adds the path to its
     * FeatureCollection features which represent the game map.
     * </p>
     */
    public void addPathToMap() {
        Position[] positions = new Position[previousPositions.size()];
        gameMap.addFlightPath(previousPositions.toArray(positions));
    }

    /**
     * <p>
     * This method is called after each move of a drone.
     * It uses the fied writer to write its move to the .txt file.
     * </p>
     *
     * @param prev previous Position of a drone which is needed in order
     *             to write required information to the file
     */
    private void writeMoveToFile(Position prev) {
        if (this.movesCount == 250) return;
        pathTxtWriter.format("%f, %f, %s, %f, %f, %f, %f \n", prev.latitude, prev.longitude, lastDirectionUsed.name(), position.latitude, position.longitude, coins, power);
        System.out.printf("Current location: (%f,%f), Coins: %f, Power: %f, moved here by going: %s \n", position.latitude, position.longitude, coins, power, lastDirectionUsed.name());
    }

    /**
     * <p>
     * This method is used when drone empties is plan queue.
     * Meaning that it reached all the green stations and now
     * all the is left is to either move until it has moved 250
     * times or until is runs out of energy.
     * Goal is to move in random directions, but avoid red stations.
     * </p>
     */
    private void moveRandomly() {
        if (movesCount > 250 || power < 1.25) return;
        HashMap<Direction, Position> possibleNextPositions = gameMap.getPossiblePositions(position);
        Direction randomDirection = gameMap.getRandomDirection(possibleNextPositions.keySet().size(), possibleNextPositions.keySet().toArray(new Direction[possibleNextPositions.keySet().size()]));
        if (checkSafetyOfDirection(randomDirection) && position.nextPosition(randomDirection).inPlayArea()) {
            Position previousPosition = position;
            lastDirectionUsed = randomDirection;
            movesCount++;
            power -= 1.25;
            position = position.nextPosition(randomDirection);
            writeMoveToFile(previousPosition);
            previousPositions.add(position);
        }
        moveRandomly();
    }
}