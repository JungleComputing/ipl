package ibis.rmi.server;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.StreamCorruptedException;

public interface RemoteCall
{
    public ObjectOutput getOutputStream()  throws IOException;
    
    public void releaseOutputStream()  throws IOException;

    public ObjectInput getInputStream()  throws IOException;

    public void releaseInputStream() throws IOException;

    public ObjectOutput getResultStream(boolean success) throws IOException, StreamCorruptedException;
    
    public void executeCall() throws Exception;

    public void done() throws IOException;
}
