package ibis.ipl.util.rpc;

import ibis.ipl.Ibis;
import ibis.ipl.MessageUpcall;
import ibis.ipl.ReadMessage;
import ibis.ipl.ReceivePort;
import ibis.ipl.ReceivePortIdentifier;
import ibis.ipl.SendPort;
import ibis.ipl.WriteMessage;

import java.io.IOException;
import java.lang.reflect.Method;
import java.rmi.RemoteException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemoteObject<InterfaceType> implements MessageUpcall {

	private static final Logger logger = LoggerFactory
			.getLogger(RemoteObject.class);

	private final Class<InterfaceType> interfaceClass;

	private final InterfaceType theObject;

	private final String name;

	public Class<InterfaceType> getInterfaceClass() {
		return interfaceClass;
	}

	public InterfaceType getObject() {
		return theObject;
	}

	public String getName() {
		return name;
	}

	public ReceivePort getReceivePort() {
		return receivePort;
	}

	private final Ibis ibis;

	private final ReceivePort receivePort;

	RemoteObject(Class<InterfaceType> interfaceClass, InterfaceType theObject,
			String name, Ibis ibis) throws RemoteException {
		this.interfaceClass = interfaceClass;
		this.theObject = theObject;
		this.ibis = ibis;

		// check if all methods of given interface throw a RemoteException
		for (Method method : interfaceClass.getDeclaredMethods()) {
			boolean found = false;
			for (Class<?> clazz : method.getExceptionTypes()) {
				if (clazz.equals(RemoteException.class)) {
					found = true;
				}
			}
			if (!found) {
				throw new RemoteException("required RemoteException not thrown"
						+ " by remote method \"" + method.getName()
						+ "\" in remote object interface \""
						+ interfaceClass.getName() + "\"");
			}
		}

		try {
			receivePort = ibis.createReceivePort(RPC.rpcRequestPortType, name,
					this);
			// enable connections
			receivePort.enableConnections();
			// enable upcalls
			receivePort.enableMessageUpcalls();

			// set after creating receive port in case of anonymous remote
			// object
			this.name = receivePort.name();

			logger.debug("remote object " + this + " created");

		} catch (IOException e) {
			throw new RemoteException(
					"cannot create receive port for remote object", e);
		}
	}

	public void unexport() throws IOException {
		receivePort.close();
	}

	/**
	 * Function called by Ibis to give us a newly arrived message. Not meant to
	 * be called by users.
	 */
	public void upcall(ReadMessage message) throws IOException,
			ClassNotFoundException {

		// read request
		ReceivePortIdentifier requestor = (ReceivePortIdentifier) message
				.readObject();
		String methodName = message.readString();
		Class<?>[] parameterTypes = (Class<?>[]) message.readObject();
		Object[] args = (Object[]) message.readObject();
		message.finish();

		if (logger.isDebugEnabled()) {
			logger.debug("received invocation for remote object. name = "
					+ name + ", method name =  " + methodName);
		}

		// create reply connection.

		SendPort replyPort = ibis.createSendPort(RPC.rpcReplyPortType);
		replyPort.connect(requestor);

		boolean success;
		Object result = null;
		try {
			Method method = interfaceClass.getDeclaredMethod(methodName,
					parameterTypes);

			result = method.invoke(theObject, args);
			success = true;
		} catch (Throwable exception) {
			// method threw an exception, return to caller
			result = exception;
			success = false;
		}

		// send reply message
		WriteMessage reply = replyPort.newMessage();
		reply.writeBoolean(success);
		reply.writeObject(result);
		System.err.println("bytes send in reply = " + reply.finish());

		// cleanup
		replyPort.close();
	}

	public String toString() {
		return name;
	}

}
