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

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * This class describes the capabilities of an ibis instance.
 * Combined with a list of {@link PortType} it is
 * used to select a particular Ibis implementation.
 * See the {@link IbisFactory#createIbis(IbisCapabilities, Properties, boolean,
 * RegistryEventHandler, PortType...) createIbis} method from
 * {@link IbisFactory}.       
 */
public final class IbisCapabilities extends CapabilitySet {

    /**
     * Capability, set when the Ibises that can join the pool are
     * determined at the start of the run. This enables the methods
     * {@link Registry#getPoolSize()} and
     * {@link Registry#waitUntilPoolClosed()}.
     * When this capability is requested, the Ibis property 
     * <code>ibis.pool.size</code> must be set to the number of Ibis instances
     * that can join the run. When <code>ibis.pool.size</code> instances have
     * joined, the pool is closed: no more instances are allowed to join the
     * run.
     */
    public final static String CLOSED_WORLD = "closed.world";
    
    /**
     * Capability, indicating that signals are supported,
     * see {@link RegistryEventHandler#gotSignal(String,IbisIdentifier)} and
     * {@link Registry#signal(String, IbisIdentifier...)}.
     * 
     * @ibis.experimental
     */
    public final static String SIGNALS = "signals";

    /**
     * Capability, indicating that termination is supported,
     * see {@link RegistryEventHandler#poolTerminated(IbisIdentifier)} and
     * {@link Registry#terminate()}.
     * 
     * @ibis.experimental
     */
    public final static String TERMINATION = "termination";
    
    /**
     * Capability, indicating that elections are supported
     * but don't have to be reliable, always give the same result, etc.
     */
    public final static String ELECTIONS_UNRELIABLE = "elections.unreliable";

    /**
     * Capability, indicating that elections are supported, reliable,
     * and give consistent results.
     */
    public final static String ELECTIONS_STRICT = "elections.strict";
    
    /**
     * Capability indicating membership administration, but joins/leaves don't
     * have to be reliable, in order, etc.
     */
    public final static String MEMBERSHIP_UNRELIABLE
            = "membership.unreliable";
    
    /**
     * Capability indicating membership administration, and joins/leaves are
     * totally ordered.
     * This means that all Ibis instances receive the joins/leaves upcalls
     * in the same order.
     */
    public final static String MEMBERSHIP_TOTALLY_ORDERED
            = "membership.totally.ordered";
    
    /**
     * Capability indicating an Ibis that can deal with malleability.
     * This means that Ibis instances can join and/or leave a run at any
     * time.
     */
    public final static String MALLEABLE = "malleable";
    
    /** 
     * Constructor for an IbisCapabilities object.
     * @param capabilities
     *          the capabilities.
     */
    public IbisCapabilities(String... capabilities) {
        super(capabilities);
    }
    
    /**
     * Constructs an IbisCapabilities object from the specified properties.
     * @param properties
     *          the properties.
     */
    protected IbisCapabilities(Properties properties) {
        super(properties);
    }

    /**
     * Constructs an IbisCapabilities from the specified capabilityset.
     * @param capabilitySet
     *          the capabilityset.
     */
    protected IbisCapabilities(CapabilitySet capabilitySet) {
         super(capabilitySet);
    }
    
    /**
     * Reads and returns the capabilities from the specified file name, which is
     * searched for in the classpath.
     * @param capabilityFileName
     *          the file name.
     * @exception IOException
     *          is thrown when an IO error occurs.
     * @return
     *          the capabilities
     */
    public static IbisCapabilities load(String capabilityFileName)
            throws IOException {
        InputStream input = ClassLoader.getSystemClassLoader()
                .getResourceAsStream(capabilityFileName);
        if (input == null) {
            throw new IOException("Could not open " + capabilityFileName);
        }
        return load(input);
    }

    /**
     * Reads and returns the capabilities from the specified input stream.
     * @param input
     *          the input stream.
     * @exception IOException
     *          is thrown when an IO error occurs.
     * @return
     *          the capabilities
     */
    public static IbisCapabilities load(InputStream input) throws IOException {
        Properties properties = new Properties();
        properties.load(input);
        input.close();
        return new IbisCapabilities(properties);
    }
}
