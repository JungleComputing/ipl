/* $Id$ */

package ibis.satin.impl;

import ibis.ipl.SendPortIdentifier;

import java.util.Vector;

class MsgStats {

    Vector remotePortHash = new Vector();

    int[] upcall;

    int[] upcallRequest;

    int[] upcallReply;

    int[] upcallResult;

    Vector localPortHash = new Vector();

    int[] sendRequest;

    int[] sendReply;

    int[] sendResult;

    int[] sent;

    int[] received;

    private int[] realloc(int[] a, int n) {
        int[] b = new int[n];
        if (a != null) {
            System.arraycopy(a, 0, b, 0, a.length);
        }
        return b;
    }

    void addRemotePort(SendPortIdentifier id) {
        if (remotePortHash.contains(id)) {
            return;
        }

        remotePortHash.add(id);

        upcall = realloc(upcall, remotePortHash.size());
        upcallRequest = realloc(upcallRequest, remotePortHash.size());
        upcallReply = realloc(upcallReply, remotePortHash.size());
        upcallResult = realloc(upcallResult, remotePortHash.size());
        received = realloc(received, remotePortHash.size());
    }

    void addLocalPort(SendPortIdentifier id) {
        if (localPortHash.contains(id)) {
            return;
        }

        localPortHash.add(id);

        sendRequest = realloc(sendRequest, localPortHash.size());
        sendReply = realloc(sendReply, localPortHash.size());
        sendResult = realloc(sendResult, localPortHash.size());
        sent = realloc(sent, localPortHash.size());
    }

}

