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

package ibis.io;

import java.io.PrintStream;
import java.io.PrintWriter;

public class SerializationError extends Error {
    /**
     * Generated
     */
    private static final long serialVersionUID = 8658882919059316317L;
    Throwable cause = null;

    public SerializationError() {
        super();
    }

    public SerializationError(String message) {
        super(message);
    }

    public SerializationError(String message, Throwable cause) {
        super(message);
        this.cause = cause;
    }

    public SerializationError(Throwable cause) {
        super();
        this.cause = cause;
    }

    @Override
    public Throwable initCause(Throwable t) {
        return cause = t;
    }

    @Override
    public Throwable getCause() {
        return cause;
    }

    @Override
    public String getMessage() {
        String res = super.getMessage();
        if (cause != null) {
            res += ": " + cause.getMessage();
        }

        return res;
    }

    @Override
    public void printStackTrace(PrintWriter s) {
        if (cause != null) {
            cause.printStackTrace(s);
        }

        super.printStackTrace(s);
    }

    @Override
    public void printStackTrace(PrintStream s) {
        if (cause != null) {
            cause.printStackTrace(s);
        }

        super.printStackTrace(s);
    }
}
