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
/* $Id$ */

package ibis.ipl.impl.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.Channel;

import ibis.ipl.impl.ReceivePortIdentifier;

/**
 * Creates and recycles/destroys writeable and readable channels. Any
 * implementation should also handle incoming connections in the run() method
 * and register them with the receiveport.
 */
public interface ChannelFactory extends Runnable {

    /**
     * Tries to connect the specified sendport to the given receiveport for
     * timeoutMillis milliseconds and returns the writechannel if it succeeded.
     * A timeout of 0 means try forever.
     * 
     * @param spi
     *            the sendport
     * @param rpi
     *            identifies the receiveport
     * @param timeoutMillis
     *            the timeout in milliseconds
     *
     * @return a new Channel connected to "rpi".
     * @throws IOException
     *             on failure
     */
    public Channel connect(NioSendPort spi, ReceivePortIdentifier rpi,
            long timeoutMillis) throws IOException;

    /**
     * Stops the factory. It will kill off any threads it made.
     * 
     * @throws IOException
     *             on failure
     */
    public void quit() throws IOException;

    /**
     * Returns the socket address that this factory is listening to.
     * 
     * @return the socket address
     */
    public InetSocketAddress getAddress();
}
