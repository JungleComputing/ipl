/* $Id$ */

package ibis.ipl.impl.nio;

import ibis.ipl.ConnectionRefusedException;
import ibis.ipl.ConnectionTimedOutException;
import ibis.ipl.PortType;
import ibis.ipl.impl.ReceivePort;
import ibis.ipl.impl.ReceivePortIdentifier;
import ibis.ipl.impl.SendPortIdentifier;
import ibis.util.IPUtils;
import ibis.util.ThreadPool;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.Channel;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * implements a channelfactory using the tcp implementation of nio
 */
class TcpChannelFactory implements ChannelFactory, Protocol {

    private static Logger logger = LoggerFactory.getLogger(TcpChannelFactory.class);

    // Server socket Channel we listen for new connection on
    private ServerSocketChannel ssc;

    // Address ssc is bound to
    private InetSocketAddress address;

    private NioIbis ibis;

    TcpChannelFactory(NioIbis ibis) throws IOException {
        int port = 0;
        InetAddress localAddress = IPUtils.getLocalHostAddress();

        this.ibis = ibis;

        // init server socket channel
        ssc = ServerSocketChannel.open();

        address = new InetSocketAddress(localAddress, port);
        ssc.socket().bind(address);

        // just in case it binded to some other port
        localAddress = ssc.socket().getInetAddress();
        port = ssc.socket().getLocalPort();
        address = new InetSocketAddress(localAddress, port);

        ThreadPool.createNew(this, "TcpChannelFactory");
    }

    public InetSocketAddress getAddress() {
        return address;
    }

    public void quit() throws IOException {
        // this will make the accept() throw an AsynchronusCloseException
        // or an ClosedChannelException and make the thread exit
        ssc.close();
    }

    /**
     * Tries to connect the sendport to the receiveport for the given time.
     * Returns the resulting channel.
     */
    public Channel connect(NioSendPort spi,
            ReceivePortIdentifier rpi, long timeoutMillis)
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

        InetSocketAddress addr = ibis.getAddress(rpi.ibis);

        while (true) {

            if (deadline == 0) {
                // do a blocking connect
                channel = SocketChannel.open();
                channel.connect(addr);
            } else {
                time = System.currentTimeMillis();

                if (time >= deadline) {
                    logger.error("timeout on connecting");

                    throw new IOException("timeout on connecting");
                }

                channel = SocketChannel.open();
                channel.configureBlocking(false);
                channel.connect(addr);

                Selector selector = Selector.open();
                channel.register(selector, SelectionKey.OP_CONNECT);

                if (selector.select(deadline - time) == 0) {
                    // nothing selected, so we had a timeout

                    logger.error("timed out while connecting socket "
                            + "to receiver");

                    throw new ConnectionTimedOutException("timed out while"
                            + " connecting socket to receiver", rpi);
                }

                if (!channel.finishConnect()) {
                    throw new IOException("finish connect failed while we made sure"
                                + " it would work");
                }

                selector.close();
                channel.configureBlocking(true);
            }

            channel.socket().setTcpNoDelay(true);
            // channel.socket().setSendBufferSize(0x8000);
            // channel.socket().setReceiveBufferSize(0x8000);

            // write out rpi name, spi identifier and spi capabilities.
            ChannelAccumulator accumulator = new ChannelAccumulator(channel);
            accumulator.writeByte(CONNECTION_REQUEST);
            DataOutputStream d = new DataOutputStream(accumulator);
            d.writeUTF(rpi.name());
            spi.ident.writeTo(d);
            spi.type.writeTo(d);
            d.flush();

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
                            + " waiting for reply from receiver", rpi);
                }
                selector.close();
                channel.configureBlocking(true);
            }

            // see what he thinks about it
            ChannelDissipator dissipator = new ChannelDissipator(channel);
            reply = dissipator.readByte();

            if (reply == ReceivePort.DENIED) {
                logger.error("Receiver denied connection");
                channel.close();
                throw new ConnectionRefusedException("Receiver denied connection", rpi);
            } else if (reply == ReceivePort.ACCEPTED) {
                if (logger.isDebugEnabled()) {
                    logger.debug("made new connection from \"" + spi
                            + "\" to \"" + rpi + "\"");
                }
                channel.configureBlocking(true);
                return channel;
            } else if (reply == ReceivePort.DISABLED) {
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
                logger.error("illegal opcode in ChannelFactory.connect()");
                throw new IOException("illegal opcode in ChannelFactory.connect()");
            }
        }
    }

    /**
     * Handles incoming requests
     */
    private void handleRequest(SocketChannel channel) {
        byte request;
        String name;
        SendPortIdentifier spi = null;
        PortType capabilities = null;
        ReceivePortIdentifier rpi;
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

            DataInputStream d = new DataInputStream(dissipator);

            name = d.readUTF();
            spi = new SendPortIdentifier(d);
            capabilities = new PortType(d);
            rpi = new ReceivePortIdentifier(name, ibis.ident);

            rp = (NioReceivePort) ibis.findReceivePort(name);

            if (rp == null) {
                logger.error("could not find receiveport, connection denied");
                accumulator.writeByte(ReceivePort.DENIED);
                accumulator.flush();
                channel.close();
                return;
            }

            if (logger.isDebugEnabled()) {
                logger.debug("giving new connection to receiveport " + rpi);
            }

            // register connection with receiveport
            byte reply = rp.connectionRequested(spi, capabilities, channel);

            // send reply
            accumulator.writeByte(reply);
            accumulator.flush();

            if (reply != ReceivePort.ACCEPTED) {
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
                // channel.socket().setSendBufferSize(0x8000);
                // channel.socket().setReceiveBufferSize(0x8000);
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
                logger.error("could not do accept");
                return;
            }

            handleRequest(channel);
        }
    }
}
