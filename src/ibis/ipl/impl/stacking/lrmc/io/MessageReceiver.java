package ibis.ipl.impl.stacking.lrmc.io;

import ibis.ipl.impl.stacking.lrmc.util.Message;

//import ibis.ipl.IbisIdentifier;

public interface MessageReceiver {
    // Returns false if the ObjectMulticaster is done().
    public boolean gotMessage(Message buffer);

    // Called at sender site when the last receiver received last message.
    public void gotDone(int id);
}
