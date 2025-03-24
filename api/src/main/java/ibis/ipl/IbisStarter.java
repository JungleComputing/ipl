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

package ibis.ipl;

import java.lang.reflect.Constructor;
import java.util.Properties;

/**
 * Every Ibis implementation must provide an <code>IbisStarter</code> which is
 * used by the Ibis factory to check capabilities, port types, and to start an
 * Ibis instance. This class is not to be used by Ibis applications. Ibis
 * applications should use {@link IbisFactory} to create Ibis instances.
 */
public abstract class IbisStarter {

    /**
     * Short name of this implementation. Usually the network stack it is based on,
     * e.g. "tcp" or "mpi".
     */
    private final String nickName;

    /**
     * Which version of the IPL does this implementation implement.
     */
    private final String iplVersion;

    /**
     * Version identifier of this implementation. Usually a checksum created from
     * the class files of the implementation.
     */
    private final String implementationVersion;

    /**
     * Creates a starter of a given class using reflection. Returns null if this
     * fails for any reason.
     *
     * @param className             the name of the class to instantiate. Must
     *                              extend IbisStarter.
     * @param classLoader           the class loader to use.
     * @param nickName              the nickname of the ibis starter.
     * @param iplVersion            the IPL version this ibis implements.
     * @param implementationVersion the version of this ibis.
     * @return an IbisStarter instance for the given class name.
     */
    @SuppressWarnings("unchecked")
    public static IbisStarter createInstance(String className, ClassLoader classLoader, String nickName, String iplVersion,
            String implementationVersion) {
        try {
            Class<? extends IbisStarter> starterClass = (Class<? extends IbisStarter>) Class.forName(className, false, classLoader);

            Constructor<?> constructor = starterClass.getConstructor(String.class, String.class, String.class);
            return (IbisStarter) constructor.newInstance(nickName, iplVersion, implementationVersion);
        } catch (Throwable t) {
            System.err.println("Could not create starter from class name " + className + " : " + t);
            t.printStackTrace();
            return null;
        }
    }

    /**
     * Constructs an <code>IbisStarter</code>.
     *
     * @param nickName              the nickname of the ibis starter.
     * @param iplVersion            the IPL version this ibis implements.
     * @param implementationVersion the version of this ibis.
     */
    protected IbisStarter(String nickName, String iplVersion, String implementationVersion) {
        this.nickName = nickName;
        this.iplVersion = iplVersion;
        this.implementationVersion = implementationVersion;
    }

    /**
     * Short name of this implementation. Usually the network stack it is based on,
     * e.g. "tcp" or "mpi".
     *
     * @return the nickName.
     */
    public String getNickName() {
        return nickName;
    }

    /**
     * Which version of the IPL does this implementation implement.
     *
     * @return the IPL version.
     */
    public String getIplVersion() {
        return iplVersion;
    }

    /**
     * Version identifier of this implementation. Usually a checksum created from
     * the class files of the implementation.
     *
     * @return the implementation version.
     */
    public String getImplementationVersion() {
        return implementationVersion;
    }

    /**
     * Returns a short name of this implementation.
     *
     * @return a short name.
     */
    @Override
    public String toString() {
        return nickName;
    }

    /**
     * Decides if this <code>IbisStarter</code> can start an Ibis instance with the
     * desired capabilities and port types.
     *
     * @param capabilities the desired capabilities.
     * @param portTypes    the desired port types.
     *
     * @return <code>true</code> if it can.
     */
    public abstract boolean matches(IbisCapabilities capabilities, PortType[] portTypes);

    /**
     * Returns the required capabilities that are not matched by this starter.
     * <strong> Note: a stacking Ibis returns the capabilities that are required of
     * the underlying Ibis implementation. </strong>
     *
     * @param capabilities the desired capabilities.
     * @param portTypes    the desired port types.
     *
     * @return the unmatched ibis capabilities.
     */
    public abstract CapabilitySet unmatchedIbisCapabilities(IbisCapabilities capabilities, PortType[] portTypes);

    /**
     * Returns the list of port types that are not matched by this starter. If all
     * required port types match, this method returns an array with 0 elements.
     * <strong> Note: a stacking Ibis returns the port types that are required of
     * the underlying Ibis implementation. </strong>
     *
     * @param capabilities the desired capabilities.
     * @param portTypes    the desired port types.
     *
     * @return the unmatched port types.
     */
    public abstract PortType[] unmatchedPortTypes(IbisCapabilities capabilities, PortType[] portTypes);

    /**
     * Actually creates an Ibis instance from this starter.
     *
     * @param factory                    the factory starting this Ibis instance.
     * @param handler                    a registry event handler.
     * @param userProperties             the user properties.
     * @param capabilities               the required capabilities.
     * @param credentials                credentials offered by a user when it wants
     *                                   to join a pool.
     * @param applicationTag             an application level tag for this Ibis
     *                                   instance.
     * @param portTypes                  the required port types.
     * @param specifiedSubImplementation for stacking ibis starters, the name of the
     *                                   underlying Ibis implementation.
     * @return the Ibis instance
     * @throws IbisCreationFailedException when the Ibis instance could not be
     *                                     created for some reason.
     */
    public abstract Ibis startIbis(IbisFactory factory, RegistryEventHandler handler, Properties userProperties, IbisCapabilities capabilities,
            Credentials credentials, byte[] applicationTag, PortType[] portTypes, String specifiedSubImplementation)
            throws IbisCreationFailedException;
}
