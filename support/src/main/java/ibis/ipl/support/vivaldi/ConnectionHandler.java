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
package ibis.ipl.support.vivaldi;

import ibis.ipl.support.Connection;
import ibis.smartsockets.virtual.VirtualServerSocket;
import ibis.util.ThreadPool;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConnectionHandler implements Runnable {

    private static final Logger logger = LoggerFactory
            .getLogger(ConnectionHandler.class);

    private final VirtualServerSocket serverSocket;
    private final VivaldiClient vivaldi;

    ConnectionHandler(VirtualServerSocket socket, VivaldiClient vivaldi)
            throws IOException {

        this.serverSocket = socket;
        this.vivaldi = vivaldi;

        ThreadPool.createNew(this, "Vivaldi Connection handler");
    }

    public void run() {
        while (true) {
            try {
                Connection connection = new Connection(serverSocket);
                vivaldi.handleConnection(connection);
            } catch (IOException e) {
                if (serverSocket.isClosed()) {
                    return;
                } else {
                    logger.error("Accept failed, waiting a second, will retry",
                            e);

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e1) {
                        // IGNORE
                    }
                }
            }

        }
    }
}
