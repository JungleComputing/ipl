package ibis.connect.parallelStreams;

import ibis.connect.socketFactory.ConnectProperties;
import ibis.connect.socketFactory.DummySocket;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;


public class PSSocket extends DummySocket
{
    private ParallelStreams ps = null;
    private PSInputStream   in = null;
    private PSOutputStream out = null;

    /* End-users are not supposed to call this constructor.
     * They should use the socket factory instead.
     */
    protected PSSocket(int numWays, int blockSize,
		       InputStream ctrlIs, OutputStream ctrlOs,
		       boolean hint, ConnectProperties p)
	throws IOException
    {
	ps = new ParallelStreams(numWays, blockSize, p);
	ps.connect(ctrlIs, ctrlOs, hint);
	in = new PSInputStream();
	out = new PSOutputStream();
    }

    public OutputStream getOutputStream()
	throws IOException
    {
	return out;
    }

    public InputStream getInputStream()
	throws IOException
    {
	return in;
    }

    public void setSendBufferSize(int n) throws SocketException {
	ps.setSendBufferSize(n);
    }

    public void setReceiveBufferSize(int n) throws SocketException {
	ps.setReceiveBufferSize(n);
    }

    public void close()
	throws IOException
    {
	if(ps != null) {
	    ps.close();
	    ps = null;
	}
	in = null;
	out = null;
    }

    public void setTcpNoDelay(boolean on) throws SocketException {
	if (ps != null) {
	    ps.setTcpNoDelay(on);
	}
    }

    public void shutdownInput() throws IOException {
	if (ps != null) {
	    ps.shutdownInput();
	}
    }

    public void shutdownOutput() throws IOException {
	if (ps != null) {
	    ps.shutdownOutput();
	}
    }

    private class PSInputStream extends InputStream
    {
	public PSInputStream() {
	    super();
	}

	public int read(byte[] b)
	    throws IOException
	{
	    return this.read(b, 0, b.length);
	}
	public int read(byte[] b, int off, int len)
	    throws IOException
	{
	    return ps.recv(b, off, len);
	}
	public int read()
	    throws IOException
	{
	    byte[] b = new byte[1];
	    int rc = 0;
	    while (rc == 0) {
		rc = this.read(b);
	    }
	    if (rc == -1) {
		return -1;
	    }
	    return b[0] & 255;
	    
	}
	public int available()
	    throws IOException
	{
	    return ps.poll();
	}
	public void close()
	    throws IOException
	{
	    in = null;
	}
    }

    private class PSOutputStream extends OutputStream
    {
	public PSOutputStream() {
	    super();
	}

	public void write(int v)
	    throws IOException
	{
	    byte[] b = new byte[1];
	    b[0] = (byte)v;
	    this.write(b);
	}
	public void write(byte[] b)
	    throws IOException
	{
	    this.write(b, 0, b.length);
	}
	public void write(byte[] b, int off, int len)
	    throws IOException
	{
	    ps.send(b, off, len);
	}
	public void flush()
	    throws IOException
	{
	}
	public void close()
	    throws IOException
	{
	    out = null;
	}
    }
    
}
