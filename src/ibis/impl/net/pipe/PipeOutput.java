package ibis.ipl.impl.net.pipe;

import ibis.ipl.impl.net.*;

import java.io.ObjectInputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import java.util.Hashtable;

public final class PipeOutput extends NetBufferedOutput {
	private Integer           rpn        = null;
	private PipedOutputStream pipeOs     = null;
        private boolean           upcallMode = false;

	PipeOutput(NetPortType pt, NetDriver driver, String context) throws NetIbisException {
		super(pt, driver, context);
		headerLength = 4;
	}

	/**
	 * {@inheritDoc}
	 */
	public synchronized void setupConnection(NetConnection cnx) throws NetIbisException {
                log.in();
                if (this.rpn != null) {
                        throw new Error("connection already established");
                }
                
		this.rpn = cnx.getNum();
	
		try {
                        ObjectInputStream is = new ObjectInputStream(cnx.getServiceLink().getInputSubStream(this, "pipe"));

			Hashtable info = (Hashtable)is.readObject();
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
		} catch (IOException e) {
			throw new NetIbisException(e);
		} catch (ClassNotFoundException e) {
			throw new Error(e);
		}
                log.out();
	}

	public void sendByteBuffer(NetSendBuffer b) throws NetIbisException {
                log.in();
		try {
			NetConvert.writeInt(b.length, b.data, 0);
			pipeOs.write(b.data, 0, b.length);
                        if (!upcallMode) {
                                Thread.currentThread().yield();
                        }
                        
		} catch (IOException e) {
			throw new NetIbisException(e);
		} 
		if (! b.ownershipClaimed) {
			b.free();
		}
                log.out();
	}

        public synchronized void close(Integer num) throws NetIbisException {
                log.in();
                if (rpn == num) {
                        try {
                                if (pipeOs != null) {
                                        pipeOs.close();
                                }
		
                                rpn = null;
                        } catch (Exception e) {
                                throw new NetIbisException(e);
                        }
                }
                log.out();
        }

	/**
	 * {@inheritDoc}
	 */
	public void free() throws NetIbisException {
                log.in();
		try {
			if (pipeOs != null) {
				pipeOs.close();
			}
		
			rpn = null;
		}
		catch (Exception e) {
			throw new NetIbisException(e);
		}

		super.free();
                log.out();
	}

}
