import ibis.rmi.*;
import ibis.rmi.impl.RTS;
import ibis.ipl.*;

public final class rmi_stub_MinimumReceiverImpl extends ibis.rmi.impl.Stub implements ibis.rmi.Remote, MinimumReceiver {

	private ibis.util.Timer timer_0;

	public rmi_stub_MinimumReceiverImpl() {
		timer_0 = RTS.createRMITimer(this.toString() + "_update_" + 0);
	}

	public final void update(int p0) throws ibis.rmi.RemoteException {
		try {
			Exception remoteex = null;
			try {
				initSend();
				WriteMessage w = newMessage();
				w.writeInt(0);
				w.writeInt(stubID);
				w.writeInt(p0);
				RTS.startRMITimer(timer_0);
				w.finish();
				ReadMessage r = reply.receive();
				RTS.stopRMITimer(timer_0);
				if (r.readByte() == RTS.EXCEPTION) {
					remoteex = (Exception) r.readObject();
				}
				else {
				}
				r.finish();
			} catch(java.io.IOException ioex) {
				throw new RemoteException("IO exception", ioex);
			}
			if (remoteex != null) throw remoteex;
		} catch (ibis.rmi.RemoteException e0) {
			throw e0;
		} catch (RuntimeException re) {
			throw re;
		} catch (Exception e) {
			throw new RemoteException("undeclared checked exception", e);
		}
	}

}

