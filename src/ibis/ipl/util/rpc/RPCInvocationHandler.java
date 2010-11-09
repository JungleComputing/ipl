package ibis.ipl.util.rpc;

import ibis.ipl.Ibis;
import ibis.ipl.IbisIdentifier;
import ibis.ipl.ReadMessage;
import ibis.ipl.ReceivePort;
import ibis.ipl.SendPort;
import ibis.ipl.WriteMessage;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.rmi.RemoteException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class RPCInvocationHandler implements InvocationHandler {

	private static final Logger logger = LoggerFactory
			.getLogger(RPCInvocationHandler.class);

	private final Ibis ibis;
	private final IbisIdentifier ibisIdentifier;
	private final String name;

	RPCInvocationHandler(IbisIdentifier ibisIdentifier, String name, Ibis ibis) {
		this.ibisIdentifier = ibisIdentifier;
		this.name = name;
		this.ibis = ibis;
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] args)
			throws Throwable {

		try {

			if (logger.isDebugEnabled()) {
				logger.debug("calling remote object " + name + ", method = "
						+ method.getName());
			}

			// Create a send port for sending the request and connect.
			SendPort sendPort = ibis.createSendPort(RPC.rpcRequestPortType);
			sendPort.connect(ibisIdentifier, name);

			// Create a receive port for receiving the reply from the server
			// this receive port does not need a name, as we will send the
			// ReceivePortIdentifier to the server directly
			ReceivePort receivePort = ibis.createReceivePort(
					RPC.rpcReplyPortType, null);
			receivePort.enableConnections();

			// Send the request message. This message contains the identifier of
			// our receive port so the server knows where to send the reply
			WriteMessage request = sendPort.newMessage();
			request.writeObject(receivePort.identifier());
			request.writeString(method.getName());
			request.writeObject(method.getParameterTypes());
			request.writeObject(args);
			request.finish();
			
			ReadMessage reply = receivePort.receive();
			boolean success = reply.readBoolean();
			Object result = reply.readObject();
			reply.finish();

			if (logger.isDebugEnabled()) {
				logger.debug("remote object \"" + name + "\", method \"" + method.getName()
						+ "\" result = " + result);
			}

			// Close ports.
			sendPort.close();
			receivePort.close();

			if (success) {
				return result;
			} else if (result instanceof InvocationTargetException) {
				InvocationTargetException exception = (InvocationTargetException) result;
				
				//throw user exception
				throw exception.getTargetException();
			} else {
				//some error occured while doing remote call
				throw new RemoteException("exception while performing remote call", (Throwable) result);
			}
		} catch (IOException e) {
			throw new RemoteException("invocation failed", e);
		}
	}
}
