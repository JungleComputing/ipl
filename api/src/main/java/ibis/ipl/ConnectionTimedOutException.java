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

package ibis.ipl;

/**
 * Signals that an attempt to set up a connection timed out. A
 * <code>ConnectionTimedOutException</code> is thrown to indicate that a
 * sendport connect timed out.
 */
public class ConnectionTimedOutException extends ConnectionFailedException {
    private static final long serialVersionUID = 1L;

    /**
     * Constructs a <code>ConnectionTimedOuException</code>. with the specified
     * parameters.
     *
     * @param detailMessage         the detail message.
     * @param receivePortIdentifier identifies the target port of the failed
     *                              connection attempt.
     * @param cause                 cause of the failure.
     */
    public ConnectionTimedOutException(String detailMessage, ReceivePortIdentifier receivePortIdentifier, Throwable cause) {
        super(detailMessage, receivePortIdentifier, cause);
    }

    /**
     * Constructs a <code>ConnectionTimedOutException</code>. with the specified
     * parameters.
     *
     * @param detailMessage         the detail message.
     * @param receivePortIdentifier identifies the target port of the failed
     *                              connection attempt.
     */
    public ConnectionTimedOutException(String detailMessage, ReceivePortIdentifier receivePortIdentifier) {
        super(detailMessage, receivePortIdentifier);
    }

    /**
     * Constructs a <code>ConnectionTimedOuException</code> with the specified
     * parameters.
     *
     * @param detailMessage   the detail message.
     * @param ibisIdentifier  identifies the Ibis instance of the target port of the
     *                        failed connection attempt.
     * @param receivePortName the name of the receive port of the failed connection
     *                        attempt.
     * @param cause           the cause of the failure.
     */
    public ConnectionTimedOutException(String detailMessage, IbisIdentifier ibisIdentifier, String receivePortName, Throwable cause) {
        super(detailMessage, ibisIdentifier, receivePortName, cause);
    }

    /**
     * Constructs a <code>ConnectionTimedOutException</code> with the specified
     * parameters.
     *
     * @param detailMessage   the detail message.
     * @param ibisIdentifier  identifies the Ibis instance of the target port of the
     *                        failed connection attempt.
     * @param receivePortName the name of the receive port of the failed connection
     *                        attempt.
     */
    public ConnectionTimedOutException(String detailMessage, IbisIdentifier ibisIdentifier, String receivePortName) {
        super(detailMessage, ibisIdentifier, receivePortName);
    }
}
