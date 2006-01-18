import ibis.satin.*;

import ibis.satin.impl.*;

public final class Satin_BarnesHut_computeForcesNoSO__LBodyTreeNode_2LBodyTreeNode_2I_ReturnRecord extends ReturnRecord {
    java.util.LinkedList result;

    public Satin_BarnesHut_computeForcesNoSO__LBodyTreeNode_2LBodyTreeNode_2I_ReturnRecord(java.util.LinkedList result, Throwable eek, int stamp) {
        super(eek);
        this.result = result;
        this.stamp = stamp;
    }

    public void assignTo(InvocationRecord rin) {
        Satin_BarnesHut_computeForcesNoSO__LBodyTreeNode_2LBodyTreeNode_2I_InvocationRecord r = (Satin_BarnesHut_computeForcesNoSO__LBodyTreeNode_2LBodyTreeNode_2I_InvocationRecord) rin;
	r.assignTo(eek, result);
    }
}
