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

import java.io.IOException;

/**
 * Describes the interface for upcall based communication.
 * <p>
 * Creating a {@link ReceivePort} with a <code>MessageUpcall</code> allows for
 * upcall based communication. The <code>ReceivePort</code> must be created with
 * the {@link Ibis#createReceivePort(PortType, String, MessageUpcall)} method,
 * or one of its other variants with a <code>MessageUpcall</code> parameter.
 * After the receive port is created, upcalls can be enabled through the
 * {@link ReceivePort#enableMessageUpcalls()} call, and disabled through the
 * {@link ReceivePort#disableMessageUpcalls()} call. Initially, upcalls are
 * disabled.
 * <p>
 * For a given receive port, only one message can be active at any time, and by
 * default, the message is active as long as the upcall is active. However, the
 * message can be de-activated using the {@link ReadMessage#finish()} call. This
 * is the only way in which a receive port can have more than one active upcall.
 * </p>
 * <p>
 * <strong>Note: to prevent deadlocks, upcalls are not allowed to block in
 * condition synchronization or network access as long as the message is active.
 * </strong> However, upcalls <strong>are</strong> allowed to enter/exit
 * synchronized methods for critical section operations.
 * </p>
 */
public interface MessageUpcall {

    /**
     * This is the upcall announcing the receipt of a {@link ReadMessage}. If the
     * upcall throws an exception, the message is to be considered finished. In this
     * case, implementations should call {@link ReadMessage#finish()} or, in case of
     * an <code>IOException</code>, {@link ReadMessage#finish(IOException)}.
     *
     * @param readMessage the message received.
     * @exception IOException            may be thrown by any of the methods invoked
     *                                   on the message.
     * @exception ClassNotFoundException may be thrown by a
     *                                   {@link ReadMessage#readObject()} call.
     */
    public void upcall(ReadMessage readMessage) throws IOException, ClassNotFoundException;
}
