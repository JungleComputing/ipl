package ibis.ipl.impl.messagePassing;

import ibis.ipl.IbisIOException;


public abstract class OutputConnection {

    protected OutputConnection() {
    }


    protected abstract void ibmp_connect(int cpu, int port,
					 int my_port, String type, String ibisId,
					 Syncer syncer, int serializationType)
	    throws IbisIOException;

    protected abstract void ibmp_disconnect(int cpu, int port,
					    int receiver_port, int messageCount)
	    throws IbisIOException;

}
