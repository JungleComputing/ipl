/* $Id$ */
/**
 * Container class for some parameters that influence the run.
 */
public final class RunParameters implements java.io.Serializable {
    /** Cell subdivision tolerance. */
    double THETA;

    /** Integration time-step. */
    double DT;

    /** Half of the integration time-step. */
    double DT_HALF;

    /** Potential softening value. */
    double SOFT;

    /** Potential softening value squared. */
    double SOFT_SQ;

    /** Maximum bumber of bodies per leaf. */
    int MAX_BODIES_PER_LEAF;

    /** Spawn threshold. */
    int THRESHOLD;

    /** Wether to use double or float for accelerations. */
    boolean useDoubleUpdates;
}
