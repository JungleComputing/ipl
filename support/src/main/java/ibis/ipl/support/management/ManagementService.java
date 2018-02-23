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
package ibis.ipl.support.management;

import java.io.IOException;

import ibis.io.Conversion;
import ibis.ipl.IbisIdentifier;
import ibis.ipl.server.ManagementServiceInterface;
import ibis.ipl.support.Connection;
import ibis.smartsockets.virtual.VirtualSocketFactory;
import ibis.util.TypedProperties;

public class ManagementService implements ibis.ipl.server.Service,
		ManagementServiceInterface {

	private static final int CONNECT_TIMEOUT = 10000;
	private final VirtualSocketFactory factory;

	public ManagementService(TypedProperties properties,
			VirtualSocketFactory factory) {
		this.factory = factory;
	}

	public void end(long deadline) {
		// NOTHING
	}

	public String getServiceName() {
		return "management";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * ibis.ipl.management.ManagementServerInterface#getAttributes(ibis.ipl.
	 * IbisIdentifier, ibis.ipl.management.AttributeDescription)
	 */
	public Object[] getAttributes(IbisIdentifier ibis,
			AttributeDescription... descriptions) throws IOException {
		ibis.ipl.impl.IbisIdentifier identifier;
		try {
			identifier = (ibis.ipl.impl.IbisIdentifier) ibis;
		} catch (ClassCastException e) {
			throw new IOException(
					"cannot cast given identifier to implementation identifier: " + e);
		}

		Connection connection = new Connection(identifier, CONNECT_TIMEOUT,
				false, factory, Protocol.VIRTUAL_PORT);
		connection.out().writeByte(Protocol.MAGIC_BYTE);
		connection.out().writeByte(Protocol.OPCODE_GET_MONITOR_INFO);

		connection.out().writeInt(descriptions.length);
		for (int i = 0; i < descriptions.length; i++) {
			connection.out().writeUTF(descriptions[i].getBeanName());
			connection.out().writeUTF(descriptions[i].getAttribute());
		}

		connection.getAndCheckReply();

		int length = connection.in().readInt();
		if (length < 0) {
			connection.close();
			throw new IOException("End of Stream on reading from connection");
		}

		byte[] resultBytes = new byte[length];

		connection.in().readFully(resultBytes);

		Object[] reply;
		try {
			reply = (Object[]) Conversion.byte2object(resultBytes);
		} catch (ClassNotFoundException e) {
			throw new IOException("Cannot cast result " + e);
		}

		connection.close();

		return reply;
	}

	public String toString() {
		return "Management service on virtual port " + Protocol.VIRTUAL_PORT;
	}

}
