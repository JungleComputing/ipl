package ibis.ipl.impl.net.multi.plugins;

import ibis.ipl.impl.net.*;
import ibis.ipl.impl.net.multi.*;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;

import java.net.InetAddress;

import java.util.Random;


/**
 * Provide an example of multiprotocol's driver plugin.
 */
public final class random implements MultiPlugin {
        public random() {};

        static Random r = new Random();

        public String getSubContext(boolean 		isOutgoing,
                                    NetIbisIdentifier  	localId,
                                    NetIbisIdentifier  	remoteId,
                                    ObjectOutputStream	os,
                                    ObjectInputStream 	is) throws NetIbisException {
                String  subContext = null;
                boolean value      = false;

                try {
                        if (isOutgoing) {
                                synchronized(r) {
                                        value = r.nextBoolean();
                                }

                                os.writeObject(new Boolean(value));
                                os.flush();
                        } else {
                                Boolean b = (Boolean)is.readObject();
                                value = b.booleanValue();
                        }
                } catch (Exception e) {
                        e.printStackTrace();
                        throw new NetIbisException(e);
                }

                if (value) {
                        subContext = "a";
                } else {
                        subContext = "b";
                }

                return subContext;
        }
}

