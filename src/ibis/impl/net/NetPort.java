package ibis.impl.net;


/**
 * Provide a common interface to both {@link NetSendPort} and {@link
 * NetReceivePort} objects.
 */
public interface NetPort {

        /**
         * Return the {@linkplain NetPortType port type}.
         *
         * @return the {@linkplain NetPortType port type}.
         */
        public NetPortType getPortType();

	public void closeFromRemote(NetConnection cnx);
}

