/* $Id$ */

import java.io.InputStream;
import java.io.OutputStream;

import ibis.io.DataInputStream;
import ibis.io.DataOutputStream;
import ibis.io.BufferedArrayInputStream;
import ibis.io.BufferedArrayOutputStream;
import ibis.io.IbisSerializationInputStream;
import ibis.io.IbisSerializationOutputStream;

import ibis.util.PoolInfo;

import java.net.Socket;
import java.net.ServerSocket;

import org.apache.log4j.Logger;

public class Main {

    public static final int LEN   = 1024*1024;
    public static final int COUNT = 100;

    static Logger logger = Logger.getLogger(Main.class.getName());

    public static final int BOOLEAN_SIZE = 1;
    public static final int BYTE_SIZE    = 1;
    public static final int CHAR_SIZE    = 2;
    public static final int SHORT_SIZE   = 2;
    public static final int INT_SIZE     = 4;
    public static final int LONG_SIZE    = 8;
    public static final int FLOAT_SIZE   = 4;
    public static final int DOUBLE_SIZE  = 8;

    public static void testBoolean(int rank, int count, int bytes, IbisSerializationInputStream min, IbisSerializationOutputStream mout) throws Exception { 

	// Create array
	long start, end;
	int len = bytes/BOOLEAN_SIZE;		
	boolean [] temp = null;

	if (rank == 0) { 

	    temp = new boolean[len];

	    System.err.println("boolean[" + len + "]");

	    // Warmup
	    for (int i=0;i<count;i++) {
		mout.writeObject(temp);					
		mout.flush();
		mout.reset();
		logger.debug("Warmup "+ i);
	    }

	    min.readByte();

	    // Real test.
	    start = System.currentTimeMillis();

	    for (int i=0;i<count;i++) {
		mout.writeObject(temp);
		mout.flush();
		mout.reset();
		logger.debug("Test "+ i);
	    }

	    logger.debug("Test done");

	    min.readByte();

	    end = System.currentTimeMillis();

	    System.err.println("Write took "
		    + (end-start) + " ms.  => "
		    + ((1000.0*(end-start))/count) + " us/call => "
		    + ((1000.0*(end-start))/(count*len)) + " us/object");

	    System.err.println("Bytes written "
		    + (count*len*BOOLEAN_SIZE)
		    + " throughput = "
		    + (((1000.0*(count*len*BOOLEAN_SIZE))/(1024*1024))/(end-start))
		    + " MBytes/s");
	} else { 
	    for (int i=0;i<count;i++) {
		temp = (boolean []) min.readObject();
	    }

	    mout.writeByte((byte)1);
	    mout.flush();

	    for (int i=0;i<count;i++) {
		temp = (boolean []) min.readObject();
	    }

	    mout.writeByte((byte)1);
	    mout.flush();

	} 
    } 

    public static void testByte(int rank, int count, int bytes, IbisSerializationInputStream min, IbisSerializationOutputStream mout) throws Exception { 

	// Create array
	long start, end;
	int len = bytes/BYTE_SIZE;		
	byte [] temp = null;

	if (rank == 0) { 

	    temp = new byte[len];

	    System.err.println("Writing byte[" + len + "]");

	    // Warmup
	    for (int i=0;i<count;i++) {
		mout.writeObject(temp);					
		mout.flush();
		mout.reset();
		logger.debug("Warmup "+ i);
	    }

	    min.readByte();

	    // Real test.
	    start = System.currentTimeMillis();

	    for (int i=0;i<count;i++) {
		mout.writeObject(temp);
		mout.flush();
		mout.reset();
		logger.debug("Test "+ i);
	    }

	    min.readByte();

	    end = System.currentTimeMillis();

	    System.err.println("Write took "
		    + (end-start) + " ms.  => "
		    + ((1000.0*(end-start))/count) + " us/call => "
		    + ((1000.0*(end-start))/(count*len)) + " us/object");

	    System.err.println("Bytes written "
		    + (count*len*BYTE_SIZE)
		    + " throughput = "
		    + (((1000.0*(count*len*BYTE_SIZE))/(1024*1024))/(end-start))
		    + " MBytes/s");
	} else { 
	    for (int j=0;j<2;j++) { 
		for (int i=0;i<count;i++) {
		    temp = (byte []) min.readObject();
		}

		mout.writeByte((byte)1);
		mout.flush();
	    } 
	} 
    } 

    public static void testChar(int rank, int count, int bytes, IbisSerializationInputStream min, IbisSerializationOutputStream mout) throws Exception { 

	// Create array
	long start, end;
	int len = bytes/CHAR_SIZE;		
	char [] temp = null;

	if (rank == 0) { 

	    temp = new char[len];

	    System.err.println("Writing char[" + len + "]");

	    // Warmup
	    for (int i=0;i<count;i++) {
		mout.writeObject(temp);					
		mout.flush();
		mout.reset();
		logger.debug("Warmup "+ i);
	    }

	    min.readByte();

	    // Real test.
	    start = System.currentTimeMillis();

	    for (int i=0;i<count;i++) {
		mout.writeObject(temp);
		mout.flush();
		mout.reset();
		logger.debug("Test "+ i);
	    }

	    min.readByte();

	    end = System.currentTimeMillis();

	    System.err.println("Write took "
		    + (end-start) + " ms.  => "
		    + ((1000.0*(end-start))/count) + " us/call => "
		    + ((1000.0*(end-start))/(count*len)) + " us/object");

	    System.err.println("Bytes written "
		    + (count*len*CHAR_SIZE)
		    + " throughput = "
		    + (((1000.0*(count*len*CHAR_SIZE))/(1024*1024))/(end-start))
		    + " MBytes/s");
	} else { 
	    for (int j=0;j<2;j++) { 
		for (int i=0;i<count;i++) {
		    temp = (char []) min.readObject();
		}

		mout.writeByte((byte)1);
		mout.flush();
	    } 
	} 
    } 

    public static void testShort(int rank, int count, int bytes, IbisSerializationInputStream min, IbisSerializationOutputStream mout) throws Exception { 

	// Create array
	long start, end;
	int len = bytes/SHORT_SIZE;		
	short [] temp = null;

	if (rank == 0) { 

	    temp = new short[len];

	    System.err.println("Writing short[" + len + "]");

	    // Warmup
	    for (int i=0;i<count;i++) {
		mout.writeObject(temp);					
		mout.flush();
		mout.reset();
		logger.debug("Warmup "+ i);
	    }

	    min.readByte();

	    // Real test.
	    start = System.currentTimeMillis();

	    for (int i=0;i<count;i++) {
		mout.writeObject(temp);
		mout.flush();
		mout.reset();
		logger.debug("Test "+ i);
	    }

	    min.readByte();

	    end = System.currentTimeMillis();

	    System.err.println("Write took "
		    + (end-start) + " ms.  => "
		    + ((1000.0*(end-start))/count) + " us/call => "
		    + ((1000.0*(end-start))/(count*len)) + " us/object");

	    System.err.println("Bytes written "
		    + (count*len*SHORT_SIZE)
		    + " throughput = "
		    + (((1000.0*(count*len*SHORT_SIZE))/(1024*1024))/(end-start))
		    + " MBytes/s");
	} else { 
	    for (int j=0;j<2;j++) { 
		for (int i=0;i<count;i++) {
		    temp = (short []) min.readObject();
		}

		mout.writeByte((byte)1);
		mout.flush();
	    } 
	} 
    } 

    public static void testInt(int rank, int count, int bytes, IbisSerializationInputStream min, IbisSerializationOutputStream mout) throws Exception { 

	// Create array
	long start, end;
	int len = bytes/INT_SIZE;		
	int [] temp = null;

	if (rank == 0) { 

	    temp = new int[len];

	    System.err.println("Writing int[" + len + "]");

	    // Warmup
	    for (int i=0;i<count;i++) {
		mout.writeObject(temp);					
		mout.flush();
		mout.reset();
		logger.debug("Warmup "+ i);
	    }

	    min.readByte();

	    // Real test.
	    start = System.currentTimeMillis();

	    for (int i=0;i<count;i++) {
		mout.writeObject(temp);
		mout.flush();
		mout.reset();
		logger.debug("Test "+ i);
	    }

	    min.readByte();

	    end = System.currentTimeMillis();

	    System.err.println("Write took "
		    + (end-start) + " ms.  => "
		    + ((1000.0*(end-start))/count) + " us/call => "
		    + ((1000.0*(end-start))/(count*len)) + " us/object");

	    System.err.println("Bytes written "
		    + (count*len*INT_SIZE)
		    + " throughput = "
		    + (((1000.0*(count*len*INT_SIZE))/(1024*1024))/(end-start))
		    + " MBytes/s");
	} else { 
	    for (int j=0;j<2;j++) { 
		for (int i=0;i<count;i++) {
		    temp = (int []) min.readObject();
		}

		mout.writeByte((byte)1);
		mout.flush();
	    } 
	} 
    } 

    public static void testLong(int rank, int count, int bytes, IbisSerializationInputStream min, IbisSerializationOutputStream mout) throws Exception { 

	// Create array
	long start, end;
	int len = bytes/LONG_SIZE;		
	long [] temp = null;

	if (rank == 0) { 

	    temp = new long[len];

	    System.err.println("Writing long[" + len + "]");

	    // Warmup
	    for (int i=0;i<count;i++) {
		mout.writeObject(temp);					
		mout.flush();
		mout.reset();
		logger.debug("Warmup "+ i);
	    }

	    min.readByte();

	    // Real test.
	    start = System.currentTimeMillis();

	    for (int i=0;i<count;i++) {
		mout.writeObject(temp);
		mout.flush();
		mout.reset();
		logger.debug("Test "+ i);
	    }

	    min.readByte();

	    end = System.currentTimeMillis();

	    System.err.println("Write took "
		    + (end-start) + " ms.  => "
		    + ((1000.0*(end-start))/count) + " us/call => "
		    + ((1000.0*(end-start))/(count*len)) + " us/object");

	    System.err.println("Bytes written "
		    + (count*len*LONG_SIZE)
		    + " throughput = "
		    + (((1000.0*(count*len*LONG_SIZE))/(1024*1024))/(end-start))
		    + " MBytes/s");
	} else { 
	    for (int j=0;j<2;j++) { 
		for (int i=0;i<count;i++) {
		    temp = (long []) min.readObject();
		}

		mout.writeByte((byte)1);
		mout.flush();
	    } 
	} 
    } 

    public static void testFloat(int rank, int count, int bytes, IbisSerializationInputStream min, IbisSerializationOutputStream mout) throws Exception { 

	// Create array
	long start, end;
	int len = bytes/FLOAT_SIZE;		
	float [] temp = null;

	if (rank == 0) { 

	    temp = new float[len];

	    System.err.println("Writing float[" + len + "]");

	    // Warmup
	    for (int i=0;i<count;i++) {
		mout.writeObject(temp);					
		mout.flush();
		mout.reset();
		logger.debug("Warmup "+ i);
	    }

	    min.readByte();

	    // Real test.
	    start = System.currentTimeMillis();

	    for (int i=0;i<count;i++) {
		mout.writeObject(temp);
		mout.flush();
		mout.reset();
		logger.debug("Test "+ i);
	    }

	    min.readByte();

	    end = System.currentTimeMillis();

	    System.err.println("Write took "
		    + (end-start) + " ms.  => "
		    + ((1000.0*(end-start))/count) + " us/call => "
		    + ((1000.0*(end-start))/(count*len)) + " us/object");

	    System.err.println("Bytes written "
		    + (count*len*FLOAT_SIZE)
		    + " throughput = "
		    + (((1000.0*(count*len*FLOAT_SIZE))/(1024*1024))/(end-start))
		    + " MBytes/s");
	} else { 
	    for (int j=0;j<2;j++) { 
		for (int i=0;i<count;i++) {
		    temp = (float []) min.readObject();
		}

		mout.writeByte((byte)1);
		mout.flush();
	    } 
	} 
    } 

    public static void testDouble(int rank, int count, int bytes, IbisSerializationInputStream min, IbisSerializationOutputStream mout) throws Exception { 

	// Create array
	long start, end;
	int len = bytes/DOUBLE_SIZE;		
	double [] temp = null;

	if (rank == 0) { 

	    temp = new double[len];

	    System.err.println("Writing double[" + len + "]");

	    // Warmup
	    for (int i=0;i<count;i++) {
		mout.writeObject(temp);					
		mout.flush();
		mout.reset();
		logger.debug("Warmup "+ i);
	    }

	    min.readByte();

	    // Real test.
	    start = System.currentTimeMillis();

	    for (int i=0;i<count;i++) {
		mout.writeObject(temp);
		mout.flush();
		mout.reset();
		logger.debug("Test "+ i);
	    }

	    min.readByte();

	    end = System.currentTimeMillis();

	    System.err.println("Write took "
		    + (end-start) + " ms.  => "
		    + ((1000.0*(end-start))/count) + " us/call => "
		    + ((1000.0*(end-start))/(count*len)) + " us/object");

	    System.err.println("Bytes written "
		    + (count*len*DOUBLE_SIZE)
		    + " throughput = "
		    + (((1000.0*(count*len*DOUBLE_SIZE))/(1024*1024))/(end-start))
		    + " MBytes/s");
	} else { 
	    for (int j=0;j<2;j++) { 
		for (int i=0;i<count;i++) {
		    temp = (double []) min.readObject();
		}

		mout.writeByte((byte)1);
		mout.flush();
	    } 
	} 
    } 

    public static void main(String args[]) {

	try {								
	    PoolInfo info = PoolInfo.createPoolInfo();

	    if (info.rank() == 0) {

		System.err.println("Main starting");

		ServerSocket server = new ServerSocket(1234);
		Socket s = server.accept();

		server.close();

		s.setTcpNoDelay(true);

		DataInputStream   in = new BufferedArrayInputStream(s.getInputStream());
		DataOutputStream out = new BufferedArrayOutputStream(s.getOutputStream());

		IbisSerializationInputStream   min = new IbisSerializationInputStream(in);
		IbisSerializationOutputStream mout = new IbisSerializationOutputStream(out);

		testBoolean(0, COUNT, LEN, min, mout);
		testByte(0, COUNT, LEN, min, mout);
		testChar(0, COUNT, LEN, min, mout);
		testShort(0, COUNT, LEN, min, mout);
		testInt(0, COUNT, LEN, min, mout);
		testLong(0, COUNT, LEN, min, mout);
		testFloat(0, COUNT, LEN, min, mout);
		testDouble(0, COUNT, LEN, min, mout);

		s.close();

	    } else {

		Socket s = null;

		while (s == null) {
		    try {
			s = new Socket(info.hostName(0), 1234);
		    } catch (Exception e) {
			Thread.sleep(1000);
			// ignore
		    }
		}

		s.setTcpNoDelay(true);

		DataInputStream   in = new BufferedArrayInputStream(s.getInputStream());
		DataOutputStream out = new BufferedArrayOutputStream(s.getOutputStream());

		IbisSerializationInputStream   min = new IbisSerializationInputStream(in);
		IbisSerializationOutputStream mout = new IbisSerializationOutputStream(out);

		testBoolean(1, COUNT, LEN, min, mout);
		testByte(1, COUNT, LEN, min, mout);
		testChar(1, COUNT, LEN, min, mout);
		testShort(1, COUNT, LEN, min, mout);
		testInt(1, COUNT, LEN, min, mout);
		testLong(1, COUNT, LEN, min, mout);
		testFloat(1, COUNT, LEN, min, mout);
		testDouble(1, COUNT, LEN, min, mout);

		s.close();
	    }

	} catch (Exception e) {
	    System.err.println("Main got exception " + e);
	    e.printStackTrace();
	}
    }
}



