package ibis.impl.messagePassing;

/**
 * messagePassing messages are fragmented into {@link ReadFragment}s.
 */
class ReadFragment {

    int msgHandle;

    int msgSize;

    ReadMessage msg;

    ReadFragment next;

    ReadFragment() {
        next = null;
    }

    void clear() {
        msg.resetMsg(msgHandle);
    }

}