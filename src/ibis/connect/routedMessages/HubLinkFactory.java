package ibis.connect.routedMessages;

import ibis.connect.util.ConnProps;
import ibis.connect.util.MyDebug;

import java.io.IOException;
import java.util.Properties;

public class HubLinkFactory {
    private static HubLink hub = null;

    private static boolean closed = false;

    private static boolean enabled = false;

    static {
        try {
            Properties p = System.getProperties();
            String host = p.getProperty(ConnProps.hub_host);
            String portString = p.getProperty(ConnProps.hub_port);

            int port;

            if (host == null) {
                host = p.getProperty("ibis.name_server.host");
            }

            if (portString == null) {
                portString = p.getProperty("ibis.name_server.port");
                if (portString != null) {
                    port = Integer.parseInt(portString) + 2;
                } else {
                    port = ibis.connect.controlHub.ControlHub.defaultPort;
                }
            } else {
                port = Integer.parseInt(portString);
            }

            if (host != null) {
                if (MyDebug.VERBOSE())
                    System.err.println("# Creating link to hub- host=" + host
                            + "; port=" + port);
                hub = new HubLink(host, port);
                hub.setDaemon(true);
                hub.start();
                enabled = true;
            } else if (MyDebug.VERBOSE()) {
                System.err.println(
                        "# HubLinkFactory: 'hub' properties not set");
                System.err.println(
                        "# (ibis.connect.hub_host or ibis.name_server.host).");
                System.err.println("# Not creating wire to hub.");
            }
        } catch (Exception e) {
            System.err.println("# HubLinkFactory: Cannot create wire to hub.");
        }
    }

    public synchronized static boolean isConnected() {
        return enabled;
    }

    public synchronized static void destroyHubLink() {
        if (enabled) {
            hub.stopHub();
            hub = null;
            closed = true;
        }
    }

    public synchronized static HubLink getHubLink() throws IOException {
        if (closed)
            throw new IOException("HubLinkFactory: wire to hub closed.");
        if (!enabled)
            throw new IOException("HubLinkFactory: wire to hub disabled.");
        return hub;
    }
}
