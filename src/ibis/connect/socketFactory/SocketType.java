package ibis.connect.socketFactory;


public abstract class SocketType
{
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


