package ibis.ipl.impl.net.def;

import ibis.ipl.impl.net.*;

import java.io.IOException;
import java.io.OutputStream;

public final class DefOutput extends NetBufferedOutput {
	private Integer      rpn   = null;
	private OutputStream defOs = null;

	/**
	 * Constructor.
	 *
	 * @param sp the properties of the output's 
	 * {@link ibis.ipl.impl.net.NetSendPort NetSendPort}.
	 * @param driver the DEF driver instance.
	 */
	DefOutput(NetPortType pt, NetDriver driver, String context)
		throws NetIbisException {
		super(pt, driver, context);
		headerLength = 4;
	}

	/*
	 * {@inheritDoc}
	 */
	public synchronized void setupConnection(NetConnection cnx) throws NetIbisException {
                if (this.rpn != null) {
                        throw new Error("connection already established");
                }                

		this.rpn = cnx.getNum();
	
                defOs = cnx.getServiceLink().getOutputSubStream(this, "def");
		mtu = 1024;
	}

	/*
	 * {@inheritDoc}
	 */
        public void finish() throws NetIbisException{
                super.finish();
                try {
                        defOs.flush();
                } catch (IOException e) {
 			throw new NetIbisException(e.getMessage());
 		} 
        }

	/*
	 * {@inheritDoc}
	 */
        public void reset(boolean doSend) throws NetIbisException {
                if (doSend) {
                        send();
                } else {
                        throw new Error("full reset unimplemented");
                }
        }

	/*
	 * {@inheritDoc}
	 */
	public void sendByteBuffer(NetSendBuffer b) throws NetIbisException {
 		try {
 			NetConvert.writeInt(b.length, b.data, 0);
                        //System.err.println("writing "+b.length+" bytes");
 			defOs.write(b.data, 0, b.length);
                        //System.err.println("writing "+b.length+" bytes - ok");
 		} catch (IOException e) {
 			throw new NetIbisException(e.getMessage());
 		} 
	}

        public synchronized void close(Integer num) throws NetIbisException {
                if (rpn == num) {
                        try {
                                defOs.close();
                        } catch (IOException e) {
                                throw new Error(e);
                        }

                        rpn = null;
                }
        }
        

	/**
	 * {@inheritDoc}
	 */
	public void free() throws NetIbisException {
                if (defOs != null) {
                        try {
                                defOs.close();
                        } catch (IOException e) {
                                throw new Error(e);
                        }
                }
                
                rpn = null;
		super.free();
	}
}
