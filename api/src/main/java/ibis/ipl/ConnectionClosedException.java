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
 * Signals that a connection has been closed. A
 * <code>ConnectionClosedException</code> is thrown to indicate that an input or
 * output operation has been terminated because the connection was broken.
 */
public class ConnectionClosedException extends IbisIOException {
    private static final long serialVersionUID = 1L;

    /**
     * Constructs a <code>ConnectionClosedException</code> with <code>null</code> as
     * its error detail message.
     */
    public ConnectionClosedException() {
        super();
    }

    /**
     * Constructs a <code>ConnectionClosedException</code> with the specified detail
     * message.
     *
     * @param detailMessage the detail message
     */
    public ConnectionClosedException(String detailMessage) {
        super(detailMessage);
    }

    /**
     * Constructs a <code>ConnectionClosedException</code> with the specified detail
     * message and cause.
     *
     * @param detailMessage the detail message
     * @param cause         the cause
     */
    public ConnectionClosedException(String detailMessage, Throwable cause) {
        super(detailMessage);
        initCause(cause);
    }

    /**
     * Constructs a <code>ConnectionClosedException</code> with the specified cause.
     *
     * @param cause the cause
     */
    public ConnectionClosedException(Throwable cause) {
        super();
        initCause(cause);
    }
}
