package ibis.ipl.support.vivaldi;

import java.io.IOException;
import java.io.Serializable;

import org.apache.log4j.Logger;

/**
 * Class representing a coordinate in a Cartesian space. Code copied from
 * Zorilla
 * 
 * @author Niels Drost
 * 
 */
public final class Coordinates implements Serializable {

    private static final Logger logger = Logger.getLogger(Coordinates.class);

    // nodes closer than this push each other off in some random direction
    public static final double CLOSEBY_THRESHOLD = 0.01;

    public static final double COORDINATE_CONTROL = 0.1;

    public static final double ERROR_CONTROL = 0.1;

    public static final int DIMENSIONS = 5;

    public static final int DOUBLE_SIZE = 8;

    public static final int LONG_SIZE = 8;

    public static final int SIZE = DOUBLE_SIZE + (DOUBLE_SIZE * DIMENSIONS);

    private static final long serialVersionUID = 0L;

    private final double error;

    private final double[] coordinates;

    private static double magnitude(double[] vector) {
        double total = 0;

        for (int i = 0; i < DIMENSIONS; i++) {
            total += (vector[i] * vector[i]);
        }

        return Math.sqrt(total);
    }

    private static double[] subtract(double[] one, double[] other) {
        double[] result = new double[DIMENSIONS];

        for (int i = 0; i < DIMENSIONS; i++) {
            result[i] = one[i] - other[i];
        }

        return result;
    }

    private static double[] add(double[] one, double[] other) {
        double[] result = new double[DIMENSIONS];

        for (int i = 0; i < DIMENSIONS; i++) {
            result[i] = one[i] + other[i];
        }

        return result;
    }

    private static double[] mult(double scaler, double[] vector) {
        double[] result = new double[DIMENSIONS];

        for (int i = 0; i < DIMENSIONS; i++) {
            result[i] = scaler * vector[i];
        }

        return result;
    }

    private static double[] randomVector() {
        logger.debug("randomness commencing!");

        double[] result = new double[DIMENSIONS];
        for (int i = 0; i < DIMENSIONS; i++) {
            result[i] = Math.random();
            if (Math.random() > 0.5) {
                result[i] *= -1;
            }
        }

        return result;
    }

    private static double[] unitVector(double[] vector) {
        double magnitude = magnitude(vector);

        // coordinates which are too close push each other off
        while (magnitude < CLOSEBY_THRESHOLD) {
            vector = randomVector();
            magnitude = magnitude(vector);
        }

        double[] result = mult(1 / magnitude, vector);

        logger.debug("unitVector(" + toString(vector) + ") = "
                + toString(result));

        magnitude = magnitude(result);
        if (magnitude < 0.99 || magnitude > 1.01) {

            logger.error("magnitude of unit vector " + toString(result)
                    + " of vector " + toString(vector)
                    + " should be 1, but is: " + magnitude(result));
        }

        return result;
    }

    private final void int2byte(int src, byte[] dst, int off) {
        dst[off + 0] = (byte) (0xff & (src >> 24));
        dst[off + 1] = (byte) (0xff & (src >> 16));
        dst[off + 2] = (byte) (0xff & (src >> 8));
        dst[off + 3] = (byte) (0xff & src);
    }

    private final int byte2int(byte[] src, int off) {
        return (((src[off + 3] & 0xff) << 0) | ((src[off + 2] & 0xff) << 8)
                | ((src[off + 1] & 0xff) << 16) | ((src[off + 0] & 0xff) << 24));
    }

    private final void long2byte(long src, byte[] dst, int off) {
        int v1 = (int) (src >> 32);
        int v2 = (int) (src);

        int2byte(v1, dst, off);
        int2byte(v2, dst, off + 4);
    }

    private final long byte2long(byte[] src, int off) {
        int t1, t2;
        t1 = byte2int(src, off);
        t2 = byte2int(src, off + 4);

        return ((((long) t1) << 32) | (t2 & 0xffffffffL));
    }

    /*
     * Creates an "unknown" coordinate.
     */
    public Coordinates() {
        error = 1.0; // no confidence, high error

        // initiate coordinates on some random coordinate (within 0.0 to 1.0)
        coordinates = randomVector();
    }

    public Coordinates(double[] coordinates, double error) {
        this.coordinates = coordinates.clone();
        this.error = error;
    }

    public Coordinates(byte[] bytes) throws IOException {
        if (bytes.length != SIZE) {
            throw new IOException("received coordinates of unknown length");
        }

        long errorBits = byte2long(bytes, 0);
        error = Double.longBitsToDouble(errorBits);

        coordinates = new double[DIMENSIONS];

        for (int i = 0; i < DIMENSIONS; i++) {
            long coordinateBits = byte2long(bytes, LONG_SIZE * (i + 1));
            coordinates[i] = Double.longBitsToDouble(coordinateBits);
        }
    }

    public byte[] toBytes() {
        byte[] bytes = new byte[SIZE];

        long errorBits = Double.doubleToLongBits(error);

        long2byte(errorBits, bytes, 0);

        for (int i = 0; i < DIMENSIONS; i++) {
            long coordinateBits = Double.doubleToLongBits(coordinates[i]);

            long2byte(coordinateBits, bytes, LONG_SIZE * (i + 1));
        }
        return bytes;
    }

    public double distance(Coordinates other) {
        return magnitude(subtract(this.coordinates, other.coordinates));
    }

    /**
     * Updates coordinate with new information...
     */
    public Coordinates update(Coordinates remoteCoordinates, double rtt) {
        logger.debug("updating " + this + " with " + remoteCoordinates
                + " at distance " + rtt);

        double newError;
        double[] newCoordinates;

        double weight = error / (error + remoteCoordinates.error);

        logger.debug("weight = " + weight);

        double distance = distance(remoteCoordinates);

        double sampleError = Math.abs(distance - rtt) / rtt;

        logger.debug("sample error = " + sampleError);

        newError = sampleError * ERROR_CONTROL * weight + error
                * (1 - ERROR_CONTROL * weight);

//        if (newError > 1.0) {
//            newError = 1.0;
//        }

        logger.debug("new error = " + newError);

        double step = COORDINATE_CONTROL * weight;

        logger.debug("step = " + step);

        double[] unitVector = unitVector(subtract(coordinates,
                remoteCoordinates.coordinates));
        logger.debug("unit vector = " + toString(unitVector));

        double[] scaled = mult(step * (rtt - distance(remoteCoordinates)),
                unitVector);
        logger.debug("scaled = " + toString(scaled));

        newCoordinates = add(coordinates, scaled);

        logger.debug("new coordinates = " + toString(newCoordinates));

        return new Coordinates(newCoordinates, newError);
    }

    private static String prettyPrint(double number) {
        return String.format("% 8.2f", number);
    }

    private static String toString(double[] vector) {
        String result = "[";

        for (int i = 0; i < DIMENSIONS - 1; i++) {
            result += prettyPrint(vector[i]) + ", ";

        }
        result += prettyPrint(vector[DIMENSIONS - 1]) + "]";

        return result;
    }

    public double getError() {
        return error;
    }

    public double[] getCoordinates() {
        return coordinates.clone();
    }

    public boolean isOrigin() {
        for (int i = 0; i < DIMENSIONS; i++) {
            if (coordinates[i] != 0) {
                return false;
            }
        }
        return true;
    }

    public String toString() {
        return toString(coordinates) + " error = " + prettyPrint(error);
    }

}
