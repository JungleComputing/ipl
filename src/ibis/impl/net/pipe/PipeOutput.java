package ibis.impl.net.pipe;

import ibis.impl.net.NetBank;
import ibis.impl.net.NetBufferedOutput;
import ibis.impl.net.NetConnection;
import ibis.impl.net.NetDriver;
import ibis.impl.net.NetPortType;
import ibis.impl.net.NetSendBuffer;
import ibis.io.Conversion;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.Hashtable;

public final class PipeOutput extends NetBufferedOutput {
	private Integer           rpn        = null;
	private PipedOutputStream pipeOs     = null;
        private boolean           upcallMode = false;

	PipeOutput(NetPortType pt, NetDriver driver, String context) throws IOException {
		super(pt, driver, context);
		headerLength = 4;
	}

	/**
	 * {@inheritDoc}
	 */
	public synchronized void setupConnection(NetConnection cnx) throws IOException {
                log.in();
                if (this.rpn != null) {
                        throw new Error("connection already established");
                }
                
		this.rpn = cnx.getNum();
	
		ObjectInputStream is = new ObjectInputStream(cnx.getServiceLink().getInputSubStream(this, "pipe"));

		Hashtable info;
		try {
			info = (Hashtable)is.readObject();
		} catch (ClassNotFoundException e) {
			throw new Error(e);
		}
		mtu  	       = ((Integer)info.get("pipe_mtu")).intValue();
		upcallMode     = ((Boolean)info.get("pipe_upcall_mode")).booleanValue();

		NetBank          bank   = driver.getIbis().getBank();
		Long             key    = (Long)info.get("pipe_istream_key");
		
		PipedInputStream pipeIs = (PipedInputStream)bank.discardKey(key);

		pipeOs = new PipedOutputStream(pipeIs);
		OutputStream os = cnx.getServiceLink().getOutputSubStream(this, "pipe");
			
		os.write(1); // Connection ack
		os.flush();
		is.close();
		os.close();
                log.out();
	}

	public void sendByteBuffer(NetSendBuffer b) throws IOException {
                log.in();
		Conversion.defaultConversion.int2byte(b.length, b.data, 0);
		pipeOs.write(b.data, 0, b.length);
		/* Flush, otherwise it takes 1 second RFHH */
		pipeOs.flush();
		if (!upcallMode) {
			Thread.yield();
		}
			
		if (! b.ownershipClaimed) {
			b.free();
		}
		log.out();
	}

        public synchronized void close(Integer num) throws IOException {
                log.in();
                if (rpn == num) {
			if (pipeOs != null) {
				pipeOs.close();
			}
	
			rpn = null;
                }
                log.out();
        }

	/**
	 * {@inheritDoc}
	 */
	public void free() throws IOException {
                log.in();
		if (pipeOs != null) {
			pipeOs.close();
		}
	
		rpn = null;

		super.free();
                log.out();
	}

}
