package ibis.ipl.impl.net.udp;

import ibis.ipl.impl.net.*;

import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.SocketException;

/* Only for java >= 1.4
 * import java.net.SocketTimeoutException;
 */
import java.io.InterruptedIOException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import java.util.Hashtable;


public final class UdpInput extends NetBufferedInput {

        /**
         * The default polling timeout in milliseconds.
         *
         * <BR><B>Note</B>: this will be replaced by a property setting in the future.
         */
        private final int             defaultPollTimeout    = 1000; // 1; // milliseconds

        /**
         * The default reception timeout in milliseconds.
         *
         * <BR><B>Note</B>: this will be replaced by a property setting in the future.
         */
        private final int             defaultReceiveTimeout = 1000; // milliseconds

        /**
         * The polling timeout in milliseconds.
         *
         * <BR><B>Note</B>: this will be replaced by a property setting in the future.
         */
        private int                   pollTimeout    = defaultPollTimeout; // milliseconds

        /**
         * The reception timeout in milliseconds.
         *
         * <BR><B>Note</B>: this will be replaced by a property setting in the future.
         */
        private int                   receiveTimeout = defaultReceiveTimeout; // milliseconds

        private DatagramSocket        socket         = null;
        private DatagramPacket        packet         = null;
        private Driver                driver         = null;
        private InetAddress           laddr          = null;
        private int                   lport          =    0;
        private int                   lmtu           =    0;
        private InetAddress           raddr          = null;
        private int                   rport          =    0;
        private int                   rmtu           =    0;
        private NetReceiveBuffer      buffer         = null;
        private volatile Integer      spn            = null;
        private int                   socketTimeout  =    0;

        private long            rcve_seqno;     /* For out-of-order debugging */
        private long            deliver_seqno;  /* For out-of-order debugging */
        private NetUDPStat            udpStat        = null;

        UdpInput(NetPortType pt, NetDriver driver, String context)
                throws IOException {
                super(pt, driver, context);
                udpStat = (NetUDPStat)stat;

                if (Driver.DEBUG) {
                        headerLength = 8;
                }

                System.err.println(this + ": constructor; upcallFunc " + upcallFunc);
                Thread.dumpStack();                
        }

        /*
          To remove
        private final class UpcallThread extends Thread {

                private volatile boolean end = false;

                public UpcallThread(String name) {
                        super("UdpInput.UpcallThread: "+name);
                }

                public void end() {
                        log.in();
                        end = true;
                        this.interrupt();
                        log.out();
                }

                public void run() {
                        log.in();
                        while (!end) {
                                if (buffer != null) {
                                        throw new Error("invalid state");
                                }

                                try {
                                        buffer = createReceiveBuffer(0);
                                        packet.setData(buffer.data, 0, buffer.data.length);

                                        setReceiveTimeout(0);
                                        socket.receive(packet);
                                        stat.begin();
                                        buffer.length = packet.getLength();
                                        initReceive(spn);
                                        upcallFunc.inputUpcall(UdpInput.this, activeNum);
                                        stat.end();
                                } catch (InterruptedIOException e) {
                                        // Nothing
                                } catch (Exception e) {
                                        throw new Error(e);
                                }
                        }
                        log.out();
                }
        }
        */

        public synchronized void setupConnection(NetConnection cnx) throws IOException {
                log.in();
                if (spn != null) {
                        throw new Error("connection already established");
                }
		if (Driver.DEBUG) {
		    System.err.println(this + ": setupConnection over " + cnx);
		}

		socket = new DatagramSocket(0, InetAddress.getLocalHost());
		lmtu = Math.min(socket.getReceiveBufferSize(), 16384);
		laddr = socket.getLocalAddress();
		lport = socket.getLocalPort();

                Hashtable lInfo = new Hashtable();
                lInfo.put("udp_address", laddr);
                lInfo.put("udp_port",    new Integer(lport));
                lInfo.put("udp_mtu",     new Integer(lmtu));
                Hashtable rInfo = null;

		ObjectOutputStream os = new ObjectOutputStream(cnx.getServiceLink().getOutputSubStream(this, "udp-request"));
		os.writeObject(lInfo);
		os.flush();
// System.err.println(this + ": setupConnection, now receive");

		ObjectInputStream is = new ObjectInputStream(cnx.getServiceLink().getInputSubStream(this, "udp-reply"));
                try {
                        rInfo = (Hashtable)is.readObject();
                } catch (ClassNotFoundException e) {
                        throw new Error(e);
                }
		is.close();

		raddr =  (InetAddress)rInfo.get("udp_address");
		rport = ((Integer)    rInfo.get("udp_port")  ).intValue();
		rmtu  = ((Integer)    rInfo.get("udp_mtu")   ).intValue();

		mtu       = Math.min(lmtu, rmtu);
		if (factory == null) {
		    factory = new NetBufferFactory(mtu, new NetReceiveBufferFactoryDefaultImpl());
		} else {
		    factory.setMaximumTransferUnit(mtu);
		}

		packet    = new DatagramPacket(new byte[mtu], mtu);

		String s = null;
		if ((s = getProperty("ReceiveTimeout")) != null) {
			receiveTimeout = Integer.valueOf(s).intValue();
		}
		if ((s = getProperty("PollingTimeout")) != null) {
			pollTimeout = Integer.valueOf(s).intValue();
		}

		setReceiveTimeout(receiveTimeout);

		/* Now that we have set up all data structures for
		 * receiving, let the sender go */

		os.write(1);
		os.close();
		if (Driver.DEBUG) {
		    System.err.println(this + ": setupConnection over " + cnx + "; finished by sending OK byte");
		}

		spn = cnx.getNum();
		startUpcallThread();

		log.out();
    }


        private void checkReceiveSeqno(NetReceiveBuffer buffer) {
                log.in();
                if (Driver.DEBUG) {
                        long seqno = NetConvert.readLong(buffer.data, 0);
                        if (seqno < rcve_seqno) {
                                System.err.println("WHHHHHHHHHOOOOOOOOOOOAAAAAA UDP Receive: packet overtakes: " + seqno + " expect " + rcve_seqno);
                        } else {
                                rcve_seqno = seqno;
                        }
                }
                log.out();
        }


        private void checkDeliverSeqno(NetReceiveBuffer buffer) {
                log.in();
                if (Driver.DEBUG) {
                        long seqno = NetConvert.readLong(buffer.data, 0);
                        if (seqno < deliver_seqno) {
                                System.err.println("WHHHHHHHHHOOOOOOOOOOOAAAAAA UDP Deliver: packet overtakes: " + seqno + " expect " + deliver_seqno);
                        } else {
                                deliver_seqno = seqno;
                        }
                }
                log.out();
        }


        /**
         * {@inheritDoc}
         *
         * <BR><B>Note</B>: This UDP polling implementation uses a timed out
         * {@link DatagramSocket#receive(DatagramPacket)}. As the minimum timeout value is one
         * millisecond, an unsuccessful polling operation is rather expensive.
         *
         * @return {@inheritDoc}
         */
        public Integer doPoll(boolean block) throws IOException {
                log.in();

                if (buffer != null) {
                        throw new IOException(this + ": call finish before you call poll() again");
		}

                Integer result = poll(block ? 0 : pollTimeout);
                log.out();
                // return poll(pollTimeout);
                return result;
        }

        protected void initReceive(Integer num) throws IOException {
                super.initReceive(num);
                checkReceiveSeqno(buffer);
        }

        private Integer poll(int timeout) throws IOException {
                Integer result = null;
                
                log.in();
                if (spn == null) {
                        log.out("unconnected");
                        return null;
                }
// System.err.print("z");

		if (buffer != null) {
		    throw new IOException(this + ": illegal state to call poll()");
		}

		buffer = createReceiveBuffer(0);
// System.err.println(this + ": poll creates buffer " + buffer);
// Thread.dumpStack();
		packet.setData(buffer.data, 0, buffer.data.length);
		setReceiveTimeout(timeout);
		udpStat.beginPoll();
		try {
			socket.receive(packet);
			buffer.length = packet.getLength();
// System.err.println(this + ": poll Receive UDP packet len " + packet.getLength() + " buffer " + buffer);
			result = spn;
			checkReceiveSeqno(buffer);
// System.err.print("^");
		} catch (InterruptedIOException e) {
			buffer.free();
			buffer = null;
			if (timeout == 0) {
				throw e;
			} else {
				udpStat.endPoll();
			}
// System.err.print("%");
		} catch (IOException e) {
// System.err.print("!");
			buffer.free();
			buffer = null;
			throw e;
		}

                stat.begin();
                log.out();

// System.err.println(this + ": poll returns " + result);

                return result;
        }


        /**
         * {@inheritDoc}
         *
         * <BR><B>Note</B>: this function may block if the expected data is not there.
         * <BR><B>Note</B>: The expectedLength argument is simply ignored because the
         * packet actually received might not be the one that is expected.
         *
         * @return {@inheritDoc}
         */
        public NetReceiveBuffer receiveByteBuffer(int expectedLength) throws IOException {
                log.in();
                while (buffer == null) {
// System.err.print("Z");
                        poll(0);
                }

// System.err.println(this + ": hi -- spn " + spn + " buffer " + buffer);
// Thread.dumpStack();
// System.err.print("_");
                if (this.buffer == null) {
                        throw new IOException(this + ": receive corrupt: poll is nonnull but no buffer");
                }

                NetReceiveBuffer temp_buffer = buffer;
                buffer = null;

                checkDeliverSeqno(temp_buffer);
                stat.addBuffer(temp_buffer.length);
                log.out();

                return temp_buffer;
        }


	public void receiveByteBuffer(NetReceiveBuffer userBuffer)
		throws IOException {
                log.in();
                if (buffer == null) {
// System.err.println(this + ": set packet length " + userBuffer.length + " base " + userBuffer.base);
                        packet.setData(userBuffer.data, userBuffer.base, userBuffer.length - userBuffer.base);

			/* This is a blocking receive call. Don't
			 * set a timeout. */
// System.err.print("p");
			setReceiveTimeout(0);
			socket.receive(packet);

			// ???
			// super.initReceive();

			checkReceiveSeqno(buffer);
// System.err.println(this + ": Receive downcall UDP packet len " + packet.getLength());
                } else {
// System.err.print("-");
// System.err.println(this + ": Fill pre-received UDP packet len " + buffer.length);
                        System.arraycopy(buffer.data, 0, userBuffer.data, userBuffer.base, userBuffer.length - userBuffer.base);
                        buffer.free();
                        buffer = null;
                }

                checkDeliverSeqno(userBuffer);
                stat.addBuffer(userBuffer.length);
                log.out();
        }

        /**
         * {@inheritDoc}
         */
        public void doFinish() throws IOException {
                log.out();
                buffer = null;
                stat.end();
                log.out();
        }

        /*
         * We need a way to set timeout through properties
         */
        // timeout should be expressed in milliseconds
        void setReceiveTimeout(int timeout) throws IOException {
                log.in();
                if (timeout != socketTimeout) {
			socket.setSoTimeout(timeout);
			socketTimeout = timeout;
                }
                log.out();
        }

        // returns the current reception timeout in milliseconds
        // 0 means an infinite timeout
        int getReceiveTimeout() throws IOException {
                log.in();
                int t = 0;

		t = socket.getSoTimeout();
                log.out();

                return t;
        }

        public synchronized void doClose(Integer num) throws IOException {
                log.in();
                if (spn == num) {
                        if (socket != null) {
                                socket.close();
                        }

                        spn = null;
                }
                log.out();
        }


        /**
         * {@inheritDoc}
         */
        public void doFree() throws IOException {
                log.in();
                if (spn != null) {
                        close(spn);
                }
                log.out();
        }

        protected NetMessageStat newMessageStat(boolean on, String moduleName) {
                return new NetUDPStat(on, moduleName);
        }


        public final class NetUDPStat extends NetMessageStat {
                private int  receiveFromPoll   = 0;
                private long t_receiveFromPoll = 0;
                private long start             = 0;
                public NetUDPStat(boolean on, String moduleName) {
                        super(on, moduleName);
                }

                public NetUDPStat(boolean on) {
                        this(on, "");
                }

                public void beginPoll() {
                        if (on) {
                                start = System.currentTimeMillis();
                        }
                }

                public void endPoll() {
                        if (on) {
                                receiveFromPoll++;
                                t_receiveFromPoll += System.currentTimeMillis() - start;
                        }
                }

                public void report() {
                        if (on) {
                                System.err.println();
                                System.err.println("Polling stats for module "+moduleName);
                                System.err.println("------------------------------------");
                                System.err.println("receiveFromPoll(timeout) " + receiveFromPoll); 
                                System.err.println("estimated loss "           + (t_receiveFromPoll / 1000.0) + " s");
                        }
                }
        }
}
