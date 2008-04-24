/**
 * 
 */
package ibis.ipl.impl.mx.io;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.WritableByteChannel;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.nio.channels.spi.SelectorProvider;

/**
 * @author Timo van Kessel
 *
 */
public class MxWritableChannel extends AbstractSelectableChannel implements
		WritableByteChannel {

	/**
	 * @param provider
	 */
	public MxWritableChannel(SelectorProvider provider) {
		super(provider);
		// TODO Auto-generated constructor stub
	}

	/* (non-Javadoc)
	 * @see java.nio.channels.spi.AbstractSelectableChannel#implCloseSelectableChannel()
	 */
	@Override
	protected void implCloseSelectableChannel() throws IOException {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see java.nio.channels.spi.AbstractSelectableChannel#implConfigureBlocking(boolean)
	 */
	@Override
	protected void implConfigureBlocking(boolean block) throws IOException {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see java.nio.channels.SelectableChannel#validOps()
	 */
	@Override
	public int validOps() {
		// TODO Auto-generated method stub
		return SelectionKey.OP_WRITE;
	}

	/* (non-Javadoc)
	 * @see java.nio.channels.WritableByteChannel#write(java.nio.ByteBuffer)
	 */
	public int write(ByteBuffer arg0) throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}

}
