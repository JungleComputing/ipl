package ibis.ipl.impl.net.def;

import ibis.ipl.impl.net.*;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.InputStream;

public final class DefInput extends NetBufferedInput {
	private Integer      spn       = null;
	private InputStream  defIs     = null;
	private NetAllocator allocator = null;
        private NetReceiveBuffer      buf             = null;
        private UpcallThread          upcallThread = null;

	DefInput(NetPortType pt, NetDriver driver, NetIO up, String context)
		throws NetIbisException {
		super(pt, driver, up, context);
		headerLength = 4;
	}

        private final class UpcallThread extends Thread {
                private volatile boolean end = false;

                public UpcallThread(String name) {
                        super("DefInput.UpcallThread: "+name);
                        setDaemon(true);
                }
                
                public void end() {
                        end = true;
                        this.interrupt();
                }
                

                public void run() {
                        while (!end) {
                                try {
                                        buf = receiveByteBuffer(0);
                                } catch (IOException e) {
                                        throw new Error(e);
                                }
                                
                                if (buf == null)
                                        break;

                                activeNum = spn;
                                initReceive();
                                try {
                                        upcallFunc.inputUpcall(DefInput.this, activeNum);
                                } catch (NetIbisInterruptedException e) {
                                        activeNum = null;
                                        break;
                                } catch (NetIbisClosedException e) {
                                        break;
                                } catch (NetIbisException e) {
                                        throw new Error(e.getMessage());
                                }
                                
                                activeNum = null;
                        }
                }
        }
        
	/*
	 * {@inheritDoc}
	 */
	synchronized public void setupConnection(NetConnection cnx)
		throws NetIbisException {
                if (this.spn != null) {
                        throw new Error("connection already established");
                }                

		this.spn = cnx.getNum();
                defIs    = cnx.getServiceLink().getInputSubStream("def");

                mtu = 1024;
		allocator = new NetAllocator(mtu);

                if (upcallFunc != null) {
                        upcallThread = new UpcallThread("this = " + this);
                        upcallThread.start();
                }
	}

	/**
	 * {@inheritDoc}
	 */
	public Integer poll() throws NetIbisException {
		activeNum = null;

		if (spn == null) {
			return null;
		}

		try {
			if (defIs.available() > 0) {
				activeNum = spn;
                                initReceive();
			}
		} catch (IOException e) {
			throw new NetIbisException(e);
		} 

		return activeNum;
	}

       	public void finish() throws NetIbisException {
                super.finish();
                activeNum = null;
        }


	/**
	 * {@inheritDoc}
	 *
	 * <BR><B>Note</B>: this function may block if the expected data is not there.
	 *
	 * @return {@inheritDoc}
	 */
	public NetReceiveBuffer receiveByteBuffer(int expectedLength) throws NetIbisException {
                if (buf != null) {
                        NetReceiveBuffer temp = buf;
                        buf = null;
                        return temp;
                }

                // NetReceiveBuffer b = createReceiveBuffer(expectedLength);

		byte [] b = allocator.allocate();
		int     l = 0;

		try {
			int offset = 0;

                        do {
                                int result = defIs.read(b, offset, 4);
                                if (result == -1) {
                                        if (offset != 0) {
                                                throw new Error("broken pipe");
                                        }
                                        
                                        return null;
                                }
                                
                                if (result == 0) {
                                        return null;
                                }

                                offset += result;
                        } while (offset < 4);

                        l = NetConvert.readInt(b);
                        
			do {
				int result = defIs.read(b, offset, l - offset);
                                if (result == -1) {
                                        throw new Error("broken pipe");
                                }                                
                                if (result == 0) {
                                        return null;
                                }
                                offset += result;
			} while (offset < l);
                } catch (InterruptedIOException e) {
                        return null;
		} catch (IOException e) {
			throw new NetIbisException(e.getMessage());
		} 

		return new NetReceiveBuffer(b, l, allocator);
	}

        public synchronized void close(Integer num) throws NetIbisException {
                if (spn == num) {
                        try {
                                defIs.close();
                        } catch (IOException e) {
                                throw new Error(e);
                        }          
              
                        spn = null;

                        if (upcallThread != null) {
                                upcallThread.end();
                        }
                }
        }
        

	/**
	 * {@inheritDoc}
	 */
	public void free() throws NetIbisException {
                if (defIs != null) {
                        try {
                                defIs.close();
                        } catch (IOException e) {
                                throw new Error(e);
                        }                        
                }

                spn = null;

                if (upcallThread != null) {
                        upcallThread.end();
                        //System.err.println("waiting for DEF upcall thread to join");
                        while (true) {
                                try {
                                        upcallThread.join();
                                        break;
                                } catch (InterruptedException e) {
                                        //
                                }
                        }
                        //System.err.println("DEF upcall thread joined");
                }
                
		super.free();
	}
}
