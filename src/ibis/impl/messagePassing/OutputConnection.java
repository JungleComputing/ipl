package ibis.impl.messagePassing;

final class OutputConnection {

    native void ibmp_connect(int remoteCPU,
			     byte[] receivePortId,
			     byte[] sendPortId,
			     Syncer syncer);

    native void ibmp_disconnect(int remoteCPU,
				byte[] receiverPortId,
				byte[] sendPortId,
				int count);

}
