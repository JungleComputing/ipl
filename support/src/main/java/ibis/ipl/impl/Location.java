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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Properties;

import ibis.ipl.IbisProperties;
import ibis.util.TypedProperties;

/**
 * Represents a location on which an Ibis instance runs. This is the data type
 * returned by {@link IbisIdentifier#location()}. It represents a number of
 * levels, for instance hostname, domain, in that order, t.i., from detailed to
 * coarse.
 */
public final class Location implements ibis.ipl.Location {

    private static final Location universe = new Location(new String[0]);

    private static final long serialVersionUID = 1L;

    /** Separates level names in a string representation of the location. */
    public static final String SEPARATOR = "@";

    /** The names of the levels. */
    private String levelNames[];

    /** Coded form. */
    private transient byte[] codedForm;

    private transient Location parent = null;

    /**
     * Constructs a location object from the specified level names.
     *
     * @param levels the level names.
     */
    public Location(String[] levels) {
        levelNames = new String[levels.length];
        for (int i = 0; i < levels.length; i++) {
            levelNames[i] = levels[i];
        }
        codedForm = computeCodedForm();
    }

    /**
     * Constructs a location object from the specified string, in which level names
     * are separated by the separator character.
     *
     * @param s the specified string.
     */
    public Location(String s) {
        if (s.equals("")) {
            levelNames = new String[0];
        } else {
            levelNames = s.split(SEPARATOR);
        }
        codedForm = computeCodedForm();
    }

    /**
     * Constructs a <code>Location</code> from the specified coded form.
     *
     * @param codedForm the coded form.
     * @exception IOException is thrown in case of trouble.
     */
    public Location(byte[] codedForm) throws IOException {
        this(codedForm, 0, codedForm.length);
    }

    /**
     * Constructs a <code>Location</code> from the specified coded form, at a
     * particular offset and size.
     *
     * @param codedForm the coded form.
     * @param offset    offset in the coded form.
     * @param size      size of the coded form.
     * @exception IOException is thrown in case of trouble.
     */
    public Location(byte[] codedForm, int offset, int size) throws IOException {
        this(new DataInputStream(new ByteArrayInputStream(codedForm, offset, size)));
    }

    /**
     * Reads a <code>Location</code> from the specified input stream.
     *
     * @param dis the input stream.
     * @exception IOException is thrown in case of trouble.
     */
    public Location(DataInput dis) throws IOException {
        this(dis.readUTF());
    }

    /**
     * Returns the coded form of this <code>Location</code>.
     *
     * @return the coded form.
     */
    public byte[] toBytes() {
        if (codedForm == null) {
            codedForm = computeCodedForm();
        }
        return codedForm.clone();
    }

    private byte[] computeCodedForm() {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(bos);
            dos.writeUTF(toString());
            dos.close();
            return bos.toByteArray();
        } catch (Exception e) {
            // Should not happen. Ignore.
            return null;
        }
    }

    /**
     * Adds coded form of this <code>Location</code> to the specified output stream.
     *
     * @param dos the output stream.
     * @exception IOException is thrown in case of trouble.
     */
    public void writeTo(DataOutput dos) throws IOException {
        if (codedForm == null) {
            codedForm = computeCodedForm();
        }
        dos.write(codedForm);
    }

    @Override
    public int numberOfLevels() {
        return levelNames.length;
    }

    @Override
    public String[] getLevels() {
        return levelNames.clone();
    }

    @Override
    public String getLevel(int level) {
        return levelNames[level];
    }

    @Override
    public int numberOfMatchingLevels(ibis.ipl.Location o) {
        int n1 = o.numberOfLevels();
        for (int i = levelNames.length - 1; i >= 0; i--) {
            n1--;
            if (n1 < 0 || !levelNames[i].equals(o.getLevel(n1))) {
                return levelNames.length - 1 - i;
            }
        }
        return levelNames.length;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ibis.ipl.impl.Location)) {
            return false;
        }
        Location l = (Location) o;
        if (l.levelNames.length != levelNames.length) {
            return false;
        }
        return numberOfMatchingLevels(l) == levelNames.length;
    }

    @Override
    public int hashCode() {
        int retval = 0;
        for (String levelName : levelNames) {
            retval += levelName.hashCode();
        }
        return retval;
    }

    /**
     * Method to retreive a default location, consisting of the domain and hostname
     * of this machine.
     *
     * @param props properties.
     * @param adds  addresses to use as a source of the hostname (may be null).
     * @return the default location of this machine.
     */
    public static Location defaultLocation(Properties props, InetAddress[] adds) {

        // If the user has specified one or more preferred IP addresses, we'll
        // try to resolve those first.

        String fullHostName = null;
        if (adds != null && adds.length >= 0) {
            for (InetAddress a : adds) {
                fullHostName = a.getCanonicalHostName();
                if (fullHostName.length() > 0 && Character.isJavaIdentifierStart(fullHostName.charAt(0))) {
                    break;
                }
                fullHostName = null;
            }
        }

        // NOTE: This may result in a unusable location when the hostname
        // of the machine is set to a crappy value (as on DAS-3).
        if (fullHostName == null) {
            try {
                InetAddress a = InetAddress.getLocalHost();
                fullHostName = a.getCanonicalHostName();
            } catch (IOException e) {
                fullHostName = "Unknown location";
            }
        }

        String[] split = fullHostName.split("\\.");

        TypedProperties p = new TypedProperties(props);

        String s = p.getProperty(IbisProperties.LOCATION);
        if (s == null) {
            s = DEFAULT_LOCATION;
        }
        char[] buf = s.toCharArray();
        StringBuffer b = new StringBuffer();

        // Find words between '%' delimiters, and replace them with the required value.
        // If the word is not recognized, it is left untouched (including the
        // '%').
        for (int i = 0; i < buf.length; i++) {
            if (buf[i] != '%') {
                b.append(buf[i]);
            } else if (i == buf.length - 1) {
                b.append('%');
            } else if (buf[i + 1] == '%') {
                b.append('%');
                i++;
            } else {
                StringBuffer keyBuffer = new StringBuffer();
                String key = null;
                for (int j = i + 1; j < buf.length; j++) {
                    if (buf[j] != '%') {
                        keyBuffer.append(buf[j]);
                    } else {
                        key = keyBuffer.toString();
                        i = j;
                        break;
                    }
                }
                if (key == null) {
                    b.append('%');
                    b.append(keyBuffer);
                    i = buf.length - 1;
                    break;
                }

                if (key.equals("HOSTNAME")) {
                    b.append(split[0]);
                } else if (key.equals("DOMAIN") || key.equals("FLAT_DOMAIN")) {
                    for (int j = 1; j < split.length; j++) {
                        b.append(split[j]);
                        if (j < split.length - 1) {
                            b.append(key.equals("DOMAIN") ? SEPARATOR : ':');
                        }
                    }
                } else if (key.equals("PID")) {
                    int pid = -1;
                    try {
                        pid = Integer.parseInt((new File("/proc/self")).getCanonicalFile().getName());
                    } catch (Throwable e) {
                        // ignore. No pid available.
                    }
                    b.append(pid);
                } else {
                    // Unrecognized key, just leave it alone.
                    b.append('%');
                    b.append(key);
                    b.append('%');
                }
            }
        }
        return new Location(b.toString());
    }

    @Override
    public String toString() {
        String retval = "";
        for (int i = 0; i < levelNames.length; i++) {
            if (i != 0) {
                retval += SEPARATOR;
            }
            retval += levelNames[i];
        }
        return retval;
    }

    @Override
    public int compareTo(ibis.ipl.Location o) {
        int n = o.numberOfLevels();
        for (int i = levelNames.length - 1; i >= 0; i--) {
            n--;
            if (n < 0) {
                return 1;
            }

            int cmp = levelNames[i].compareTo(o.getLevel(n));
            if (cmp != 0) {
                return cmp;
            }
        }
        if (n == 0) {
            return 0;
        }
        return -1;
    }

    @Override
    public ibis.ipl.Location getParent() {
        if (parent == null) {
            if (levelNames.length <= 1) {
                parent = universe;
            } else {
                String[] names = new String[levelNames.length - 1];
                for (int i = 1; i < levelNames.length; i++) {
                    names[i - 1] = levelNames[i];
                }
                parent = new Location(names);
            }
        }
        return parent;
    }

    @Override
    public Iterator<String> iterator() {
        return new Iter();
    }

    private class Iter implements Iterator<String> {
        int index = levelNames.length;

        @Override
        public boolean hasNext() {
            return index > 0;
        }

        @Override
        public String next() {
            if (hasNext()) {
                return levelNames[--index];
            }
            throw new NoSuchElementException("Iterator exhausted");
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("remove() not supported");
        }
    }
}
