package ibis.ipl.server;

import ibis.advert.Advert;
import ibis.advert.MetaData;
import ibis.ipl.IbisProperties;
import ibis.smartsockets.SmartSocketsProperties;
import ibis.smartsockets.direct.DirectSocketAddress;
import ibis.smartsockets.virtual.InitializationException;
import ibis.smartsockets.virtual.VirtualSocketAddress;
import ibis.smartsockets.virtual.VirtualSocketFactory;
import ibis.util.TypedProperties;

import java.io.IOException;
import java.net.URI;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
     * Private function to fetch a server address from the Ibis Advert server.
     * 
     * @param typedProperties
     * 		Fetches Advert server address and credentials from 
     * 		{@link TypedProperties}.
     * @return
     * 		A {@link String} containing the server address found, or <code>null
     * 		</code> if none was found.
     */
    private static String getAddressFromAdvert(TypedProperties typedProperties) {
    	
    	logger.info("Setting up Advert server connection.");
    	String serverAdvertString = typedProperties
        		.getProperty(IbisProperties.ADVERT_ADDRESS);
    	if (serverAdvertString == null || serverAdvertString.equals("")) {
    		return null;
    	}

		/* Found an Advert address, try to parse and connect. */
		URI advertUri       = null;
    	Advert advertServer = null;

		try {
			advertUri = new URI(serverAdvertString);
		}
		catch (Exception e) {
			System.err.print("Parsing Advert server address failed: ");
			e.printStackTrace();
			return null;
		}

		/* Determine whether to use a authenticated or public service. */
		if (typedProperties.getProperty(IbisProperties.ADVERT_USER) == null ||
			typedProperties.getProperty(IbisProperties.ADVERT_USER).equals("") ||
			typedProperties.getProperty(IbisProperties.ADVERT_PASS) == null ||
			typedProperties.getProperty(IbisProperties.ADVERT_PASS).equals("")) {
			
			/* Let the Advert class decide whether to use authentication. */ 
			try { 
				logger.info("Creating either public/private server class.");
				advertServer = new Advert(advertUri);
			}
			catch (Exception e) {
				System.err.print("Connecting to the Advert server failed: ");
				e.printStackTrace();
				return null;
			}
		}
		else {
			if (advertUri.getUserInfo() != null &&
    				!advertUri.getUserInfo().equals("")) {
    			logger.warn("Found two authentication credentials, " +
    					"using {}/********", 
    					typedProperties.getProperty(IbisProperties.ADVERT_USER));
    		}
    		/* Connect to authenticated version. */
			try {
				logger.info("Creating private server class.");
				advertServer = new Advert(advertUri, 
						typedProperties.getProperty(IbisProperties.ADVERT_USER),
						typedProperties.getProperty(IbisProperties.ADVERT_PASS));
			}
			catch (Exception e) {
				System.err.print("Connecting to the Advert server failed: ");
				e.printStackTrace();
				return null;
			}
		}
		
		/* See if a path (ID) is given. */
		if (advertUri.getPath() != null && 
				!advertUri.getPath().equals("")) {
			
    		/* Found a path, try to fetch registry server's address. */
			try {
				logger.info("Calling get()");
				byte[] b = advertServer.get(advertUri.getPath());
				typedProperties.setProperty(IbisProperties.SERVER_ADDRESS,
						new String(b));
				return new String(b);
				
			}
			catch (Exception e) {
				System.err.print("Getting Advert data failed: ");
				e.printStackTrace();
				return null;
			}
		}
		else {
    		
			/* No path given. Check for meta data. */
    		String serverMetaDataString = typedProperties
			.getProperty(IbisProperties.ADVERT_MD);
    		if (serverMetaDataString != null && 
    				!serverMetaDataString.equals("")) {
    			
    			/* Connecting using MD. */
    			MetaData md = new MetaData(serverMetaDataString);
        		
        		try {
        			logger.info("Calling find()");
        			String[] results = advertServer.find(md);
        			if (results == null || results.length < 1) {
        				throw new ConfigurationException( 
            					"No server matching MD found.");
        			}
        			logger.info("Calling get()");
        			byte[] b = advertServer.get(results[0]);
        			typedProperties.setProperty(IbisProperties.SERVER_ADDRESS,
    						new String(b));
    				return new String(b);
        		}
        		catch (Exception e) {
        			System.err.print("Getting Advert data failed: ");
    				e.printStackTrace();
    				return null;
        		}
    		}
    		else {
    			
    			/* Did not find a path and did not find meta data. */
    			System.err.println("No path nor meta data was given.");
    			return null;
    		}
		}
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
        if (serverAddressString == null || serverAddressString.equals("") ||
        		(serverAddressString = getAddressFromAdvert(typedProperties))
        		== null) {
        	throw new ConfigurationException(IbisProperties.SERVER_ADDRESS
                    + " undefined, cannot locate server");

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
        if (server == null || server.equals("")) {
        	server = getAddressFromAdvert(typedProperties); //fetch from advert
        }
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
