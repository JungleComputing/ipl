package ibis.connect.socketFactory;

import ibis.connect.util.ConnProps;

public abstract class SocketType
{
    public interface ConnectProperties {
	public String getProperty(String name);
    }

    public static class DefaultConnectProperties 
	implements ConnectProperties {
	public String getProperty(String name) {
	    return System.getProperties().getProperty(ConnProps.PROPERTY_PREFIX+name);
	}
    }

    private String socketTypeName = null;
    protected SocketType(String name)
    {
	socketTypeName = name;
    }
    public void destroySocketType()
    {
	/* as default, do nothing */
    }
    public String getSocketTypeName()
    {
	return socketTypeName;
    }
    public boolean supportsClientServer()
    {
	return ClientServerSocketFactory.class.isAssignableFrom(this.getClass());
    }
    public boolean supportsBrokeredLinks()
    {
	return BrokeredSocketFactory.class.isAssignableFrom(this.getClass());
    }
}


