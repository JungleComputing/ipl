package ibis.ipl.impl.mx;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;

public interface ReadChannel {

	public void close();

	public int read(ByteBuffer buffer) throws IOException;

	public boolean isOpen();

	public int poll() throws ClosedChannelException;

	public int poll(long timeout) throws ClosedChannelException;

}