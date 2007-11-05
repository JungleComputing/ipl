package ibis.ipl.benchmarks.rpc;

/* $Id: VBFField.java 6546 2007-10-05 13:21:40Z ceriel $ */

import java.io.Serializable;

final public class VBFField implements Serializable {
    private static final long serialVersionUID = 6697247545896124734L;
    public int indexOfRemoteCopy;
    public double[] field = new double[3];
    transient public int indexOfSubDomain;

    public VBFField() {
    }

    public VBFField(int subDomIndex, int indexOfRemoteCopy, double[] field) {
        this.indexOfRemoteCopy = indexOfRemoteCopy;
        this.field = field;
        this.indexOfSubDomain = subDomIndex;
    }
}
