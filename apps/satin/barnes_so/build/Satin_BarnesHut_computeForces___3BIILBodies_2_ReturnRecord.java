import ibis.satin.*;

import ibis.satin.impl.*;

public final class Satin_BarnesHut_computeForces___3BIILBodies_2_ReturnRecord extends ReturnRecord {
    java.util.LinkedList result;

    public Satin_BarnesHut_computeForces___3BIILBodies_2_ReturnRecord(java.util.LinkedList result, Throwable eek, int stamp) {
        super(eek);
        this.result = result;
        this.stamp = stamp;
    }

    public void assignTo(InvocationRecord rin) {
        Satin_BarnesHut_computeForces___3BIILBodies_2_InvocationRecord r = (Satin_BarnesHut_computeForces___3BIILBodies_2_InvocationRecord) rin;
	r.assignTo(eek, result);
    }
}
