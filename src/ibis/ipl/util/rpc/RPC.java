package ibis.ipl.util.rpc;

import java.io.IOException;
import java.lang.reflect.Proxy;

import ibis.ipl.Ibis;
import ibis.ipl.IbisIdentifier;
import ibis.ipl.PortType;
import ibis.ipl.ReceivePortIdentifier;

public class RPC {

	/**
	 * Port type used for sending a request to the server.
	 */
	public static final PortType rpcRequestPortType = new PortType(
			PortType.COMMUNICATION_RELIABLE, PortType.SERIALIZATION_OBJECT,
			PortType.RECEIVE_AUTO_UPCALLS, PortType.CONNECTION_MANY_TO_ONE);

	/**
	 * Port type used for sending a reply back.
	 */
	public static final PortType rpcReplyPortType = new PortType(
			PortType.COMMUNICATION_RELIABLE, PortType.SERIALIZATION_OBJECT,
			PortType.RECEIVE_EXPLICIT, PortType.CONNECTION_ONE_TO_ONE);

	public static final PortType[] rpcPortTypes = { rpcRequestPortType,
			rpcReplyPortType };

	public static <InterfaceType extends Object> RemoteObject<InterfaceType> exportObject(
			Class<InterfaceType> interfaceClass, InterfaceType theObject,
			String name, Ibis ibis) throws IOException, RemoteException {
		return new RemoteObject<InterfaceType>(interfaceClass, theObject, name,
				ibis);
	}

	public static <InterfaceType extends Object> InterfaceType createProxy(
			Class<InterfaceType> interfaceClass, ReceivePortIdentifier address,
			Ibis ibis) {
		return createProxy(interfaceClass, address.ibisIdentifier(), address
				.name(), ibis);
	}

	@SuppressWarnings("unchecked")
	public static <InterfaceType extends Object> InterfaceType createProxy(
			Class<InterfaceType> interfaceClass, IbisIdentifier ibisIdentifier,
			String name, Ibis ibis) {

		RPCInvocationHandler handler = new RPCInvocationHandler(ibisIdentifier,
				name, ibis);

		InterfaceType result = (InterfaceType) Proxy.newProxyInstance(
				interfaceClass.getClassLoader(),
				new Class[] { interfaceClass }, handler);

		return result;
	}
}
