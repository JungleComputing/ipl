import java.io.InputStream;
import java.io.OutputStream;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import java.net.Socket;
import java.net.ServerSocket;

import ibis.util.PoolInfo;

public class Main { 

	public static void main(String args[]) { 
			       	       
		try { 
			Data temp = null;
			long start, end;

			int count = 10000;
			int len = 100;

			int count2 = 10000;

			int count3 = 10;

			PoolInfo info = new PoolInfo();

			if (info.rank() == 0) { 
				
				System.err.println("Main starting");

				ServerSocket server = new ServerSocket(1234);
				Socket s = server.accept();

				server.close();

				s.setTcpNoDelay(true);

				InputStream   in = s.getInputStream();
				OutputStream out = s.getOutputStream();

				CountingOutputStream counter = new CountingOutputStream(out);

				ObjectOutputStream mout = new ObjectOutputStream(counter);
				ObjectInputStream   min = new ObjectInputStream(in);
				
				for (int i=0;i<len;i++) { 
					temp = new Data((i+0.8)/1.3, temp);
				} 

				for (int i=0;i<count;i++) { 
					mout.writeObject(temp);					
					mout.flush();
					mout.reset();
				} 

				start = System.currentTimeMillis();
				
				for (int i=0;i<count;i++) { 
					mout.writeObject(temp);					
					mout.flush();
					mout.reset();
				} 

				min.readByte();

				end = System.currentTimeMillis();

				System.err.println("Write took "
						   + (end-start) + " ms.  => " 
						   + ((1000.0*(end-start))/count) + " us/call => " 
						   + ((1000.0*(end-start))/(count*len)) + " us/object");

				System.err.println("Bytes written " 
						   + counter.bytesWritten() 
						   + " throughput = " 
						   + ((1000.0*counter.bytesWritten()/(1024*1024))/(end-start))
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
				s.close();
			
			} else { 

				Socket s = null;

				while (s == null) { 
					try { 
						Thread.sleep(1000);
						s = new Socket(info.hostName(0), 1234);
					} catch (Exception e) {
						// ignore
					} 
				} 

				s.setTcpNoDelay(true);
				
				InputStream   in = s.getInputStream();
				OutputStream out = s.getOutputStream();
				
				ObjectInputStream   min = new ObjectInputStream(in);
				ObjectOutputStream mout = new ObjectOutputStream(out);

				for (int i=0;i<count;i++) { 
					temp = (Data) min.readObject();
				} 

				for (int i=0;i<count;i++) { 
					temp = (Data) min.readObject();
				} 

				mout.writeByte(1);
				mout.flush();

				for (int i=0;i<count2;i++) { 
					min.readByte();
					mout.writeByte(1);					
					mout.flush();
				} 

				byte [] b = new byte[1024*1024];

				for (int i=0;i<count3;i++) { 
					min.readFully(b);					
				} 

				mout.writeByte(1);
				mout.flush();					
				
				s.close();
			}
			
		} catch (Exception e) { 
			System.err.println("Main got exception " + e);
			e.printStackTrace();
		} 
	} 
} 



