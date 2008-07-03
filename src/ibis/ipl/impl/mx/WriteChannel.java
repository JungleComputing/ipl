package ibis.ipl.impl.mx;

import java.io.IOException;
import java.nio.ByteBuffer;

public interface WriteChannel {

	/**
	 * @param buffer The data buffer that is sent. The data between the position and the limit will be sent. Upon completion the position will be equal to the limit.
	 * @throws IOException An exception when sending went wrong. When an exception occurs, the message is not sent and the buffer is unharmed.
	 */
	public void write(ByteBuffer buffer) throws IOException;

	public void write(SendBuffer buffer) throws IOException;

	public void finish() throws IOException;

	public boolean isFinished() throws IOException;
	
	public void close();

}