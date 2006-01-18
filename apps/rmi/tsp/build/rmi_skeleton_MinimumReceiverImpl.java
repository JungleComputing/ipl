import ibis.rmi.*;
import ibis.rmi.impl.RTS;
import java.lang.reflect.*;
import ibis.ipl.*;
import java.io.IOException;
import colobus.Colobus;

public final class rmi_skeleton_MinimumReceiverImpl extends ibis.rmi.impl.Skeleton {

	private ibis.util.Timer timer_0;

	private static final Colobus colobus = Colobus.getColobus(rmi_skeleton_MinimumReceiverImpl.class.getName());

	public rmi_skeleton_MinimumReceiverImpl() {
		stubType = "rmi_stub_MinimumReceiverImpl";
		timer_0 = RTS.createRMITimer(this.toString() + "_update_" + 0);
	}

	public final void upcall(ReadMessage r, int method, int stubID) throws ibis.rmi.RemoteException {


		RTS.setClientHost(r.origin().ibis().toString());
		Exception ex = null;

		switch(method) {
		case 0: /* method update */
		{
			long handle = colobus.fireStartEvent("RMI parameter deserialization of method update");
			RTS.startRMITimer(timer_0);
			/* First - Extract the parameters */
			int p0;
			try {
				p0 = r.readInt();
			colobus.fireStopEvent(handle, "RMI parameter deserialization of method update");
				r.finish();
			} catch(IOException e) {
			colobus.fireStopEvent(handle, "RMI parameter deserialization of method update");
				throw new ibis.rmi.UnmarshalException("error unmarshalling arguments", e);
			}

			handle = colobus.fireStartEvent("RMI user method invocation of method update");
			/* Second - Invoke the method */
			try {
				((MinimumReceiverImpl) destination).update(p0);
			} catch (ibis.rmi.RemoteException e) {
				ex = new ibis.rmi.ServerException("server exception", e);
			} catch (RuntimeException e) {
				ex = new ibis.rmi.ServerRuntimeException("server runtime exception", e);
			} catch (Error e) {
				ex = new ibis.rmi.ServerError("server error", e);
			} catch (Exception e) {
				ex = e;
			}
			RTS.stopRMITimer(timer_0);
			colobus.fireStopEvent(handle, "RMI user method invocation of method update");
			handle = colobus.fireStartEvent("RMI reply message of method update");
			try {
				WriteMessage w = stubs[stubID].newMessage();
				if (ex != null) {
					w.writeByte(RTS.EXCEPTION);
					w.writeObject(ex);
				} else {
					w.writeByte(RTS.RESULT);
				}
				w.finish();
			} catch(IOException e) {
				colobus.fireStopEvent(handle, "RMI reply message of method update");
				throw new ibis.rmi.MarshalException("error marshalling return", e);
			}
			colobus.fireStopEvent(handle, "RMI reply message of method update");
			break;
		}

		case -1:
		{
			/* Special case: new stub connecting */
			ReceivePortIdentifier rpi;
			try {
				rpi = (ReceivePortIdentifier) r.readObject();
				r.finish();
			} catch(ClassNotFoundException e) {
				throw new ibis.rmi.UnmarshalException("while reading ReceivePortIdentifier", e);
			} catch(IOException e) {
				throw new ibis.rmi.UnmarshalException("while reading ReceivePortIdentifier", e);
			}
			int id = addStub(rpi);
			try {
				WriteMessage w = stubs[id].newMessage();
				w.writeInt(id);
				w.writeInt(skeletonId);
				w.writeObject(destination);
				w.finish();
			} catch(IOException e) {
				throw new ibis.rmi.MarshalException("error sending skeletonId", e);
			}
			break;
		}

		default:
			throw new ibis.rmi.UnmarshalException("invalid method number");
		}

	}

}

