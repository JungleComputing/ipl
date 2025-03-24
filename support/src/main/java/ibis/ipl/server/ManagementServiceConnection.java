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
package ibis.ipl.server;

import ibis.ipl.IbisIdentifier;
import ibis.ipl.support.Connection;
import ibis.ipl.support.management.AttributeDescription;
import ibis.smartsockets.virtual.VirtualSocketAddress;
import ibis.smartsockets.virtual.VirtualSocketFactory;

class ManagementServiceConnection implements ManagementServiceInterface {

    public static final int TIMEOUT = 10000;

    private VirtualSocketAddress address;
    private VirtualSocketFactory socketFactory;

    ManagementServiceConnection(VirtualSocketAddress address, VirtualSocketFactory socketFactory) {
        this.address = address;
        this.socketFactory = socketFactory;
    }

    // Java 1.5 Does not allow @Override for interface methods
    // @Override
    @Override
    public Object[] getAttributes(IbisIdentifier ibis, AttributeDescription... descriptions) throws Exception {
        Connection connection = new Connection(address, TIMEOUT, true, socketFactory);
        try {

            connection.out().writeByte(ServerConnectionProtocol.MAGIC_BYTE);
            connection.out().writeByte(ServerConnectionProtocol.OPCODE_MANAGEMENT_GET_ATTRIBUTES);
            connection.writeObject(ibis);
            connection.writeObject(descriptions);
            connection.getAndCheckReply();

            return (Object[]) connection.readObject();
        } finally {
            connection.close();
        }
    }

}
