/* $Id$ */

/*
 * Created on 19.02.2005
 */
package ibis.mpj;

import ibis.ipl.IbisIdentifier;

import java.util.HashMap;


/**
 * Collection of all MPJ connections.
 */
public class ConnectionTable {
    private final HashMap<IbisIdentifier, Connection> conTable;
    private IbisIdentifier myId;

    public ConnectionTable() {
        this.conTable = new HashMap<IbisIdentifier, Connection>();

    }

    protected void addConnection(IbisIdentifier id, Connection con) {
        conTable.put(id, con);
    }

    protected Connection getConnection(IbisIdentifier id) throws MPJException {
        if (conTable.containsKey(id)) {
            return conTable.get(id);
        }
        throw new MPJException("" + id + " not found in ConnectionTable.");
    }
}
