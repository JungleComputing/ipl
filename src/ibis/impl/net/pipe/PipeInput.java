package ibis.ipl.impl.net.pipe;

import ibis.ipl.impl.net.*;

import ibis.ipl.IbisIOException;

import java.io.InterruptedIOException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PipedInputStream;

import java.util.Hashtable;

public final class PipeInput extends NetBufferedInput {
	private int              defaultMtu   = 512;
	private Integer          spn          = null;
	private PipedInputStream pipeIs       = null;
	private NetAllocator     allocator    = null;
        private NetReceiveBuffer buf          = null;
        private boolean          upcallMode   = false;
        private UpcallThread     upcallThread = null;
        private NetMutex         upcallEnd    = new NetMutex(true);

	PipeInput(NetPortType pt, NetDriver driver, NetIO up, String context) throws IbisIOException {
		super(pt, driver, up, context);
		headerLength = 4;
	}

        private final class UpcallThread extends Thread {
                
                public UpcallThread(String name) {
                        super("PipeInput.UpcallThread: "+name);
                }
                
                public void run() {
                        while (true) {
                                try {
                                        buf = receiveByteBuffer(0);
                                        if (buf == null)
                                                break;
                                        
                                        activeNum = spn;
                                        initReceive();
                                        upcallFunc.inputUpcall(PipeInput.this, activeNum);
                                        activeNum = null;
                                } catch (Exception e) {
                                        throw new Error(e);
                                }
                        }

                        upcallEnd.unlock();
                }
        }

	public void setupConnection(Integer spn, ObjectInputStream is, ObjectOutputStream os, NetServiceListener nls) throws IbisIOException {
                if (this.spn != null) {
                        throw new Error("connection already established");
                }
                
		this.spn = spn;
		mtu      = defaultMtu;
		 
                if (upcallFunc != null) {
                        upcallMode = true;
                }
		try {
			pipeIs = new PipedInputStream();
			
			NetBank bank = driver.getIbis().getBank();
			Long    key  = bank.getUniqueKey();
			bank.put(key, pipeIs);
			
			Hashtable info = new Hashtable();
			info.put("pipe_mtu",         new Integer(mtu));
			info.put("pipe_istream_key", key);
			info.put("pipe_upcall_mode", new Boolean(upcallMode));
			sendInfoTable(os, info);
			int ack = is.read();
		} catch (IOException e) {
			throw new IbisIOException(e);
		}

		allocator = new NetAllocator(mtu);
                if (upcallFunc != null) {
                        (upcallThread = new UpcallThread("this = "+this)).start();
                }
	}

	public Integer poll() throws IbisIOException {
		activeNum = null;

		if (spn == null) {
			return null;
		}

		try {
			if (pipeIs.available() > 0) {
				activeNum = spn;
                                initReceive();
			}
		} catch (IOException e) {
			throw new IbisIOException(e);
		} 

		return activeNum;
	}	

	public NetReceiveBuffer receiveByteBuffer(int expectedLength)
		throws IbisIOException {
                if (buf != null) {
                        NetReceiveBuffer temp = buf;
                        buf = null;
                        return temp;
                }
                
                final boolean upcallMode = this.upcallMode;
		byte [] b = allocator.allocate();
		int     l = 0;
		
		try {
			int offset = 0;
			do {
                                if (!upcallMode) {
                                        while (pipeIs.available() < 4) {
                                                Thread.currentThread().yield();
                                        }
                                }
				
                                int result = pipeIs.read(b, offset, 4);
                                if (result == -1) {
                                        if (offset != 0) {
                                                throw new Error("broken pipe");
                                        }
                                        
                                        return null;
                                }
                                
				offset += result;
			} while (offset < 4);

			l = NetConvert.readInt(b, 0);
			
			do {
                                if (!upcallMode) {
                                        while (pipeIs.available() < (l - offset)) {
                                                Thread.currentThread().yield();
                                        }
                                }
                                
				
				int result = pipeIs.read(b, offset, l - offset);
                                if (result == -1) {
                                        throw new Error("broken pipe");
                                }                                
                                offset += result;
			} while (offset < l);
		} catch (Exception e) {
			throw new IbisIOException(e);
		}
		
		return new NetReceiveBuffer(b, l, allocator);
	}

	public void free() throws IbisIOException {
		try {
			if (pipeIs != null) {
				pipeIs.close();
			}
                        if (upcallThread != null) {
                                upcallThread.interrupt();
                                upcallEnd.lock();
                                upcallThread = null;
                        }
		}
		catch (Exception e) {
			throw new IbisIOException(e);
		}

		super.free();
	}
}
