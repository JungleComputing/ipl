/* $Id$ */

/*
 * Created on 19.02.2005
 */
package ibis.mpj;

import java.util.*;


/**
 * Collection of all MPJ connections.
 */
public class ConnectionTable {
    private HashMap conTable = null;
    private String myMPJHostName = null;

    public ConnectionTable() {
        this.conTable = new HashMap();

    }

    protected String addConnection(String ibisHostName, Connection con) {
        int i = 0;
        String host =  ibisHostName + "_" + i;

        boolean written = false;

        while(!written) {
            if (conTable.containsKey(host)) {
                i++;
                host =  ibisHostName + "_" + i;
            }
            else {

                conTable.put(host, con);
                written = true;
            }
        }
        return host;
    }

    protected Connection getConnection(String mpjHostName) throws MPJException {
        if (conTable.containsKey(mpjHostName)) {
            return (Connection)conTable.get(mpjHostName);
        }
        throw new MPJException(mpjHostName + " not found in ConnectionTable.");
     }



    protected void setMyMPJHostName(String hostName) {
        myMPJHostName = hostName;
    }

    protected String getMyMPJHostName() {
        return(myMPJHostName);
    }
}
