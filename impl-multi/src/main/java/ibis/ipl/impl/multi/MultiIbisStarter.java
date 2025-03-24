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

package ibis.ipl.impl.multi;

import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ibis.ipl.CapabilitySet;
import ibis.ipl.Credentials;
import ibis.ipl.Ibis;
import ibis.ipl.IbisCapabilities;
import ibis.ipl.IbisCreationFailedException;
import ibis.ipl.IbisFactory;
import ibis.ipl.IbisStarter;
import ibis.ipl.PortType;
import ibis.ipl.RegistryEventHandler;

public final class MultiIbisStarter extends IbisStarter {

    static final Logger logger = LoggerFactory.getLogger(MultiIbisStarter.class);

    public MultiIbisStarter(String nickName, String iplVersion, String implementationVersion) {
        super(nickName, iplVersion, implementationVersion);
    }

    @Override
    public boolean matches(IbisCapabilities capabilities, PortType[] types) {
        return true;
    }

    @Override
    public CapabilitySet unmatchedIbisCapabilities(IbisCapabilities capabilities, PortType[] types) {
        return new CapabilitySet();
    }

    @Override
    public PortType[] unmatchedPortTypes(IbisCapabilities capabilities, PortType[] types) {
        return new PortType[0];
    }

    @Override
    public Ibis startIbis(IbisFactory factory, RegistryEventHandler registryEventHandler, Properties userProperties, IbisCapabilities capabilities,
            Credentials credentials, byte[] applicationTag, PortType[] portTypes, String specifiedSubImplementation)
            throws IbisCreationFailedException {
        return new MultiIbis(factory, registryEventHandler, userProperties, capabilities, credentials, applicationTag, portTypes,
                specifiedSubImplementation, this);
    }
}
