package uk.ac.ed.inf.powergrab;

/**
 * <h1>
 *     Drone which represent superclass ofboth stateless
 *     and stateful drones.
 * </h1>
 * <p>
 *     Its fields are the ones which are common for both
 *     drone types: coins, power, position and moves.
 * </p>
 */
class Drone {
    double coins;
    double power;
    Position position;
    int moves;

    public Drone(Position initialPosition) {
        this.position = initialPosition;
        this.coins = 0.0;
        this.power = 250.0;
        this.moves = 0;
    }
}
