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
/**
 * 
 */
package ibis.ipl.registry.gossip;

import ibis.smartsockets.virtual.VirtualSocketAddress;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

class ARRGCacheEntry {
    private final VirtualSocketAddress address;

    private final boolean arrgOnly;

    ARRGCacheEntry(VirtualSocketAddress address, boolean arrgOnly) {
        this.address = address;
        this.arrgOnly = arrgOnly;
    }

    ARRGCacheEntry(DataInputStream in) throws IOException {
        try {
            address = new VirtualSocketAddress(in);
            arrgOnly = in.readBoolean();
        } catch (IOException e) {
            throw e;
        } catch (Throwable e) {
            IOException exception = new IOException("could not read entry"); 
            exception.initCause(e);
            throw exception;
        }
    }

    void writeTo(DataOutputStream out) throws IOException {
        address.write(out);
        out.writeBoolean(arrgOnly);
    }

    /**
     * @return the address
     */
    public VirtualSocketAddress getAddress() {
        return address;
    }

    /**
     * @return true if this peer only runs the ARRG algorithm, not the registry
     *         service
     */
    public boolean isArrgOnly() {
        return arrgOnly;
    }

    public boolean sameAddressAs(ARRGCacheEntry entry) {
        return address.equals(entry.address);
    }

    public String toString() {
        return "address: " + address + ", arrg only: " + arrgOnly;
    }

}