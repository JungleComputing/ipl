package ibis.rmi.server;


abstract public class RemoteStub extends RemoteObject
{
    protected RemoteStub() {
    	super();
    }
    
    protected RemoteStub(RemoteRef ref) {
	super(ref);
    }

    static protected void setRef(RemoteStub stub, RemoteRef ref)
    {
	stub.ref = ref;
    }
    
}
