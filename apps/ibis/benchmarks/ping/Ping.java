import ibis.ipl.*;

import ibis.util.nativeCode.Rdtsc;
import ibis.util.Input;

import java.util.Random;

/**
 * One-way and two-way benchmarking application.
 *
 * Average results are displayed through STDOUT. Individual results are displayed through STDERR.
 * The fields of the 'average results' line are:
 * <OL>
 * <LI> the 'ping' processus rank (0 or 1);
 * <LI> the 'pong' processus rank (1 or 0);
 * <LI> the message size;
 * <LI> the bandwidth expressed in 10^6 bytes;
 * <LI> the bandwidth expressed in 1024^2 bytes;
 * <LI> the latency in microseconds.
 * </OL>
 * The 'individual results' line also display the test cycle number. The test cycle numbered 0 is a warmup test cycle and is ignored when computing the average results.
 */
public final class Ping {

	/**
	 * Flag indicating whether the application should use the
	 * {@link ibis.util.nativeCode.Rdtsc} timer implementation or the
	 * {@link ibis.util.timer implementation}. */
	final static boolean 	    param_rdtsc_timer       = 	      true;

	/**
	 * Flag indicating whether the application should perform one-way
	 * benchmarking or two-way benchmarking.
	 */
	final static boolean 	    param_one_way           = 	      false;

	/**
	 * Flag activating data transfer checking.
	 *
	 * When this flag is set, a specific data pattern is used to mark to buffer before sending.
	 * Once received, the actual buffer content is checked against the expected pattern
	 * to detect missing or incorrect data.
	 * <BR><B>Note</B>: this flag should of course not be set while benchmarking, to avoid
	 * performance biasing!
	 */
	final static boolean 	    param_control_receive   = 	      false;

	/**
	 * Parameter indicating how many measured testing cycles
	 * should be performed for each message size.
	 *
	 * The {@link param_nb_tests} individual results are sent over STDERR.
	 * The average of {@link #param_nb_tests} individual results is sent over STDOUT.
	 *
	 * <BR><B>Note</B>: an additional dummy testing cycle is performed as a warm-up before the
	 * {@link param_nb_tests} testing cycles. 
	 */
	final static int     	    param_nb_tests          = 		  5;

	/**
	 * Parameter indicating the number of sample iteration within one testing cycle.
	 */
	final static int     	    param_nb_samples        = 	       1000;

	/**
	 * The minimum message size.
	 */
	final static int     	    param_min_size          =             1;

	/**
	 * The maximum message size.
	 */
	final static int     	    param_max_size          =     1024*1024;

	/**
	 * The increasing message size step.
	 * If {@link #param_step} value is <code>0</code> the message size is doubled
	 * on each iteration.
	 */
	final static int     	    param_step              = 		  0;

	/**
	 * Flag indicating whether empty messages should automatically be replaced
	 * by one-byte messages
	 */
	final static boolean 	    param_no_zero           = 	      false;

	/**
	 * Flag indicating whether the buffer should be filled before sending.
	 */
	final static boolean 	    param_fill_buffer       = 	       true;

	/**
	 * The value to use for filling the buffer if {@link #param_fill_buffer} is set.
	 * <BR><B>Note</B>: if the {@link #param_control_receive} flag is set,
	 * the buffer is actually filled using a specific data pattern.
	 */
	final static byte    	    param_fill_buffer_value = 		  1;


	/* Internal variables */
	final static Timer	    timer                   = (param_rdtsc_timer)?(Timer)new Rdtsc():(Timer)new ibis.util.Timer();
	final static byte[]  	    buffer	   	    = new byte[param_max_size];
	static 	     int     	    rank      		    =		  0;
	static 	     int     	    remoteRank		    =		  1;
	static       SendPort       sport                   =          null;
	static       ReceivePort    rport                   =          null;
	static 	     Ibis     	    ibis       	      	    =	       null;
	static 	     Registry 	    registry   	      	    =	       null;
	

	static void mark_buffer(int len) {
		byte n = 0;
		int  i = 0;
		
		for (i = 0; i < len; i++) {
			n += 7;			
			buffer[i] = n;
		}
	}

	static void clear_buffer() {
		int i = 0;

		for (i = 0; i < buffer.length; i++) {
			buffer[i] = 0;
		}
	}

	static void control_buffer(int len) {
		boolean ok = true;
		byte    n  = 0;
		int     i  = 0;
		
		for (i = 0; i < len; i++) {
			n += 7;

			if (buffer[i] != n) {
				byte b1 = 0;
				byte b2 = 0;

				b1 = n;
				b2 = buffer[i];
				System.out.println("Bad data at byte "+i+": expected "+b1+", received "+b2);
				ok = false;
			}
		}

		if (!ok) {
			System.out.println(len+" bytes reception failed");
		}
	}

	static void fill_buffer() {
		int i = 0;
		for (i = 0; i < buffer.length; i++) {
			buffer[i] = param_fill_buffer_value;
		}
	}

	static void dispTestResult(int test, int _size, double result) {
		double millionbyte = (_size * (double)param_nb_samples) / (result / (2 - (param_one_way?1:0)));
		double megabyte    = millionbyte / 1.048576;
		double latency     = result / param_nb_samples / (2 - (param_one_way?1:0));
		
		System.err.println("* test "+test+": "+rank+" "+remoteRank+" "+_size+" "+millionbyte+" "+megabyte+" "+latency);
	}
	
	static void dispAverageResult(int _size, double result) {
		double millionbyte = (_size * (double)param_nb_tests * (double)param_nb_samples) / (result / (2 - (param_one_way?1:0)));
		double megabyte    = millionbyte / 1.048576;
		double latency     = result / param_nb_tests / param_nb_samples / (2 - (param_one_way?1:0));
		
		System.out.println("= "+rank+" "+remoteRank+" "+_size+" "+millionbyte+" "+megabyte+" "+latency);
	}
	


	static double oneWayPing(final int _size) throws Exception {
		double sum      = 0.0;
		int    nb_tests = param_nb_tests + 1;

		while ((nb_tests--) > 0) {
			int nb_samples = param_nb_samples;

			double time = System.currentTimeMillis();
			timer.reset();
			timer.start();
			if (param_control_receive) {
				mark_buffer(_size);
			}

			while ((nb_samples--) > 0) {
				WriteMessage out = sport.newMessage();
				out.writeSubArrayByte(buffer, 0, _size);
				out.send();
				out.finish();
			}

			if (param_control_receive) {
				clear_buffer();
			}

			ReadMessage in = rport.receive();

			if (param_control_receive) {
				in.readSubArrayByte(buffer, 0, _size);
				in.finish();
				control_buffer(_size);
			}
			else {
				in.finish();
			}			

			time = 1000 * (System.currentTimeMillis() - time);
			timer.stop();
			double _time =  timer.totalTimeVal();

			dispTestResult(param_nb_tests - nb_tests, _size, time);
			dispTestResult(param_nb_tests - nb_tests, _size, _time);
			System.err.println();
			
			if ((param_nb_tests - nb_tests) > 0) {
				sum += time;
			}
		}
		
		return sum;
	}

	static double twoWayPing(final int _size) throws Exception {
		double sum      = 0.0;
		int    nb_tests = param_nb_tests + 1;

		while ((nb_tests--) > 0) {
			int  nb_samples = param_nb_samples;

			double time = System.currentTimeMillis();
			timer.reset();
			timer.start();
			while ((nb_samples--) > 0) {
				if (param_control_receive) {
					mark_buffer(_size);
				}

				WriteMessage out = sport.newMessage();
				out.writeSubArrayByte(buffer, 0, _size);
				out.send();
				out.finish();

				if (param_control_receive) {
					clear_buffer();
				}

				ReadMessage in = rport.receive();
				in.readSubArrayByte(buffer, 0, _size);
				in.finish();

				if (param_control_receive) {
					control_buffer(_size);
				}
			}
			timer.stop();
			time = 1000 * (System.currentTimeMillis() - time);

			double _time = timer.totalTimeVal();
			dispTestResult(param_nb_tests - nb_tests, _size, time);
			dispTestResult(param_nb_tests - nb_tests, _size, _time);
			System.err.println();
			if ((param_nb_tests - nb_tests) > 0) {
				sum += time;
			}
		}

		return sum;
	}
	
	
	static void oneWayPong(final int _size) throws Exception {
		int nb_tests = param_nb_tests + 1;

		while ((nb_tests--) > 0) {
			int nb_samples = param_nb_samples;;

			while ((nb_samples--) > 0) {
				ReadMessage in = rport.receive();
				in.readSubArrayByte(buffer, 0, _size);
				in.finish();
			}
			
			WriteMessage out = sport.newMessage();

			if (param_control_receive) {
				out.writeSubArrayByte(buffer, 0, _size);
			}

			out.send();
			out.finish();
		}
	}
	
	
	static void twoWayPong(final int _size) throws Exception {
		int nb_tests = param_nb_tests + 1;

		while ((nb_tests--) > 0) {
			int nb_samples = param_nb_samples;;

			while ((nb_samples--) > 0) {
				ReadMessage in = rport.receive();
				in.readSubArrayByte(buffer, 0, _size);
				in.finish();

				WriteMessage out = sport.newMessage();
				out.writeSubArrayByte(buffer, 0, _size);
				out.send();
				out.finish();
			}
		}
	}

	static void ping() throws Exception {
		if (param_fill_buffer) {
			fill_buffer();
		}

		int size = 0;
		for (size = param_min_size; size <= param_max_size; size = (param_step != 0)?size + param_step:size * 2) {
			final int _size = (size == 0 && param_no_zero)?1:size;
			double    result  = 0;

			System.err.println("--------------------");
			System.err.println("size = "+_size+"\n");

			if (param_one_way) {
				result = oneWayPing(_size);
			} else {		      
				result = twoWayPing(_size);
			}

			System.err.println();
			dispAverageResult(_size, result);
		}
	}

	static void pong() throws Exception {
		int size = 0;

		for (size = param_min_size; size <= param_max_size; size = (param_step != 0)?size + param_step:size * 2) {
			final int _size = (size == 0 && param_no_zero)?1:size;

			if (param_one_way) {
				oneWayPong(_size);
			} else {	   
				twoWayPong(_size);
			}			
		}
	}

	static ReceivePortIdentifier lookup(String name) throws Exception {
		ReceivePortIdentifier temp = null;

		do {
			temp = registry.lookup(name);

			if (temp == null) {
				try {
					Thread.sleep(500);
				} catch (Exception e) {
					// ignore
				}
			}
			
		} while (temp == null);
				
		return temp;
	} 

	static void connect(SendPort s, ReceivePortIdentifier ident) {
		boolean success = false;
		do {
			try {
				s.connect(ident);
				success = true;
			} catch (Exception e) {
				System.err.println(e.getMessage());
				e.printStackTrace();
				
				try {
					Thread.sleep(500);
				} catch (Exception e2) {
					// ignore
				}
			}
		} while (!success);
	}

	static void master() throws Exception  {
		if (param_one_way) {
			ping();
			pong();
		} else {
			ping();
		}		
	}
	
	static void slave() throws Exception  {
		if (param_one_way) {
			pong();
			ping();
		} else {
			pong();
		}
	}
	
	private static String readKey(Input in) {
		// Skip comment lines starting with a '#' at col 0
		if (!in.eof() && !in.eoln() && in.nextChar() == '#') {
			return null;
		}

		// Skip empty lines
		if (in.eoln()) {
			return null;
		}

		StringBuffer s = new StringBuffer();
		while (!in.eof() && !in.eoln() && in.nextChar() != '=') {
			s.append(in.readChar());
		}

		return s.toString();
	}

	private static String readVal(Input in) {
		StringBuffer s = new StringBuffer();
		while (!in.eof() && !in.eoln()) {
			s.append(in.readChar());
		}

		return s.toString();
	}
	private static void readProperties(Input in, StaticProperties sp) {
		while(!in.eof()) {
			String key = readKey(in);

			if (key == null) {
				in.readln();
				continue;
			}
			
			in.readChar();
			in.skipWhiteSpace();
			String val = readVal(in);
			in.readln();

			try {
				// set to 'true' to debug property extraction
				if (false)
					System.out.println("adding: ["+key+"]='"+val+"'");
				sp.add(key, val);
			} catch (Exception e) {
				System.err.println("error adding property (" + key + "," + val + ")");
				System.exit(1);
			}
		}
	}

	public static void main(String [] args) { 
		try {
			// Local initialization
			
			String id   = "ibis:" + (new Random()).nextInt();
			//String name = "ibis.ipl.impl.tcp.TcpIbis";
			String name = "ibis.ipl.impl.net.NetIbis";
			ibis = Ibis.createIbis(id, name, null);

			// Configuration information
			registry = ibis.registry();
			IbisIdentifier master = (IbisIdentifier) registry.elect("latency", ibis.identifier());

			if(master.equals(ibis.identifier())) {
				rank       = 0;
				remoteRank = 1;
			} else {
				rank       = 1;
				remoteRank = 0;
			}

			// Local communication setup
			StaticProperties s = new StaticProperties();

			/*
			 * Attempt to read send/receive port settings from the 'ping_port_type'
			 */
			try {
				Input in = new Input("ping_port_type");
				readProperties(in, s);
			} catch (Exception e) {
				// nothing
			}

			PortType t = ibis.createPortType("ping", s);
			sport  	   = t.createSendPort();
			rport  	   = null;


			// Connection setup and test
			if (rank == 0) { 
				rport = t.createReceivePort("ping 0");
				rport.enableConnections();
				ReceivePortIdentifier ident = lookup("ping 1");
				connect(sport, ident);

				master();
			} else { 
				ReceivePortIdentifier ident = lookup("ping 0");
				connect(sport, ident);
				rport = t.createReceivePort("ping 1");
				rport.enableConnections();

				slave();
			}
                        sport.free();
                        rport.free();
			ibis.end();

		} catch (Exception e) { 
			System.out.println("Got exception " + e);
			System.out.println("StackTrace:");
			e.printStackTrace();
		}
	}
}
