package ibis.connect.socketFactory;

import ibis.connect.util.ConnProps;

public abstract class SocketType {
    public static class DefaultConnectProperties implements ConnectProperties {
        ConnectProperties prevail = null;

        public DefaultConnectProperties() {
            // empty constructor
        }

        public DefaultConnectProperties(ConnectProperties prevail) {
            this.prevail = prevail;
        }

        public String getProperty(String name) {
            if (prevail != null) {
                String retval = prevail.getProperty(name);
                if (retval != null) {
                    return retval;
                }
            }
            return System.getProperties().getProperty(
                    ConnProps.PROPERTY_PREFIX + name);
        }
    }

    private String socketTypeName = null;

    protected SocketType(String name) {
        socketTypeName = name;
    }

    public void destroySocketType() {
        /* as default, do nothing */
    }

    public String getSocketTypeName() {
        return socketTypeName;
    }

    public boolean supportsClientServer() {
        return ClientServerSocketFactory.class.isAssignableFrom(
                this.getClass());
    }

    public boolean supportsBrokeredLinks() {
        return BrokeredSocketFactory.class.isAssignableFrom(this.getClass());
    }
}

