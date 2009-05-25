package ibis.ipl.server;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ibis.advert.Advert;
import ibis.advert.MetaData;
import ibis.advert.UriNotSupportedException;
import ibis.ipl.IbisProperties;
import ibis.smartsockets.SmartSocketsProperties;
import ibis.smartsockets.direct.DirectSocketAddress;
import ibis.smartsockets.virtual.InitializationException;
import ibis.smartsockets.virtual.VirtualSocketAddress;
import ibis.smartsockets.virtual.VirtualSocketFactory;
import ibis.util.TypedProperties;

/**
 * Convenience class to retrieve information on the server, and create a
 * suitable VirtualSocketFactory.
 */
public class Client {

    private static final Logger logger = LoggerFactory.getLogger(Client.class);

    private Client() {
        // DO NOT USE
    }

  
    private static DirectSocketAddress createAddressFromString(
            String serverString, int defaultPort) throws ConfigurationException {

        if (serverString == null) {
            throw new ConfigurationException("serverString undefined");
        }

        // maybe it is a DirectSocketAddress?
        try {
            return DirectSocketAddress.getByAddress(serverString);
        } catch (Throwable e) {
            // IGNORE
        }

        Throwable throwable = null;
        // or only a host address
        try {
            return DirectSocketAddress.getByAddress(serverString, defaultPort);
        } catch (Throwable e) {
            throwable = e;
            // IGNORE
        }

        throw new ConfigurationException(
                "could not create server address from given string: "
                        + serverString, throwable);
    }

    /**
     * Get the address of a service running on a given port
     * 
     * @param port
     *            the port the service is running on
     * @param properties
     *            object containing any server properties needed (such as the
     *            servers address)
     */
    public static VirtualSocketAddress getServiceAddress(int port,
            Properties properties) throws ConfigurationException {
        TypedProperties typedProperties = ServerProperties
                .getHardcodedProperties();
        typedProperties.addProperties(properties);

        String serverAddressString = typedProperties
                .getProperty(IbisProperties.SERVER_ADDRESS);
        if (serverAddressString == null || serverAddressString.equals("")) {
        	
        	/* No server address found. Try to connect via Advert server. */
        	String serverAdvertString = typedProperties
            		.getProperty(IbisProperties.ADVERT_URI);
        	if (serverAdvertString != null && !serverAdvertString.equals("")) {

        		/* Found an Advert address, try to parse and connect. */
        		URI advertUri       = null;
            	Advert advertServer = null;

        		try {
        			advertUri    = new URI(serverAdvertString);
        			advertServer = new Advert(advertUri);
        		}
        		catch (Exception e) {
        			throw new ConfigurationException(IbisProperties.ADVERT_URI
        					+ " not properly structured.");
        		}
        		
        		/* See if a path (ID) is given. */
        		if (advertUri.getPath() != null && 
        				!advertUri.getPath().equals("")) {
        			
	        		/* Found a path, try to fetch registry server's address. */
        			try {
        				byte[] b = advertServer.get(advertUri.getPath());
        				serverAddressString = new String(b);
        			}
        			catch (Exception e) {
        				throw new ConfigurationException("Fetching from Advert"
        						+ " server failed", e);
        			}
        		}
        		else {
	        		
        			/* Check for meta data. */
	        		String serverMetaDataString = typedProperties
	    			.getProperty(IbisProperties.ADVERT_MD);
	        		if (serverMetaDataString != null && 
	        				!serverMetaDataString.equals("")) {
	        			
	        			/* Connecting using MD. */
	        			MetaData md = new MetaData(serverMetaDataString);
                		
                		try {
                			String[] results = advertServer.find(md);
                			if (results == null || results.length < 1) {
                				throw new ConfigurationException( 
                    					"No server matching MD found.");
                			}
                			byte[] b = advertServer.get(results[0]);
                			serverAddressString = new String(b);
                		}
                		catch (Exception e) {
                			throw new ConfigurationException("Fetching from " + 
                					"Advert server (using MD) failed", e);
                		}
	        		}
	        		else {
	        			throw new ConfigurationException("No meta data found, "
	        					+ "cannot connect.");
	        		}
        		}
        	}
        	else {
	            throw new ConfigurationException(IbisProperties.SERVER_ADDRESS
	                    + " undefined, cannot locate server");
        	}
        }

        logger.debug("server address = \"" + serverAddressString + "\"");

        int defaultPort = typedProperties.getIntProperty(ServerProperties.PORT);

        DirectSocketAddress serverMachine = createAddressFromString(
                serverAddressString, defaultPort);

        if (serverMachine == null) {
            throw new ConfigurationException("cannot get address of server");
        }

        return new VirtualSocketAddress(serverMachine, port, serverMachine,
                null);
    }

    public static synchronized VirtualSocketFactory getFactory(
            TypedProperties typedProperties) throws ConfigurationException,
            IOException {
        return getFactory(typedProperties, 0);
    }

    public static synchronized VirtualSocketFactory getFactory(
            TypedProperties typedProperties, int port)
            throws ConfigurationException, IOException {

        String hubs = typedProperties.getProperty(IbisProperties.HUB_ADDRESSES);

        // did the server also start a hub?
        boolean serverIsHub = typedProperties
                .getBooleanProperty(IbisProperties.SERVER_IS_HUB);

        String server = typedProperties
                .getProperty(IbisProperties.SERVER_ADDRESS);
        if (server != null && !server.equals("") && serverIsHub) {
            // add server to hub addresses
            DirectSocketAddress serverAddress = createAddressFromString(server,
                    typedProperties.getIntProperty(ServerProperties.PORT,
                            ServerProperties.DEFAULT_PORT));
            if (hubs == null || hubs.equals("")) {
                hubs = serverAddress.toString();
            } else {
                hubs = hubs + "," + serverAddress.toString();
            }
        }

        Properties smartProperties = new Properties();

        if (port > 0) {
            smartProperties.put(SmartSocketsProperties.PORT_RANGE, Integer
                    .toString(port));
        }

        if (hubs != null) {
            smartProperties.put(SmartSocketsProperties.HUB_ADDRESSES, hubs);
        }

        try {
            VirtualSocketFactory result = VirtualSocketFactory
                    .getSocketFactory("ibis");

            if (result == null) {
                result = VirtualSocketFactory.getOrCreateSocketFactory("ibis",
                        smartProperties, true);
            } else if (hubs != null) {
                result.addHubs(hubs.split(","));
            }
            return result;
        } catch (InitializationException e) {
            throw new IOException(e.getMessage());
        }
    }

}
