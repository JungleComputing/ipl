package ibis.ipl.impl.net.gm;

import ibis.ipl.impl.net.__;
import ibis.ipl.impl.net.NetBufferedOutput;
import ibis.ipl.impl.net.NetDriver;
import ibis.ipl.impl.net.NetMutex;
import ibis.ipl.impl.net.NetIO;
import ibis.ipl.impl.net.NetOutput;
import ibis.ipl.impl.net.NetSendBuffer;

import ibis.ipl.IbisIOException;
import ibis.ipl.StaticProperties;

import java.net.Socket;
import java.net.InetAddress;
import java.net.SocketException;

import java.io.ObjectInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

import java.util.Hashtable;

/**
 * The GM output implementation (block version).
 */
public class GmOutput extends NetBufferedOutput {

        /**
         * The peer {@link ibis.ipl.impl.net.NetReceivePort NetReceivePort}
         * local number.
         */
        private Integer    rpn           = null;
                           
        private long       deviceHandle = 0;
        private long       outputHandle = 0;
        private NetMutex   mutex         = new NetMutex(true);
        

        static private byte [] dummyBuffer = new byte[4];

        native long nInitOutput(long deviceHandle) throws IbisIOException;
        native int  nGetOutputNodeId(long outputHandle) throws IbisIOException;
        native int  nGetOutputPortId(long outputHandle) throws IbisIOException;
        native void nConnectOutput(long outputHandle, int remoteNodeId, int remotePortId) throws IbisIOException;
        native void nPostSend(long outputHandle, byte []b, int base, int length) throws IbisIOException;
        native void nCompleteSend(long outputHandle, byte []b, int base, int length) throws IbisIOException;
        native void nCloseOutput(long outputHandle) throws IbisIOException;


        /**
         * Constructor.
         *
         * @param sp the properties of the output's 
         * {@link ibis.ipl.impl.net.NetSendPort NetSendPort}.
         * @param driver the GM driver instance.
         * @param output the controlling output.
         */
        GmOutput(StaticProperties sp,
                 NetDriver        driver,
                 NetIO            up)
                throws IbisIOException {
                super(sp, driver, up);
                headerLength = 0;

                Driver.gmLock.lock();
                deviceHandle = Driver.nInitDevice(0);
                outputHandle = nInitOutput(deviceHandle);
                Driver.gmLock.unlock();
        }

        /*
         * Sets up an outgoing GM connection.
         *
         * @param rpn {@inheritDoc}
         * @param is {@inheritDoc}
         * @param os {@inheritDoc}
         */
        public void setupConnection(Integer                  rpn,
                                    ObjectInputStream        is,
                                    ObjectOutputStream       os)
                throws IbisIOException {
                this.rpn = rpn;
        
                Hashtable rInfo    = receiveInfoTable(is);
                int       rnodeId = ((Integer) rInfo.get("gm_node_id")).intValue();
                int       rportId = ((Integer) rInfo.get("gm_port_id")).intValue();
                Driver.gmLock.lock();
                int       lnodeId = nGetOutputNodeId(outputHandle);
                int       lportId = nGetOutputPortId(outputHandle);
                Driver.gmLock.unlock();

                Hashtable lInfo = new Hashtable();
                lInfo.put("gm_node_id", new Integer(lnodeId));
                lInfo.put("gm_port_id", new Integer(lportId));
                sendInfoTable(os, lInfo);

                Driver.gmLock.lock();
                //System.err.println(lnodeId+"["+lportId+"]"+" connecting to "+rnodeId+"["+rportId+"]");
                
                nConnectOutput(outputHandle, rnodeId, rportId);
                Driver.gmLock.unlock();

		try {
                        is.read();
                        os.write(1);
                        os.flush();
		} catch (IOException e) {
			throw new IbisIOException(e);
		}

                mtu = 2*1024*1024;
        }

        public void initSend() throws IbisIOException {
                super.initSend();
                Driver.gmLock.lock();
                nPostSend(outputHandle, dummyBuffer, 0, dummyBuffer.length);
                do {                                
                        Driver.nGmThread();
                } while (!mutex.trylock());        
                nCompleteSend(outputHandle, dummyBuffer, 0, dummyBuffer.length);
                Driver.gmLock.unlock();
        }

        /**
         * {@inheritDoc}
         */
        public void writeByteBuffer(NetSendBuffer b) throws IbisIOException {
                // System.err.println("sending buffer: "+b.length+" bytes");
                Driver.gmLock.lock();
                nPostSend(outputHandle, b.data, b.base, b.length);
                do {                                
                        Driver.nGmThread();
                } while (!mutex.trylock());        
                nCompleteSend(outputHandle, b.data, b.base, b.length);
                Driver.gmLock.unlock();
                // System.err.println("buffer sent: "+b.length+" bytes");
        }

        /**
         * {@inheritDoc}
         */
        public void free() throws IbisIOException {
                rpn = null;

                Driver.gmLock.lock();
                if (outputHandle != 0) {
                        nCloseOutput(outputHandle);
                        outputHandle = 0;
                }
                
                if (deviceHandle != 0) {
                        Driver.nCloseDevice(deviceHandle);
                        deviceHandle = 0;
                }
                Driver.gmLock.unlock();

                super.free();
        }
        
}
