import ibis.satin.impl.*;

import ibis.satin.*;

public final class Satin_BarnesHut_computeForces___3BIILBodies_2_InvocationRecord extends InvocationRecord {
    public BarnesHut self;
    public byte[] param0;
    public int param1;
    public int param2;
    public String satin_so_reference_param3;
    public transient java.util.LinkedList result;
    public transient int index;
    public transient java.util.LinkedList[] array;
    public transient Object ref;

    public Satin_BarnesHut_computeForces___3BIILBodies_2_InvocationRecord(BarnesHut self, byte[] param0, int param1, int param2, Bodies param3, SpawnCounter s, InvocationRecord next, int storeId, int spawnId, LocalRecord parentLocals) {
        super(s, next, storeId, spawnId, parentLocals);
        this.self = self;
        this.param0 = param0;
        this.param1 = param1;
        this.param2 = param2;
        this.satin_so_reference_param3 = param3.objectId;
    }

    public static Satin_BarnesHut_computeForces___3BIILBodies_2_InvocationRecord getNew(BarnesHut self, byte[] param0, int param1, int param2, Bodies param3, SpawnCounter s, InvocationRecord next, int storeId, int spawnId, LocalRecord parentLocals) {
            return new Satin_BarnesHut_computeForces___3BIILBodies_2_InvocationRecord(self,  param0,  param1,  param2,  param3, s, next, storeId, spawnId, parentLocals);
    }

    public static Satin_BarnesHut_computeForces___3BIILBodies_2_InvocationRecord getNewArray(java.util.LinkedList[] array, int index, BarnesHut self, byte[] param0, int param1, int param2, Bodies param3, SpawnCounter s, InvocationRecord next, int storeId, int spawnId, LocalRecord parentLocals) {
            Satin_BarnesHut_computeForces___3BIILBodies_2_InvocationRecord res = getNew(self,  param0,  param1,  param2,  param3, s, next, storeId, spawnId, parentLocals);
        res.index = index;
        res.array = array;
        return res;
    }

    public static Satin_BarnesHut_computeForces___3BIILBodies_2_InvocationRecord getNewRef(Object ref, BarnesHut self, byte[] param0, int param1, int param2, Bodies param3, SpawnCounter s, InvocationRecord next, int storeId, int spawnId, LocalRecord parentLocals) {
            Satin_BarnesHut_computeForces___3BIILBodies_2_InvocationRecord res = getNew(self,  param0,  param1,  param2,  param3, s, next, storeId, spawnId, parentLocals);
        res.ref = ref;
        return res;
    }

    public static void delete(Satin_BarnesHut_computeForces___3BIILBodies_2_InvocationRecord w) {
    }

    public void runLocal() throws Throwable {
        if (ibis.satin.impl.Config.ABORTS) {
            try {
            result =             self.computeForces(param0, param1, param2, (Bodies)ibis.satin.impl.Satin.getSatin().getSOReference(satin_so_reference_param3));
            } catch (Throwable e) {
            if (Config.inletLogger.isDebugEnabled()) {
                    Config.inletLogger.debug("caught exception in runlocal: " + e, e);
                }
                eek = e;
            }
            if (eek != null && !inletExecuted) {
            if (Config.inletLogger.isDebugEnabled()) {
                    Config.inletLogger.debug("runlocal: calling inlet for: " + this);
                }
                if(parentLocals != null)
                    parentLocals.handleException(spawnId, eek, this);
            if (Config.inletLogger.isDebugEnabled()) {
                    Config.inletLogger.debug("runlocal: calling inlet for: " + this + " DONE");
                }
                if(parentLocals == null)
                    throw eek;
            }
        } else {
            result =             self.computeForces(param0, param1, param2, (Bodies)ibis.satin.impl.Satin.getSatin().getSOReference(satin_so_reference_param3));
        }
    }

    public ibis.satin.impl.ReturnRecord runRemote() {
        try {
            result = self.computeForces(param0, param1, param2, (Bodies)ibis.satin.impl.Satin.getSatin().getSOReference(satin_so_reference_param3));
        } catch (Throwable e) {
            if (ibis.satin.impl.Config.ABORTS) {
                eek = e;
            } else {
                if (e instanceof Error) {
                    throw (Error) e;
                }
                if (e instanceof RuntimeException) {
                    throw (RuntimeException) e;
                }
                throw new RuntimeException(e);
            }
        }
        return new Satin_BarnesHut_computeForces___3BIILBodies_2_ReturnRecord(result, eek, stamp);
    }
    public void assignTo(Throwable eek, java.util.LinkedList result
) {
        this.eek = eek;
        this.result = result;
    }
    public ibis.satin.impl.ParameterRecord getParameterRecord() {
        return new Satin_BarnesHut_computeForces___3BIILBodies_2_ParameterRecord(param0, param1, param2, satin_so_reference_param3);
    }

    public ibis.satin.impl.ReturnRecord getReturnRecord() {
        return new Satin_BarnesHut_computeForces___3BIILBodies_2_ReturnRecord(result, null, stamp);
    }

    public void setSOReferences()
        throws ibis.satin.impl.SOReferenceSourceCrashedException {
        ibis.satin.impl.Satin.getSatin().setSOReference(satin_so_reference_param3, owner);
    }

    public java.util.Vector getSOReferences() {
        java.util.Vector refs = new java.util.Vector();
        refs.add(satin_so_reference_param3);
        return refs;
    }

    public boolean guard() {
        return self.guard_computeForces(param0, param1, param2, (Bodies)ibis.satin.impl.Satin.getSatin().getSOReference(satin_so_reference_param3));
    }

}
