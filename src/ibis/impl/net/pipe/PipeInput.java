package ibis.ipl.impl.net.pipe;

import ibis.ipl.impl.net.*;

import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PipedInputStream;

import java.util.Hashtable;

public final class PipeInput extends NetBufferedInput {
	private int              defaultMtu   = 512;
	private volatile Integer spn          = null;
	private PipedInputStream pipeIs       = null;
        private NetReceiveBuffer buf          = null;
        private boolean          upcallMode   = false;

	PipeInput(NetPortType pt, NetDriver driver, String context) throws NetIbisException {
		super(pt, driver, context);
		headerLength = 4;
		// Create the factory in the constructor. This allows
		// subclasses to override the factory.
		factory = new NetBufferFactory(new NetReceiveBufferFactoryDefaultImpl());
	}

	public synchronized void setupConnection(NetConnection cnx) throws NetIbisException {
                log.in();
                if (this.spn != null) {
                        throw new Error("connection already established");
                }

		mtu = defaultMtu;

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

			ObjectOutputStream os = new ObjectOutputStream(cnx.getServiceLink().getOutputSubStream(this, "pipe"));
			os.writeObject(info);
                        InputStream is = cnx.getServiceLink().getInputSubStream(this, "pipe");
			int ack = is.read();
                        os.close();
                        is.close();
		} catch (IOException e) {
			throw new NetIbisException(e);
		}

		// Don't create a new factory here, just specify the mtu.
		// Possibly a subclass overrode the factory, and we must leave
		// that factory in place.
		factory.setMaximumTransferUnit(mtu);

		this.spn = cnx.getNum();
                startUpcallThread();
                log.out();
	}

        protected void initReceive(Integer num) {
                //
        }

	public Integer doPoll(boolean block) throws NetIbisException {
                log.in();
		if (spn == null) {
                        log.out("not connected");
			return null;
		}

		try {
                        if (block) {
                                buf = receiveByteBuffer(1);
                                return spn;
                        } else if (pipeIs.available() > 0) {
                                return spn;
			}
		} catch (IOException e) {
			throw new NetIbisException(e);
		}
                log.out();

		return null;
	}

	public NetReceiveBuffer receiveByteBuffer(int expectedLength) throws NetIbisException {
                log.in();
                if (buf != null) {
                        NetReceiveBuffer temp = buf;
                        buf = null;
                        return temp;
                }

                final boolean upcallMode = this.upcallMode;
		NetReceiveBuffer buf = createReceiveBuffer(0);
		byte [] b = buf.data;
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

                                        log.out("connection lost");
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
			throw new NetIbisException(e);
		}

		buf.length = l;
                log.out();

		return buf;
	}

        public synchronized void doClose(Integer num) throws NetIbisException {
                log.in();
                if (spn == num) {
                        try {
                                if (pipeIs != null) {
                                        pipeIs.close();
                                }
                        }
                        catch (Exception e) {
                                throw new NetIbisException(e);
                        }

                        spn = null;
                }
                log.out();
        }

	public synchronized void doFree() throws NetIbisException {
                log.in();
		try {
			if (pipeIs != null) {
				pipeIs.close();
			}
		}
		catch (Exception e) {
			throw new NetIbisException(e);
		}

                spn = null;
                log.out();
	}
}
