/*
 * Created on 12.03.2005
 */
package ibis.mpj;


import java.io.*;
import java.util.*;

/**
 * Implementation of the collective operation: split.
 */
public class ColSplit {
	
	private int colour = 0;
	private int key = 0;
	private Intracomm comm = null;
	private int tag = 0;
	
	public ColSplit(int colour, int key, Intracomm comm, int tag) throws MPJException {
		this.colour = colour;
		this.key = key;
		this.comm = comm;
		this.tag = tag;
		
	}
	
	protected Intracomm call() throws MPJException {
		int size = this.comm.size();
		int rank = this.comm.rank();
		SplitItem[] localItem = new SplitItem[1];
		localItem[0] = new SplitItem(colour, key, rank);
		SplitItem[] gatheredTable = new SplitItem[size];
		
		for (int i = 0; i < size; i++) {
			gatheredTable[i] = new SplitItem();
		}
		
		
		this.comm.allgather(localItem, 0, 1, MPJ.OBJECT, gatheredTable, 0, 1, MPJ.OBJECT);
		
		
		if (gatheredTable[rank].colour == MPJ.UNDEFINED) {
			return(null);
		}
		else {
			Arrays.sort(gatheredTable, new SplitComp());
			
			
			Group oldGroup = this.comm.group();
			
			int count =0;
			int[] newRanks = new int[size];
			for (int i = 0; i < size; i++) {
				
				if (gatheredTable[i].colour == colour) {
					count++;
					
					newRanks[count-1] = gatheredTable[i].rank;
					
				}
			}
			
			int[] newRanksSized = new int[count];
			System.arraycopy(newRanks, 0, newRanksSized, 0, count);
			
			Group newGroup = oldGroup.incl(newRanksSized);
			
			Intracomm newComm = new Intracomm();
			
			int[] newContextId = new int[1];
			int[] redContextId = new int[1];
			newContextId[0] = MPJ.getNewContextId();
			
			this.comm.allreduce(newContextId, 0, redContextId, 0, 1, MPJ.INT, MPJ.MAX);
			
			MPJ.setNewContextId(redContextId[0]);
			newComm.contextId = redContextId[0];
			
			newComm.group = newGroup;
			
			return(newComm);
		}
	}
}

class SplitItem implements Serializable {
	public int colour = 0;
	public int key = 0;
	public int rank = 0;
	
	public SplitItem() {
		
	}
	
	public SplitItem(int colour, int key, int rank) {
		this.colour = colour;
		this.key = key;
		this.rank = rank;
	}
}

class SplitComp implements Comparator {
	public int compare(Object o1, Object o2) {
		if (((SplitItem)o1).key < ((SplitItem)o2).key) {
			return(-1);
		}
		else if (((SplitItem)o1).key == ((SplitItem)o2).key) {
			return(0);
		}
		else {
			return(1);
		}
		
		
	}
}