import ibis.rmi.*;
import ibis.rmi.impl.RTS;
import ibis.ipl.*;

public final class rmi_stub_MinimumImpl extends ibis.rmi.impl.Stub implements ibis.rmi.Remote, Minimum {

	private ibis.util.Timer timer_0;
	private ibis.util.Timer timer_1;
	private ibis.util.Timer timer_2;

	public rmi_stub_MinimumImpl() {
		timer_0 = RTS.createRMITimer(this.toString() + "_set_" + 0);
		timer_1 = RTS.createRMITimer(this.toString() + "_get_" + 1);
		timer_2 = RTS.createRMITimer(this.toString() + "_register_" + 2);
	}

	public final void set(int p0) throws ibis.rmi.RemoteException {
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

	public final int get() throws ibis.rmi.RemoteException {
		int result = 0;
		try {
			Exception remoteex = null;
			try {
				initSend();
				WriteMessage w = newMessage();
				w.writeInt(1);
				w.writeInt(stubID);
				RTS.startRMITimer(timer_1);
				w.finish();
				ReadMessage r = reply.receive();
				RTS.stopRMITimer(timer_1);
				if (r.readByte() == RTS.EXCEPTION) {
					remoteex = (Exception) r.readObject();
				}
				else {
					result = r.readInt();
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
		return result;
	}

	public final void register(MinimumReceiver p0) throws ibis.rmi.RemoteException {
		try {
			Exception remoteex = null;
			try {
				initSend();
				WriteMessage w = newMessage();
				w.writeInt(2);
				w.writeInt(stubID);
				w.writeObject(p0);
				RTS.startRMITimer(timer_2);
				w.finish();
				ReadMessage r = reply.receive();
				RTS.stopRMITimer(timer_2);
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

