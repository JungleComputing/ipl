import java.util.Random;
import javatimer.*;
import java.io.*;
import java.net.*;

final class TTReceiver extends Thread {
	TranspositionTable tt;
	DataInputStream in;

	int[] bufindex = new int[tt.BUF_SIZE];
	long[] buftags = new long[tt.BUF_SIZE]; // index bits are redundant...
	short[] bufvalues = new short[tt.BUF_SIZE];
	short[] bufbestChildren = new short[tt.BUF_SIZE];
	byte[] bufdepths = new byte[tt.BUF_SIZE];
	boolean[] buflowerBounds = new boolean[tt.BUF_SIZE];

	TTReceiver(TranspositionTable tt, Socket s) {
		this.tt = tt;

		try {
			InputStream is = s.getInputStream();
			BufferedInputStream bi = new BufferedInputStream(is, 60*1024);
			in = new DataInputStream(bi);
		} catch (Exception e) {
			System.err.println("Exc in TTReceiver ctor: " + e);
		}
	}

	public void run() {
		while (true) {
			try {
				int opcode = in.readByte();
				if(opcode < 0) {
//					System.err.println(tt.rank + ": exit TTReceiver");
					in.close();
					return;
				}
//				System.err.println(tt.rank + ": receive tt buf");

				for (int i=0; i<tt.BUF_SIZE; i++) {
					bufindex[i] = in.readInt();
				}

				for (int i=0; i<tt.BUF_SIZE; i++) {
					buftags[i] = in.readLong();
				}

				for (int i=0; i<tt.BUF_SIZE; i++) {
					bufvalues[i] = in.readShort();
				}

				for (int i=0; i<tt.BUF_SIZE; i++) {
					bufbestChildren[i] = in.readShort();
				}

//				in.read(bufdepths, 0, tt.BUF_SIZE);

				for (int i=0; i<tt.BUF_SIZE; i++) {
					bufdepths[i] = in.readByte();
				}

				for (int i=0; i<tt.BUF_SIZE; i++) {
					buflowerBounds[i] = in.readBoolean();
				}

				tt.remoteStore(bufindex, buftags, bufvalues, bufbestChildren, bufdepths, buflowerBounds);
//				System.err.println(tt.rank + ": receive of tt buf DONE");
			} catch (java.io.EOFException e) {
				return;
			} catch (Exception e) {
				System.err.println("Exc in TTReceiver: " + e);
				return;
			}
		}
	}
}
