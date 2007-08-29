/* $Id$ */

/*
 * Created on 25.01.2005
 */

package ibis.mpj;

/**
 * Superclass of all operations used by collective reduce operations.
 * When the user defines his/her own operation, the new operation must
 * extend this class. 
 */
public abstract class Op {
    protected boolean commute;

    public Op(boolean commute) {
        this.commute = commute;
    }


    public abstract void call(Object invec, int inoffset, Object inoutvec, int outoffset, int count, Datatype datatype) throws MPJException;

    public boolean isCommute() {
        return commute;
    }
}
