package ibis.impl.net.multi.plugins;

import ibis.impl.net.NetIbisIdentifier;
import ibis.impl.net.multi.MultiPlugin;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
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
                                    ObjectInputStream 	is) throws IOException {
                String  subContext = null;
                boolean value      = false;

		if (isOutgoing) {
			synchronized(r) {
				value = r.nextBoolean();
			}

			os.writeObject(new Boolean(value));
			os.flush();
		} else {
			Boolean b;
			try {
				b = (Boolean)is.readObject();
			} catch (ClassNotFoundException e) {
				throw new Error("Cannot find class Boolean", e);
			}
			value = b.booleanValue();
		}

                if (value) {
                        subContext = "a";
                } else {
                        subContext = "b";
                }

                return subContext;
        }
}

