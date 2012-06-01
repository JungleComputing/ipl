package ibis.ipl.impl.stacking.cache;

import ibis.ipl.*;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Map;
import java.util.Properties;

public class CacheIbis implements Ibis {

    Ibis baseIbis;
    IbisStarter starter;

    public CacheIbis(IbisFactory factory,
            RegistryEventHandler registryEventHandler,
            Properties userProperties, IbisCapabilities capabilities,
            Credentials credentials, byte[] applicationTag, PortType[] portTypes,
            String specifiedSubImplementation,
            CacheIbisStarter cacheIbisStarter)
            throws IbisCreationFailedException {

        starter = cacheIbisStarter;

        /**
         * The received capabilites may or may not contain PORT_CACHING, so we
         * need to remove this when creating the under-the-hood ibis, since no
         * other ibis implementation holds this capability.
         */
        if (!capabilities.hasCapability(IbisCapabilities.PORT_CACHING)) {
            baseIbis = factory.createIbis(registryEventHandler, capabilities,
                    userProperties, credentials, applicationTag, portTypes,
                    specifiedSubImplementation);
        } else {

            String[] subCapabilitiesArray =
                    new String[capabilities.getCapabilities().length - 1];

            int i = 0;
            for (String capability : capabilities.getCapabilities()) {
                if (capability.equals(IbisCapabilities.PORT_CACHING)) {
                    continue;
                }
                subCapabilitiesArray[i++] = capability;
            }
            IbisCapabilities subCapabilities = new IbisCapabilities(subCapabilitiesArray);

            baseIbis = factory.createIbis(registryEventHandler, subCapabilities,
                    userProperties, credentials, applicationTag, portTypes,
                    specifiedSubImplementation);
        }
    }

    @Override
    public void end() throws IOException {
        baseIbis.end();
    }

    @Override
    public Registry registry() {
        // return new
        // ibis.ipl.impl.registry.ForwardingRegistry(base.registry());
        return baseIbis.registry();
    }

    @Override
    public Map<String, String> managementProperties() {
        return baseIbis.managementProperties();
    }

    @Override
    public String getManagementProperty(String key)
            throws NoSuchPropertyException {
        return baseIbis.getManagementProperty(key);
    }

    @Override
    public void setManagementProperties(Map<String, String> properties)
            throws NoSuchPropertyException {
        baseIbis.setManagementProperties(properties);
    }

    @Override
    public void setManagementProperty(String key, String val)
            throws NoSuchPropertyException {
        baseIbis.setManagementProperty(key, val);
    }

    @Override
    public void printManagementProperties(PrintStream stream) {
        baseIbis.printManagementProperties(stream);
    }

    @Override
    public void poll() throws IOException {
        baseIbis.poll();
    }

    @Override
    public IbisIdentifier identifier() {
        return baseIbis.identifier();
    }

    @Override
    public String getVersion() {
        return starter.getNickName() + " on top of " + baseIbis.getVersion();
    }

    @Override
    public Properties properties() {
        return baseIbis.properties();
    }

    @Override
    public SendPort createSendPort(PortType portType) throws IOException {
        return createSendPort(portType, null, null, null);
    }

    @Override
    public SendPort createSendPort(PortType portType, String name)
            throws IOException {
        return createSendPort(portType, name, null, null);
    }

    @Override
    public SendPort createSendPort(PortType portType, String name,
            SendPortDisconnectUpcall cU, Properties props) throws IOException {
        // TODO:
        // aici bagi undeva portul. cand acel |undeva| > nr_max_porturi,
        // inchizi unul, retii info despre el, si apoi il bagi pe asta.
        return new CacheSendPort(portType, this, name, cU, props);
    }

    @Override
    public ReceivePort createReceivePort(PortType portType, String name)
            throws IOException {
        return createReceivePort(portType, name, null, null, null);
    }

    @Override
    public ReceivePort createReceivePort(PortType portType, String name,
            MessageUpcall u) throws IOException {
        return createReceivePort(portType, name, u, null, null);
    }

    @Override
    public ReceivePort createReceivePort(PortType portType, String name,
            ReceivePortConnectUpcall cU) throws IOException {
        return createReceivePort(portType, name, null, cU, null);
    }

    @Override
    public ReceivePort createReceivePort(PortType portType, String name,
            MessageUpcall u, ReceivePortConnectUpcall cU, Properties props)
            throws IOException {
        // TODO: si aici isi are locul cache policy
        return new CacheReceivePort(portType, this, name, u, cU, props);
    }
}
