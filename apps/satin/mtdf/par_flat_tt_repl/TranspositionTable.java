import java.rmi.*;
import java.rmi.registry.*;
import java.rmi.server.*;
import java.util.Random;
import javatimer.*;

import ibis.util.PoolInfo;

final class TranspositionTable extends UnicastRemoteObject implements TranspositionTableIntr {

	static final boolean SUPPORT_TT = true;

	static final int SIZE = 1 << 22;
	static final int BUF_SIZE = 10000;

	static int lookups = 0, hits = 0, sorts = 0, stores = 0,
		overwrites = 0, visited = 0, scoreImprovements = 0, cutOffs = 0,
		bcasts = 0;

	static final byte LOWER_BOUND = 1;
	static final byte UPPER_BOUND = 2;
	static final byte EXACT_BOUND = 3;

	int poolSize;
	int rank;

	long[] tags = new long[SIZE]; // index bits are redundant...
	short[] values = new short[SIZE];
	short[] bestChildren = new short[SIZE];
	byte[] depths = new byte[SIZE];
	boolean[] lowerBounds = new boolean[SIZE];
	boolean[] valid = new boolean[SIZE];

	int bufCount = 0;
	int[] bufindex = new int[BUF_SIZE];
	long[] buftags = new long[BUF_SIZE]; // index bits are redundant...
	short[] bufvalues = new short[BUF_SIZE];
	short[] bufbestChildren = new short[BUF_SIZE];
	byte[] bufdepths = new byte[BUF_SIZE];
	boolean[] buflowerBounds = new boolean[BUF_SIZE];

	TranspositionTableIntr[] others;

	Timer bcastTimer = new Timer();

	TranspositionTable() throws RemoteException {
		Random random = new Random();
		Registry r = null;

		PoolInfo info = PoolInfo.createPoolInfo();
		rank = info.rank();
		poolSize = info.size();

		System.err.println("hosts = " + poolSize + ", rank = " + rank);

		try {
			r = LocateRegistry.createRegistry(5555);
		} catch (Exception e) {
			System.err.println(rank + ": failed to create registry: " + e);
			try {Thread.sleep(1000);} catch (Exception x) {}
		}

		while (true) {
			try {
				r.bind("TT", this);
				break;
			} catch (Exception e) {
				System.err.println(rank + ": bind error: " + e);
//				e.printStackTrace();
				try {Thread.sleep(1000);} catch (Exception x) {}
//System.exit(1);
			}
		}
		System.err.println(rank + ": bound my remote obj");

		// OK, all are there, now do lookups...
		others = new TranspositionTableIntr[poolSize-1];
		int index = 0;
		for(int i=0; i<poolSize; i++) {
			if (i != rank) {
				while(true) {
					try {
						others[index] = (TranspositionTableIntr) Naming.lookup(
							"//" + info.hostName(i) + ":5555/TT");
						index++;
						break;
					} catch (Exception e) {
						System.err.println(rank + ": could not do lookup: " + e);
//						System.exit(1);
						try {Thread.sleep(1000);} catch (Exception x) {}
					}
				}
			}
		}

		System.err.println(rank + ": got all remote refs");
	}

	synchronized int lookup(long signature) {
		lookups++;
		if(SUPPORT_TT) {
			int index = (int)signature & (SIZE-1);
			if(valid[index]) return index;
			return -1;
		} else {
			return -1;
		}
	}

	void store(long tag, short value, short bestChild, byte depth, boolean lowerBound) {
		if(!SUPPORT_TT) return;

		int index = (int)tag & (SIZE-1);

		if(localStore(index, tag, value, bestChild, depth, lowerBound)) {
			forwardStore(index, tag, value, bestChild, depth, lowerBound);
		}
	}

	synchronized boolean localStore(int index, long tag, short value, short bestChild, byte depth, boolean lowerBound) {
		stores++;

		if(valid[index] && depth < depths[index]) {
			return false;
		}

		overwrites++;

		tags[index] = tag;
		values[index] = value;
		bestChildren[index] = bestChild;
		depths[index] = depth;
		lowerBounds[index] = lowerBound;
		valid[index] = true;

		return true;
	}

	void doRemoteStore(int index, long tag, short value, short bestChild, byte depth, boolean lowerBound) {
		stores++;

		if(valid[index] && depth < depths[index]) {
			return;
		}

		overwrites++;

		tags[index] = tag;
		values[index] = value;
		bestChildren[index] = bestChild;
		depths[index] = depth;
		lowerBounds[index] = lowerBound;
		valid[index] = true;
	}

	private void forwardStore(int index, long tag, short value, short bestChild, byte depth, boolean lowerBound) {
		bufindex[bufCount] = index;
		buftags[bufCount] = tag;
		bufvalues[bufCount] = value;
		bufbestChildren[bufCount] = bestChild;
		bufdepths[bufCount] = depth;
		buflowerBounds[bufCount] = lowerBound;
		bufCount++;

		// ok, send it off
		if(bufCount == BUF_SIZE) {
//			System.err.println("doing bcast of tt buf");
			bcastTimer.start();
			bcasts++;
			for(int i=0; i<others.length; i++) {
				try {
					others[i].remoteStore(bufindex, buftags, bufvalues, bufbestChildren, bufdepths, buflowerBounds);
				} catch (Exception e) {
					System.err.println("eek, rmi failed");
					System.exit(1);
				}
			}
			bufCount = 0;
			bcastTimer.stop();
		}
	}

	protected void finalize() throws Throwable {
		System.err.println("tt: lookups: " + lookups + ", hits: " + hits + ", sorts: " + sorts +
				   ", stores: " + stores + ", overwrites: " + overwrites + 
				   ", score incs: " + scoreImprovements + 
				   ", bcasts: " + bcasts + ", bcast time: " + bcastTimer.totalTime() + ", bcast avg: " + bcastTimer.averageTime() + ", cutoffs: " + cutOffs + ", visited: " + visited);
	}

	public synchronized void remoteStore(int[] aindex, long[] atag, short[] avalue, short[] abestChild, byte[] adepth, boolean[] alowerBound)
	    throws RemoteException {
//		System.err.println("received bcast of tt buf");
		for(int i=0; i<aindex.length; i++) {
			doRemoteStore(aindex[i], atag[i], avalue[i], abestChild[i], adepth[i], alowerBound[i]);
		}
//		System.err.println("received bcast of tt buf handled");
	}
}
