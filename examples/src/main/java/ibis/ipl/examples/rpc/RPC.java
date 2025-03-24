/*
 * Copyright 2010 Vrije Universiteit
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ibis.ipl.examples.rpc;

import java.io.IOException;
import java.lang.reflect.Proxy;

import ibis.ipl.Ibis;
import ibis.ipl.IbisIdentifier;
import ibis.ipl.PortType;
import ibis.ipl.ReceivePortIdentifier;

/**
 * Small, Simple Remote Procedure Call (RPC) implementation which uses the IPL
 * for communication.
 * 
 * @author Niels Drost
 * 
 */
public class RPC {

	/**
	 * Port type used for sending a request to the server. Must be added to port
	 * type of Ibis used for RPC
	 */
	public static final PortType rpcRequestPortType = new PortType(
			PortType.COMMUNICATION_RELIABLE, PortType.SERIALIZATION_OBJECT,
			PortType.RECEIVE_AUTO_UPCALLS, PortType.CONNECTION_MANY_TO_ONE);

	/**
	 * Port type used for sending a reply back. Must be added to port type of
	 * Ibis used for RPC
	 */
	public static final PortType rpcReplyPortType = new PortType(
			PortType.COMMUNICATION_RELIABLE, PortType.SERIALIZATION_OBJECT,
			PortType.RECEIVE_EXPLICIT, PortType.CONNECTION_ONE_TO_ONE);

	/**
	 * Port type used in RPC. Must be added to port type of Ibis used for RPC
	 */
	public static final PortType[] rpcPortTypes = { rpcRequestPortType,
			rpcReplyPortType };

	/**
	 * Exports an object, making it remotely accessible. Creates an IPL
	 * ReceivePort to receive messages/calls for the object.
	 * 
	 * 
	 * @param <InterfaceType>
	 *            Type of Interface which defines all the remotely accessible
	 *            functions.
	 * @param interfaceClass
	 *            Interface which defines all the remotely accessible functions.
	 *            All functions in this interface must declare to throw a
	 *            {@link RemoteException}.
	 * @param theObject
	 *            the object to be remotely accessible. Must implement the
	 *            interface given
	 * @param name
	 *            the name of the remote object. Used as the name of the
	 *            receiveport. null for anonymous.
	 * @param ibis
	 *            the ibis used to create the receive port.
	 * @return the RemoteObject
	 * @throws IOException
	 *             if creating the receive port failed
	 * @throws RemoteException
	 *             if the given interface does not meet the requirements.
	 */
	public static <InterfaceType extends Object> RemoteObject<InterfaceType> exportObject(
			Class<InterfaceType> interfaceClass, InterfaceType theObject,
			String name, Ibis ibis) throws IOException, RemoteException {
		return new RemoteObject<InterfaceType>(interfaceClass, theObject, name,
				ibis);
	}

	/**
	 * Creates a proxy to the remote object specified. Hides all communication,
	 * presenting the user with the given interface. All calls to methods in the
	 * interface will be forwarded to the remote object. In case of a
	 * communication error, a {@link RemoteException} will be thrown.
	 * 
	 * 
	 * @param <InterfaceType>
	 *            Type of Interface which defines all the remotely accessible
	 *            functions.
	 * @param interfaceClass
	 *            Interface which defines all the remotely accessible functions.
	 *            All functions in this interface must declare to throw a
	 *            {@link RemoteException}.
	 * @param address
	 *            Address of the remote object
	 * @param ibis
	 *            the ibis used to connect to the remote object
	 * @return a proxy to the remote object.
	 */
	public static <InterfaceType extends Object> InterfaceType createProxy(
			Class<InterfaceType> interfaceClass, ReceivePortIdentifier address,
			Ibis ibis) {
		return createProxy(interfaceClass, address.ibisIdentifier(), address
				.name(), ibis);
	}

	/**
	 * Creates a proxy to the remote object specified. Hides all communication,
	 * presenting the user with the given interface. All calls to methods in the
	 * interface will be forwarded to the remote object. In case of a
	 * communication error, a {@link RemoteException} will be thrown.
	 * 
	 * 
	 * @param <InterfaceType>
	 *            Type of Interface which defines all the remotely accessible
	 *            functions.
	 * @param interfaceClass
	 *            Interface which defines all the remotely accessible functions.
	 *            All functions in this interface must declare to throw a
	 *            {@link RemoteException}.
	 * @param address
	 *            Address of the Ibis of the remote object
	 * @param name
	 *            Name of the (receiveport of the) remote object.
	 * @param ibis
	 *            the ibis used to connect to the remote object
	 * @return a proxy to the remote object.
	 */
	@SuppressWarnings("unchecked")
	public static <InterfaceType extends Object> InterfaceType createProxy(
			Class<InterfaceType> interfaceClass, IbisIdentifier address,
			String name, Ibis ibis) {

		RPCInvocationHandler handler = new RPCInvocationHandler(address, name,
				ibis);

		InterfaceType result = (InterfaceType) Proxy.newProxyInstance(
				interfaceClass.getClassLoader(),
				new Class[] { interfaceClass }, handler);

		return result;
	}
}