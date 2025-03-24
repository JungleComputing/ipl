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
package ibis.ipl;

/**
 * Container class for a single connection failure.
 */
public class ConnectionFailedException extends IbisIOException {

    private static final long serialVersionUID = 1L;
    private final ReceivePortIdentifier receivePortIdentifier;
    private final IbisIdentifier ibisIdentifier;
    private final String receivePortName;

    /**
     * Constructs a <code>ConnectionFailedException</code> for a failed attempt to
     * connect to a specific named receiveport at a specific ibis instance.
     *
     * @param detailMessage   the detail message.
     * @param ibisIdentifier  the Ibis identifier of the ibis instance.
     * @param receivePortName the receivePortName of the receive port.
     */
    public ConnectionFailedException(String detailMessage, IbisIdentifier ibisIdentifier, String receivePortName) {
        super(detailMessage);
        this.ibisIdentifier = ibisIdentifier;
        this.receivePortName = receivePortName;
        this.receivePortIdentifier = null;
    }

    /**
     * Constructs a <code>ConnectionFailedException</code> for a failed attempt to
     * connect to a specific named receiveport at a specific ibis instance.
     *
     * @param detailMessage   the detail message.
     * @param ibisIdentifier  the Ibis identifier of the ibis instance.
     * @param receivePortName the receivePortName of the receive port.
     * @param cause           the cause of the failure.
     */
    public ConnectionFailedException(String detailMessage, IbisIdentifier ibisIdentifier, String receivePortName, Throwable cause) {
        super(detailMessage);
        initCause(cause);
        this.ibisIdentifier = ibisIdentifier;
        this.receivePortName = receivePortName;
        this.receivePortIdentifier = null;
    }

    /**
     * Constructs a <code>ConnectionFailedException</code> for a failed attempt to
     * connect to a specific receiveport. at a specific ibis instance.
     *
     * @param detailMessage         the detail message.
     * @param receivePortIdentifier the receiveport identifier.
     * @param cause                 the cause of the failure.
     */
    public ConnectionFailedException(String detailMessage, ReceivePortIdentifier receivePortIdentifier, Throwable cause) {
        super(detailMessage);
        initCause(cause);
        this.receivePortIdentifier = receivePortIdentifier;
        this.ibisIdentifier = receivePortIdentifier.ibisIdentifier();
        this.receivePortName = receivePortIdentifier.name();
    }

    /**
     * Constructs a <code>ConnectionFailedException</code> for a failed attempt to
     * connect to a specific receiveport.
     *
     * @param detailMessage         the detail message.
     * @param receivePortIdentifier the receiveport identifier.
     */
    public ConnectionFailedException(String detailMessage, ReceivePortIdentifier receivePortIdentifier) {
        super(detailMessage);
        this.receivePortIdentifier = receivePortIdentifier;
        this.ibisIdentifier = receivePortIdentifier.ibisIdentifier();
        this.receivePortName = receivePortIdentifier.name();
    }

    /**
     * Returns the ibis identifier of the ibis instance running the receive port
     * that was the target of the failed connection attempt.
     *
     * @return the ibis identifier.
     */
    public IbisIdentifier ibisIdentifier() {
        if (ibisIdentifier == null) {
            return receivePortIdentifier.ibisIdentifier();
        }
        return ibisIdentifier;
    }

    /**
     * Returns the receiveport identifier of the failed connection attempt. If the
     * connection attempt specified ibis identifiers and names, this call may return
     * <code>null</code>.
     *
     * @return the receiveport identifier, or <code>null</code>.
     */
    public ReceivePortIdentifier receivePortIdentifier() {
        return receivePortIdentifier;
    }

    /**
     * Returns the name of the receive port that was the target of the failed
     * connection attempt.
     *
     * @return the receivePortName.
     */
    public String receivePortName() {
        if (receivePortName == null) {
            return receivePortIdentifier.name();
        }
        return receivePortName;
    }
}
