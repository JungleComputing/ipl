/* $Id$ */

import java.io.Serializable;

final public class VBFField implements Serializable {
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
