package ibis.ipl.impl.net.def;

import ibis.ipl.impl.net.*;

import ibis.ipl.IbisIOException;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import java.util.Hashtable;


/**
 * The DEF input implementation.
 */
public class DefInput extends NetInput {
	/**
	 * The peer {@link ibis.ipl.impl.net.NetSendPort NetSendPort}
	 * local number.
	 */
	private Integer           rpn    = null;

	/**
	 * The communication input stream.
	 */
	private ObjectInputStream defIs  = null;

        private NetServiceListener nls   = null;
        private int                nlsId =    0;

	/**
	 * Constructor.
	 *
	 * @param sp the properties of the input's 
	 * {@link ibis.ipl.impl.net.NetSendPort NetSendPort}.
	 * @param driver the DEF driver instance.
	 * @param input the controlling input.
	 */
	DefInput(NetPortType pt, NetDriver driver, NetIO up, String context)
		throws IbisIOException {
		super(pt, driver, up, context);
	}

	/*
	 * {@inheritDoc}
	 */
	public void setupConnection(Integer            rpn,
				    ObjectInputStream  is,
				    ObjectOutputStream os,
                                    NetServiceListener nls)
		throws IbisIOException {
		this.rpn = rpn;
                this.nls = nls;
                defIs    = is;
                nlsId     = nls.getId();

                Hashtable info    = new Hashtable();
                info.put("def_nls_id", new Integer(nlsId));
                sendInfoTable(os, info);
	}

	/**
	 * {@inheritDoc}
	 */
	public Integer poll() throws IbisIOException {
		activeNum = null;

		if (rpn == null) {
			return null;
		}

                if (nls.tryAcquire(nlsId)) {
                        activeNum = rpn;
                        nls.release();
                }

		return activeNum;
	}

       	public void finish() throws IbisIOException {
                super.finish();
                activeNum = null;
        }

        private void lock() {
                nls.acquire(nlsId);
        }

        private void unlock() {
                nls.release();
        }
        
        public NetReceiveBuffer readByteBuffer(int expectedLength) throws IbisIOException {
                NetReceiveBuffer b = new NetReceiveBuffer(new byte[expectedLength], 0);
                
                try {
                        lock();
                        for (int i = 0; i < expectedLength; i++) {
			        b.data[i] = readByte();
                                b.length++;
                        }
		} catch (IOException e) {
			throw new IbisIOException(e);
		} finally {
                        unlock();
                }

                return b;
        }
        

        public void readByteBuffer(NetReceiveBuffer b) throws IbisIOException {
                try {
                        lock();
                        for (int i = 0; i < b.data.length; i++) {
			        b.data[i] = readByte();
                                b.length++;
                        }
		} catch (IOException e) {
			throw new IbisIOException(e);
		} finally {
                        unlock();
                }
        }
        


	public boolean readBoolean() throws IbisIOException {
                boolean result = false;
                
		try {
                        lock();
                        result = defIs.readBoolean();
                } catch (IOException e) {
                        throw new IbisIOException(e);
		} finally {
                        unlock();
                }
   
                return result;
        }
        

        
	public byte readByte() throws IbisIOException {
                byte result = 0;
                
		try {
                        lock();
                        result = defIs.readByte();
		} catch (IOException e) {
			throw new IbisIOException(e);
		} finally {
                        unlock();
                }
   
                return result;
        }
        
        
	public char readChar() throws IbisIOException {
                char result = 0;
                
		try {
                        lock();
                        result = defIs.readChar();
		} catch (IOException e) {
			throw new IbisIOException(e);
		} finally {
                        unlock();
                }
   
                return result;
        }
        
	public short readShort() throws IbisIOException {
                short result = 0;
                
		try {
                        lock();
                        result = defIs.readShort();
		} catch (IOException e) {
			throw new IbisIOException(e);
		} finally {
                        unlock();
                }
   
                return result;
        }
        
	public int readInt() throws IbisIOException {
                int result = 0;
                
		try {
                        lock();
                        result = defIs.readInt();
                } catch (IOException e) {
			throw new IbisIOException(e);
		} finally {
                        unlock();
                }
   
                return result;
        }
        
	public long readLong() throws IbisIOException {
                long result = 0;
                
		try {
                        lock();
                        result = defIs.readLong();
		} catch (IOException e) {
			throw new IbisIOException(e);
		} finally {
                        unlock();
                }
   
                return result;
        }
        
	public float readFloat() throws IbisIOException {
                float result = 0;
                
		try {
                        lock();
                        result = defIs.readFloat();
		} catch (IOException e) {
			throw new IbisIOException(e);
		} finally {
                        unlock();
                }
   
                return result;
        }
        
	public double readDouble() throws IbisIOException {
                double result = 0;
                
		try {
                        lock();
                        result = defIs.readDouble();
		} catch (IOException e) {
			throw new IbisIOException(e);
		} finally {
                        unlock();
                }
   
                return result;
        }

	public String readString() throws IbisIOException {
                String result = "";
                
		try {
                        lock();
                        result = defIs.readUTF();
		} catch (IOException e) {
			throw new IbisIOException(e);
		} finally {
                        unlock();
                }
   
                return result;
        }

        public Object readObject() throws IbisIOException {
                Object o = null;
                try {
                        lock();
                        o = defIs.readObject();
                } catch (Exception e) {
                        throw new IbisIOException(e.getMessage());
                } finally {
                        unlock();
                }
                
                return o;
        }
        
        
	public void readSubArrayBoolean(boolean [] b, int o, int l) throws IbisIOException {
                try {
                        lock();
                        for (int i = 0; i < l; i++) {
			        b[o+i] = readBoolean();
                        }
		} catch (IOException e) {
			throw new IbisIOException(e);
		} finally {
                        unlock();
                }
        }


	public void readSubArrayByte(byte [] b, int o, int l) throws IbisIOException {
                try {
                        lock();
                        for (int i = 0; i < l; i++) {
			        b[o+i] = readByte();
                        }
		} catch (IOException e) {
			throw new IbisIOException(e);
		} finally {
                        unlock();
                }
        }


	public void readSubArrayChar(char [] b, int o, int l) throws IbisIOException {
                try {
                        lock();
                        for (int i = 0; i < l; i++) {
			        b[o+i] = readChar();
                        }
		} catch (IOException e) {
			throw new IbisIOException(e);
		} finally {
                        unlock();
                }
        }


	public void readSubArrayShort(short [] b, int o, int l) throws IbisIOException {
                try {
                        lock();
                        for (int i = 0; i < l; i++) {
			        b[o+i] = readShort();
                        }
		} catch (IOException e) {
			throw new IbisIOException(e);
		} finally {
                        unlock();
                }
        }


	public void readSubArrayInt(int [] b, int o, int l) throws IbisIOException {
                try {
                        lock();
                        for (int i = 0; i < l; i++) {
			        b[o+i] = readInt();
                        }
		} catch (IOException e) {
			throw new IbisIOException(e);
		} finally {
                        unlock();
                }
        }


	public void readSubArrayLong(long [] b, int o, int l) throws IbisIOException {
                try {
                        lock();
                        for (int i = 0; i < l; i++) {
			        b[o+i] = readLong();
                        }
		} catch (IOException e) {
			throw new IbisIOException(e);
		} finally {
                        unlock();
                }
        }


	public void readSubArrayFloat(float [] b, int o, int l) throws IbisIOException {
                try {
                        lock();
                        for (int i = 0; i < l; i++) {
			        b[o+i] = readFloat();
                        }
		} catch (IOException e) {
			throw new IbisIOException(e);
		} finally {
                        unlock();
                }
        }


	public void readSubArrayDouble(double [] b, int o, int l) throws IbisIOException {
                try {
                        lock();
                        for (int i = 0; i < l; i++) {
			        b[o+i] = defIs.readDouble();
                        }
		} catch (IOException e) {
			throw new IbisIOException(e);
		} finally {
                        unlock();
                }
        }

	public void readArrayBoolean(boolean [] b) throws IbisIOException {
                readSubArrayBoolean(b, 0, b.length);
        }


	public void readArrayByte(byte [] b) throws IbisIOException {
                readSubArrayByte(b, 0, b.length);
        }


	public void readArrayChar(char [] b) throws IbisIOException {
                readSubArrayChar(b, 0, b.length);
        }


	public void readArrayShort(short [] b) throws IbisIOException {
                readSubArrayShort(b, 0, b.length);
        }


	public void readArrayInt(int [] b) throws IbisIOException {
                readSubArrayInt(b, 0, b.length);
        }


	public void readArrayLong(long [] b) throws IbisIOException {
                readSubArrayLong(b, 0, b.length);
        }


	public void readArrayFloat(float [] b) throws IbisIOException {
                readSubArrayFloat(b, 0, b.length);
        }


	public void readArrayDouble(double [] b) throws IbisIOException {
                readSubArrayDouble(b, 0, b.length);
        }
        


	/**
	 * {@inheritDoc}
	 */
	public void free() throws IbisIOException {
                nls = null;
                rpn = null;
		super.free();
	}
}
