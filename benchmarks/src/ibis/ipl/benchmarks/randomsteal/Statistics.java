package ibis.ipl.benchmarks.randomsteal;

import java.io.Serializable;

public class Statistics implements Serializable {

	private static final long serialVersionUID = 1416698109821581926L;

	public long time;
	public long stealRequests;
	
	public long connectionFailed;
	public long alreadyConnected;
	
	public Statistics() { 
		// empty
	}
	
	public Statistics(Statistics s) { 
		time = s.time;
		stealRequests = s.stealRequests;
		connectionFailed = s.connectionFailed;
		alreadyConnected = s.alreadyConnected;		
	}
	
	public synchronized long getAlreadyConnected() {
		return alreadyConnected;
	}

	public synchronized void addAlreadyConnected() {
		this.alreadyConnected++;
	}

	public synchronized long getStealRequests() {
		return stealRequests;
	}

	public synchronized void addStealRequest() {
		this.stealRequests++;
	}

	public synchronized long getConnectionFailed() {
		return connectionFailed;
	}

	public synchronized void addConnectionFailed() {
		this.connectionFailed++;
	}

	public synchronized long getTime() {
		return time;
	}

	public synchronized void setTime(long time) {
		this.time = time;
	}
	
	public synchronized void add(Statistics s) { 
		time += s.time;
		
		stealRequests += s.stealRequests;
		connectionFailed += s.connectionFailed;
		alreadyConnected += s.alreadyConnected;		
	}
	
	public synchronized void div(int n) { 
		time = time/n;
		
		stealRequests = stealRequests/n;
		connectionFailed = connectionFailed/n;
		alreadyConnected = alreadyConnected/n;		
	}
	
	public synchronized void reset() {
		time = 0;
		
		stealRequests = 0;
		connectionFailed = 0;
		alreadyConnected = 0;		
	}
	
	public static Statistics sum(Statistics [] in) { 
		
		Statistics sum = new Statistics();
		
		for (Statistics s : in) { 
			System.out.println("$$$$$$$$ SUM " + s.time + " "+ s.stealRequests);
			sum.add(s);
		}
		
		return sum;
	}
	
	public static Statistics avg(Statistics [] in) { 
		Statistics sum = sum(in);
		sum.div(in.length);
		return sum;
	}	
	
	public synchronized String getStatistics(String header, int nodes) { 
		
		double stealsPerSecond     = (stealRequests * 1000.0) / (((double)time)/nodes);
    	double stealsPerSecondNode = ((stealsPerSecond * 1000.0) / nodes) / (((double)time)/nodes);
    	double failedPerNode   = connectionFailed / nodes;
    	double allreadyPerNode = alreadyConnected / nodes;
        	
    	return header + "- Avg time: " + (((double)time) / nodes) + " ms. (" 
    			+ time + " " + stealRequests + " " + nodes + ") "
    			+ stealsPerSecondNode + " st/sec/n, " + stealsPerSecond + " st/sec, " +
    		    + connectionFailed + " / " + alreadyConnected + " failed/race " 
    		    + failedPerNode + " / " + allreadyPerNode + " (failed/races) / node";
   }
	
}
