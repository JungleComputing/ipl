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

/**
 * This class maintains information about Java classes.
 */
class IbiscEntry {
    /** The Java class. */
    private ClassInfo cl;

    /**
     * The name of the file from which the class was read (or the name of the jar
     * file entry).
     */
    public final String fileName;

    /**
     * Wether the entry was modified. Should be set to <code>true</code> by any
     * {@link ibis.compile.IbiscComponent} that modifies this class.
     */
    private boolean modified = false;

    /**
     * When the class comes from a jar file, information about this jar file can be
     * found here.
     */
    private final JarInfo jarInfo;

    /**
     * Constructs an <code>IbiscEntry</code> from the specified class and filename.
     * 
     * @param cl the class.
     * @param fn the filename (or name of jar file entry).
     */
    public IbiscEntry(ClassInfo cl, String fn) {
        this.cl = cl;
        this.fileName = fn;
        this.jarInfo = null;
    }

    public IbiscEntry(ClassInfo cl, String fn, JarInfo jarInfo) {
        this.cl = cl;
        this.fileName = fn;
        this.jarInfo = jarInfo;
    }

    public void setModified(boolean val) {
        modified = val;
    }

    public boolean getModified() {
        return modified;
    }

    public JarInfo getJarInfo() {
        return jarInfo;
    }

    public ClassInfo getClassInfo() {
        return cl;
    }

    public void setClassInfo(ClassInfo cl) {
        this.cl = cl;
        this.modified = true;
    }
}
