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
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Properties;

/**
 * A factory for creating serialization streams.
 */
public class SerializationFactory {

    /**
     * Returns the implementation name for the specified nickname. For now, this is
     * hardcoded, but it could be driven by for instance a configuration or
     * properties file.
     *
     * @param name  the nickname of the serialization type.
     * @param props the properties to use
     * @return the implementation name.
     */
    private static String implName(String name, Properties props) {
        String dflt = "ibis";
        if (props != null) {
            String v = props.getProperty(IOProperties.s_serialization_default);
            if ("sun".equals(v)) {
                dflt = "sun";
            }
        }
        if (name == null || name.equals("object")) {
            name = dflt;
        }
        if (name.equals("ibis")) {
            return "ibis.io.IbisSerialization";
        }
        if (name.equals("sun")) {
            return "ibis.io.SunSerialization";
        }
        if (name.equals("data")) {
            return "ibis.io.DataSerialization";
        }
        if (name.equals("byte")) {
            return "ibis.io.ByteSerialization";
        }
        return name;
    }

    /**
     * Creates a {@link SerializationInput} as specified by the name.
     *
     * @param name  the nickname for this serialization type.
     * @param in    the underlying input stream.
     * @param props the properties to use
     * @return the serialization input stream.
     * @exception IOException gets thrown when an IO error occurs.
     */
    public static SerializationInput createSerializationInput(String name, DataInputStream in, Properties props) throws IOException {
        String impl = implName(name, props) + "InputStream";
        try {
            Class<?> cl = Class.forName(impl);
            Constructor<?> cons = cl.getConstructor(new Class[] { DataInputStream.class });
            return (SerializationInput) cons.newInstance(new Object[] { in });
        } catch (ClassNotFoundException e) {
            throw new IbisIOException("No such class: " + impl, e);
        } catch (NoSuchMethodException e) {
            throw new IbisIOException("No suitable constructor in class: " + impl, e);
        } catch (IllegalArgumentException e) {
            throw new IbisIOException("No suitable constructor in class: " + impl, e);
        } catch (InstantiationException e) {
            throw new IbisIOException("class " + impl + " is abstract", e);
        } catch (InvocationTargetException e) {
            throw new IbisIOException("constructor of " + impl + " threw an exception", e.getCause());
        } catch (IllegalAccessException e) {
            throw new IbisIOException("access to constructor of " + impl + " is denied", e);
        } catch (Throwable e) {
            throw new IbisIOException("got unexpected error", e);
        }
    }

    /**
     * Creates a {@link SerializationOutput} as specified by the name.
     *
     * @param name  the nickname for this serialization type.
     * @param out   the underlying output stream.
     * @param props the properties to use.
     * @return the serialization output stream.
     * @exception IOException gets thrown when an IO error occurs.
     */
    public static SerializationOutput createSerializationOutput(String name, DataOutputStream out, Properties props) throws IOException {
        String impl = implName(name, props) + "OutputStream";
        try {
            Class<?> cl = Class.forName(impl);
            Constructor<?> cons = cl.getConstructor(new Class[] { DataOutputStream.class });
            return (SerializationOutput) cons.newInstance(new Object[] { out });
        } catch (ClassNotFoundException e) {
            throw new IbisIOException("No such class: " + impl, e);
        } catch (NoSuchMethodException e) {
            throw new IbisIOException("No suitable constructor in class: " + impl, e);
        } catch (IllegalArgumentException e) {
            throw new IbisIOException("No suitable constructor in class: " + impl, e);
        } catch (InstantiationException e) {
            throw new IbisIOException("class " + impl + " is abstract", e);
        } catch (InvocationTargetException e) {
            throw new IbisIOException("constructor of " + impl + " threw an exception", e.getCause());
        } catch (IllegalAccessException e) {
            throw new IbisIOException("access to constructor of " + impl + " is denied", e);
        } catch (Throwable e) {
            throw new IbisIOException("got unexpected error", e);
        }
    }
}
