package ibis.ipl.impl.net.multi.plugins;

import ibis.ipl.impl.net.*;
import ibis.ipl.impl.net.multi.*;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;

import java.net.InetAddress;



/**
 * Provide an example of multiprotocol's driver plugin.
 */
public final class abc implements MultiPlugin {
        public abc() {};

        //
        public String getSubContext(boolean 		isOutgoing,
                                    NetIbisIdentifier  	localId,
                                    NetIbisIdentifier  	remoteId,
                                    ObjectOutputStream	os,
                                    ObjectInputStream 	is) throws NetIbisException {
                String subContext = null;

                try {
                        InetAddress localHostAddr  = InetAddress.getLocalHost();
                        InetAddress remoteHostAddr = null;

                        if (isOutgoing) {
                                os.writeObject(localHostAddr);
                                os.flush();
                                remoteHostAddr = (InetAddress)is.readObject();
                        } else {
                                remoteHostAddr = (InetAddress)is.readObject();
                                os.writeObject(localHostAddr);
                                os.flush();
                        }

                        if (localId.equals(remoteId)) {
                                subContext = "process";
                        } else {
                                byte [] l = localHostAddr.getAddress();
                                byte [] r = remoteHostAddr.getAddress();
                                int n = 0;

                                while (n < 4 && l[n] == r[n])
                                        n++;

                                switch (n) {
                                case 4:
                                        {
                                                subContext = "node";
                                                break;
                                        }

                                case 3:
                                        {
                                                subContext = "net_c";
                                                break;
                                        }

                                case 2:
                                        {
                                                subContext = "net_b";
                                                break;
                                        }

                                case 1:
                                        {
                                                subContext = "net_a";
                                                break;
                                        }

                                default:
                                        {
                                                subContext = "internet";
                                                break;
                                        }
                                }
                        }
                } catch (Exception e) {
                        e.printStackTrace();
                        throw new NetIbisException(e);
                }

                return subContext;
        }
}

