import ibis.satin.*;

import ibis.satin.impl.*;

public final class Satin_BarnesHut_computeForcesNoSO__LBodyTreeNode_2LBodyTreeNode_2I_ParameterRecord extends ibis.satin.impl.ParameterRecord {
    public BodyTreeNode param0;
    public BodyTreeNode param1;
    public int param2;

    public Satin_BarnesHut_computeForcesNoSO__LBodyTreeNode_2LBodyTreeNode_2I_ParameterRecord(BodyTreeNode param0,BodyTreeNode param1,int param2) {
        this.param0 = param0;
        this.param1 = param1;
        this.param2 = param2;
    }

    public boolean equals(Object obj) {
        Satin_BarnesHut_computeForcesNoSO__LBodyTreeNode_2LBodyTreeNode_2I_ParameterRecord other = (Satin_BarnesHut_computeForcesNoSO__LBodyTreeNode_2LBodyTreeNode_2I_ParameterRecord) obj;
	     if (this.param0==null && other.param0==null) return true;
	     if (this.param0==null || other.param0==null) return false;
        if (!this.param0.equals(other.param0)) return false;
	     if (this.param1==null && other.param1==null) return true;
	     if (this.param1==null || other.param1==null) return false;
        if (!this.param1.equals(other.param1)) return false;
        if (this.param2 != other.param2) return false;
        return true;
    }

    public int hashCode() {
        int hash = 0;
        if (param0!=null) {
            hash += param0.hashCode();
        }
        if (param1!=null) {
            hash += param1.hashCode();
        }
        hash += (int) param2;
        return hash;
    }

    public String toString() {
        String str = "(";
        str += param0 + ",";
        str += param1 + ",";
        str += param2;
        str += ")";
        return str;
    }
}
