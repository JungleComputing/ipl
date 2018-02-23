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

package ibis.ipl.impl;

/**
 * Abstract class for implementation-dependent connection info for sendports.
 */
public abstract class SendPortConnectionInfo {
    /** Identifies the receiveport side of the connection. */
    public final ReceivePortIdentifier target;

    /** The sendport of the connection. */
    public final SendPort port;

    /**
     * Constructs a <code>SendPortConnectionInfo</code> with the specified parameters.
     * @param port the sendport.
     * @param target identifies the receiveport.
     */
    protected SendPortConnectionInfo(SendPort port, ReceivePortIdentifier target) {
        this.port = port;
        this.target = target;
    }
    
    public String connectionType() {
        return "unknown";
    }

    /**
     * Should close this particular connection.
     */
    public abstract void closeConnection();
}
