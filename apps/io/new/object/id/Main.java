import java.io.InputStream;
import java.io.OutputStream;

import ibis.io.ArrayInputStream;
import ibis.io.ArrayOutputStream;
import ibis.io.BufferedArrayInputStream;
import ibis.io.BufferedArrayOutputStream;
import ibis.io.IbisSerializationInputStream;
import ibis.io.IbisSerializationOutputStream;

import ibis.util.PoolInfo;

import java.net.Socket;
import java.net.ServerSocket;
import java.net.InetAddress;

public class Main {

    public static final boolean DEBUG = false;
    public static final int COUNT = 10000;

    public static void main(String args[]) {

	try {
	    Data temp = null;
	    long start, end;

	    int count = COUNT;

	    PoolInfo info = PoolInfo.createPoolInfo();

	    if (info.rank() == 0) {

		System.err.println("Main starting");

		ServerSocket server = new ServerSocket(1234);
		Socket s = server.accept();

		server.close();

		s.setTcpNoDelay(true);

		ArrayInputStream   in = new BufferedArrayInputStream(s.getInputStream());
		ArrayOutputStream out = new BufferedArrayOutputStream(s.getOutputStream());

		IbisSerializationInputStream   min = new IbisSerializationInputStream(in);
		IbisSerializationOutputStream mout = new IbisSerializationOutputStream(out);

		// Create object;
		temp = new Data("1234567890123456789012", InetAddress.getLocalHost());

		System.err.println("Writing object");

		// Warmup
		for (int i=0;i<count;i++) {
		    mout.writeObject(temp);					
		    mout.flush();
		    mout.reset();
		    if (DEBUG) { 
			System.err.println("Warmup "+ i);
		    }
		    min.readByte();
		}

		// Real test.
		start = System.currentTimeMillis();

		for (int i=0;i<count;i++) {
		    mout.writeObject(temp);
		    mout.flush();
		    mout.reset();
		    if (DEBUG) { 
			System.err.println("Test "+ i);
		    }
		    min.readByte();
		}

		end = System.currentTimeMillis();

		System.err.println("Write took "
			+ (end-start) + " ms.  => "
			+ ((1000.0*(end-start))/count) + " us/call => "
			+ ((1000.0*(end-start))/(count)) + " us/object");

		//				System.err.println("Bytes written "
		//						   + (count*len*Data.OBJECT_SIZE)
		//						   + " throughput = "
		//						   + (((1000.0*(count*len*Data.OBJECT_SIZE))/(1024*1024))/(end-start))
		//						   + " MBytes/s");

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

		ArrayInputStream   in = new BufferedArrayInputStream(s.getInputStream());
		ArrayOutputStream out = new BufferedArrayOutputStream(s.getOutputStream());

		IbisSerializationInputStream   min = new IbisSerializationInputStream(in);
		IbisSerializationOutputStream mout = new IbisSerializationOutputStream(out);

		for (int i=0;i<count;i++) {
		    temp = (Data) min.readObject();
		    if (DEBUG) { 
			System.err.println("Warmup "+ i);
		    }

		    mout.writeByte(1);
		    mout.flush();

		}


		for (int i=0;i<count;i++) {
		    temp = (Data) min.readObject();
		    if (DEBUG) { 
			System.err.println("Test "+ i);
		    }

		    mout.writeByte(1);
		    mout.flush();		
		}

		s.close();
	    }

	} catch (Exception e) {
	    System.err.println("Main got exception " + e);
	    e.printStackTrace();
	}
    }
}



