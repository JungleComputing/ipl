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

package ibis.compile;

import java.io.IOException;
import java.io.InputStream;

/**
 * This interface serves as a wrapper interface for different bytecode
 * rewriters. This way, different <code>IbiscComponents</code> can use different
 * bytecode rewriters. For each bytecode rewriter, the methods in this interface
 * must be implemented, as well as the methods in <code>ClassInfo</code>.
 */
public interface ByteCodeWrapper {
    /**
     * Obtains the <code>ClassInfo</code> object that encapsulates the parameter,
     * which represents a class for a specific bytecode rewriter.
     * 
     * @param cl the class as represented by a specific bytecode rewriter.
     * @return the corresponding <code>ClassInfo</code> object.
     */
    public ClassInfo getInfo(Object cl);

    /**
     * Reads a class from the specified file and returns the corresponding
     * <code>ClassInfo</code> object.
     * 
     * @param fileName the name of the file to read the class from.
     * @exception IOException is thrown on a read error.
     * @return the corresponding <code>ClassInfo</code> object.
     */
    public ClassInfo parseClassFile(String fileName) throws IOException;

    /**
     * Reads a class from the specified input stream and returns the corresponding
     * <code>ClassInfo</code> object.
     * 
     * @param in       the input stream to read the class from.
     * @param fileName the name of the file/jar entry.
     * @exception IOException is thrown on a read error.
     * @return the corresponding <code>ClassInfo</code> object.
     */
    public ClassInfo parseInputStream(InputStream in, String fileName) throws IOException;
}
