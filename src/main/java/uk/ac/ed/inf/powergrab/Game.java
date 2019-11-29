package uk.ac.ed.inf.powergrab;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Random;

/**
 * <h1>PowerGrab Game</h1>
 * The Game program implements an application that allows user
 * to play PowerGrab game.
 * Given console arguments, it creates the Game instance based on them.
 * Game implements AutoCloseable interface which is used to close all files
 * that are being used here.
 * <p>
 * <b>Note:</b> Assumes that console arguments are given and are correct.
 */

public class Game implements AutoCloseable {
    private String[] date;
    private Position initialPosition;
    private String type;
    private Random rnd;
    private GameStateMap gameStateMap;
    private FileWriter pathGeojsonWriter;
    private PrintWriter pathTxtWriter;

    public Game(String[] args) throws IOException {
        date = new String[]{args[0], args[1], args[2]};
        initialPosition = new Position(Double.parseDouble(args[3]),Double.parseDouble(args[4]));
        int seed = Integer.parseInt(args[5]);
        rnd = new Random(seed);
        type = args[6];
        gameStateMap = new GameStateMap(date, rnd);
        String file = String.format("%s-%s-%s-%s.txt", type, date[0], date[1], date[2]);
        String fileJson = String.format("%s-%s-%s-%s.geojson", type, date[0], date[1], date[2]);
        pathGeojsonWriter = new FileWriter(fileJson);
        pathTxtWriter = new PrintWriter(file, "UTF-8");
    }

    /**
     * <p>
     *     Method used to close the writers that are used to fill .txt and
     *     .geojson file while the game is running
     * </p>
     * @throws Exception
     * @see Exception
     */
    @Override
    public void close() throws Exception {
        pathTxtWriter.close();
        pathGeojsonWriter.close();
    }

    /**
     * <p>
     *     Method that starts the game and runs until the game is finished:
     *     either 250 moves are made or drone run out of power.
     *     This method depends on the type of drone.
     * </p>
     * @throws IOException
     * @see IOException
     */
    public void play() throws IOException {
        if(type.equals("stateless")) {
            StatelessMapController mapC = new StatelessMapController(gameStateMap, initialPosition);
            StatelessDrone drone = new StatelessDrone(initialPosition, mapC);
            while(drone.power > 1.25 && drone.movesCount < 250) {
                Position prev = drone.position;
                drone.move();
                pathTxtWriter.format("%f, %f, %s, %f, %f, %f, %f \n", prev.latitude, prev.longitude, mapC.lastDirectionUsed.name(), drone.position.latitude, drone.position.longitude, drone.coins, drone.power);
                System.out.printf("Current location: (%f,%f), Coins: %f, Power: %f, moved here by going: %s \n", drone.position.latitude, drone.position.longitude, drone.coins, drone.power, mapC.lastDirectionUsed.name());
            }
            mapC.addPath();
            String json = gameStateMap.features.toJson();
            pathGeojsonWriter.write(json);
            pathGeojsonWriter.flush();
            return;
        }

        if(type.equals("stateful")) {
            String file = String.format("%s-%s-%s-%s.txt", type, date[0], date[1], date[2]);
            StatefulDrone drone = new StatefulDrone(initialPosition, gameStateMap, file);
            while(drone.power > 0 && drone.movesCount < 250) {
                drone.move();
            }
            drone.addPathToMap();
            String json = gameStateMap.features.toJson();
            pathGeojsonWriter.write(json);
            pathGeojsonWriter.flush();
            drone.pathTxtWriter.close();
        }
    }
}

