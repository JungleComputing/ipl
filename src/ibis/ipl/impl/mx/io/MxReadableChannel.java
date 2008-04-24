/**
 * 
 */
package ibis.ipl.impl.mx.io;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.nio.channels.spi.SelectorProvider;

/**
 * @author Timo van Kessel
 *
 */
public class MxReadableChannel extends AbstractSelectableChannel implements
		ReadableByteChannel {

	/**
	 * @param arg0
	 */
	public MxReadableChannel(SelectorProvider arg0) {
		super(arg0);
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
	protected void implConfigureBlocking(boolean arg0) throws IOException {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see java.nio.channels.SelectableChannel#validOps()
	 */
	@Override
	public int validOps() {
		// TODO Auto-generated method stub
		return SelectionKey.OP_READ;
	}

	/* (non-Javadoc)
	 * @see java.nio.channels.ReadableByteChannel#read(java.nio.ByteBuffer)
	 */
	public int read(ByteBuffer dst) throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}

}
