package ibis.satin.impl;

import ibis.ipl.IbisIdentifier;
import ibis.ipl.ReceivePortIdentifier;
import ibis.ipl.SendPort;

import java.io.IOException;

public abstract class Malleability extends FaultTolerance {
	private void handleJoin(IbisIdentifier joiner) {
		try {
			ReceivePortIdentifier r = null;
			SendPort s = portType.createSendPort("satin sendport");

			r = lookup("satin port on " + joiner.name());

			if (FAULT_TOLERANCE) {
				if (!connect(s, r, connectTimeout)) {
					if (COMM_DEBUG) {
						out.println("SATIN '" + ident.name()
								+ "': unable to connect to " + joiner.name()
								+ ", might have crashed");
					}
					return;
				}
			} else {
				connect(s, r);
			}

			if (SUPPORT_TUPLE_MULTICAST) {
				connect(tuplePort, r);
			}

			synchronized (this) {
				if (FAULT_TOLERANCE) {
					globalResultTable.addReplica(joiner);
				}
				victims.add(joiner, s);
				notifyAll();
			}

			if (COMM_DEBUG) {
				out.println("SATIN '" + ident.name() + "': " + joiner.name()
						+ " JOINED");
			}
		} catch (Exception e) {
			System.err.println("SATIN '" + ident
					+ "': got an exception in Satin.join: " + e);
			e.printStackTrace(System.err);
			System.exit(1);
		}
	}

	public void join(IbisIdentifier joiner) {

		if (joiner.name().equals("ControlCentreIbis"))
			return;
		//		allIbises.add(joiner);
		if (joiner.equals(ident))
			return;

		if (COMM_DEBUG) {
			out.println("SATIN '" + ident.name() + "': '" + joiner.name()
					+ "' from cluster '" + joiner.cluster()
					+ "' is trying to join");
		}
		//		if (!victims.contains(joiner)) {
		handleJoin(joiner);
	}

	public void leave(IbisIdentifier leaver) {
		if (leaver.equals(this.ident))
			return;

		if (COMM_DEBUG) {
			out.println("SATIN '" + ident.name() + "': " + leaver.name()
					+ " left");
		}

		Victim v;

		synchronized (this) {
			if (FAULT_TOLERANCE) {
				globalResultTable.removeReplica(leaver);
			}
			v = victims.remove(leaver);
			notifyAll();

			if (v != null && v.s != null) {
				try {
					v.s.close();
				} catch (IOException e) {
					System.err.println("port.close() throws " + e);
				}
			}
		}
	}
}