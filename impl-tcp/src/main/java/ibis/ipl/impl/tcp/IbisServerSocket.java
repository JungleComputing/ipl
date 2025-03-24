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
package ibis.ipl.impl.tcp;

import java.io.DataOutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

import ibis.util.IPUtils;

class IbisServerSocket {

    ServerSocket socket = null;

    IbisServerSocket(ServerSocket s) {
        socket = s;
    }

    IbisSocket accept() throws java.io.IOException {
        Socket s = socket.accept();
        int b = s.getInputStream().read();
        if (b > 1) {
            ServerSocket n = new ServerSocket();
            Socket[] result = new Socket[b];
            result[0] = s;
            try {
                InetSocketAddress local = new InetSocketAddress(IPUtils.getLocalHostAddress(), 0);
                n.bind(local);
                IbisSocketAddress addr = new IbisSocketAddress(n.getLocalSocketAddress());
                byte[] baddr = addr.toBytes();
                DataOutputStream d = new DataOutputStream(s.getOutputStream());
                d.writeInt(baddr.length);
                d.write(baddr);
                d.flush();
                for (int i = 1; i < b; i++) {
                    System.out.println("Accept from address " + addr.toString());
                    result[i] = n.accept();
                }
                return new IbisSocket(result);
            } finally {
                n.close();
            }
        } else {
            return new IbisSocket(s);
        }
    }

    IbisSocketAddress getLocalSocketAddress() {
        return new IbisSocketAddress(socket.getLocalSocketAddress());
    }

    void close() throws java.io.IOException {
        try {
            socket.close();
        } finally {
            socket = null;
        }
    }
}
