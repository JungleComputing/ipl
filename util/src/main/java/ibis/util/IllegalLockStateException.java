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

package ibis.util;

/**
 * An <code>IllegalLockStateException</code> is thrown to indicate that a thread
 * has attempted to lock, unlock, wait, or notify a {@link Monitor} that it does
 * not own, or that has been cleaned up.
 */
public class IllegalLockStateException extends RuntimeException {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    /**
     * Constructs a <code>AlreadyConnectedException</code> with <code>null</code> as
     * its error detail message.
     */
    public IllegalLockStateException() {
        super();
    }

    /**
     * Constructs a <code>IllegalLockStateException</code> with the specified detail
     * message.
     *
     * @param s the detail message
     */
    public IllegalLockStateException(String s) {
        super(s);
    }

    /**
     * Constructs a <code>IllegalLockStateException</code> with the specified detail
     * message and cause
     *
     * @param s     the detail message
     * @param cause the cause
     */
    public IllegalLockStateException(String s, Throwable cause) {
        super(s, cause);
    }

    /**
     * Constructs a <code>IllegalLockStateException</code> with the specified cause
     *
     * @param cause the cause
     */
    public IllegalLockStateException(Throwable cause) {
        super(cause);
    }
}
