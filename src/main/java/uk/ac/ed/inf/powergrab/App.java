package uk.ac.ed.inf.powergrab;

import java.io.IOException;

public class App {

    public static void main(String[] args) throws IOException {
        if(args.length != 7){
            String params = "<day> <month> <year> <initialLatitude> <initialLongitude> <seed> <droneType>";
            System.out.println("Incorrect number of arguments. \n The only accepted arguments are: \n " + params + "\n divided by space.");
            System.exit(1);
        }

        if(Integer.parseInt(args[0]) > 31 || Integer.parseInt(args[1]) > 12 || Integer.parseInt(args[2]) != 2019
            || Integer.parseInt(args[0]) < 1 || Integer.parseInt(args[1]) < 1){
            System.out.println("Incorrect date, please note that first three arguments represent day, month and year");
            System.exit(1);
        }

        if((!args[6].equals("stateless"))&& !(args[6].equals("stateful"))){
            System.out.println("Incorrect type of drone, only stateful and stateless are supported.");
            System.exit(1);
        }

        Position initialPosition = new Position(Double.parseDouble(args[3]),Double.parseDouble(args[4]));
        if(!initialPosition.inPlayArea()){
            System.out.println("Initial position given is not in a play area, play area corresponds to: ");
            System.out.println("Latitude between 55.942617 and 55.946233.");
            System.out.println("Longitude between −3.184319 and −3.192473.");
            System.exit(1);
        }

        new Game(args).play();
    }
}
