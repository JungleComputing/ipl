package ibis.impl.net;

import ibis.ipl.DynamicProperties;
import ibis.ipl.ReceivePortIdentifier;
import ibis.ipl.SendPort;
import ibis.ipl.SendPortConnectUpcall;
import ibis.ipl.SendPortIdentifier;
import ibis.ipl.WriteMessage;
import ibis.ipl.ConnectionTimedOutException;
import ibis.ipl.Replacer;

import java.io.ObjectInputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

import java.util.Iterator;
import java.util.Hashtable;
import java.util.Vector;

/**
 * Provides an implementation of the {@link SendPort} and {@link
 * WriteMessage} interfaces of the IPL.
 */
public final class NetSendPort implements SendPort, WriteMessage, NetPort, NetEventQueueConsumer {





        /* ___ LESS-IMPORTANT OBJECTS ______________________________________ */

        /**
         * The {@link NetIbis} instance.
         */
        private NetIbis               ibis                   = null;

	/**
	 * The port settings.
	 */
	private NetPortType           type       	     = null;

	/**
	 * The port name used for <I>name server</I> lookup requests.
	 */
	private String                name       	     = null;

	/**
	 * The port connection identifier.
	 */
	private NetSendPortIdentifier identifier 	     = null;

        /**
         * Replacer ???.
         */
        private Replacer              replacer               = null;

        /**
         * The next connection identification number.
         */
	private int       	      nextReceivePortNum     = 0;

        /**
         * Optional (fine grained) logging object.
         *
         * This logging object should be used to display code-level information
         * like function calls, args and variable values.
         */
        protected NetLog           log                    = null;

        /**
         * Optional (coarse grained) logging object.
         *
         * This logging object should be used to display concept-level information
         * about high-level algorithmic steps (e.g. message send, new connection
         * initialization.
         */
        protected NetLog           trace                  = null;

        /**
         * Optional (general purpose) logging object.
         *
         * This logging object should only be used temporarily for debugging purpose.
         */
        protected NetLog           disp                   = null;

        /**
         * Optional statistic object.
         */
        protected NetMessageStat   stat                   = null;





        /* ___ IMPORTANT OBJECTS ___________________________________________ */

	/**
         * The table of network {@linkplain NetConnection connections} indexed by connection identification numbers. */
        private Hashtable 	      connectionTable        = null;

	/**
	 * The topmost network driver.
	 */
	private NetDriver             driver     	     = null;

	/**
	 * The topmost network output.
	 */
	private NetOutput             output     	     = null;





        /* ___ STATE _______________________________________________________ */

	/**
	 * The empty message detection flag.
	 *
	 * The flag is set on each new {@link #newMessage} call and should
	 * be cleared as soon as at least a byte as been added to the living message.
	 */
	private boolean       	      emptyMsg     	     = true;

        /**
         * Internal message counter, for debugging purpose.
         */
        private int                   messageCount           = 0;

        /**
         * Internal send port counter, for debugging purpose.
         */
        static private volatile int   sendPortCount          = 0;

        /**
         * Process rank, for debugging purpose.
         */
        private int                   sendPortMessageRank    = 0;

        /**
         * Internal send port ID, for debugging purpose.
         */
        private int                   sendPortMessageId      = -1;

        /**
         * Trace log message prefix, for debugging purpose.
         */
        private String                sendPortTracePrefix    = null;

        /**
         * String made of peer receive ports' prefixes, for debugging purpose.
         *
         * <BR><B>Node:</B>&nbsp; this is string is currently _not_
         * updated when a connection is closed.
         */
        private String                receiversPrefixes      = null;

        private Vector                disconnectedPeers      = null;






        /* ___ EVENT QUEUE _________________________________________________ */

        /**
         * The general purpose {@linkplain NetPortEvent event} queue.
         */
	private NetEventQueue         eventQueue             = null;

        /**
         * The asynchronous {@link #eventQueue} listening & {@linkplain NetPortEvent event} processing thread.
         */
        private NetEventQueueListener eventQueueListener     = null;

        private SendPortConnectUpcall spcu                   = null;






        /* ___ LOCKS _______________________________________________________ */

	/**
	 * The network output synchronization lock.
         *
         * Note: Only the owner of this lock may interact with the topmost {@link #output}
	 */
	private NetMutex              outputLock   	     = null;






        /* ................................................................. */







        /* ___ NET EVENT QUEUE CONSUMER RELATED FUNCTIONS __________________ */

        /**
         * The callback function for processing incoming events from the {@link #eventQueue} and called by the {@link #eventQueueListener} thread.
         *
         * <BR><B>Note 1:</B> the only {@linkplain NetPortEvent event} supported currently is the <I>close</I> {@linkplain NetPortEvent event} ({@link NetPortEvent#CLOSE_EVENT}) which is added to the {@link #eventQueue} when a {@linkplain NetConnection connection} is detected to have been remotely closed. The argument of the <I>close</I> {@linkplain NetPortEvent event} is the {@linkplain NetConnection connection} identification {@link Integer}.
         * <BR><B>Note 2:</B> there is a possible race condition in the case that the <I>close</I> {@linkplain NetPortEvent event} is triggered before the {@linkplain NetConnection connection} is added to the {@link #connectionTable}. In that case, the {@linkplain NetPortEvent event} is ignored and when the {@linkplain NetConnection connection} later gets finally added to the {@link #connectionTable}, there is no mechanism to remember that it has actually been closed and has no need to be kept in the {@link #connectionTable}. There is no "simple light" solution to this problem as there is no "simple light way" to know whether a {@linkplain NetConnection connection} is not in the {@link #connectionTable} because it has not yet been added to it or because it as already been closed earlier.
         *
         * @param e the {@linkplain NetPortEvent event}.
         */
        public void event(NetEvent e) {
                log.in();
                NetPortEvent event = (NetPortEvent)e;

                switch (event.code()) {
                        case NetPortEvent.CLOSE_EVENT:
                                {
                                        Integer num = (Integer)event.arg();
                                        NetConnection cnx = null;
                                        NetReceivePortIdentifier nrpi = null;

                                        /*
                                         * Potential race condition here:
                                         * The event can be triggered _before_
                                         * the connection is added to the table.
                                         */
                                        synchronized(connectionTable) {
                                                cnx = (NetConnection)connectionTable.remove(num);
                                                if (cnx == null)
                                                        break;

                                                nrpi = cnx.getReceiveId();
                                                disconnectedPeers.add(nrpi);
                                        }

                                        try {
                                                close(cnx);
                                        } catch (IOException nie) {
                                                throw new Error(nie);
                                        }

                                        if (spcu != null) {
                                                spcu.lostConnection(this, nrpi, new Exception());
                                        }
                                }
                        break;

                default:
                        throw new Error("invalid event code");
                }
                log.out();
        }

        /* ................................................................. */







        /* ___ NETPORT RELATED FUNCTIONS ___________________________________ */


        /**
         * Returns the port type.
         *
         * @return the contents of {@link #type}.
         */
        public NetPortType getPortType() {
                log.in();
                log.out();
                return type;
        }

        /* ................................................................. */







        /* ___ SENDPORT RELATED FUNCTIONS __________________________________ */



        /* ----- CONSTRUCTORS ______________________________________________ */

	/**
	 * General purpose constructor.
	 *
	 * @param type the {@linkplain NetPortType port type}.
         * @param replacer the replacer for this object.
	 * @param name the name of the port.
	 */
	public NetSendPort(NetPortType type, Replacer replacer, String name, SendPortConnectUpcall spcu,
			   boolean connectionAdministration) throws IOException {
		this.name     = name;
		this.type     = type;
                this.replacer = replacer;
                this.ibis     = type.getIbis();
                this.spcu     = spcu;

                initDebugStreams();

                initPassiveState();
                initActiveState();
	}





        /* ----- CLEAN-UP __________________________________________________ */

	/**
	 * The class unloading time cleaning function.
         *
         * <BR><B>Note 1:</B> the {@link #free} method is forcibly called, just in case it was not called before, in the user application.
         * <BR><B>Note 2:</B> the {@link #eventQueue} is closed there (that is, not in the {@link #free} method).
	 */
	protected void finalize() throws Throwable {
                log.in();
		free();

                if (eventQueueListener != null) {
                        eventQueueListener.end();

                        while (true) {
                                try {
                                        eventQueueListener.join();
                                        break;
                                } catch (InterruptedException e) {
                                        //
                                }
                        }
                }

		super.finalize();
                log.out();
	}


        /* ----- PASSIVE STATE INITIALIZATION ______________________________ */

        private void initDebugStreams() {
                sendPortMessageId = sendPortCount++;
                sendPortMessageRank = ((NetIbis)type.getIbis())._closedPoolRank();
                sendPortTracePrefix = "_s"+sendPortMessageRank+"-"+sendPortMessageId+"_ ";

                String s = "//"+type.name()+" sendPort("+name+")/";

                boolean log   = type.getBooleanStringProperty(null, "Log",   false);
                boolean trace = type.getBooleanStringProperty(null, "Trace", false);
                boolean disp  = type.getBooleanStringProperty(null, "Disp",  true );
                boolean stat  = type.getBooleanStringProperty(null, "Stat",  false);

                this.log   = new NetLog(log,   s, "LOG");
                this.trace = new NetLog(trace, s, "TRACE");
                this.disp  = new NetLog(disp,  s, "DISP");
                this.stat  = new NetMessageStat(stat, s);
                this.trace.disp(sendPortTracePrefix, " send port created");
        }


        /**
         * The port connection {@link #identifier} generation.
         */
        private void initIdentifier() throws IOException {
                log.in();
                if (this.identifier != null)
                        throw new IOException("identifier already initialized");

		NetIbisIdentifier ibisId = (NetIbisIdentifier)ibis.identifier();

		this.identifier = new NetSendPortIdentifier(name, type.name(), ibisId);
                log.out();
        }

        /**
         * The <I>passive</I> port state initialization part.
         */
        private void initPassiveState() throws IOException {
                log.in();
                initIdentifier();
                log.out();
        }





        /* ----- ACTIVE STATE INITIALIZATION _______________________________ */

        /**
         * The {@link #eventQueue} construction and the {@link #eventQueueListener} thread activation.
         */
        private void initEventQueue() {
                log.in();
                disconnectedPeers  = new Vector();
                eventQueue         = new NetEventQueue();
                eventQueueListener = new NetEventQueueListener(this, "SendPort: " + ((name != null)?name:"anonymous"), eventQueue);
                eventQueueListener.start();
                log.out();
        }

        /**
         * The topmost {@link #driver} initialization.
         *
         * <BR><B>Note:</B> the driver's name is looked for in the <code>"Driver"</code> property of the context <code>'/'</code> and (currently) with the <code>null</code> subcontext.
         * @exception IOException in case of trouble.
         */
        private void loadMainDriver() throws IOException {
                log.in();
                if (this.driver != null)
                        throw new IOException("driver already loaded");

                String mainDriverName = type.getStringProperty("/", "Driver");

                if (mainDriverName == null) {
                        throw new IOException("root driver not specified");
                }

                NetDriver driver = ibis.getDriver(mainDriverName);

                if (driver == null) {
                        throw new IOException("driver not found");
                }

                this.driver = driver;
                log.out();
        }

        /**
         * The initialization of communication related data structures and objects.
         * @exception IOException in case of trouble.
         */
        private void initCommunicationEngine() throws IOException {
                log.in();
		this.connectionTable = new Hashtable();
                loadMainDriver();
		this.outputLock = new NetMutex(false);
		this.output     = driver.newOutput(type, null);
                log.out();
        }

        /**
         * The <I>active</I> port state initialization part.
         * @exception IOException in case of trouble.
         */
        private void initActiveState() throws IOException {
                log.in();
                initEventQueue();
                initCommunicationEngine();
                log.out();
        }





        /* ----- INTERNAL MANAGEMENT FUNCTIONS _____________________________ */

        /**
         * The setup of an new outgoing <I>service</I> connection.
         *
         * The service connection is an internal-use only streamed connection. A logical NetIbis {@linkplain NetConnection connection} between a {@linkplain NetSendPort send port} and a {@linkplain NetReceivePort receive port} is made of a <I>service</I> connection <B>and</B> an <I>application</I> connection.
         * <BR><B>Note:</B> establishing the 'service' part of a new {@linkplain NetConnection connection} is the first step in building the connection with a remote {@linkplain NetReceivePort port}.
         * @return    the new {@linkplain NetConnection connection}.
         * @exception IOException in case of trouble.
         */
        private NetConnection establishServiceConnection(NetReceivePortIdentifier nrpi) throws IOException {
                log.in();
                Hashtable      info = nrpi.connectionInfo();
                NetServiceLink link = new NetServiceLink(eventQueue, info);

                Integer num = null;

                synchronized(this) {
                        num = new Integer(nextReceivePortNum++);
                }

                link.init(num);

                String peerPrefix = null;
		ObjectOutputStream os = new ObjectOutputStream(link.getOutputSubStream("__port__"));
		os.writeObject(identifier);
		os.writeInt(sendPortMessageRank);
		os.writeInt(sendPortMessageId);
		os.flush();
		os.close();
		ObjectInputStream is = new ObjectInputStream(link.getInputSubStream("__port__"));
		int rank  = is.readInt();
		int rpmid = is.readInt();

		trace.disp(sendPortTracePrefix+"New connection to: _r"+rank+"-"+rpmid+"_");

		peerPrefix = "_r"+rank+"-"+rpmid+"_";
		if (receiversPrefixes == null) {
			receiversPrefixes = peerPrefix;
		} else {
			receiversPrefixes = ","+peerPrefix;
		}

		is.close();

                NetConnection cnx = new NetConnection(this, num, identifier, nrpi, link, replacer);
                log.out();

                return cnx;
        }

        /**
         * The setup of an new outgoing <I>application</I> connection.
         *
         * The application connection is an application-use only network connection. A logical NetIbis {@linkplain NetConnection connection} between a {@linkplain NetSendPort send port} and a {@linkplain NetReceivePort receive port} is made of a <I>service</I> connection <B>and</B> an <I>application</I> connection.
         * <BR><B>Note:</B> establishing the 'application' part of a new {@linkplain NetConnection connection} is the last step in building the connection with a remote {@linkplain NetReceivePort port}.
         * @param     cnx the {@linkplain NetConnection connection} to setup.
         * @exception IOException in case of trouble.
         */
        private void establishApplicationConnection(NetConnection cnx) throws IOException {
                log.in();
		output.setupConnection(cnx);
                log.out();
        }

        /**
         * The unconditionnal closing of a {@link NetConnection}.
         *
         * This function is mainly called by the {@link #event event-processing callback}.
         * <BR><B>Note:</B> The <code>cnx</code> connection should be removed from the {@link #connectionTable} before being passed to this function.
         * @param     cnx the {@linkplain NetConnection connection} to close.
         * @exception IOException in case of trouble.
         */
        private void close(NetConnection cnx) throws IOException {
                log.in();
                if (cnx == null) {
                        log.out("cnx = null");

                        return;
                }

                try {
                        output.close(cnx.getNum());
                } catch (Exception e) {
                        throw new Error(e.getMessage());
                }

                cnx.close();
                log.out();
        }





        /* ----- PUBLIC SendPort API _______________________________________ */

	/**
	 * Starts the construction of a new message.
	 *
	 * @return The message instance.
	 */
	public WriteMessage newMessage() throws IOException {
                log.in();
		outputLock.lock();
                stat.begin();
		emptyMsg = true;
                output.initSend();
                if (trace.on()) {
                        final String messageId = (((NetIbis)type.getIbis())._closedPoolRank())+"-"+sendPortMessageId+"-"+(messageCount++);
                        trace.disp(sendPortTracePrefix, "message "+messageId+" send to "+receiversPrefixes+"-->");
                        writeString(messageId);
                }

                log.out();

		return this;
	}

	public SendPort localPort() {
		return this;
	}

	/**
	 * Unimplemented.
	 *
	 * @return null.
	 */
	public DynamicProperties properties() {
                log.in();
                log.out();
		return null;
	}

	/**
	 * Returns the port {@linkplain NetSendPortIdentifier identifier}.
	 *
	 * @return The identifier instance.
	 */
	public SendPortIdentifier identifier() {
                log.in();
                log.out();
		return identifier;
	}

	public String name() {
		return name;
	}

	/**
	 * Attempts to connect the send port to a specified receive port.
	 *
	 * @param rpi the identifier of the peer port.
	 */
	public synchronized void connect(ReceivePortIdentifier rpi) throws IOException {
                log.in();
		outputLock.lock();
		NetReceivePortIdentifier nrpi = (NetReceivePortIdentifier)rpi;
                NetConnection cnx = establishServiceConnection(nrpi);

                synchronized(connectionTable) {
                        connectionTable.put(cnx.getNum(), cnx);
                }

                establishApplicationConnection(cnx);
		outputLock.unlock();
                log.out();
	}

	/**
	 * Interruptible connect.
	 *
	 * <strong>Not implemented.</strong>
	 * @param rpi the identifier of the peer port.
	 * @param timeout_millis the connection timeout in milliseconds.
	 */
	public void connect(ReceivePortIdentifier rpi,
			    long                   timeout_millis)
		throws IOException {
                log.in();
				long start = System.currentTimeMillis();
				boolean success = false;
				do {
					if (timeout_millis > 0 &&
						System.currentTimeMillis() > start + timeout_millis) {
						throw new ConnectionTimedOutException("could not connect");
					}
					try {
						connect(rpi);
						success = true;
					} catch (IOException e) {
						int timeLeft = (int)(start + timeout_millis - System.currentTimeMillis());
						try {
							if (timeLeft > 0) Thread.sleep(Math.min(timeLeft, 500));
						} catch (InterruptedException e2) {
							// ignore               
						}
					}
				} while (!success);
                log.out();
	}

        /*
         * Shouldn't the return type be an array?
         */
	public ReceivePortIdentifier[] connectedTo() {
                synchronized(connectionTable) {
                        ReceivePortIdentifier t[] = new ReceivePortIdentifier[connectionTable.size()];

                        Iterator it = connectionTable.values().iterator();
                        int i = 0;

                        while (it.hasNext()) {
                                NetConnection cnx = (NetConnection)it.next();
                                t[i++] = cnx.getReceiveId();
                        }

                        return t;
                }
        }

	public ReceivePortIdentifier[] lostConnections() {
                synchronized(connectionTable) {
                        ReceivePortIdentifier t[] = new ReceivePortIdentifier[disconnectedPeers.size()];
                        disconnectedPeers.copyInto(t);
                        disconnectedPeers.clear();

                        return t;
                }
        }

	/**
	 * Closes the port.
	 *
	 * Note: this function might block until the living message is finalized.
	 */
	public void free()
		throws IOException {
                log.in();
                trace.disp(sendPortTracePrefix, "send port shutdown-->");
                synchronized(this) {
                        try {
                                if (outputLock != null) {
                                        outputLock.lock();
                                }

                                trace.disp(sendPortTracePrefix, "send port shutdown: output locked");

                                if (connectionTable != null) {
                                        while (true) {
                                                NetConnection cnx = null;

                                                synchronized(connectionTable) {
                                                        Iterator i = connectionTable.values().iterator();
                                                        if (!i.hasNext())
                                                                break;

                                                        cnx = (NetConnection)i.next();
                                                        i.remove();
                                                }

                                                if (cnx != null) {
							if (spcu != null) {
								spcu.lostConnection(this, cnx.getReceiveId(), new Exception());
							}
                                                        close(cnx);
                                                }
                                        }
                                }
                                trace.disp(sendPortTracePrefix, "send port shutdown: all connections closed");

                                if (output != null) {
                                        output.free();
                                }

                                trace.disp(sendPortTracePrefix, "send port shutdown: all outputs freed");

                                if (outputLock != null) {
                                        outputLock.unlock();
                                }

                                trace.disp(sendPortTracePrefix, "send port shutdown: output lock released");
                        } catch (Exception e) {
                                __.fwdAbort__(e);
                        }
                }

                trace.disp(sendPortTracePrefix, "send port shutdown<--");
                log.out();
	}



        /* ----- PUBLIC WriteMessage API ___________________________________ */

	/**
	 * Sends what remains to be sent.
	 */
	public void send() throws IOException{
                log.in();
		output.send();
                log.out();
	}

	/**
	 * Completes the message transmission.
	 *
	 * Note: if it is detected that the message is actually empty,
	 * a single byte is forced to be sent over the network.
	 */
	private void _finish() throws  IOException{
                log.in();
		if (emptyMsg) {
			output.handleEmptyMsg();
		}
                log.out();
	}

	/**
	 * Completes the message transmission and releases the send port.
	 */
	public void finish() throws IOException{
                log.in();
		_finish();
		output.finish();
                stat.end();
                trace.disp(sendPortTracePrefix, "message send <--");
		outputLock.unlock();
                log.out();
	}

	/**
	 * Unconditionnaly completes the message transmission and
	 * releases the send port. The writeMessage is kept by
	 * the application for the next send.
	 *
	 * @param doSend {@inheritDoc}
	 */
	public void reset(boolean doSend) throws IOException {
                log.in();
		if (doSend) {
			send();
		} else {
                        throw new Error("full reset unimplemented");
                }
		_finish();
		output.reset(doSend);
		emptyMsg = true;
                output.initSend();
                log.out();
	}


	public long getCount() {
                log.in();
                log.out();
		return 0;
	}

	public void resetCount() {
                log.in();
		//
                log.out();
	}

	public void writeBoolean(boolean v) throws IOException {
                log.in();
		emptyMsg = false;
                stat.addBoolean();
		output.writeBoolean(v);
                log.out();
	}

	public void writeByte(byte v) throws IOException {
                log.in();
		emptyMsg = false;
                stat.addByte();
		output.writeByte(v);
                log.out();
	}

	public void writeChar(char v) throws IOException {
                log.in();
		emptyMsg = false;
                stat.addChar();
		output.writeChar(v);
                log.out();
	}

	public void writeShort(short v) throws IOException {
                log.in();
		emptyMsg = false;
                stat.addShort();
		output.writeShort(v);
                log.out();
	}

	public void writeInt(int v) throws IOException {
                log.in();
		emptyMsg = false;
                stat.addInt();
		output.writeInt(v);
                log.out();
	}

	public void writeLong(long v) throws IOException {
                log.in();
		emptyMsg = false;
                stat.addLong();
		output.writeLong(v);
                log.out();
	}

	public void writeFloat(float v) throws IOException {
                log.in();
		emptyMsg = false;
                stat.addFloat();
		output.writeFloat(v);
                log.out();
	}

	public void writeDouble(double v) throws IOException {
                log.in();
		emptyMsg = false;
                stat.addDouble();
		output.writeDouble(v);
                log.out();
	}

	public void writeString(String v) throws IOException {
                log.in();
		emptyMsg = false;
                stat.addString();
		output.writeString(v);
                log.out();
	}

	public void writeObject(Object v) throws IOException {
                log.in();
		emptyMsg = false;
                stat.addObject();
		output.writeObject(v);
                log.out();
	}

	public void writeArray(boolean [] b) throws IOException {
                log.in();
		writeArray(b, 0, b.length);
                log.out();
	}

	public void writeArray(byte [] b) throws IOException {
                log.in();
		writeArray(b, 0, b.length);
                log.out();
	}

	public void writeArray(char [] b) throws IOException {
                log.in();
                writeArray(b, 0, b.length);
                log.out();
	}

	public void writeArray(short [] b) throws IOException {
                log.in();
                writeArray(b, 0, b.length);
                log.out();
	}

	public void writeArray(int [] b) throws IOException {
                log.in();
                writeArray(b, 0, b.length);
                log.out();
	}

	public void writeArray(long [] b) throws IOException {
                log.in();
                writeArray(b, 0, b.length);
                log.out();
	}

	public void writeArray(float [] b) throws IOException {
                log.in();
                writeArray(b, 0, b.length);
                log.out();
	}

	public void writeArray(double [] b) throws IOException {
                log.in();
                writeArray(b, 0, b.length);
                log.out();
	}

	public void writeArray(Object [] b) throws IOException {
                log.in();
                writeArray(b, 0, b.length);
                log.out();
	}

	public void writeArray(boolean [] b, int o, int l) throws IOException {
                log.in();
		if (l == 0) {
			log.out("l = 0");
			return;
		}

                stat.addBooleanArray(l);
		emptyMsg = false;
                output.writeArray(b, o, l);
                log.out();
	}

	public void writeArray(byte [] b, int o, int l) throws IOException {
                log.in();
		if (l == 0) {
			log.out("l = 0");
			return;
		}

		emptyMsg = false;
                stat.addByteArray(l);
                output.writeArray(b, o, l);
                log.out();
	}

	public void writeArray(char [] b, int o, int l) throws IOException {
                log.in();
		if (l == 0) {
			log.out("l = 0");
			return;
		}

		emptyMsg = false;
                stat.addCharArray(l);
                output.writeArray(b, o, l);
                log.out();
	}

	public void writeArray(short [] b, int o, int l) throws IOException {
                log.in();
		if (l == 0) {
			log.out("l = 0");
			return;
		}

		emptyMsg = false;
                stat.addShortArray(l);
                output.writeArray(b, o, l);
                log.out();
	}

	public void writeArray(int [] b, int o, int l) throws IOException {
                log.in();
		if (l == 0) {
			log.out("l = 0");
			return;
		}

		emptyMsg = false;
                stat.addIntArray(l);
                output.writeArray(b, o, l);
                log.out();
	}

	public void writeArray(long [] b, int o, int l) throws IOException {
                log.in();
		if (l == 0) {
			log.out("l = 0");
			return;
		}

		emptyMsg = false;
                stat.addLongArray(l);
                output.writeArray(b, o, l);
                log.out();
	}

	public void writeArray(float [] b, int o, int l) throws IOException {
                log.in();
		if (l == 0) {
			log.out("l = 0");
			return;
		}

                emptyMsg = false;
                stat.addFloatArray(l);
                output.writeArray(b, o, l);
                log.out();
	}

	public void writeArray(double [] b, int o, int l) throws IOException {
                log.in();
		if (l == 0) {
			log.out("l = 0");
			return;
		}

		emptyMsg = false;
                stat.addDoubleArray(l);
                output.writeArray(b, o, l);
                log.out();
	}

	public void writeArray(Object [] b, int o, int l) throws IOException {
                log.in();
		if (l == 0) {
			log.out("l = 0");
			return;
		}

		emptyMsg = false;
                stat.addObjectArray(l);
                output.writeArray(b, o, l);
                log.out();
	}
}
