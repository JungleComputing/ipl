package ibis.impl.net;

import ibis.io.Conversion;
import ibis.ipl.ConnectionTimedOutException;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

/**
 * Provide a TCP connection parallel to an actual application network connection and dedicated for internal use.
 *
 * This service link is supposed to be used by the various drivers that compose the driver stack. It is split into
 * several multiplexed 'sub-streams' that can be allocated dynamically.
 */
public final class NetServiceLink {

	private final static boolean DEBUG = false;

        /**
         * 'End of file' opcode for the commands sent over the main sub-stream.
         *
         * @see ServiceThread#run
         */
        private final static int _OP_eof                  = 0;

        /**
         * 'Request substream id' opcode for the commands sent over the main sub-stream.
         *
         *  This opcode is sent by a node when allocating a new outgoing sub-stream in order to
         *  get the identification value for packets corresponding to thus sub-stream.
         * @see ServiceThread#requestSubstreamId
         * @see ServiceThread#run
         */
        private final static int _OP_request_substream_id = 1;

        /**
         * 'Receive substream id' opcode for the commands sent over the main sub-stream.
         *
         * This opcode is sent along with the answer to the {@link #_OP_request_substream_id 'Request substream id'} request.
         * @see ServiceThread#receiveSubstreamId
         * @see ServiceThread#run
         */
        private final static int _OP_receive_substream_id = 2;

        /**
         * Set to true once the link is closed.
         */
        private volatile boolean closed = false;

        /**
         * Set to true for an 'accept' side {@link NetServiceLink} object, to false for a 'connect' side {@link NetServiceLink} object.
         */
        private boolean      incoming = false;

        /**
         * Reference the TCP socket supporting the service link.
         */
        private Socket       socket   = null;

        /**
         * Reference the outgoing TCP {@link #socket} stream.
         */
        private OutputStream os       = null;

        /**
         * Reference the incoming TCP {@link #socket} stream.
         */
        private InputStream  is       = null;

        /**
         * Reference the main outgoing sub-stream.
         *
         * This sub-stream is used by the {@link NetServiceLink} object to send commands to its peer object.
         */
        private ObjectOutputStream main_oos = null;

        /**
         * Reference the main incoming sub-stream.
         *
         * This sub-stream is used by the {@link NetServiceLink} object to receive commands from its peer object.
         */
        private ObjectInputStream  main_ois = null;

        /**
         * Store the {@linkplain OutputClient output client} info structures.
         *
         * This map is indexed by the name of the sub-streams.
         */
        private HashMap            outputMap    = null;

        /**
         * Store the {@linkplain InputClient input client} info structures.
         *
         * This map is indexed by the name of the sub-streams.
         */
        private HashMap            inputMap     = null;

        /**
         * Store references to the incoming sub-streams.
         *
         * This vector is indexed by the sub-stream ids.
         */
        private Vector             inputVector  = null;

        /**
         * Store the next incoming sub-stream id.
         */
        private volatile int       nextId       =    1;

        /**
         * Store the reference to the thread responsible of listening
         * to the {@link #is incoming socket stream} and to dispatch
         * the packets over the various active input sub-streams.
         */
        private ListenerThread     listenThread = null;

        /**
         * Store the reference to the thread responsible of listening to the
         * main incoming sub-stream in order to process internal commands.
         */
        private ServiceThread      serviceThread     = null;

        /**
         * Control the synchronization on request completion.
         */
        private NetMutex           requestCompletion = new NetMutex(true);

        /**
         * Control the synchronization on request posting.
         */
        private NetMutex           requestReady      = new NetMutex(false);

        /**
         * Store the optional request result.
         *
         * Access to this attribute should be properly synchronized.
         */
        private Object             requestResult     = null;

        /**
         * Reference to the port's event queue used.
         *
         * Used to send events to the port. Currently, the only known
         * event is the 'close' event which indicates that the link
         * (hence the connection) is closed.
         */
        private NetEventQueue      portEventQueue    = null;

        /**
         * Store the network connection id.
         *
         * <BR><B>Note:</B>&nbsp;This attribute is set by the {@link
         * #init} method, not by the constructor.
         */
        private Integer            num               = null;


        /* ___ CONSTRUCTORS ________________________________________________ */

        /**
         * Incoming connection constructor.
         *
         * @param portEventQueue a reference to the port's event queue.
         * @param ss the TCP {@link ServerSocket server socket} to listen to.
         * @exception IOException if the 'accept' syscall over
         * the server socket fails.
         */
        protected NetServiceLink(NetEventQueue portEventQueue, ServerSocket ss) throws IOException {
                this.portEventQueue = portEventQueue;
                incoming = true;

                try {
                        socket = ss.accept();
			if (DEBUG) {
			    System.err.println(this + "@" + NetIbis.hostName() + "/" + Thread.currentThread() + ": " + socket.getLocalAddress() + "/" + socket.getLocalPort() + " accept succeeds from " + socket.getInetAddress() + "/" + socket.getPort());
			}
                } catch (SocketException e) {
                        throw new InterruptedIOException(e);
                } catch (SocketTimeoutException e) {
                        throw new ConnectionTimedOutException(e);
                }
        }

        /**
         * Outgoing connection constructor.
         *
         * @param portEventQueue a reference to the port's event queue.
         * @param nfo a {@link Hashtable table} containing the peer
         * TCP connection info. The peer address should be stored as
         * an {@link InetAddress} under the
         * <code>"accept_address"</code> key and the peer port number
         * should be stored as an {@link Integer} under the
         * <code>"accept_port"</code>.
         */
        protected NetServiceLink(NetEventQueue portEventQueue, Hashtable nfo) throws IOException {
                this.portEventQueue = portEventQueue;
                incoming = false;
                InetAddress raddr =  (InetAddress)nfo.get("accept_address");
		int         rport = ((Integer)    nfo.get("accept_port"   )).intValue();

		socket = NetIbis.socketFactory.createSocket(raddr, rport);
		// Else, I fear the read() would appear high up in the profile:
		socket.setSoTimeout(0);
		if (DEBUG) {
		    System.err.println(this + "@" + NetIbis.hostName() + "/" + Thread.currentThread() + ": " + socket.getLocalAddress() + "/" + socket.getLocalPort() + " Socket - connect to " + raddr + "/" + rport);
		}
        }



	private int threadCount = 0;


        /* ___ CONNECTION MANAGEMENT ROUTINES ______________________________ */

        /**
         * Initialize the service link.
         *
         * @param num the connection id associated to the connection.
         */
        protected synchronized void init(Integer num) throws IOException {
                if (this.num != null) {
                        throw new Error("invalid call");
                }
                this.num = num;

		os = socket.getOutputStream();
		is = socket.getInputStream();

                inputMap  = new HashMap();
                outputMap = new HashMap();

                inputVector = new Vector(1, 1);

                listenThread = new ListenerThread("is = "+is);

                ServiceInputStream  sis = new ServiceInputStream(0);
                InputClient         ic  = new InputClient();
                ic.name = "__main__";
                ic.id  = 0;
                ic.sis = sis;

                inputMap.put("__main__", ic);
                inputVector.setSize(1);
                inputVector.setElementAt(sis, 0);


                ServiceOutputStream sos = new ServiceOutputStream(0);
                OutputClient        oc  = new OutputClient();
                oc.name = "__main__";
                oc.id  = 0;
                oc.sos = sos;

                outputMap.put("__main__", oc);


                listenThread.start();

                serviceThread = new ServiceThread("anonymous-" + (threadCount++));

		if (incoming) {
			main_ois = new ObjectInputStream(sis);
			main_oos = new ObjectOutputStream(sos);
			main_oos.flush();
		} else {
			main_oos = new ObjectOutputStream(sos);
			main_oos.flush();
			main_ois = new ObjectInputStream(sis);
		}


                serviceThread.start();
        }

        /**
         * Close the service link.
         *
         * The service link cannot be reopened once it has been closed.
         * This method can safely be called multiple time.
         */
        public synchronized void close() throws IOException {
                if (closed) {
                        return;
                }

                closed = true;

                if (listenThread != null) {
                        listenThread.end();

                        while (true) {
                                try {
                                        listenThread.join();
                                        break;
                                } catch (InterruptedException e) {
                                        //
                                }
                        }

                        listenThread = null;
                }

                if (serviceThread != null) {
                        serviceThread.end();

                        while (true) {
                                try {
                                        serviceThread.join();
                                        break;
                                } catch (InterruptedException e) {
                                        //
                                }
                        }

                        serviceThread = null;
                }

                synchronized(outputMap) {
                        if (outputMap != null) {
                                Iterator i = outputMap.values().iterator();

                                while (i.hasNext()) {
                                        OutputClient oc = (OutputClient)i.next();

					oc.sos.close();
                                }

                                outputMap.clear();
                                outputMap = null;
                        }
                }


                inputVector = null;

                synchronized(inputMap) {
                        if (inputMap != null) {
                                Iterator i = inputMap.values().iterator();

                                while (i.hasNext()) {
                                        InputClient ic  = (InputClient)i.next();

					ic.sis.close();
                                }


                                inputMap.clear();
                                inputMap = null;
                        }
                }

		os.close();
		is.close();
		socket.close();
        }

        /**
         * Allocate a new output sub-stream.
         *
         * <BR><B>Note:</B>&nbsp;The peer node is not required to synchronously allocate the corresponding input sub-stream.
         * <BR><B>Note 2:</B>&nbsp;Requesting a output sub-stream after it has been closed will <B>not</B> reopen the stream.
         *
         * @param name the name associated to the stream, used to match this with the corresponding input sub-stream on the peer node.
         * @exception IOException when the operation fails.
         */
        protected synchronized OutputStream getOutputSubStream(String name) throws IOException {
                OutputClient oc = null;

                synchronized(outputMap) {
                        oc = (OutputClient)outputMap.get(name);

                        if (oc == null) {
                                oc = new OutputClient();

				synchronized(main_oos) {
					main_oos.writeInt(_OP_request_substream_id);
					main_oos.writeUTF(name);
					main_oos.flush();
				}

                                requestReady.unlock();
                                requestCompletion.lock();
                                oc.name = name;
                                oc.id = ((Integer)requestResult).intValue();

                                oc.sos = new ServiceOutputStream(oc.id);
                                outputMap.put(name, oc);
			}
                }

		if (oc.sos.closed) {
		    System.err.println(this + ": OutputSubStream already closed!!!!!");
		}

                return oc.sos;
        }

        /**
         * Allocate a new input sub-stream.
         *
         * <BR><B>Note:</B>&nbsp;The peer node is not required to
         * synchronously allocate the corresponding output sub-stream.
         *
         * <BR><B>Note 2:</B>&nbsp;Requesting a input sub-stream after it has been
         * closed will <B>not</B> reopen the stream.
         *
         * @param name the name associated to the stream, used to
         * match this with the corresponding output sub-stream on the peer node.
         *
         * @exception IOException when the operation fails.
         */
        protected synchronized InputStream getInputSubStream(String name) throws IOException {
                InputClient ic = null;

                synchronized(inputMap) {
                        ic = (InputClient)inputMap.get(name);

                        if (ic == null) {
                                ic = new InputClient();
                                ic.name = name;
                                ic.id = nextId++;
                                ic.sis = new ServiceInputStream(ic.id);

                                if (ic.id >= inputVector.size()) {
                                        inputVector.setSize(ic.id+1);
                                }

                                inputVector.setElementAt(ic.sis, ic.id);
                                inputMap.put(name, ic);
			}
                }

		if (ic.sis.closed) {
		    // throw new IOException("InputSubStream already closed!!!!!");
		    System.err.println(this + ": InputSubStream already closed!!!!!");
		}

                return ic.sis;
        }

        /**
         * Allocate a new output sub-stream.
         *
         * @see #getOutputSubStream(String)
         *
         * @param io the input or output object requesting the
         * sub-stream.
         *
         * @param name the name associated to the stream, used to
         * match this with the corresponding input sub-stream on the peer node;
         * <B>Note:</B>&nbsp;this name will be prefixed by the {@link
         * NetIO}'s {@linkplain NetIO#context() context string} to
         * provide some kind of dynamic namespace.
         *
         * @exception IOException when the operation fails.
         */
        public OutputStream getOutputSubStream(NetIO io, String name) throws IOException {
                return getOutputSubStream(io.context()+name);
        }

        /**
         * Allocate a new input sub-stream.
         *
         * @see #getInputSubStream(String)
         *
         * @param io the input or output object requesting the
         * sub-stream.
         *
         * @param name the name associated to the stream, used to
         * match this with the corresponding output sub-stream on the peer node;
         * <B>Note:</B>&nbsp;this name will be prefixed by the {@link
         * NetIO}'s {@linkplain NetIO#context() context string} to
         * provide some kind of dynamic namespace.
         *
         * @exception IOException when the operation fails.
         */
        public InputStream getInputSubStream (NetIO io, String name) throws IOException {
                return getInputSubStream(io.context()+name);
        }


	public String toString() {
	    if (socket == null) {
		return "NetServiceLink@" + Integer.toHexString(hashCode()) +
			"-unconnected";
	    } else {
		return "NetServiceLink@" + Integer.toHexString(hashCode()) +
			"-my_addr=" + socket.getLocalAddress() +
			":" + socket.getLocalPort() +
			"-rem_addr=" + socket.getInetAddress() +
			":" + socket.getPort();
	    }
	}



        /* ___ INTERNAL CLASSES ____________________________________________ */

        /**
         * Store some information about an outgoing sub-stream.
         */
        private final static class OutputClient {

                /**
                 * The 'name' of the output sub-stream.
                 *
                 * This 'name' must match the name of the peer input sub-stream.
                 */
                String              name = null;

                /**
                 * The packet identificator.
                 */
                int                 id   = 0;

                /**
                 * The output sub-stream.
                 */
                ServiceOutputStream sos  = null;
        }



        /**
         * Store some information about an incoming sub-stream.
         */
        private final static class InputClient {

                /**
                 * The 'name' of the input sub-stream.
                 *
                 * This 'name' must match the name of the peer output sub-stream.
                 */
                String             name = null;

                /**
                 * The packet identificateor.
                 */
                int                id   = 0;

                /**
                 * The input sub-stream.
                 */
                ServiceInputStream sis = null;
        }



        /**
         * Provide a multiplexed output sub-stream over the socket output stream.
         */
        private final class ServiceOutputStream extends OutputStream  {

                /**
                 * Set to true once the stream is closed.
                 *
                 * The stream cannot be re-opened after having been closed (same semantics as a socket stream).
                 */
                private boolean   closed    = false;

                /**
                 * Store the packet identifier.
                 */
                private int       id        = -1;

                /**
                 * Indicate the length of the buffer.
                 */
                private final int length    = 65536;

                /**
                 * Store the current bufferized bytes.
                 */
                private byte []   buffer    = new byte[length];

                /**
                 * Store the current offset in the buffer.
                 */
                private int       offset    = 0;

                /**
                 * Permanent 4-byte buffer for 'buffer-length to bytes' conversion.
                 */
                private byte []   intBuffer = new byte[4];


                /**
                 * Write a byte block to the sub-stream.
                 *
                 * Only this method actually access the sub-stream.
                 *
                 * @param b the byte block.
                 * @param o the offset in the block of the first byte to write.
                 * @param l the number of bytes to write.
                 * @exception IOtion when the write operation to the {@link #os socket stream} fails.
                 */
                private void writeBlock(byte[] b, int o, int l) throws IOException {
                        synchronized(os) {
                                Conversion.defaultConversion.int2byte(l, intBuffer, 0);
                                os.write(id);
                                os.write(intBuffer);
                                os.write(b, o, l);
                        }
                }


                /**
                 * Construct an outgoing sub-stream.
                 *
                 * The {@link #id} value must be unique among this
                 * service link outgoing sub-streams.
                 *
                 * @param id the sub-stream packets id.
                 */
                public ServiceOutputStream(int id) {
                        this.id = id;
                }

                /**
                 * Flush the {@link #buffer} to the stream if the
                 * current {@link #offset} is not <code>0</code>.
                 *
                 * @exception IOException when the flush operation fails.
                 */
                private void doFlush() throws IOException {
                        if (offset > 0) {
                                writeBlock(buffer, 0, offset);
                                offset = 0;
                        }
                }

                /**
                 * {@inheritDoc}
                 */
                public void close() throws IOException {
                        closed = true;
                        doFlush();
                }

                /**
                 * {@inheritDoc}
                 */
                public void flush() throws IOException {
                        if (closed) {
                                throw new IOException("stream closed");
                        }
                        doFlush();
                }

                /**
                 * {@inheritDoc}
                 */
                public void write(byte[] buf) throws IOException {
                        if (closed) {
                                throw new IOException("stream closed");
                        }

                        write(buf, 0, buf.length);
                }

                /**
                 * {@inheritDoc}
                 */
                public void write(byte[] buf, int off, int len) throws IOException {
                        if (closed) {
                                throw new IOException("stream closed");
                        }

                        if (len <= length-offset) {
                                System.arraycopy(buf, off, buffer, offset, len);
                                offset += len;

                                if (offset == length) {
                                        flush();
                                }
                        } else {
                                doFlush();
                                writeBlock(buf, off, len);
                        }
                }

                /**
                 * {@inheritDoc}
                 */
                public void write(int val) throws IOException {
                        if (closed) {
                                throw new IOException("stream closed");
                        }

                        buffer[offset++] = (byte)(val & 0xFF);

                        if (offset == length) {
                                doFlush();
                        }
                }

        }



        /**
         * Provide a multiplexed input sub-stream over the socket input stream.
         */
        private final static class ServiceInputStream extends InputStream {

                /**
                 * Provide a double-linked list of incoming buffers.
                 */
                private static class BufferList {

                        /**
                         * Reference the next buffer list element.
                         */
                        BufferList    next     = null;

                        /**
                         * Reference the list element's buffer.
                         */
                        byte       [] buf      = null;
                }

                /**
                 * Reference the buffer list head.
                 */
                private BufferList first  = null;

                /**
                 * Reference the buffer list tail.
                 */
                private BufferList last   = null;

                /**
                 * Set to true once the stream is closed.
                 *
                 * The stream cannot be re-opened after having been closed (same semantics as a socket stream).
                 */
                private volatile boolean closed = false;

                /**
                 * Store the packet identifier.
                 */
                private int id     = -1;

                /**
                 * Store the total number of bytes available in the
                 * incoming buffer list.
                 */
                private int avail  =  0;

                /**
                 * Store the reading offset in the current buffer.
                 */
                private int offset =  0;

                /**
                 * Construct an incoming sub-stream.
                 *
                 * The {@link #id} value must be unique among this
                 * service link incoming sub-streams.
                 *
                 * @param id the sub-stream packets id.
                 */
                public ServiceInputStream(int id) {
                        this.id = id;
                }

                /**
                 * Called by the {@link #listenThread} to add a block of bytes
                 * to the incoming buffer list.
                 *
                 * @param b the byte block to add to the list.
                 */
                protected synchronized void addBuffer(byte [] b) {
                        BufferList bl = new BufferList();
                        bl.buf = b;

                        if (first == null) {
                                first = bl;
                        } else {
                                last.next   = bl;
                        }
			last = bl;

                        avail += bl.buf.length;

                        notifyAll();
                }

                /**
                 * Return the number of bytes immediately availables.
                 *
                 * The value returned is the value of the {@link
                 * #avail} attribute.
                 *
                 * @return the number of bytes immediately availables.
                 * @exception IOException to conform to the {@link
                 * InputStream} definition.
                 */
                public synchronized int available() throws IOException {
                        return avail;
                }

                /**
                 * Close the stream.
                 *
                 * The stream cannot be re-opened afterwards. Any
                 * unread data is lost.
                 */
                public synchronized void close() throws IOException {
                        closed = true;
                        notifyAll();
                }

                /**
                 * Return false.
                 */
                public boolean markSupported() {
                        return false;
                }

                /**
                 * Switch to the next block in the incoming buffer list.
                 */
                private void nextBlock() {
                        offset = 0;
                        if (first.next == null) {
                                first = null;
                                last  = null;
                        } else {
                                BufferList temp = first.next;
                                first.next = null;
                                first = temp;
                        }
                }

                /**
                 * {@inheritDoc}
                 */
                public synchronized int read() throws IOException {
                        if (closed && avail == 0) {
                                throw new EOFException("stream closed");
                        }

                        int result = 0;

                        if (avail == 0) {
                                try {
                                        wait();
                                        if (closed) {
                                                return -1;
                                        }
                                } catch (InterruptedException e) {
                                        throw new InterruptedIOException(e);
                                }
                        }

                        if (avail > 0) {
                                result = 0xFF & (int)first.buf[offset++];
                                avail--;
                        }

                        if (offset == first.buf.length) {
                                nextBlock();
                        }

                        return result;
                }

                /**
                 * {@inheritDoc}
                 */
                public int read(byte[] b) throws IOException {
                        return read(b, 0, b.length);
                }

                /**
                 * {@inheritDoc}
                 */
                public synchronized int read(byte[] buf, int off, int len) throws IOException {
                        if (closed && avail == 0) {
                                throw new EOFException("stream closed");
                        }

                        int result = 0;

                        while (len > 0) {
                                if (avail == 0) {
                                        if (closed) {
                                                break;
                                        }

                                        try {
                                                wait();
                                                if (closed) {
                                                        break;
                                                }
                                        } catch (InterruptedException e) {
                                                throw new InterruptedIOException(e);
                                        }
                                }

                                int copylength = Math.min(len, first.buf.length - offset);
                                System.arraycopy(first.buf, offset, buf, off, copylength);
                                result += copylength;
                                offset += copylength;
                                off    += copylength;
                                len    -= copylength;
                                avail  -= copylength;

                                if (offset == first.buf.length) {
                                        nextBlock();
                                }
                        }

                        return result;
                }
        }





        /* ..... LISTENER THREAD ___________________________________________ */

        /**
         * Provide a thread responsible of listening to a incoming
         * multiplexed byte stream an to dispatch incoming packets to
         * their corresponding incoming sub-streams.
         */
        private final class ListenerThread extends Thread {

                /**
                 * If set to true, the end of the thread has been requested.
                 */
                volatile boolean exit = false;

                /**
                 * Provide a buffer dedicated to integer reception.
                 */
                private byte[]  intBuffer = new byte[4];

                /**
                 * Constructor.
                 *
                 * @param name the name of the thread, used mainly for
                 * debugging purpose.
                 */
                ListenerThread(String name) {
                        super("ListenerThread: "+name);
                }

                /**
                 * Listen to the {@link #is incoming socket stream}
                 * and dispatch the incoming packets to their
                 * corresponding input sub-streams according to their
                 * id.
                 */
                public void run() {
                main_loop:
                        while (!exit) {

                                try {
                                        int id  = is.read();

                                        if (id == -1) {
                                                exit = true;
						portEventQueue.put(new NetPortEvent(NetServiceLink.this, NetPortEvent.CLOSE_EVENT, num));
                                                continue;
                                        }

                                        ServiceInputStream sis = null;

                                        synchronized(inputMap) {
                                                sis = (ServiceInputStream)inputVector.elementAt(id);
                                        }

                                        if (sis == null) {
                                                throw new Error("invalid id");
                                        }

                                        is.read(intBuffer);
                                        byte [] b = new byte[Conversion.defaultConversion.byte2int(intBuffer, 0)];
                                        is.read(b);

                                        sis.addBuffer(b);
                                } catch (SocketException e) {
                                        exit = true;
                                        portEventQueue.put(new NetPortEvent(NetServiceLink.this, NetPortEvent.CLOSE_EVENT, num));
                                        continue;
                                } catch (EOFException e) {
                                        exit = true;
                                        continue;
                                } catch (InterruptedIOException e) {
System.err.println(this + ": NetServiceLink catches " + e);
                                        continue;
                                } catch (Exception e) {
                                        throw new Error(e);
                                }
                        }
                }

                /**
                 * Set the {@link #exit} flag to true and close the
                 * {@linkplain #is incoming socket stream}.
                 *
                 * @exception IOException it the close operation fails.
                 */
                protected void end() throws IOException {
                        exit = true;
			is.close();
                }
        }

        /**
         * Provide a thread processing commands received over the {@linkplain #main_ois main input sub-stream}.
         */
        private final class ServiceThread extends Thread {

                /**
                 * If set to true, the end of the thread has been requested.
                 */
                volatile boolean exit = false;

                /**
                 * Constructor.
                 *
                 * @param name the name of the thread, used mainly for
                 * debugging purpose.
                 */
                ServiceThread(String name) {
                        super("ServiceThread: "+name);
                }

                /**
                 * Set the {@link #exit} flag to true and close the
                 * {@linkplain #main_oos main output sub-stream} and the
                 * {@linkplain #main_ois main input sub-stream}.
                 *
                 * @exception IOException when the operation fails.
                 */
                protected void end() throws IOException {
                        exit = true;
			main_oos.close();
			main_ois.close();
                }

                /**
                 * Process a request for a sub-stream id.
                 *
                 * Such a request is supposed to be send by the node
                 * that is creating the output sub-stream end of the
                 * sub-stream and is processed by the node that owns
                 * the input sub-stream part of the stream. If the
                 * input sub-stream corresponding
                 * to the <code>name</code> parameter does not already exist,
                 * it is created on the fly.
                 *
                 * @param name the name of the sub-stream.
                 * @see #_OP_request_substream_id
                 */
                private void requestSubstreamId(String name) {

                        synchronized(inputMap) {
                                InputClient ic = (InputClient)inputMap.get(name);

                                if (ic == null) {
                                        ic = new InputClient();
                                        ic.name = name;
                                        ic.id = nextId++;
                                        ic.sis = new ServiceInputStream(ic.id);
                                        if (ic.id >= inputVector.size()) {
                                                inputVector.setSize(ic.id+1);
                                        }

                                        inputVector.setElementAt(ic.sis, ic.id);
                                        inputMap.put(name, ic);
                                }

                                synchronized(main_oos) {
                                        try {
                                                main_oos.writeInt(_OP_receive_substream_id);
                                                main_oos.writeObject(new Integer(ic.id));
                                                main_oos.flush();
                                        } catch (IOException e) {
                                                throw new Error(e.getMessage());
                                        }
                                }
                        }
                }

                /**
                 * Process an answer to the {@linkplain
                 * #requestSubstreamId sub-stream id request}.
                 *
                 * @param id the sub-stream id.
                 * @see #_OP_receive_substream_id
                 */
                private void receiveSubstreamId(Integer id) {
                        try {
                                requestReady.lock();
                        } catch (InterruptedIOException e) {
                                return;
                        }

                        requestResult = id;
                        requestCompletion.unlock();
                }

                /**
                 * Read commands from the {@linkplain #main_ois main object input sub-stream} and dispatch those commands to the right functions.
                 *
                 * <BR><B>Note:</B>&nbsp;With the exception of the
                 * '{@link #_OP_eof End of file}', each command is
                 * currently run asynchronously on a separate thread
                 * to avoid deadlocks. <BR><B>Note: 2</B>&nbsp;Only
                 * this thread is allowed to read from the {@linkplain
                 * #main_ois main object input sub-stream}, and in
                 * particular, the methods implementing the various
                 * commands should not attempt to extract any data
                 * from this sub-stream.
                 */
                public void run() {
                        while (!exit) {
                                try {
                                        int op = main_ois.readInt();

                                        switch (op) {

                                        case _OP_eof:
                                                {
                                                        exit = true;
                                                        close();
                                                }
                                                break;

                                        case _OP_request_substream_id:
                                                {
                                                        final String name = main_ois.readUTF();
                                                        Runnable r = new Runnable() {public void run() {requestSubstreamId(name);}};
                                                        (new Thread(r, "request_substream_id")).start();
                                                }
                                                break;

                                        case _OP_receive_substream_id:
                                                {
                                                        final Integer id = (Integer)main_ois.readObject();
                                                        Runnable r = new Runnable() {public void run() {receiveSubstreamId(id);}};
                                                        (new Thread(r, "receive_substream_id")).start();
                                                }
                                                break;

                                        default:
                                                {
                                                        throw new Error("invalid operation");
                                                }
                                        }
                                } catch (InterruptedIOException e) {
                                        exit = true;
                                        continue;
                                } catch (EOFException e) {
                                        exit = true;
                                        continue;
                                } catch (Exception e) {
                                        throw new Error(e);
                                }
                        }
                }
        }
}
