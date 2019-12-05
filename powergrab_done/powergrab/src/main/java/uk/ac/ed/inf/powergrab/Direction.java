package uk.ac.ed.inf.powergrab;

/**
 * <h1>
 * Enum Direction that represents the change in latitude
 * and longitude for each sixteen directions that drone
 * can move in.
 * </h1>
 */
public enum Direction {
    N(0, 0.0003),
    NNE(0.0003 * Math.cos(Math.toRadians(22.5)), 0.0003 * Math.sin(Math.toRadians(22.5))),
    NE(0.0003 * Math.cos(Math.toRadians(45)), 0.0003 * Math.sin(Math.toRadians(45))),
    ENE(0.0003 * Math.cos(Math.toRadians(67.5)), 0.0003 * Math.sin(Math.toRadians(67.5))),
    E(0.0003, 0),
    ESE(0.0003 * Math.cos(Math.toRadians(22.5)), -0.0003 * Math.sin(Math.toRadians(22.5))),
    SE(0.0003 * Math.cos(Math.toRadians(45)), -0.0003 * Math.sin(Math.toRadians(45))),
    SSE(0.0003 * Math.cos(Math.toRadians(67.5)), -0.0003 * Math.sin(Math.toRadians(67.5))),
    S(0, -0.0003),
    SSW(-0.0003 * Math.cos(Math.toRadians(22.5)), -0.0003 * Math.sin(Math.toRadians(22.5))),
    SW(-0.0003 * Math.cos(Math.toRadians(45)), -0.0003 * Math.sin(Math.toRadians(45))),
    WSW(-0.0003 * Math.cos(Math.toRadians(67.5)), -0.0003 * Math.sin(Math.toRadians(67.5))),
    W(-0.0003, 0),
    WNW(-0.0003 * Math.cos(Math.toRadians(22.5)), 0.0003 * Math.sin(Math.toRadians(22.5))),
    NW(-0.0003 * Math.cos(Math.toRadians(45)), 0.0003 * Math.sin(Math.toRadians(45))),
    NNW(-0.0003 * Math.cos(Math.toRadians(67.5)), 0.0003 * Math.sin(Math.toRadians(67.5)));

    private final double changeOfLatitude;
    private final double changeOfLongitude;

    Direction(double changeOfLongitude, double changeOfLatitude) {
        this.changeOfLongitude = changeOfLongitude;
        this.changeOfLatitude = changeOfLatitude;
    }

    public double changeOfLatitude() {
        return changeOfLatitude;
    }

    public double changeOfLongitude() {
        return changeOfLongitude;
    }

    /**
     * <p>
     * This method transforms a Direction to the anti-clockwise angle.
     * Starting at East which represents 0 angle.
     * </p>
     *
     * @return double angle that is between East direction (0 angle) and given one
     */
    public double toAnticlockwiseAngle() {
        if (this.name().equals("E")) {
            return 0;
        }
        if (this.name().equals("ENE")) {
            return 22.5;
        }
        if (this.name().equals("NE")) {
            return 45;
        }
        if (this.name().equals("NNE")) {
            return 67.5;
        }
        if (this.name().equals("N")) {
            return 90;
        }
        if (this.name().equals("NNW")) {
            return 112.5;
        }
        if (this.name().equals("NW")) {
            return 135;
        }
        if (this.name().equals("WNW")) {
            return 157.5;
        }
        if (this.name().equals("W")) {
            return 180;
        }
        if (this.name().equals("WSW")) {
            return 202.5;
        }
        if (this.name().equals("SW")) {
            return 225;
        }
        if (this.name().equals("SSW")) {
            return 247.5;
        }
        if (this.name().equals("S")) {
            return 270;
        }
        if (this.name().equals("SSE")) {
            return 292.5;
        }
        if (this.name().equals("SE")) {
            return 315;
        } else {
            return 337.5;
        }
    }
}