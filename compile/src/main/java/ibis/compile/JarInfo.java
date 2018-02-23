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
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.NoSuchElementException;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Maintains information about the contents of a jarfile.
 */
public class JarInfo {

    /** Wether the contents of the jarfile is modified. */
    private boolean     modified;

    /** A list of entries of the jarfile, including their contents. */
    private ArrayList<JarEntryInfo>  entries = new ArrayList<JarEntryInfo>();

    /** Reference to the jarfile itself. */
    private JarFile     jarFile;

    private class Enum implements Enumeration<JarEntryInfo> {
        int count = 0;

        public boolean hasMoreElements() {
            return count < entries.size();
        }

        public JarEntryInfo nextElement() {
            if (count >= entries.size()) {
                throw new NoSuchElementException();
            }
            return entries.get(count++);
        }
    }

    /**
     * Creates a container for information about a jar file being processed by Ibisc.
     * @param jf the jar file.
     * @throws IOException when there is something wrong with the jar file.
     */
    public JarInfo(JarFile jf) throws IOException {
        this.jarFile = jf;
        modified = false;
        
        for (Enumeration<JarEntry> iitems = jf.entries(); iitems.hasMoreElements();) {
            JarEntry ient = iitems.nextElement();
            entries.add(new JarEntryInfo(ient, this));
        }
    }

    /**
     * Returns an enumeration of {@link JarEntryInfo}s for the jar file.
     * @return an enumeration.
     */
    public Enumeration<JarEntryInfo> entries() {
        return new Enum();
    }

    /**
     * Adds the specified entry to this info object.
     * @param e the entry to add.
     */
    public void addEntry(IbiscEntry e) {
        JarEntry je = new JarEntry(e.fileName);
        JarEntryInfo jei = new JarEntryInfo(je, this, null, e);
        entries.add(jei);
    }
    
    /**
     * Notifies the info object about the "modified" status of the jar file.
     * @param val the "modified" status, either {@code true} or {@code false}
     */
    public void setModified(boolean val) {
        modified = val;
    }
    
    /**
     * Return the "modified" status of the jar file.
     * @return the "modified" status.
     */
    public boolean getModified() {
        return modified;
    }
 
    /**
     * See {@link JarFile#getName()}. 
     */
    public String getName() {
        return jarFile.getName();
    }

    /**
     * See {@link JarFile#getInputStream(java.util.zip.ZipEntry)}. 
     */
    public InputStream getInputStream(JarEntry j) throws IOException {
        return jarFile.getInputStream(j);
    }
}
