/* $Id$ */


import java.io.IOException;

import java.net.Socket;
import java.net.ServerSocket;
import java.net.InetAddress;

import java.rmi.server.RMISocketFactory;

import ibis.util.IPUtils;
import ibis.util.TypedProperties;

public class IPMapSocketFactory extends RMISocketFactory {

    private final static boolean DEBUG = TypedProperties.booleanProperty(
            "IPMapSF.verbose", false);

    private InetAddress myAddr = IPUtils.getLocalHostAddress();

    public IPMapSocketFactory() {
        if (DEBUG) {
            System.err.println("My local hostaddr " + myAddr);
            try {
                System.err.println("My default hostaddr "
                        + InetAddress.getLocalHost());
            } catch (IOException e) {
                // give up
            }
        }
    }

    public ServerSocket createServerSocket(int port) throws IOException {
        ServerSocket s = new ServerSocket(port, 0, myAddr);
        if (DEBUG) {
            System.err.println("Created new ServerSocket " + s);
        }
        // s.setTcpNoDelay(true);
        return s;
    }

    public Socket createSocket(String host, int port) throws IOException {
        Socket s = null;

        InetAddress toAddr = InetAddress.getByName(host);
        if (toAddr.equals(InetAddress.getLocalHost())) {
            if (DEBUG) {
                System.err.println("Replace " + host + " with "
                        + myAddr.getHostName());
            }

            try {
                s = new Socket(myAddr, port, myAddr, 0);
            } catch (IOException e) {
                if (DEBUG) {
                    System.err.println("Replaced connect fails, try default "
                            + e);
                }
            }
        }

        if (s == null) {
            s = new Socket(host, port, myAddr, 0);
        }

        if (DEBUG) {
            System.err.println("Created new Socket " + s);
        }
        s.setTcpNoDelay(true);
        return s;
    }

}