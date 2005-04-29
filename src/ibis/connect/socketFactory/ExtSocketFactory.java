/* $Id$ */

package ibis.connect.socketFactory;

import ibis.connect.util.ConnectionProperties;
import ibis.util.IPUtils;
import ibis.util.TypedProperties;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.Vector;

import org.apache.log4j.Logger;

/* A generalized SocketFactory which supports:
 -- client/server connection scheme
 -- brokered connections through a control link
 */
public class ExtSocketFactory {
    static Logger logger = ibis.util.GetLogger.getLogger(ExtSocketFactory.class.getName());

    /** Vector of SocketTypes in the order of preference
     * for the current strategy.
     */
    private static Vector types = new Vector();

    /** Map which converts a SocketType nicknames into
     * the class name which implements it.
     */
    private static Hashtable nicknames = new Hashtable();

    private static SocketType defaultClientServer = null;

    private static SocketType defaultBrokeredLink = null;

    // Some possible default strategies...
    // -1- Plain TCP only- no firewall support.
    private static final String[] strategyTCP = { "PlainTCP" };

    // -2- full range of conection methods: supports firewalls
    private static final String[] strategyFirewall = { "TCPSplice",
            "RoutedMessages", "PlainTCP" };

    // -3- supports firewall for control only, no splicing.
    // Usefull for tests only.
    private static final String[] strategyControl = { "RoutedMessages",
            "PlainTCP" };

    // -4- TCP splicing only- for tests.
    private static final String[] strategySplicing = { "TCPSplice",
            "PlainTCP" };

    // Pick one of the above choices for defaults
    private static String[] defaultTypes = strategyTCP;

    private static boolean parallel = false;

    /* static constructor
     */
    static {
        ConnectionProperties.checkProps();
        logger.info("# ExtSocketFactory: starting configuration."); 
        // init types table
        declareNickname("PlainTCP",
                "ibis.connect.socketFactory.PlainTCPSocketType");
        declareNickname("TCPSplice",
                "ibis.connect.tcpSplicing.TCPSpliceSocketType");
        declareNickname("RoutedMessages",
                "ibis.connect.routedMessages.RoutedMessagesSocketType");
        declareNickname("ParallelStreams",
                "ibis.connect.parallelStreams.ParallelStreamsSocketType");
        declareNickname("PortRange",
                "ibis.connect.socketFactory.PortRangeSocketType");
        declareNickname("SSL", "ibis.connect.socketFactory.SSLSocketType");
        declareNickname("AnyTCP",
                "ibis.connect.socketFactory.AnyTCPSocketType");
        // Declare new nicknames here.

        String bl = TypedProperties.stringProperty(ConnectionProperties.datalinks);
        String cs = TypedProperties.stringProperty(ConnectionProperties.controllinks);

        if (bl != null) {
            StringTokenizer st = new StringTokenizer(bl, " ,\t\n\r\f");
            bl = null;
            while (st.hasMoreTokens()) {
                String s = st.nextToken();
                loadSocketType(s);
                if (s.equals("ParallelStreams")) {
                    // ParallelStreams has an underlying brokered socket type.
                    parallel = true;
                } else {
                    bl = s;
                }
            }
        }

        if (bl == null || cs == null) {
            logger.info("# Loading defaults...");
            for (int i = 0; i < defaultTypes.length; i++) {
                String n = defaultTypes[i];
                loadSocketType(n);
            }
        }
        if (bl == null) {
            defaultBrokeredLink = findBrokeredType();
        } else {
            defaultBrokeredLink = loadSocketType(bl);
        }
        if (cs == null) {
            defaultClientServer = findClientServerType();
        } else {
            defaultClientServer = loadSocketType(cs);
        }
        logger.info("# Default for client-server: "
                + defaultClientServer.getSocketTypeName());
        logger.info("# Default for brokered link: "
                + defaultBrokeredLink.getSocketTypeName());
        logger.info("# ### ExtSocketFactory: configuration ok.");
    }

    /* static destructor
     */
    public static void shutdown() {
        for (int i = 0; i < types.size(); i++) {
            SocketType t = (SocketType) types.get(i);
            t.destroySocketType();
        }
    }

    private static void declareNickname(String nickname, String className) {
        nicknames.put(nickname.toLowerCase(), className);
    }

    /* loads a SocketType into the factory
     *   name: a nickname from the 'nicknames' hashtable, or a 
     *   fully-qualified class name which extends SocketType
     */
    private static synchronized SocketType loadSocketType(String socketType) {
        SocketType t = null;
        String className = (String) nicknames.get(socketType.toLowerCase());
        Class c = null;
        if (className == null) {
            className = socketType;
        }
        try {
            c = Class.forName(className);
        } catch (Exception e) {
            logger.error("# ExtSocketFactory: socket type " + socketType
                    + " not found.");
            logger.error("#   known types are:");
            Enumeration i = nicknames.keys();
            while (i.hasMoreElements()) {
                logger.error((String) i.nextElement());
            }
            throw new Error("ExtSocketFactory: class not found: " + className,
                    e);
        }
        try {
            t = (SocketType) c.newInstance();
            logger.info("# Registering socket type: " + t.getSocketTypeName());
            logger.info("#   class name: " + t.getClass().getName());
            logger.info("#   supports client/server:  "
                    + t.supportsClientServer());
            logger.info("#   supports brokered links: "
                    + t.supportsBrokeredLinks());
            types.add(t);
        } catch (Exception e) {
            logger.error("# ExtSocketFactory: Socket type constructor "
                    + className + " got exception:", e);
            logger.error("# ExtSocketFactory: loadSocketType returns null");
        }
        return t;
    }

    // Bootstrap client sockets: Socket(addr, port);
    public static Socket createClientSocket(InetAddress addr, int port)
            throws IOException {
        Socket s = null;
        SocketType t = defaultClientServer;
        if (t == null) {
            throw new Error("no socket type found!");
        }
        ClientServerSocketFactory f = null;
        try {
            f = (ClientServerSocketFactory) t;
        } catch (Exception e) {
            logger.error("SocketFactory: SocketType " + t.getSocketTypeName()
                   + " does not support client/sever connection establishment"
                   , e);
            throw new Error(e);
        }
        s = f.createClientSocket(addr, port);
        tuneSocket(s, null);
        return s;
    }

    // Bootstrap server sockets: ServerSocket(port, backlog, addr);
    public static ServerSocket createServerSocket(int port, int backlog,
            InetAddress addr) throws IOException {
        return createServerSocket(new InetSocketAddress(addr, port), backlog);
    }

    public static ServerSocket createServerSocket(InetSocketAddress addr,
            int backlog) throws IOException {
        ServerSocket s = null;
        SocketType t = defaultClientServer;
        if (t == null) {
            throw new Error("no socket type found!");
        }
        ClientServerSocketFactory f = null;
        try {
            f = (ClientServerSocketFactory) t;
        } catch (Exception e) {
            logger.error("SocketFactory: SocketType " + t.getSocketTypeName()
                   + " does not support client/sever connection establishment",
                   e);
            throw new Error(e);
        }
        s = f.createServerSocket(addr, backlog);
        return s;
    }

    private static Socket createBrokeredSocket(InputStream in,
            OutputStream out, boolean hintIsServer, SocketType t,
            ConnectionPropertiesProvider props) throws IOException {
        Socket s = null;
        BrokeredSocketFactory f = null;
        try {
            f = (BrokeredSocketFactory) t;
        } catch (Exception e) {
            logger.error("SocketFactory: SocketType " + t.getSocketTypeName()
                    + " does not support brokered connection establishment", e);
            throw new Error(e);
        }
        logger.debug("SocketFactory: creating brokered socket- hint="
                + hintIsServer + "; type=" + t.getSocketTypeName());
        s = f.createBrokeredSocket(in, out, hintIsServer, props);
        tuneSocket(s, props);
        return s;
    }

    // Data connections: when a service link is available
    public static Socket createBrokeredSocket(InputStream in, OutputStream out,
            boolean hintIsServer) throws IOException {
        SocketType t = parallel ? loadSocketType("ParallelStreams")
                : defaultBrokeredLink;
        ConnectionPropertiesProvider props = new SocketType.DefaultConnectProperties();
        if (t == null) {
            throw new Error("no socket type found!");
        }
        return createBrokeredSocket(in, out, hintIsServer, t, props);
    }

    public static Socket createBrokeredSocket(InputStream in, OutputStream out,
            boolean hintIsServer, ConnectionPropertiesProvider props) throws IOException {
        SocketType t = null;
        String st = null;
        if (props != null) {
            st = props.getProperty("SocketType");
        }
        props = new SocketType.DefaultConnectProperties(props);
        if (st == null) {
            t = parallel ? loadSocketType("ParallelStreams")
                    : defaultBrokeredLink;
        } else {
            logger.info("# Selected socket type '" + st
                    + "' through properties.");
            t = findSocketType(st);
        }
        if (t == null) {
            throw new Error("Socket type not found: " + st);
        }
        return createBrokeredSocket(in, out, hintIsServer, t, props);
    }

    /* Helper function to automatically design the 'Brokered' part 
     * of new SocketTypes which are basically client/server.
     */
    /* TODO: the 'hint' shouldn't be needed. There should be an
     * election to chose who is client and who is server.
     * WARNING: this will introduce a change in the API!
     */
    public static Socket createBrokeredSocketFromClientServer(
            ClientServerSocketFactory type, InputStream in, OutputStream out,
            boolean hintIsServer, ConnectionPropertiesProvider p) throws IOException {
        Socket s = null;
        if (hintIsServer) {
            ServerSocket server = type.createServerSocket(
                    new InetSocketAddress(IPUtils.getLocalHostAddress(), 0), 1);
            ObjectOutputStream os = new ObjectOutputStream(out);
            Hashtable lInfo = new Hashtable();
            lInfo.put("socket_address", server.getInetAddress());
            lInfo.put("socket_port", new Integer(server.getLocalPort()));
            os.writeObject(lInfo);
            os.flush();
            s = server.accept();
        } else {
            ObjectInputStream is = new ObjectInputStream(in);
            Hashtable rInfo = null;
            try {
                rInfo = (Hashtable) is.readObject();
            } catch (ClassNotFoundException e) {
                throw new Error(e);
            }
            InetAddress raddr = (InetAddress) rInfo.get("socket_address");
            int rport = ((Integer) rInfo.get("socket_port")).intValue();
            s = type.createClientSocket(raddr, rport);
        }
        tuneSocket(s, p);
        return s;
    }

    /* Find by name a SocketType in the list of known SocketTypes
     */
    // TODO: ugly code. This should use a Hashtable.
    private static synchronized SocketType findSocketType(String name) {
        SocketType t = null;
        for (int i = 0; i < types.size(); i++) {
            t = (SocketType) types.get(i);
            if (t.getSocketTypeName().equalsIgnoreCase(name)) {
                return t;
            }
        }
        return loadSocketType(name);
    }

    /* Find a default client/server SocketType when none is given
     * by system properties (ibis.connect.control_links).
     */
    private static synchronized SocketType findClientServerType() {
        for (int i = 0; i < types.size(); i++) {
            SocketType t = (SocketType) types.get(i);
            if (t.supportsClientServer()) {
                logger.info("# Selected type: '" + t.getSocketTypeName()
                            + "' for client/server connection.");
                return t;
            }
        }
        logger.error("# ExtSocketFactory: warning- no SocketType found "
                + "for client/server link!");
        return null;
    }

    /* Find a default brokered SocketType when none is given
     * by system properties (ibis.connect.data_links).
     */
    private static synchronized SocketType findBrokeredType() {
        for (int i = 0; i < types.size(); i++) {
            SocketType t = (SocketType) types.get(i);
            if (t.supportsBrokeredLinks()) {
                logger.info("# Selected type: '" + t.getSocketTypeName()
                        + "' for brokered link.");
                return t;
            }
        }
        logger.warn("# ExtSocketFactory: warning- no SocketType found "
                + "for brokered links!");
        return null;
    }

    public static BrokeredSocketFactory getBrokeredType() {
        return (BrokeredSocketFactory) defaultBrokeredLink;
    }

    private static void tuneSocket(Socket s, ConnectionPropertiesProvider p)
            throws IOException {
        int ibufsiz = 0x10000;
        int obufsiz = 0x10000;

        if (p != null) {
            String str = p.getProperty("InputBufferSize");
            if (str != null) {
                ibufsiz = Integer.parseInt(str);
            }
            str = p.getProperty("OutputBufferSize");
            if (str != null) {
                obufsiz = Integer.parseInt(str);
            }
        }

        s.setSendBufferSize(obufsiz);
        s.setReceiveBufferSize(ibufsiz);
        s.setTcpNoDelay(true);
    }
}
