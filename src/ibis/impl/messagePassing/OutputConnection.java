package ibis.ipl.impl.messagePassing;

class OutputConnection {

    native void ibmp_connect(int cpu, int port,
				       int my_port, String type, String ibisId,
				       Syncer syncer, int serializationType);

    native void ibmp_disconnect(int cpu, int port, int receiver_port, int count);

}
