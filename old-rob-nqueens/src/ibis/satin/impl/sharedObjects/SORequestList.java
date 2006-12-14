/* $Id$ */

/*
 * Created on Jan 17, 2006
 */
package ibis.satin.impl.sharedObjects;

import ibis.ipl.IbisIdentifier;
import ibis.satin.impl.Config;

final class SORequestList implements Config {
    private String[] ids = new String[50];

    private IbisIdentifier[] owners = new IbisIdentifier[50];

    private boolean[] demands = new boolean[50];
    
    private int count = 0;

    protected void add(IbisIdentifier owner, String id, boolean demand) {
        if (count >= ids.length) {
            String[] nids = new String[ids.length * 2];
            System.arraycopy(ids, 0, nids, 0, ids.length);
            ids = nids;

            IbisIdentifier[] nowners = new IbisIdentifier[owners.length * 2];
            System.arraycopy(owners, 0, nowners, 0, owners.length);
            owners = nowners;

            boolean[] ndemands = new boolean[demands.length * 2];
            System.arraycopy(demands, 0, ndemands, 0, demands.length);
            demands = ndemands;
        }

        ids[count] = id;
        owners[count] = owner;
        demands[count] = demand;
        count++;
    }

    protected int getCount() {
        return count;
    }

    protected IbisIdentifier getRequester(int index) {
        return owners[index];
    }

    protected String getobjID(int index) {
        return ids[index];
    }

    protected boolean isDemand(int index) {
        return demands[index];
    }
    
    protected void removeIndex(int i) {
        if (ASSERTS) {
            if (i >= count || i < 0) {
                System.err.println("removeIndex of strange index: " + i);
                new Exception().printStackTrace();
                System.exit(1); // Failed assertion
            }
        }

        count--;
        ids[i] = ids[count];
        owners[i] = owners[count];
    }
}
