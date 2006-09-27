/*
 * Created on Jun 1, 2006
 */
package ibis.satin.impl.faultTolerance;

import ibis.ipl.IbisIdentifier;
import ibis.satin.impl.Config;
import ibis.satin.impl.Satin;
import ibis.satin.impl.spawnSync.InvocationRecord;
import ibis.satin.impl.spawnSync.ReturnRecord;

class GlobalResultTableValue implements java.io.Serializable, Config {
    protected static final int TYPE_RESULT = 1;

    protected static final int TYPE_POINTER = 2;

    protected int type;

    protected transient IbisIdentifier sendTo;

    protected ReturnRecord result;

    protected IbisIdentifier owner;

    protected GlobalResultTableValue(int type, InvocationRecord r) {
        this.type = type;
        this.owner = Satin.getSatin().ident;
        if (type == TYPE_RESULT) {
            result = r.getReturnRecord();
        }
    }

    public String toString() {
        String str = "";
        switch (type) {
        case TYPE_RESULT:
            str += "(RESULT,result:" + result + ")";
            break;
        case TYPE_POINTER:
            str += "(POINTER,owner:" + owner + ")";
            break;
        default:
            grtLogger.error("SATIN '" + Satin.getSatin().ident
                + "': illegal type in value");
        }
        return str;
    }
}
