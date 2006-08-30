/* $Id$ */

import java.io.Serializable;

import org.apache.log4j.Logger;

strictfp public class GlobalData implements Cloneable, Serializable {

    static Logger logger = Logger.getLogger(GlobalData.class.getName());

    public Body gdBodies[];

    public static final double GD_DOUBLE_EPSILON = 0.0000001;

    public static final double GD_START_TIME = 0.0;

    public double gdEndTime = 10.0;

    public double gdDt = 0.025;

    public double gdDtHalf = gdDt / 2.0;

    public double gdTheta = 5.0;

    public double gdThetaSq = gdTheta * gdTheta;

    public static final double GD_BODY_RANGE = 30.0;

    public double gdSoft = 0.0000025;

    public double gdSoftSQ = gdSoft * gdSoft;

    public static final double GD_DIM_SLACK = 0.00001;

    public int gdIterations = (int) (((gdEndTime - GD_START_TIME) / gdDt) + 1.1);

    public int gdNumBodies = 300;

    public int gdNumEssentialBodies;

    public int gdTotNumBodies = 300;

    public int gdMaxBodies = (int) ((double) gdNumBodies * 1.2);

    public int gdMaxLocalBodies;

    public int gdMaxTempBodies;

    public int gdMaxTempCentersOfMass;

    public int gdMaxBodiesPerNode = 3;

    public static final double GD_EXTRA_BODY_SPACE_FACTOR = 2;

    public static final int GD_MEDIAN_BITS = 28;

    public static final int GD_MEDIAN_SHIFT = 7;

    public static final int GD_MEDIAN_RADIX = 128;

    public boolean gdSerialize = false;

    public boolean gdTrimArrays = false;

    public boolean gdThreads = false;

    public int gdGCInterval = 0;

    public int gdMyProc = 1;

    public int gdNumProcs = 1;

    public int gdMaxProcs = 128;

    public int gdLogProcs = 1;

    public int gdMaxLogProcs = 7;

    public int gdFirstLoc = 1;

    public double gdMaxLoad = 0.01;

    public int gdIteration;

    public RandomNumber gdRand;

    public Plummer gdPlummer;

    public ProcessorImpl Proc;

    // Options

    public static final boolean GD_PRINT_TIME = true;

    public static final boolean GD_PRINT_STATS = false;

    public static final boolean GD_PRINT_BODIES = true;

    public boolean gdComputeAccelerationsDirect = false;

    void Initialize() {

        for (gdLogProcs = 0; ((gdNumProcs - 1) >> gdLogProcs) > 0; gdLogProcs++)
            ;

        gdFirstLoc = (1 << gdLogProcs) - 1;

        gdMaxBodies = (int) ((double) gdTotNumBodies * GD_EXTRA_BODY_SPACE_FACTOR);
        gdMaxLocalBodies = (int) ((double) (gdTotNumBodies / gdNumProcs) * GD_EXTRA_BODY_SPACE_FACTOR);

        gdMaxTempBodies = Math.max(gdTotNumBodies / 10, 500);
        gdMaxTempCentersOfMass = Math.max(gdTotNumBodies / 10, 500);
    }

    void InitializeBodies() {

        gdBodies = new Body[gdMaxBodies];

        // Scale ?
/* @@@ this scale stuff is very strange. It is not there in the original suel code. --Rob

        double scale = Math.pow(gdTotNumBodies / 16384.0, -0.25);

        gdDt *= scale;
        gdEndTime *= scale;
        gdTheta *= 1.0 / scale;
*/
        debugStr("theta: " + gdTheta);

        gdDtHalf = gdDt / 2;
        gdThetaSq = gdTheta * gdTheta;
        gdSoftSQ = gdSoft * gdSoft;
        gdIterations = 10; // (int) (((gdEndTime - GD_START_TIME) / gdDt) + 1.1);

//        debugStr("scale: " + scale + "theta: " + gdTheta + ", theta sq: "
//            + gdThetaSq);

        debugStr("Parameters: " + gdTotNumBodies + " bodies, " + gdDt + " dt, "
            + gdSoft + " eps, " + (2.0 / gdTheta) + " tol, " + gdNumProcs
            + " procs, " + gdDtHalf + " dthalf, " + gdMaxLocalBodies
            + " Max LocalBodies ");

    }

    public GlobalData GenerateClone() {
        try {
            return (GlobalData) clone();
        } catch (Exception e) {
            throw new Error("could not clone!", e);
        }
    }

    void debugStr(String s) {
        System.err.println(gdMyProc + ": " + s);
        logger.debug(gdMyProc + ": " + s);
    }

    GlobalData() {
        gdRand = new RandomNumber();
        gdPlummer = new Plummer(gdRand);
    }
}
