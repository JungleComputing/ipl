package ibis.ipl.impl.mx;

import java.io.IOException;
import java.nio.ByteBuffer;

public interface WriteChannel {

	/**
	 * @param buffer The data buffer that is sent. The data between the position and the limit will be sent. Upon completion the position will be equal to the limit.
	 * @throws IOException An exception when sending went wrong. When an exception occurs, the message is not sent and the buffer is unharmed.
	 */
	void write(ByteBuffer buffer) throws IOException;

	void write(SendBuffer buffer) throws IOException;

	void finish() throws IOException;

	boolean isFinished() throws IOException;
	
	void close();

}