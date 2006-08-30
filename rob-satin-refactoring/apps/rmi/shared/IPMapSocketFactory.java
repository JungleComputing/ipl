/* $Id$ */


import java.io.IOException;

import java.net.Socket;
import java.net.ServerSocket;
import java.net.InetAddress;

import java.rmi.server.RMISocketFactory;

import ibis.util.IPUtils;
import ibis.util.TypedProperties;

import org.apache.log4j.Logger;

public class IPMapSocketFactory extends RMISocketFactory {

    static Logger logger = Logger.getLogger(IPMapSocketFactory.class.getName());

    private InetAddress myAddr = IPUtils.getLocalHostAddress();

    public IPMapSocketFactory() {
        logger.debug("My local hostaddr " + myAddr);
        try {
            logger.debug("My default hostaddr "
                    + InetAddress.getLocalHost());
        } catch (IOException e) {
            // give up
        }
    }

    public ServerSocket createServerSocket(int port) throws IOException {
        ServerSocket s = new ServerSocket(port, 0, myAddr);
        logger.debug("Created new ServerSocket " + s);
        // s.setTcpNoDelay(true);
        return s;
    }

    public Socket createSocket(String host, int port) throws IOException {
        Socket s = null;

        InetAddress toAddr = InetAddress.getByName(host);
        if (toAddr.equals(InetAddress.getLocalHost())) {
            logger.debug("Replace " + host + " with " + myAddr.getHostName());

            try {
                s = new Socket(myAddr, port, myAddr, 0);
            } catch (IOException e) {
                logger.debug("Replaced connect fails, try default " + e);
            }
        }

        if (s == null) {
            s = new Socket(host, port, myAddr, 0);
        }

        logger.debug("Created new Socket " + s);
        s.setTcpNoDelay(true);
        return s;
    }

}
