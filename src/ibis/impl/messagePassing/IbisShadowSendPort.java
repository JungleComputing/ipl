/* $Id$ */

package ibis.impl.messagePassing;

import ibis.io.IbisSerializationInputStream;

import java.io.IOException;

/**
 * Receiver-side stub for an {@link IbisSendPort}
 */
final class IbisShadowSendPort extends ShadowSendPort {

    IbisSerializationInputStream obj_in;

    /* Create a shadow SendPort, used by the local ReceivePort to refer to */
    IbisShadowSendPort(ReceivePortIdentifier rId, SendPortIdentifier sId,
            int startSeqno, int group, int groupStartSeqno) throws IOException {
        super(rId, sId, startSeqno, group, groupStartSeqno);
        obj_in = new IbisSerializationInputStream(in);
    }

    ReadMessage getMessage(int seqno) {
        ReadMessage msg = cachedMessage;

        if (Ibis.DEBUG) {
            System.err.println("Get a Ibis ReadMessage ");
            System.err.println(" >>>>>> >>>>>>> >>>>>>> "
                    + " Don't forget to set the stream in the ReadMessage");
        }

        if (msg != null) {
            cachedMessage = null;

        } else {
            msg = new IbisReadMessage(this, receivePort);
            if (Ibis.DEBUG) {
                System.err.println("Create an -ibis-serialization- ReadMessage "
                                + msg);
            }
        }

        msg.msgSeqno = seqno;
        return msg;
    }

}
