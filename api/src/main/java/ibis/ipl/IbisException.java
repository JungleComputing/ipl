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
 * There are three base classes for Ibis exceptions: this one (which is a
 * checked exception), IbisRuntimeException (which is an unchecked exception),
 * and IbisIOException. The latter exists because we want it to be a subclass of
 * java.io.IOException.
 */
public class IbisException extends Exception {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs an <code>IbisException</code> with <code>null</code> as its error
     * detail message.
     */
    public IbisException() {
        super();
    }

    /**
     * Constructs an <code>IbisException</code> with the specified detail message.
     *
     * @param message the detail message
     */
    public IbisException(String message) {
        super(message);
    }

    /**
     * Constructs an <code>IbisException</code> with the specified cause.
     *
     * @param cause the cause
     */
    public IbisException(Throwable cause) {
        super(cause);
    }

    /**
     * Constructs an <code>IbisException</code> with the specified detail message
     * and cause.
     *
     * @param message the detail message
     * @param cause   the cause
     */
    public IbisException(String message, Throwable cause) {
        super(message, cause);
    }
}
