/* $Id$ */

package ibis.connect.routedMessages;

import ibis.connect.util.ConnectionProperties;

import java.io.IOException;
import java.util.Properties;

import org.apache.log4j.Logger;

public class HubLinkFactory {
    private static HubLink hub = null;

    static Logger logger = Logger.getLogger(HubLinkFactory.class.getName());

    private static boolean closed = false;

    private static boolean enabled = false;

    static {
        try {
            Properties p = System.getProperties();
            String host = p.getProperty(ConnectionProperties.hub_host);
            String portString = p.getProperty(ConnectionProperties.hub_port);

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
                logger.info("# Creating link to hub- host=" + host + "; port="
                        + port);
                hub = new HubLink(host, port);
                hub.setDaemon(true);
                hub.start();
                enabled = true;
            } else {
                logger.info(
                        "# HubLinkFactory: 'hub' properties not set");
                logger.info(
                        "# (ibis.connect.hub_host or ibis.name_server.host).");
                logger.info("# Not creating wire to hub.");
            }
        } catch (Exception e) {
            logger.error("# HubLinkFactory: Cannot create wire to hub.");
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
        if (closed) {
            throw new IOException("HubLinkFactory: wire to hub closed.");
        }
        if (!enabled) {
            throw new IOException("HubLinkFactory: wire to hub disabled.");
        }
        return hub;
    }
}
