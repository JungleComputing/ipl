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
public final class DefInput extends NetInput {
	private Integer            spn   = null;
	private ObjectInputStream  defIs = null;
        private NetServiceListener nls   = null;
        private int                nlsId =    0;
        
	DefInput(NetPortType pt, NetDriver driver, NetIO up, String context)
		throws IbisIOException {
		super(pt, driver, up, context);
	}

        private final class UpcallThread extends Thread {
                public UpcallThread(String name) {
                        super("DefInput.UpcallThread: "+name);
                }
                
                public void run() {
                        while (true) {
                                lock();
                                activeNum = spn;
                                unlock();
                                upcallFunc.inputUpcall(DefInput.this, activeNum);
                                activeNum = null;
                        }
                }
        }
        
        private void lock() {
                nls.acquire(nlsId);
        }

        private boolean trylock() {
                return nls.tryAcquire(nlsId);
        }

        private void unlock() {
                nls.release();
        }
        
	/*
	 * {@inheritDoc}
	 */
	public void setupConnection(Integer            spn,
				    ObjectInputStream  is,
				    ObjectOutputStream os,
                                    NetServiceListener nls)
		throws IbisIOException {
                if (this.spn != null) {
                        throw new Error("connection already established");
                }                

		this.spn = spn;
                this.nls = nls;
                defIs    = is;
                nlsId     = nls.getId();

                Hashtable info    = new Hashtable();
                info.put("def_nls_id", new Integer(nlsId));
                sendInfoTable(os, info);
                if (upcallFunc != null) {
                        (new UpcallThread("id = " + nlsId)).start();
                }
	}

	/**
	 * {@inheritDoc}
	 */
	public Integer poll() throws IbisIOException {
		activeNum = null;

		if (spn == null) {
			return null;
		}

                if (trylock()) {
                        activeNum = spn;
                        unlock();
                }

		return activeNum;
	}

       	public void finish() throws IbisIOException {
                super.finish();
                activeNum = null;
        }

        public NetReceiveBuffer readByteBuffer(int expectedLength) throws IbisIOException {
                NetReceiveBuffer b = new NetReceiveBuffer(new byte[expectedLength], 0);
                
                try {
                        lock();
                        for (int i = 0; i < expectedLength; i++) {
			        b.data[i] = defIs.readByte();
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
			        b.data[i] = defIs.readByte();
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
        
	public void readArraySliceBoolean(boolean [] b, int o, int l) throws IbisIOException {
                try {
                        lock();
                        for (int i = 0; i < l; i++) {
			        b[o+i] = defIs.readBoolean();
                        }
		} catch (IOException e) {
			throw new IbisIOException(e);
		} finally {
                        unlock();
                }
        }

	public void readArraySliceByte(byte [] b, int o, int l) throws IbisIOException {
                try {
                        lock();
                        for (int i = 0; i < l; i++) {
			        b[o+i] = defIs.readByte();
                        }
		} catch (IOException e) {
			throw new IbisIOException(e);
		} finally {
                        unlock();
                }
        }

	public void readArraySliceChar(char [] b, int o, int l) throws IbisIOException {
                try {
                        lock();
                        for (int i = 0; i < l; i++) {
			        b[o+i] = defIs.readChar();
                        }
		} catch (IOException e) {
			throw new IbisIOException(e);
		} finally {
                        unlock();
                }
        }

	public void readArraySliceShort(short [] b, int o, int l) throws IbisIOException {
                try {
                        lock();
                        for (int i = 0; i < l; i++) {
			        b[o+i] = defIs.readShort();
                        }
		} catch (IOException e) {
			throw new IbisIOException(e);
		} finally {
                        unlock();
                }
        }

	public void readArraySliceInt(int [] b, int o, int l) throws IbisIOException {
                try {
                        lock();
                        for (int i = 0; i < l; i++) {
			        b[o+i] = defIs.readInt();
                        }
		} catch (IOException e) {
			throw new IbisIOException(e);
		} finally {
                        unlock();
                }
        }

	public void readArraySliceLong(long [] b, int o, int l) throws IbisIOException {
                try {
                        lock();
                        for (int i = 0; i < l; i++) {
			        b[o+i] = defIs.readLong();
                        }
		} catch (IOException e) {
			throw new IbisIOException(e);
		} finally {
                        unlock();
                }
        }

	public void readArraySliceFloat(float [] b, int o, int l) throws IbisIOException {
                try {
                        lock();
                        for (int i = 0; i < l; i++) {
			        b[o+i] = defIs.readFloat();
                        }
		} catch (IOException e) {
			throw new IbisIOException(e);
		} finally {
                        unlock();
                }
        }

	public void readArraySliceDouble(double [] b, int o, int l) throws IbisIOException {
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

	public void readArraySliceObject(Object [] b, int o, int l) throws IbisIOException {
                try {
                        lock();
                        for (int i = 0; i < l; i++) {
			        b[o+i] = defIs.readObject();
                        }
		} catch (Exception e) {
			throw new IbisIOException(e);
		} finally {
                        unlock();
                }
        }


	/**
	 * {@inheritDoc}
	 */
	public void free() throws IbisIOException {
                nls = null;
                spn = null;
		super.free();
	}
}
