/* $Id$ */

package ibis.connect.routedMessages;

import ibis.util.IPUtils;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;

import org.apache.log4j.Logger;

public class RoutedMessagesServerSocket extends ServerSocket {

    static Logger logger
            = ibis.util.GetLogger.getLogger(RoutedMessagesServerSocket.class.getName());

    private HubLink hub = null;

    private int serverPort = -1;

    private InetAddress addr;

    private boolean socketOpened = false;

    private ArrayList requests = new ArrayList();

    private static class Request {
        int requestPort;

        String requestHost;

        int requestHubPort;

        Request(int clientPort, String clientHost, int clienthubport) {
            requestPort = clientPort;
            requestHost = clientHost;
            requestHubPort = clienthubport;
        }
    }

    public RoutedMessagesServerSocket(int port, InetAddress addr) throws IOException {
        hub = HubLinkFactory.getHubLink();
        serverPort = hub.newPort(port);
        this.addr = addr;
        socketOpened = true;
        hub.addServer(this, serverPort);
        logger.debug("# RoutedMessagesServerSocket() addr=" + addr + "; port="
                + serverPort);
    }

    public InetAddress getInetAddress() {
        InetAddress address = addr;
        if (address == null) {
            try {
                address = IPUtils.getLocalHostAddress();
            } catch (Exception e) {
                throw new Error(e);
            }
        }
        logger.debug("# RoutedMessagesServerSocket.getInetAddress() addr="
                + address);
        return address;
    }

    public int getLocalPort() {
        logger.debug("# RoutedMessagesServerSocket.getLocalPort() port="
                + serverPort);
        return serverPort;
    }

    public Socket accept() throws IOException {
        Socket s = null;
        logger.debug("# RoutedMessagesServerSocket.accept()- waiting on port "
                + serverPort);
        hub = HubLinkFactory.getHubLink();
        Request r = null;
        synchronized (this) {
            while (requests.size() == 0) {
                if (!socketOpened) {
                    throw new SocketException();
                }
                try {
                    this.wait();
                } catch (InterruptedException e) { /* ignore */
                }
            }
            r = (Request) requests.remove(0);
        }

        int localPort = hub.newPort(0);
        logger.debug("# RoutedMessagesServerSocket.accept()- on port "
                + serverPort + " unlocked; from port=" + r.requestPort
                + "; host=" + r.requestHost);
        s = new RoutedMessagesSocket(r.requestHost, r.requestPort, localPort,
                r.requestHubPort);
        logger.debug("# RoutedMessagesServerSocket.accept()- new "
                + "RoutedMessagesSocket created on port=" + localPort
                + "- Sending ACK.");
        hub.sendPacket(r.requestHost, r.requestHubPort,
                new HubProtocol.HubPacketAccept(r.requestPort,
                        hub.localHostName, localPort));
        return s;
    }

    public synchronized void close() {
        logger.debug("# RoutedMessagesServerSocket.close() of port "
                + serverPort);
        socketOpened = false;
        this.notifyAll();
        hub.removeServer(serverPort);
    }

    /* Method for the HubLink to feed us with new incoming connections
     * returns: true=ok; false=connection refused
     */
    protected synchronized void enqueueConnect(String clientHost,
            int clientPort, int clienthubport) {
        requests.add(new Request(clientPort, clientHost, clienthubport));
        logger.debug("# RoutedMessagesServerSocket.enqueueConnect() for port "
                + serverPort + ", size = " + requests.size());
        if (requests.size() == 1) {
            logger.debug("# RoutedMessagesServerSocket.enqueueConnect(): notify");
            this.notify();
        }
    }
}
