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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import ibis.ipl.support.Connection;
import ibis.smartsockets.virtual.VirtualSocketAddress;
import ibis.smartsockets.virtual.VirtualSocketFactory;

class RegistryServiceConnection implements RegistryServiceInterface {

    public static final int TIMEOUT = 10000;

    private final VirtualSocketAddress address;
    private final VirtualSocketFactory socketFactory;

    RegistryServiceConnection(VirtualSocketAddress address, VirtualSocketFactory socketFactory) {
        this.address = address;
        this.socketFactory = socketFactory;
    }

    // 1.5 Does not allow @Override for interface methods
    // @Override
    @Override
    public String[] getPools() throws IOException {
        Connection connection = new Connection(address, TIMEOUT, true, socketFactory);
        try {
            connection.out().writeByte(ServerConnectionProtocol.MAGIC_BYTE);
            connection.out().writeByte(ServerConnectionProtocol.OPCODE_REGISTRY_GET_POOLS);
            connection.getAndCheckReply();
            int nrOfHubs = connection.in().readInt();
            if (nrOfHubs < 0) {
                throw new IOException("Negative number of hubs");
            }
            String[] result = new String[nrOfHubs];
            for (int i = 0; i < nrOfHubs; i++) {
                result[i] = connection.in().readUTF();
            }
            return result;
        } finally {
            connection.close();
        }
    }

    // @Override
    @Override
    public ibis.ipl.IbisIdentifier[] getMembers(String poolName) throws IOException {
        Connection connection = new Connection(address, TIMEOUT, true, socketFactory);
        try {

            connection.out().writeByte(ServerConnectionProtocol.MAGIC_BYTE);
            connection.out().writeByte(ServerConnectionProtocol.OPCODE_REGISTRY_GET_MEMBERS);
            connection.out().writeUTF(poolName);
            connection.getAndCheckReply();

            int nrOfMembers = connection.in().readInt();

            if (nrOfMembers < 0) {
                throw new IOException("negative number of members received");
            }

            ibis.ipl.IbisIdentifier[] result = new ibis.ipl.IbisIdentifier[nrOfMembers];
            for (int i = 0; i < nrOfMembers; i++) {
                result[i] = new ibis.ipl.impl.IbisIdentifier(connection.in());
            }

            return result;
        } finally {
            connection.close();
        }
    }

    // @Override
    @Override
    public String[] getLocations(String poolName) throws IOException {
        Connection connection = new Connection(address, TIMEOUT, true, socketFactory);
        try {
            connection.out().writeByte(ServerConnectionProtocol.MAGIC_BYTE);
            connection.out().writeByte(ServerConnectionProtocol.OPCODE_REGISTRY_GET_LOCATIONS);
            connection.out().writeUTF(poolName);
            connection.getAndCheckReply();
            int nrOfLocations = connection.in().readInt();
            if (nrOfLocations < 0) {
                throw new IOException("Negative number of locations");
            }
            String[] result = new String[nrOfLocations];
            for (int i = 0; i < nrOfLocations; i++) {
                result[i] = connection.in().readUTF();
            }
            return result;
        } finally {
            connection.close();
        }
    }

    // @Override
    @Override
    public Map<String, Integer> getPoolSizes() throws IOException {
        Connection connection = new Connection(address, TIMEOUT, true, socketFactory);
        try {
            connection.out().writeByte(ServerConnectionProtocol.MAGIC_BYTE);
            connection.out().writeByte(ServerConnectionProtocol.OPCODE_REGISTRY_GET_POOL_SIZES);
            connection.getAndCheckReply();
            int nrOfPoolSizes = connection.in().readInt();
            if (nrOfPoolSizes < 0) {
                throw new IOException("Negative number of pool sizes");
            }
            Map<String, Integer> result = new HashMap<>();
            for (int i = 0; i < nrOfPoolSizes; i++) {
                result.put(connection.in().readUTF(), connection.in().readInt());
            }
            return result;
        } finally {
            connection.close();
        }
    }

}
