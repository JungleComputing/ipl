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

import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;

/**
 * Collects IOExceptions for multicast output streams.
 */
public class CollectedWriteException extends IOException {

    /** Added. */
    private static final long serialVersionUID = 5494793976122105110L;

    private ArrayList<IOException> exceptions = new ArrayList<>();

    /**
     * Constructs a <code>CollectedWriteException</code> with the specified detail
     * message.
     *
     * @param s the detail message
     */
    public CollectedWriteException(String s) {
        super(s);
    }

    /**
     * Constructs a <code>CollectedWriteException</code> with <code>null</code> as
     * its error detail message.
     */
    public CollectedWriteException() {
        super();
    }

    /**
     * Adds an exception.
     * 
     * @param e the exception to be added.
     */
    public void add(IOException e) {
        exceptions.add(e);
    }

    /**
     * Returns the exceptions.
     * 
     * @return an array with one element for each exception.
     */
    public IOException[] getExceptions() {
        return exceptions.toArray(new IOException[exceptions.size()]);
    }

    @Override
    public String toString() {
        String res = "";

        if (exceptions.size() == 0) {
            return super.toString();
        }

        res = "\n--- START OF COLLECTED EXCEPTIONS ---\n";
        for (IOException f : exceptions) {
            String msg = f.getMessage();
            if (msg == null) {
                msg = f.toString();
            }
            res += msg;
            res += "\n";
        }
        res += "--- END OF COLLECTED EXCEPTIONS ---\n";
        return res;
    }

    @Override
    public void printStackTrace() {
        printStackTrace(System.err);
    }

    @Override
    public void printStackTrace(PrintStream s) {
        if (exceptions.size() == 0) {
            super.printStackTrace(s);
            return;
        }

        s.println("--- START OF COLLECTED EXCEPTIONS STACK TRACE ---");
        for (IOException f : exceptions) {
            f.printStackTrace(s);
        }
        s.println("--- END OF COLLECTED EXCEPTIONS STACK TRACE ---");
    }

    @Override
    public void printStackTrace(PrintWriter s) {
        if (exceptions.size() == 0) {
            super.printStackTrace(s);
            return;
        }

        s.println("--- START OF COLLECTED EXCEPTIONS STACK TRACE ---");
        for (IOException f : exceptions) {
            f.printStackTrace(s);
        }
        s.println("--- END OF COLLECTED EXCEPTIONS STACK TRACE ---");
    }
}
