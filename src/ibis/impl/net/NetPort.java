package ibis.impl.net;


/**
 * Provide a common interface to both {@link ibis.impl.net.NetSendPort} and {@link
 * NetReceivePort} objects.
 */
public interface NetPort {

        /**
         * Return the {@linkplain ibis.impl.net.NetPortType port type}.
         *
         * @return the {@linkplain ibis.impl.net.NetPortType port type}.
         */
        public NetPortType getPortType();

	public void closeFromRemote(NetConnection cnx);
}

