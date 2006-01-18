import ibis.rmi.*;
import ibis.rmi.impl.RTS;
import java.lang.reflect.*;
import ibis.ipl.*;
import java.io.IOException;
import colobus.Colobus;

public final class rmi_skeleton_MinimumImpl extends ibis.rmi.impl.Skeleton {

	private ibis.util.Timer timer_0;
	private ibis.util.Timer timer_1;
	private ibis.util.Timer timer_2;

	private static final Colobus colobus = Colobus.getColobus(rmi_skeleton_MinimumImpl.class.getName());

	public rmi_skeleton_MinimumImpl() {
		stubType = "rmi_stub_MinimumImpl";
		timer_0 = RTS.createRMITimer(this.toString() + "_set_" + 0);
		timer_1 = RTS.createRMITimer(this.toString() + "_get_" + 1);
		timer_2 = RTS.createRMITimer(this.toString() + "_register_" + 2);
	}

	public final void upcall(ReadMessage r, int method, int stubID) throws ibis.rmi.RemoteException {


		RTS.setClientHost(r.origin().ibis().toString());
		Exception ex = null;

		switch(method) {
		case 0: /* method set */
		{
			long handle = colobus.fireStartEvent("RMI parameter deserialization of method set");
			RTS.startRMITimer(timer_0);
			/* First - Extract the parameters */
			int p0;
			try {
				p0 = r.readInt();
			colobus.fireStopEvent(handle, "RMI parameter deserialization of method set");
				r.finish();
			} catch(IOException e) {
			colobus.fireStopEvent(handle, "RMI parameter deserialization of method set");
				throw new ibis.rmi.UnmarshalException("error unmarshalling arguments", e);
			}

			handle = colobus.fireStartEvent("RMI user method invocation of method set");
			/* Second - Invoke the method */
			try {
				((MinimumImpl) destination).set(p0);
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
			colobus.fireStopEvent(handle, "RMI user method invocation of method set");
			handle = colobus.fireStartEvent("RMI reply message of method set");
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
				colobus.fireStopEvent(handle, "RMI reply message of method set");
				throw new ibis.rmi.MarshalException("error marshalling return", e);
			}
			colobus.fireStopEvent(handle, "RMI reply message of method set");
			break;
		}

		case 1: /* method get */
		{
			long handle = colobus.fireStartEvent("RMI parameter deserialization of method get");
			RTS.startRMITimer(timer_1);
			/* First - Extract the parameters */
			colobus.fireStopEvent(handle, "RMI parameter deserialization of method get");

			handle = colobus.fireStartEvent("RMI user method invocation of method get");
			/* Second - Invoke the method */
			int result = 0;
			try {
				result = ((MinimumImpl) destination).get();
			} catch (ibis.rmi.RemoteException e) {
				ex = new ibis.rmi.ServerException("server exception", e);
			} catch (RuntimeException e) {
				ex = new ibis.rmi.ServerRuntimeException("server runtime exception", e);
			} catch (Error e) {
				ex = new ibis.rmi.ServerError("server error", e);
			} catch (Exception e) {
				ex = e;
			}
			RTS.stopRMITimer(timer_1);
			colobus.fireStopEvent(handle, "RMI user method invocation of method get");
			handle = colobus.fireStartEvent("RMI reply message of method get");
			try {
				WriteMessage w = stubs[stubID].newMessage();
				if (ex != null) {
					w.writeByte(RTS.EXCEPTION);
					w.writeObject(ex);
				} else {
					w.writeByte(RTS.RESULT);
					w.writeInt(result);
				}
				w.finish();
			} catch(IOException e) {
				colobus.fireStopEvent(handle, "RMI reply message of method get");
				throw new ibis.rmi.MarshalException("error marshalling return", e);
			}
			colobus.fireStopEvent(handle, "RMI reply message of method get");
			break;
		}

		case 2: /* method register */
		{
			long handle = colobus.fireStartEvent("RMI parameter deserialization of method register");
			RTS.startRMITimer(timer_2);
			/* First - Extract the parameters */
			MinimumReceiver p0;
			try {
				p0 = (MinimumReceiver) r.readObject();
			colobus.fireStopEvent(handle, "RMI parameter deserialization of method register");
			} catch(ClassNotFoundException e) {
			colobus.fireStopEvent(handle, "RMI parameter deserialization of method register");
				throw new ibis.rmi.UnmarshalException("error unmarshalling arguments", e);
			} catch(IOException e) {
			colobus.fireStopEvent(handle, "RMI parameter deserialization of method register");
				throw new ibis.rmi.UnmarshalException("error unmarshalling arguments", e);
			}

			handle = colobus.fireStartEvent("RMI user method invocation of method register");
			/* Second - Invoke the method */
			try {
				((MinimumImpl) destination).register(p0);
			} catch (ibis.rmi.RemoteException e) {
				ex = new ibis.rmi.ServerException("server exception", e);
			} catch (RuntimeException e) {
				ex = new ibis.rmi.ServerRuntimeException("server runtime exception", e);
			} catch (Error e) {
				ex = new ibis.rmi.ServerError("server error", e);
			} catch (Exception e) {
				ex = e;
			}
			RTS.stopRMITimer(timer_2);
			colobus.fireStopEvent(handle, "RMI user method invocation of method register");
			handle = colobus.fireStartEvent("RMI reply message of method register");
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
				colobus.fireStopEvent(handle, "RMI reply message of method register");
				throw new ibis.rmi.MarshalException("error marshalling return", e);
			}
			colobus.fireStopEvent(handle, "RMI reply message of method register");
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

