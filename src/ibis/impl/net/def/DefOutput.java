package ibis.ipl.impl.net.def;

import ibis.ipl.impl.net.*;

import ibis.ipl.IbisIOException;

import java.io.ObjectInputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

import java.util.Hashtable;

/**
 * The DEF output implementation.
 */
public final class DefOutput extends NetOutput {

	/**
	 * The peer {@link ibis.ipl.impl.net.NetReceivePort NetReceivePort}
	 * local number.
	 */
	private Integer            rpn   = null;

	/**
	 * The communication output stream.
	 */
	private ObjectOutputStream defOs = null;

        private int                nlsId =   -1;

	/**
	 * Constructor.
	 *
	 * @param sp the properties of the output's 
	 * {@link ibis.ipl.impl.net.NetSendPort NetSendPort}.
	 * @param driver the DEF driver instance.
	 * @param output the controlling output.
	 */
	DefOutput(NetPortType pt, NetDriver driver, NetIO up, String context)
		throws IbisIOException {
		super(pt, driver, up, context);
	}

	/*
	 * {@inheritDoc}
	 */
	public void setupConnection(Integer rpn, ObjectInputStream is, ObjectOutputStream os, NetServiceListener nls) throws IbisIOException {
                if (this.rpn != null) {
                        throw new Error("connection already established");
                }                

		this.rpn = rpn;
	
		Hashtable remoteInfo = receiveInfoTable(is);
		nlsId = ((Integer)remoteInfo.get("def_nls_id")).intValue();
                defOs = os;
		mtu = 0;
	}

	public void initSend() throws IbisIOException {
                synchronized(defOs) {
                        try {
                                defOs.writeInt(nlsId);
                                defOs.flush();
                        } catch (IOException e) {
                                throw new IbisIOException(e);
                        }  
                }
                
                super.initSend();
        }

        public void finish() throws IbisIOException{
                super.finish();
        }

        public void reset(boolean doSend) throws IbisIOException {
                if (doSend) {
                        send();
                } else {
                        throw new Error("full reset unimplemented");
                }
        }
        public void writeByteBuffer(NetSendBuffer b) throws IbisIOException {
                synchronized(defOs) {
                        try {
                                defOs.writeInt(nlsId);
                                for (int i = 0; i < b.length; i++) {
                                        defOs.writeByte((int)b.data[i]);
                                }
                                defOs.flush();
                        } catch (IOException e) {
                                throw new IbisIOException(e);
                        }
		}
        }
        
        
        public void writeBoolean(boolean b) throws IbisIOException {
                synchronized(defOs) {
                        try {
                                defOs.writeInt(nlsId);
                                defOs.writeBoolean(b);
                                defOs.flush();
                        } catch (IOException e) {
                                throw new IbisIOException(e);
                        }
		}
        }
        
        public void writeByte(byte b) throws IbisIOException {
                synchronized(defOs) {
                        try {
                                defOs.writeInt(nlsId);
                                defOs.writeByte((int)b);
                                defOs.flush();
                        } catch (IOException e) {
                                throw new IbisIOException(e);
                        }
		}
        }
        
        public void writeChar(char b) throws IbisIOException {
                synchronized(defOs) {
                        try {
                                defOs.writeInt(nlsId);
                                defOs.writeChar((int)b);
                                defOs.flush();
                        } catch (IOException e) {
                                throw new IbisIOException(e);
                        }
		}
        }

        public void writeShort(short b) throws IbisIOException {
                synchronized(defOs) {
                        try {
                                defOs.writeInt(nlsId);
                                defOs.writeShort((int)b);
                                defOs.flush();
                        } catch (IOException e) {
                                throw new IbisIOException(e);
                        }
		}
        }

        public void writeInt(int b) throws IbisIOException {
                synchronized(defOs) {
                        try {
                                defOs.writeInt(nlsId);
                                defOs.writeInt((int)b);
                                defOs.flush();
                        } catch (IOException e) {
                                throw new IbisIOException(e);
			}
                }
        }

        public void writeLong(long b) throws IbisIOException {
                synchronized(defOs) {
                        try {
                                defOs.writeInt(nlsId);
                                defOs.writeLong(b);
                                defOs.flush();
                        } catch (IOException e) {
                                throw new IbisIOException(e);
                        }
		}
        }

        public void writeFloat(float b) throws IbisIOException {
                synchronized(defOs) {
                        try {
                                defOs.writeInt(nlsId);
                                defOs.writeFloat(b);
                                defOs.flush();
                        } catch (IOException e) {
                                throw new IbisIOException(e);
                        }
		}
        }

        public void writeDouble(double b) throws IbisIOException {
                synchronized(defOs) {
                        try {
                                defOs.writeInt(nlsId);
                                defOs.writeDouble(b);
                                defOs.flush();
                        } catch (IOException e) {
                                throw new IbisIOException(e);
                        }
		}
        }

        public void writeString(String b) throws IbisIOException {
                synchronized(defOs) {
                        try {
                                defOs.writeInt(nlsId);
                                defOs.writeUTF(b);
                                defOs.flush();
                        } catch (IOException e) {
                                throw new IbisIOException(e);
                        }
		}
        }

        public void writeObject(Object o) throws IbisIOException {
                synchronized(defOs) {
                        try {
                                defOs.writeInt(nlsId);
                                defOs.writeObject(o);
                                defOs.flush();
                        } catch (IOException e) {
                                throw new IbisIOException(e);
                        }
		}
        }

        public void writeArraySliceBoolean(boolean [] b, int o, int l) throws IbisIOException {
                synchronized(defOs) {
                        try {
                                defOs.writeInt(nlsId);
                                for (int i = 0; i < l; i++) {
                                        writeBoolean(b[o+i]);
                                }
                                defOs.flush();
                        } catch (IOException e) {
                                throw new IbisIOException(e);
                        }
		}
        }

        public void writeArraySliceByte(byte [] b, int o, int l) throws IbisIOException {
                synchronized(defOs) {
                        try {
                                defOs.writeInt(nlsId);
                                for (int i = 0; i < l; i++) {
                                        defOs.writeByte(b[o+i]);
                                }
                                defOs.flush();
                        } catch (IOException e) {
                                throw new IbisIOException(e);
                        }
		}
        }
        public void writeArraySliceChar(char [] b, int o, int l) throws IbisIOException {
                synchronized(defOs) {
                        try {
                                defOs.writeInt(nlsId);
                                for (int i = 0; i < l; i++) {
                                        defOs.writeChar(b[o+i]);
                                }
                                defOs.flush();
                        } catch (IOException e) {
                                throw new IbisIOException(e);
                        }
		}
        }

        public void writeArraySliceShort(short [] b, int o, int l) throws IbisIOException {
                synchronized(defOs) {
                        try {
                                defOs.writeInt(nlsId);
                                for (int i = 0; i < l; i++) {
                                        defOs.writeShort(b[o+i]);
                                }
                                defOs.flush();
                        } catch (IOException e) {
                                throw new IbisIOException(e);
                        }
		}
        }

        public void writeArraySliceInt(int [] b, int o, int l) throws IbisIOException {
                synchronized(defOs) {
                        try {
                                defOs.writeInt(nlsId);
                                for (int i = 0; i < l; i++) {
                                        defOs.writeInt(b[o+i]);
                                }
                                defOs.flush();
                        } catch (IOException e) {
                                throw new IbisIOException(e);
                        }
		}
        }

        public void writeArraySliceLong(long [] b, int o, int l) throws IbisIOException {
                synchronized(defOs) {
                        try {
                                defOs.writeInt(nlsId);
                                for (int i = 0; i < l; i++) {
                                        defOs.writeLong(b[o+i]);
                                }
                                defOs.flush();
                        } catch (IOException e) {
                                throw new IbisIOException(e);
                        }
		}
        }

        public void writeArraySliceFloat(float [] b, int o, int l) throws IbisIOException {
                synchronized(defOs) {
                        try {
                                defOs.writeInt(nlsId);
                                for (int i = 0; i < l; i++) {
                                        defOs.writeFloat(b[o+i]);
                                }
                                defOs.flush();
                        } catch (IOException e) {
                                throw new IbisIOException(e);
                        }
		}
        }

        public void writeArraySliceDouble(double [] b, int o, int l) throws IbisIOException {
                synchronized(defOs) {
                        try {
                                defOs.writeInt(nlsId);
                                for (int i = 0; i < l; i++) {
                                        defOs.writeDouble(b[o+i]);
                                }
                                defOs.flush();
                        } catch (IOException e) {
                                throw new IbisIOException(e);
                        }
		}
        }

        public void writeArraySliceObject(Object [] b, int o, int l) throws IbisIOException {
                synchronized(defOs) {
                        try {
                                defOs.writeInt(nlsId);
                                for (int i = 0; i < l; i++) {
                                        defOs.writeObject(b[o+i]);
                                }
                                defOs.flush();
                        } catch (IOException e) {
                                throw new IbisIOException(e);
                        }
		}
        }

	/**
	 * {@inheritDoc}
	 */
	public void free() throws IbisIOException {
                defOs = null;
                rpn = null;
		super.free();
	}
}
