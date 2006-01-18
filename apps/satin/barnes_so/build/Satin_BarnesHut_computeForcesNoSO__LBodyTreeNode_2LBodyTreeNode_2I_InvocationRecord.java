import ibis.satin.impl.*;

import ibis.satin.*;

public final class Satin_BarnesHut_computeForcesNoSO__LBodyTreeNode_2LBodyTreeNode_2I_InvocationRecord extends InvocationRecord {
    public BarnesHut self;
    public BodyTreeNode param0;
    public BodyTreeNode param1;
    public int param2;
    public transient java.util.LinkedList result;
    public transient int index;
    public transient java.util.LinkedList[] array;
    public transient Object ref;

    public Satin_BarnesHut_computeForcesNoSO__LBodyTreeNode_2LBodyTreeNode_2I_InvocationRecord(BarnesHut self, BodyTreeNode param0, BodyTreeNode param1, int param2, SpawnCounter s, InvocationRecord next, int storeId, int spawnId, LocalRecord parentLocals) {
        super(s, next, storeId, spawnId, parentLocals);
        this.self = self;
        this.param0 = param0;
        this.param1 = param1;
        this.param2 = param2;
    }

    public static Satin_BarnesHut_computeForcesNoSO__LBodyTreeNode_2LBodyTreeNode_2I_InvocationRecord getNew(BarnesHut self, BodyTreeNode param0, BodyTreeNode param1, int param2, SpawnCounter s, InvocationRecord next, int storeId, int spawnId, LocalRecord parentLocals) {
            return new Satin_BarnesHut_computeForcesNoSO__LBodyTreeNode_2LBodyTreeNode_2I_InvocationRecord(self,  param0,  param1,  param2, s, next, storeId, spawnId, parentLocals);
    }

    public static Satin_BarnesHut_computeForcesNoSO__LBodyTreeNode_2LBodyTreeNode_2I_InvocationRecord getNewArray(java.util.LinkedList[] array, int index, BarnesHut self, BodyTreeNode param0, BodyTreeNode param1, int param2, SpawnCounter s, InvocationRecord next, int storeId, int spawnId, LocalRecord parentLocals) {
            Satin_BarnesHut_computeForcesNoSO__LBodyTreeNode_2LBodyTreeNode_2I_InvocationRecord res = getNew(self,  param0,  param1,  param2, s, next, storeId, spawnId, parentLocals);
        res.index = index;
        res.array = array;
        return res;
    }

    public static Satin_BarnesHut_computeForcesNoSO__LBodyTreeNode_2LBodyTreeNode_2I_InvocationRecord getNewRef(Object ref, BarnesHut self, BodyTreeNode param0, BodyTreeNode param1, int param2, SpawnCounter s, InvocationRecord next, int storeId, int spawnId, LocalRecord parentLocals) {
            Satin_BarnesHut_computeForcesNoSO__LBodyTreeNode_2LBodyTreeNode_2I_InvocationRecord res = getNew(self,  param0,  param1,  param2, s, next, storeId, spawnId, parentLocals);
        res.ref = ref;
        return res;
    }

    public static void delete(Satin_BarnesHut_computeForcesNoSO__LBodyTreeNode_2LBodyTreeNode_2I_InvocationRecord w) {
    }

    public void runLocal() throws Throwable {
        if (ibis.satin.impl.Config.ABORTS) {
            try {
            result =             self.computeForcesNoSO(param0, param1, param2);
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
            result =             self.computeForcesNoSO(param0, param1, param2);
        }
    }

    public ibis.satin.impl.ReturnRecord runRemote() {
        try {
            result = self.computeForcesNoSO(param0, param1, param2);
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
        return new Satin_BarnesHut_computeForcesNoSO__LBodyTreeNode_2LBodyTreeNode_2I_ReturnRecord(result, eek, stamp);
    }
    public void assignTo(Throwable eek, java.util.LinkedList result
) {
        this.eek = eek;
        this.result = result;
    }
    public ibis.satin.impl.ParameterRecord getParameterRecord() {
        return new Satin_BarnesHut_computeForcesNoSO__LBodyTreeNode_2LBodyTreeNode_2I_ParameterRecord(param0, param1, param2);
    }

    public ibis.satin.impl.ReturnRecord getReturnRecord() {
        return new Satin_BarnesHut_computeForcesNoSO__LBodyTreeNode_2LBodyTreeNode_2I_ReturnRecord(result, null, stamp);
    }

    public void setSOReferences()
        throws ibis.satin.impl.SOReferenceSourceCrashedException {
    }

    public java.util.Vector getSOReferences() {
        java.util.Vector refs = new java.util.Vector();
        return refs;
    }

}
