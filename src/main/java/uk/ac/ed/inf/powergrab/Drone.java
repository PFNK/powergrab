package uk.ac.ed.inf.powergrab;

class Drone {
    double coins;
    double power;
    Position position;
    int moves;

    public Drone(Position initial_position) {
        this.position = initial_position;
        this.coins = 0.0;
        this.power = 250.0;
        this.moves = 0;
    }


}
