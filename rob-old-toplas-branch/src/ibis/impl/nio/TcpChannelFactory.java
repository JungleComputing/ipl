/* $Id$ */

package ibis.impl.nio;

import ibis.ipl.ConnectionRefusedException;
import ibis.ipl.ConnectionTimedOutException;
import ibis.ipl.IbisError;
import ibis.util.IPUtils;
import ibis.util.ThreadPool;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.Channel;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;

import org.apache.log4j.Logger;

/**
 * implements a channelfactory using the tcp implementation of nio
 */
class TcpChannelFactory implements ChannelFactory, Protocol {

    private static Logger logger = ibis.util.GetLogger.getLogger(TcpChannelFactory.class);

    // Server socket Channel we listen for new connection on
    private ServerSocketChannel ssc;

    // Address ssc is bound to
    private InetSocketAddress address;

    // list of ReceivePorts we listen for
    private ArrayList receivePorts;

    TcpChannelFactory() throws IOException {
        int port = 0;
        InetAddress localAddress = IPUtils.getLocalHostAddress();

        // init server socket channel
        ssc = ServerSocketChannel.open();

        address = new InetSocketAddress(localAddress, port);
        ssc.socket().bind(address);

        // just in case it binded to some other port
        localAddress = ssc.socket().getInetAddress();
        port = ssc.socket().getLocalPort();
        address = new InetSocketAddress(localAddress, port);

        receivePorts = new ArrayList();

        ThreadPool.createNew(this, "TcpChannelFactory");
    }

    /**
     * Register a receiveport with this factory, so it will listen for
     * connections to it from now on.
     * 
     * @return the address of the socket we wil listen for connections on
     */
    public synchronized InetSocketAddress register(NioReceivePort rp) {
        if (logger.isInfoEnabled()) {
            logger.info("Receiveport \"" + rp + "\" registered with factory");
        }

        receivePorts.add(rp);

        return address;
    }

    public synchronized void deRegister(NioReceivePort rp) throws IOException {
        NioReceivePort temp;

        if (logger.isInfoEnabled()) {
            logger.info("Receiveport[" + rp + "] DE-registers with factory");
        }

        for (int i = 0; i < receivePorts.size(); i++) {
            temp = (NioReceivePort) receivePorts.get(i);

            if (temp == rp) {
                receivePorts.remove(i);
                return;
            }
        }
        throw new IbisError("Receiveport " + rp + "tried to de-register "
                + "without being registerred!");
    }

    public void quit() throws IOException {
        // this will make the accept() throw an AsynchronusCloseException
        // or an ClosedChannelException and make the thread exit
        ssc.close();
    }

    /**
     * Finds the ReceivePort wich has the given identifier.
     * 
     * @return the ReceivePort wich has the given identifier, or null if not
     *         found.
     */
    private synchronized NioReceivePort findReceivePort(
            NioReceivePortIdentifier rpi) {
        NioReceivePort temp;
        for (int i = 0; i < receivePorts.size(); i++) {
            temp = (NioReceivePort) receivePorts.get(i);

            if (rpi.equals(temp.ident)) {
                return temp;
            }
        }
        return null;
    }

    /**
     * Tries to connect the sendport to the receiveport for the given time.
     * Returns the resulting channel.
     */
    public Channel connect(NioSendPortIdentifier spi,
            NioReceivePortIdentifier rpi, long timeoutMillis)
            throws IOException {
        int reply;
        SocketChannel channel;

        long deadline = 0;
        long time;

        if (logger.isDebugEnabled()) {
            logger.debug("connecting \"" + spi + "\" to \"" + rpi + "\"");
        }

        if (timeoutMillis > 0) {
            deadline = System.currentTimeMillis() + timeoutMillis;
        }

        while (true) {

            if (deadline == 0) {
                // do a blocking connect
                channel = SocketChannel.open();
                channel.connect(rpi.address);
            } else {
                time = System.currentTimeMillis();

                if (time >= deadline) {
                    logger.error("timeout on connecting");

                    throw new IOException("timeout on connecting");
                }

                channel = SocketChannel.open();
                channel.configureBlocking(false);
                channel.connect(rpi.address);

                Selector selector = Selector.open();
                channel.register(selector, SelectionKey.OP_CONNECT);

                if (selector.select(deadline - time) == 0) {
                    // nothing selected, so we had a timeout

                    logger.error("timed out while connecting socket "
                            + "to receiver");

                    throw new ConnectionTimedOutException("timed out while"
                            + " connecting socket to receiver");
                }

                if (!channel.finishConnect()) {
                    throw new IbisError(
                            "finish connect failed while we made sure"
                                    + " it would work");
                }

                selector.close();
                channel.configureBlocking(true);
            }

            channel.socket().setTcpNoDelay(true);
            channel.socket().setSendBufferSize(0x8000);
            channel.socket().setReceiveBufferSize(0x8000);

            // write out rpi and spi
            ChannelAccumulator accumulator = new ChannelAccumulator(channel);
            accumulator.writeByte(CONNECTION_REQUEST);
            spi.writeTo(accumulator);
            rpi.writeTo(accumulator);
            accumulator.flush();

            if (logger.isDebugEnabled()) {
                logger.debug("waiting for reply on connect");
            }

            if (timeoutMillis > 0) {
                time = System.currentTimeMillis();

                if (time >= deadline) {
                    logger.warn("timeout on waiting for reply on connecting");
                    throw new IOException("timeout on waiting for reply");
                }

                channel.configureBlocking(false);

                Selector selector = Selector.open();
                channel.register(selector, SelectionKey.OP_READ);

                if (selector.select(deadline - time) == 0) {
                    // nothing selected, so we had a timeout
                    try {
                        channel.close();
                    } catch (IOException e) {
                        // IGNORE
                    }

                    logger.error("timed out while for reply from receiver");

                    throw new ConnectionTimedOutException("timed out while"
                            + " waiting for reply from receiver");
                }
                selector.close();
                channel.configureBlocking(true);
            }

            // see what he thinks about it
            ChannelDissipator dissipator = new ChannelDissipator(channel);
            reply = dissipator.readByte();
            dissipator.close();

            if (reply == CONNECTION_DENIED) {
                logger.error("Receiver denied connection");
                channel.close();
                throw new ConnectionRefusedException(
                        "Receiver denied connection");
            } else if (reply == CONNECTION_ACCEPTED) {
                if (logger.isDebugEnabled()) {
                    logger.debug("made new connection from \"" + spi
                            + "\" to \"" + rpi + "\"");
                }
                channel.configureBlocking(true);
                return channel;
            } else if (reply == CONNECTIONS_DISABLED) {
                // receiveport not (yet) enabled, wait for a while
                try {
                    channel.close();
                } catch (Exception e) {
                    // IGNORE
                }
                try {
                    Thread.sleep(100);
                } catch (Exception e) {
                    // IGNORE
                }
                // and retry
                continue;
            } else {
                logger.error("illigal opcode in ChannelFactory.connect()");
                throw new IbisError("illigal opcode in"
                        + " ChannelFactory.connect()");
            }
        }
    }

    /**
     * Handles incoming requests
     */
    private void handleRequest(SocketChannel channel) {
        byte request;
        NioSendPortIdentifier spi = null;
        NioReceivePortIdentifier rpi;
        NioReceivePort rp = null;
        ChannelDissipator dissipator = new ChannelDissipator(channel);
        ChannelAccumulator accumulator = new ChannelAccumulator(channel);

        if (logger.isDebugEnabled()) {
            logger.debug("got new connection from "
                    + channel.socket().getInetAddress() + ":"
                    + channel.socket().getPort());
        }

        try {
            request = dissipator.readByte();

            if (request != CONNECTION_REQUEST) {
                logger.error("received unknown request");
                try {
                    dissipator.close();
                    accumulator.close();
                    channel.close();
                } catch (IOException e) {
                    // IGNORE
                }
                return;
            }

            spi = new NioSendPortIdentifier(dissipator);
            rpi = new NioReceivePortIdentifier(dissipator);
            dissipator.close();

            rp = findReceivePort(rpi);

            if (rp == null) {
                logger.error("could not find receiveport, connection denied");
                accumulator.writeByte(CONNECTION_DENIED);
                accumulator.flush();
                accumulator.close();
                channel.close();
                return;
            }

            if (logger.isDebugEnabled()) {
                logger.debug("giving new connection to receiveport " + rpi);
            }

            // register connection with receiveport
            byte reply = rp.connectionRequested(spi, channel);

            // send reply
            accumulator.writeByte(reply);
            accumulator.flush();
            accumulator.close();

            if (reply != CONNECTION_ACCEPTED) {
                channel.close();
                if (logger.isInfoEnabled()) {
                    logger.info("receiveport rejected connection");
                }
                return;
            }
        } catch (IOException e) {
            logger.error("got an exception on handling an incoming request"
                    + ", closing channel" + e);
            try {
                channel.close();
            } catch (IOException e2) {
                // IGNORE
            }
            return;
        }

        if (logger.isDebugEnabled()) {
            logger.debug("set up new connection");
        }
    }

    /**
     * Accepts connections on the server socket channel
     */
    public void run() {
        SocketChannel channel = null;

        Thread.currentThread().setName("ChannelFactory");

        logger.info("ChannelFactory running on " + ssc);

        while (true) {
            try {
                channel = ssc.accept();
                channel.socket().setTcpNoDelay(true);
                channel.socket().setSendBufferSize(0x8000);
                channel.socket().setReceiveBufferSize(0x8000);
                channel.configureBlocking(true);
            } catch (ClosedChannelException e) {
                // the channel was closed before we started the accept
                // OR while we were doing the accept
                // take the hint, and exit
                ssc = null;
                return;
            } catch (Exception e3) {
                try {
                    ssc.close();
                    channel.close();
                } catch (IOException e4) {
                    // IGNORE
                }
                logger.fatal("could not do accept");
                return;
            }

            handleRequest(channel);
        }
    }
}
