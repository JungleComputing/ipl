/* $Id$ */

/*
 * Created on Jan 17, 2006
 */
package ibis.satin.impl;

import ibis.ipl.IbisIdentifier;

public class SORequestList implements Config {
    String[] ids = new String[50];

    IbisIdentifier[] owners = new IbisIdentifier[50];

    int count = 0;

    public void add(IbisIdentifier owner, String id) {
        if (count >= ids.length) {
            String[] nids = new String[ids.length * 2];
            System.arraycopy(ids, 0, nids, 0, ids.length);
            ids = nids;

            IbisIdentifier[] nowners = new IbisIdentifier[owners.length * 2];
            System.arraycopy(owners, 0, nowners, 0, owners.length);
            owners = nowners;
        }

        ids[count] = id;
        owners[count] = owner;
        count++;
    }

    int getCount() {
        return count;
    }
    
    IbisIdentifier getRequester(int index) {
        return owners[index];
    }
    
    String getobjID(int index) {
        return ids[index];
    }

    void removeIndex(int i) {
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
