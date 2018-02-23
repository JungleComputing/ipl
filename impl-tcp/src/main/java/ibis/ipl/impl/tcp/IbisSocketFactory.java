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

import ibis.ipl.IbisConfigurationException;
import ibis.ipl.impl.IbisIdentifier;
import ibis.util.IPUtils;
import ibis.util.TypedProperties;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class IbisSocketFactory {

    private static final Logger logger = LoggerFactory
            .getLogger(IbisSocketFactory.class);

    IbisSocketFactory(TypedProperties properties)
            throws IbisConfigurationException, IOException {
    }

    void setIdent(IbisIdentifier id) {
    }

    IbisServerSocket createServerSocket(int port, int backlog, boolean retry,
            Properties properties) throws IOException {
        ServerSocket server = new ServerSocket();
        InetSocketAddress local = new InetSocketAddress(
                IPUtils.getLocalHostAddress(), port);
        server.bind(local, backlog);
        return new IbisServerSocket(server);
    }

    IbisSocket createClientSocket(IbisSocketAddress addr, int timeout,
            boolean fillTimeout, Map<String, String> properties)
            throws IOException {

        int nparallel = 1;
        Socket s = new Socket();
        s.connect(addr.address, timeout);
        if (properties != null) {
            String np = properties.get("nParallelStreams");
            System.out.println("nParallelStreams = " + np);
            if (np != null) {
                try {
                    nparallel = Integer.parseInt(np);
                } catch (Throwable e) {
                    // ignore
                }
            }
        }
        s.getOutputStream().write(nparallel);
        s.getOutputStream().flush();
        if (nparallel > 1) {
            Socket[] result = new Socket[nparallel];
            result[0] = s;
            DataInputStream b = new DataInputStream(s.getInputStream());
            int sz = b.readInt();
            byte[] buf = new byte[sz];
            b.readFully(buf);
            addr = new IbisSocketAddress(buf);
            for (int i = 1; i < nparallel; i++) {
                result[i] = new Socket();
                System.out.println("Connecting to " + addr.toString());
                result[i].connect(addr.address, timeout);
            }
            return new IbisSocket(result);
        } else {
            return new IbisSocket(s);
        }
    }

    void printStatistics(String s) {
    }
}
