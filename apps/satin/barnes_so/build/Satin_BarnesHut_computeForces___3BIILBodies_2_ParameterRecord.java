import ibis.satin.*;

import ibis.satin.impl.*;

public final class Satin_BarnesHut_computeForces___3BIILBodies_2_ParameterRecord extends ibis.satin.impl.ParameterRecord {
    public byte[] param0;
    public int param1;
    public int param2;
    public String param3;

    public Satin_BarnesHut_computeForces___3BIILBodies_2_ParameterRecord(byte[] param0,int param1,int param2,String param3) {
        this.param0 = param0;
        this.param1 = param1;
        this.param2 = param2;
        this.param3 = param3;
    }

    public boolean equals(Object obj) {
        Satin_BarnesHut_computeForces___3BIILBodies_2_ParameterRecord other = (Satin_BarnesHut_computeForces___3BIILBodies_2_ParameterRecord) obj;
	     if (this.param0==null && other.param0==null) return true;
	     if (this.param0==null || other.param0==null) return false;
        if (this.param0.length != other.param0.length) return false;
        for (int i0=0; i0<param0.length; i0++) {
            if (param0[i0] != other.param0[i0]) return false;
        }
        if (this.param1 != other.param1) return false;
        if (this.param2 != other.param2) return false;
	     if (this.param3==null && other.param3==null) return true;
	     if (this.param3==null || other.param3==null) return false;
        if (!this.param3.equals(other.param3)) return false;
        return true;
    }

    public int hashCode() {
        int hash = 0;
        for (int i0=0; i0<param0.length; i0++) {
            hash += (int) param0[i0];
        }
        hash += (int) param1;
        hash += (int) param2;
        if (param3!=null) {
            hash += param3.hashCode();
        }
        return hash;
    }

    public String toString() {
        String str = "(";
        str += param0 + ",";
        str += param1 + ",";
        str += param2 + ",";
        str += param3;
        str += ")";
        return str;
    }
}
