import ibis.rmi.*;
import ibis.rmi.impl.RTS;
import java.lang.reflect.*;
import ibis.ipl.*;
import java.io.IOException;
import colobus.Colobus;

public final class rmi_skeleton_JobQueueImpl extends ibis.rmi.impl.Skeleton {

	private ibis.util.Timer timer_0;
	private ibis.util.Timer timer_1;
	private ibis.util.Timer timer_2;

	private static final Colobus colobus = Colobus.getColobus(rmi_skeleton_JobQueueImpl.class.getName());

	public rmi_skeleton_JobQueueImpl() {
		stubType = "rmi_stub_JobQueueImpl";
		timer_0 = RTS.createRMITimer(this.toString() + "_getJob_" + 0);
		timer_1 = RTS.createRMITimer(this.toString() + "_jobDone_" + 1);
		timer_2 = RTS.createRMITimer(this.toString() + "_allStarted_" + 2);
	}

	public final void upcall(ReadMessage r, int method, int stubID) throws ibis.rmi.RemoteException {


		RTS.setClientHost(r.origin().ibis().toString());
		Exception ex = null;

		switch(method) {
		case 0: /* method getJob */
		{
			long handle = colobus.fireStartEvent("RMI parameter deserialization of method getJob");
			RTS.startRMITimer(timer_0);
			/* First - Extract the parameters */
			try {
			colobus.fireStopEvent(handle, "RMI parameter deserialization of method getJob");
				r.finish();
			} catch(IOException e) {
			colobus.fireStopEvent(handle, "RMI parameter deserialization of method getJob");
				throw new ibis.rmi.UnmarshalException("error unmarshalling arguments", e);
			}

			handle = colobus.fireStartEvent("RMI user method invocation of method getJob");
			/* Second - Invoke the method */
			Job result = null;
			try {
				result = ((JobQueueImpl) destination).getJob();
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
			colobus.fireStopEvent(handle, "RMI user method invocation of method getJob");
			handle = colobus.fireStartEvent("RMI reply message of method getJob");
			try {
				WriteMessage w = stubs[stubID].newMessage();
				if (ex != null) {
					w.writeByte(RTS.EXCEPTION);
					w.writeObject(ex);
				} else {
					w.writeByte(RTS.RESULT);
					w.writeObject(result);
				}
				w.finish();
			} catch(IOException e) {
				colobus.fireStopEvent(handle, "RMI reply message of method getJob");
				throw new ibis.rmi.MarshalException("error marshalling return", e);
			}
			colobus.fireStopEvent(handle, "RMI reply message of method getJob");
			break;
		}

		case 1: /* method jobDone */
		{
			long handle = colobus.fireStartEvent("RMI parameter deserialization of method jobDone");
			RTS.startRMITimer(timer_1);
			/* First - Extract the parameters */
			try {
			colobus.fireStopEvent(handle, "RMI parameter deserialization of method jobDone");
				r.finish();
			} catch(IOException e) {
			colobus.fireStopEvent(handle, "RMI parameter deserialization of method jobDone");
				throw new ibis.rmi.UnmarshalException("error unmarshalling arguments", e);
			}

			handle = colobus.fireStartEvent("RMI user method invocation of method jobDone");
			/* Second - Invoke the method */
			try {
				((JobQueueImpl) destination).jobDone();
			} catch (RuntimeException e) {
				ex = new ibis.rmi.ServerRuntimeException("server runtime exception", e);
			} catch (Error e) {
				ex = new ibis.rmi.ServerError("server error", e);
			} catch (Exception e) {
				ex = e;
			}
			RTS.stopRMITimer(timer_1);
			colobus.fireStopEvent(handle, "RMI user method invocation of method jobDone");
			handle = colobus.fireStartEvent("RMI reply message of method jobDone");
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
				colobus.fireStopEvent(handle, "RMI reply message of method jobDone");
				throw new ibis.rmi.MarshalException("error marshalling return", e);
			}
			colobus.fireStopEvent(handle, "RMI reply message of method jobDone");
			break;
		}

		case 2: /* method allStarted */
		{
			long handle = colobus.fireStartEvent("RMI parameter deserialization of method allStarted");
			RTS.startRMITimer(timer_2);
			/* First - Extract the parameters */
			int p0;
			try {
				p0 = r.readInt();
			colobus.fireStopEvent(handle, "RMI parameter deserialization of method allStarted");
				r.finish();
			} catch(IOException e) {
			colobus.fireStopEvent(handle, "RMI parameter deserialization of method allStarted");
				throw new ibis.rmi.UnmarshalException("error unmarshalling arguments", e);
			}

			handle = colobus.fireStartEvent("RMI user method invocation of method allStarted");
			/* Second - Invoke the method */
			try {
				((JobQueueImpl) destination).allStarted(p0);
			} catch (RuntimeException e) {
				ex = new ibis.rmi.ServerRuntimeException("server runtime exception", e);
			} catch (Error e) {
				ex = new ibis.rmi.ServerError("server error", e);
			} catch (Exception e) {
				ex = e;
			}
			RTS.stopRMITimer(timer_2);
			colobus.fireStopEvent(handle, "RMI user method invocation of method allStarted");
			handle = colobus.fireStartEvent("RMI reply message of method allStarted");
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
				colobus.fireStopEvent(handle, "RMI reply message of method allStarted");
				throw new ibis.rmi.MarshalException("error marshalling return", e);
			}
			colobus.fireStopEvent(handle, "RMI reply message of method allStarted");
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

