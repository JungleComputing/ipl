package ibis.impl.messagePassing;

class ReadFragment {

    int		msgHandle;
    int		msgSize;
    ReadMessage msg;
    ReadFragment next;

    ReadFragment() {
	next = null;
    }

    void clear() {
	msg.resetMsg(msgHandle);
    }

}
