package ibis.impl.net.def;

import ibis.impl.net.NetBufferedOutput;
import ibis.impl.net.NetConnection;
import ibis.impl.net.NetDriver;
import ibis.impl.net.NetPortType;
import ibis.impl.net.NetSendBuffer;
import ibis.io.Conversion;

import java.io.IOException;
import java.io.OutputStream;

public final class DefOutput extends NetBufferedOutput {
	private Integer      rpn   = null;
	private OutputStream defOs = null;

	static {
	    System.err.println("WARNING: Class netDefOutput (still) uses Conversion.defaultConversion");
	}

	/**
	 * Constructor.
	 *
	 * @param pt the properties of the output's 
	 * {@link ibis.impl.net.NetSendPort NetSendPort}.
	 * @param driver the DEF driver instance.
	 */
	DefOutput(NetPortType pt, NetDriver driver, String context)
		throws IOException {
		super(pt, driver, context);
		headerLength = 4;
	}

	public synchronized void setupConnection(NetConnection cnx) throws IOException {
                if (this.rpn != null) {
                        throw new Error("connection already established");
                }                

		this.rpn = cnx.getNum();
	
                defOs = cnx.getServiceLink().getOutputSubStream(this, "def");
		mtu = 1024;
	}

        public long finish() throws IOException{
                super.finish();
		defOs.flush();
		// TODO: return byte count of message
		return 0;
        }

	public void sendByteBuffer(NetSendBuffer b) throws IOException {
		Conversion.defaultConversion.int2byte(b.length, b.data, 0);
		//System.err.println("writing "+b.length+" bytes");
		defOs.write(b.data, 0, b.length);
		//System.err.println("writing "+b.length+" bytes - ok");
	}

        public synchronized void close(Integer num) throws IOException {
                if (rpn == num) {
			defOs.close();
                        rpn = null;
                }
        }
        

	public void free() throws IOException {
                if (defOs != null) {
			defOs.close();
                }
                
                rpn = null;
		super.free();
	}
}
