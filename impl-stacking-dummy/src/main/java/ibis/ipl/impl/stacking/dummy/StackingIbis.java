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
package ibis.ipl.impl.stacking.dummy;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Map;
import java.util.Properties;

import ibis.ipl.Credentials;
import ibis.ipl.Ibis;
import ibis.ipl.IbisCapabilities;
import ibis.ipl.IbisCreationFailedException;
import ibis.ipl.IbisFactory;
import ibis.ipl.IbisIdentifier;
import ibis.ipl.MessageUpcall;
import ibis.ipl.NoSuchPropertyException;
import ibis.ipl.PortType;
import ibis.ipl.ReceivePort;
import ibis.ipl.ReceivePortConnectUpcall;
import ibis.ipl.Registry;
import ibis.ipl.RegistryEventHandler;
import ibis.ipl.SendPort;
import ibis.ipl.SendPortDisconnectUpcall;

public class StackingIbis implements Ibis {

    Ibis base;

    public StackingIbis(IbisFactory factory, RegistryEventHandler registryEventHandler, Properties userProperties, IbisCapabilities capabilities,
            Credentials credentials, byte[] applicationTag, PortType[] portTypes, String specifiedSubImplementation,
            StackingIbisStarter stackingIbisStarter) throws IbisCreationFailedException {
        base = factory.createIbis(registryEventHandler, capabilities, userProperties, credentials, applicationTag, portTypes,
                specifiedSubImplementation);
    }

    @Override
    public void end() throws IOException {
        base.end();
    }

    @Override
    public Registry registry() {
        // return new
        // ibis.ipl.impl.registry.ForwardingRegistry(base.registry());
        return base.registry();
    }

    @Override
    public Map<String, String> managementProperties() {
        return base.managementProperties();
    }

    @Override
    public String getManagementProperty(String key) throws NoSuchPropertyException {
        return base.getManagementProperty(key);
    }

    @Override
    public void setManagementProperties(Map<String, String> properties) throws NoSuchPropertyException {
        base.setManagementProperties(properties);
    }

    @Override
    public void setManagementProperty(String key, String val) throws NoSuchPropertyException {
        base.setManagementProperty(key, val);
    }

    @Override
    public void printManagementProperties(PrintStream stream) {
        base.printManagementProperties(stream);
    }

    @Override
    public void poll() throws IOException {
        base.poll();
    }

    @Override
    public IbisIdentifier identifier() {
        return base.identifier();
    }

    @Override
    public String getVersion() {
        return "StackingIbis on top of " + base.getVersion();
    }

    @Override
    public Properties properties() {
        return base.properties();
    }

    @Override
    public SendPort createSendPort(PortType portType) throws IOException {
        return createSendPort(portType, null, null, null);
    }

    @Override
    public SendPort createSendPort(PortType portType, String name) throws IOException {
        return createSendPort(portType, name, null, null);
    }

    @Override
    public SendPort createSendPort(PortType portType, String name, SendPortDisconnectUpcall cU, Properties props) throws IOException {
        return new StackingSendPort(portType, this, name, cU, props);
    }

    @Override
    public ReceivePort createReceivePort(PortType portType, String name) throws IOException {
        return createReceivePort(portType, name, null, null, null);
    }

    @Override
    public ReceivePort createReceivePort(PortType portType, String name, MessageUpcall u) throws IOException {
        return createReceivePort(portType, name, u, null, null);
    }

    @Override
    public ReceivePort createReceivePort(PortType portType, String name, ReceivePortConnectUpcall cU) throws IOException {
        return createReceivePort(portType, name, null, cU, null);
    }

    @Override
    public ReceivePort createReceivePort(PortType portType, String name, MessageUpcall u, ReceivePortConnectUpcall cU, Properties props)
            throws IOException {
        return new StackingReceivePort(portType, this, name, u, cU, props);
    }

}
