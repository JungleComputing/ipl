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

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;

public class SplitterException extends IOException {

    /**
     * Generated
     */
    private static final long serialVersionUID = 9005051418523286737L;

    // Transient, because OutputStream is not Serializable. (Thanks, Selmar Smit!)
    // Note that some of the methods here are meaningless after serialization.
    private transient ArrayList<OutputStream> streams = new ArrayList<>();

    private ArrayList<Exception> exceptions = new ArrayList<>();

    public SplitterException() {
        // empty constructor
    }

    public void add(OutputStream s, Exception e) {
        if (streams.contains(s)) {
            System.err.println("AAA, stream was already in splitter exception");
        }

        streams.add(s);
        exceptions.add(e);
    }

    public int count() {
        return streams.size();
    }

    public OutputStream[] getStreams() {
        return streams.toArray(new OutputStream[0]);
    }

    public Exception[] getExceptions() {
        return exceptions.toArray(new Exception[0]);
    }

    public OutputStream getStream(int pos) {
        return streams.get(pos);
    }

    public Exception getException(int pos) {
        return exceptions.get(pos);
    }

    @Override
    public String toString() {
        String res = "got " + exceptions.size() + " exceptions: ";
        for (Exception exception : exceptions) {
            res += "   " + exception + "\n";
        }

        return res;
    }

    @Override
    public void printStackTrace(PrintStream s) {
        for (Exception exception : exceptions) {
            s.println("Exception: " + exception);
            (exception).printStackTrace(s);
        }
    }

    @Override
    public void printStackTrace(PrintWriter s) {
        for (Exception exception : exceptions) {
            s.println("Exception: " + exception);
            (exception).printStackTrace(s);
        }
    }

    @Override
    public String getMessage() {
        return toString();
    }
}
