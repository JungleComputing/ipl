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

package ibis.ipl.impl.nio;

import java.util.ArrayList;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ibis.ipl.CapabilitySet;
import ibis.ipl.Credentials;
import ibis.ipl.Ibis;
import ibis.ipl.IbisCapabilities;
import ibis.ipl.IbisCreationFailedException;
import ibis.ipl.IbisFactory;
import ibis.ipl.PortType;
import ibis.ipl.RegistryEventHandler;

public final class NioIbisStarter extends ibis.ipl.IbisStarter {

    static final Logger logger = LoggerFactory.getLogger(NioIbisStarter.class);

    static final IbisCapabilities ibisCapabilities = new IbisCapabilities(IbisCapabilities.CLOSED_WORLD, IbisCapabilities.MEMBERSHIP_UNRELIABLE,
            IbisCapabilities.MEMBERSHIP_TOTALLY_ORDERED, IbisCapabilities.SIGNALS, IbisCapabilities.ELECTIONS_UNRELIABLE,
            IbisCapabilities.ELECTIONS_STRICT);

    static final PortType portCapabilities = new PortType(PortType.SERIALIZATION_OBJECT_SUN, PortType.SERIALIZATION_OBJECT_IBIS,
            PortType.SERIALIZATION_OBJECT, PortType.SERIALIZATION_DATA, PortType.SERIALIZATION_BYTE, PortType.COMMUNICATION_FIFO,
            PortType.COMMUNICATION_NUMBERED, PortType.COMMUNICATION_RELIABLE, PortType.CONNECTION_DOWNCALLS, PortType.CONNECTION_UPCALLS,
            PortType.CONNECTION_TIMEOUT, PortType.CONNECTION_MANY_TO_MANY, PortType.CONNECTION_MANY_TO_ONE, PortType.CONNECTION_ONE_TO_MANY,
            PortType.CONNECTION_ONE_TO_ONE, PortType.RECEIVE_POLL, PortType.RECEIVE_AUTO_UPCALLS, PortType.RECEIVE_EXPLICIT,
            PortType.RECEIVE_POLL_UPCALLS, PortType.RECEIVE_TIMEOUT, "sendport.blocking", "sendport.nonblocking", "sendport.thread",
            "receiveport.blocking", "receivport.nonblocking", "receiveport.thread");

    public NioIbisStarter(String nickName, String iplVersion, String implementationVersion) {
        super(nickName, iplVersion, implementationVersion);
    }

    @Override
    public boolean matches(IbisCapabilities capabilities, PortType[] types) {
        if (!capabilities.matchCapabilities(ibisCapabilities)) {
            return false;
        }
        for (PortType portType : types) {
            if (!portType.matchCapabilities(portCapabilities)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public CapabilitySet unmatchedIbisCapabilities(IbisCapabilities capabilities, PortType[] types) {
        return capabilities.unmatchedCapabilities(ibisCapabilities);
    }

    @Override
    public PortType[] unmatchedPortTypes(IbisCapabilities capabilities, PortType[] types) {
        ArrayList<PortType> result = new ArrayList<>();

        for (PortType portType : types) {
            if (!portType.matchCapabilities(portCapabilities)) {
                result.add(portType);
            }
        }
        return result.toArray(new PortType[0]);
    }

    @Override
    public Ibis startIbis(IbisFactory factory, RegistryEventHandler registryEventHandler, Properties userProperties, IbisCapabilities capabilities,
            Credentials credentials, byte[] applicationTag, PortType[] portTypes, String specifiedSubImplementation)
            throws IbisCreationFailedException {
        return new NioIbis(registryEventHandler, capabilities, credentials, applicationTag, portTypes, userProperties, this);
    }
}
