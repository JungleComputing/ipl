import java.io.InputStream;
import java.io.OutputStream;

import ibis.io.ArrayInputStream;
import ibis.io.ArrayOutputStream;
import ibis.io.BufferedArrayInputStream;
import ibis.io.BufferedArrayOutputStream;
import ibis.io.MantaInputStream;
import ibis.io.MantaOutputStream;
import ibis.io.MantaTypedBufferInputStream;
import ibis.io.MantaTypedBufferOutputStream;

import java.net.Socket;
import java.net.ServerSocket;

public class Main {

    public static void main(String args[]) {

	try {
	    Data temp = null;
	    long start, end;

	    int count = 100; // 10000;
	    int len = 5; // 100;

	    int count2 = 0; // 10000;

	    int count3 = 100;

	    DasInfo info = new DasInfo();

	    if (info.hostNumber() == 0) {

		System.err.println("Main starting");

		ServerSocket server = new ServerSocket(1234);
		Socket s = server.accept();

		server.close();

		s.setTcpNoDelay(true);

		ArrayInputStream   in = new BufferedArrayInputStream(s.getInputStream());
		ArrayOutputStream out = new BufferedArrayOutputStream(s.getOutputStream());

		MantaInputStream   min = new MantaTypedBufferInputStream(in);
		MantaOutputStream mout = new MantaTypedBufferOutputStream(out);

		for (int i=0;i<len;i++) {
		    temp = new Data((i+0.8)/1.3, temp);
		}

		/*
		for (int i=0;i<count;i++) {
		    mout.writeObject(temp);
		    System.err.print("+");
		    mout.flush();
		    mout.reset();
		}
		*/

		start = System.currentTimeMillis();

		for (int i=0;i<count;i++) {
		    mout.writeObject(temp);
		    mout.flush();
		    mout.reset();
System.err.println("=========================================================");
		}

		min.readByte();

		end = System.currentTimeMillis();

		System.err.println("Write took "
			   + (end-start) + " ms.  => "
			   + ((1000.0*(end-start))/count) + " us/call => "
			   + ((1000.0*(end-start))/(count*len)) + " us/object");

		System.err.println("Bytes written "
			   + mout.bytesWritten()
			   + " throughput = "
			   + ((1000.0*mout.bytesWritten()/(1024*1024))/(end-start))
			   + " MBytes/s");

		start = System.currentTimeMillis();

		for (int i=0;i<count2;i++) {
		    mout.writeByte(1);
		    mout.flush();
		    min.readByte();
		}

		end = System.currentTimeMillis();

		System.err.println("Write took "
			   + (end-start) + " ms.  => 1 byte latency : "
			   + ((1000.0*(end-start))/count2) + " us. ");

		byte [] b = new byte[1024*1024];

		for (int warmup = 0; warmup < 2; warmup++) {
		    start = System.currentTimeMillis();

		    for (int i=0;i<count3;i++) {
			mout.write(b);
		    }

		    mout.flush();
		    min.readByte();

		    end = System.currentTimeMillis();

		    System.err.println("Bytes written "
			       + (1024*1024*count3)
			       + " throughput = "
			       + ((1000.0*(1024*1024*count3)/(1024*1024))/(end-start))
			       + " MBytes/s");

		    mout.reset();

		    start = System.currentTimeMillis();

		    for (int i=0;i<count3;i++) {
			mout.writeObject(b);
		    }

		    mout.flush();

		    min.readByte();

		    end = System.currentTimeMillis();

		    System.err.println("Bytes written "
			       + (1024*1024*count3)
			       + " throughput = "
			       + ((1000.0*(1024*1024*count3)/(1024*1024))/(end-start))
			       + " MBytes/s");
		}


		double [] d = new double[128*1024];

		for (int warmup = 0; warmup < 2; warmup++) {

		    start = System.currentTimeMillis();

		    mout.writeObject(d);

		    mout.flush();
		    mout.reset();

		    min.readByte();

		    end = System.currentTimeMillis();

		    System.err.println("Doubles written "
			       + (128*1024)
			       + " throughput = "
			       + ((1000.0*(1024*1024)/(1024*1024))/(end-start))
			       + " MBytes/s");
		}

		s.close();

	    } else {

		Socket s = null;

		while (s == null) {
		    try {
			s = new Socket(info.getHost(0), 1234);
		    } catch (Exception e) {
			Thread.sleep(1000);
			// ignore
		    }
		}

		s.setTcpNoDelay(true);

		ArrayInputStream   in = new BufferedArrayInputStream(s.getInputStream());
		ArrayOutputStream out = new BufferedArrayOutputStream(s.getOutputStream());

		MantaInputStream   min = new MantaTypedBufferInputStream(in);
		MantaOutputStream mout = new MantaTypedBufferOutputStream(out);

		/*
		for (int i=0;i<count;i++) {
		    temp = (Data) min.readObject();
		    System.err.print("-");
		}
		*/

		for (int i=0;i<count;i++) {
		    temp = (Data) min.readObject();
System.err.println("=========================================================");
		}

		mout.writeByte(1);
		mout.flush();

		for (int i=0;i<count2;i++) {
		    min.readByte();
		    mout.writeByte(1);
		    mout.flush();
		}

		byte [] b = new byte[1024*1024];

		for (int warmup = 0; warmup < 2; warmup++) {
		    for (int i=0;i<count3;i++) {
			min.readFully(b);
		    }

		    mout.writeByte(1);
		    mout.flush();

		    for (int i=0;i<count3;i++) {
			b = (byte[])min.readObject();
		    }

		    mout.writeByte(1);
		    mout.flush();
		}

		for (int warmup = 0; warmup < 2; warmup++) {

		    double[] d = (double[])min.readObject();

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



