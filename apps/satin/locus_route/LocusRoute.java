import ibis.satin.SatinObject;
import java.io.*;
import java.util.*;

/** This is a simplified version of the LocusRoute application. 
 *  Wires have only two end-points and thus do not consist of 
 *  multiple segments. Only vertical height, i.e. height of the
 *  routing channels is used as a measure of area. The height of
 *  a routing channel is defined as the maximum number of wires 
 *  running horizontally through any point of the channel. Currently
 *  only wire-level parallelism is used. Route-level parallelism 
 *  will be added later
 */

class LocusRoute extends SatinObject implements LocusRouteInterface {

    public final static int WIRE_SPAWN_THRESHOLD = 1; 
    public final static int ROUTE_SPAWN_THRESHOLD = 5;

    static int numOfRoutingChannels;
    static int widthOfRoutingChannels;

    public LocusRoute() {};


    /*returns the first half of the list*/
    private LinkedList firstHalf(LinkedList list) {
	LinkedList newList = new LinkedList();
	for (int i = 0; i < list.size() / 2; i++) {
	    newList.add(list.get(i));
	}
	return newList;
    }

    /*returns the second half of the list*/
    private LinkedList secondHalf(LinkedList list) {
	LinkedList newList = new LinkedList();
	for (int i = list.size() / 2; i < list.size(); i++) {
	    newList.add(list.get(i));
	}
	return newList;
    }

    private LinkedList merge(LinkedList list1, LinkedList list2) {
	LinkedList newList = new LinkedList();
	newList.addAll(list1);
	newList.addAll(list2);
	return newList;
    }

    /*spawnable*/
    public LinkedList computeWires(LinkedList wires, CostArray costArray) {
    
	if (wires.size() <= WIRE_SPAWN_THRESHOLD) {
	    /*compute wires sequentially*/
	    for (int i=0; i<wires.size(); i++) {
		Wire wire = (Wire) wires.get(i);
		computeWire(wire, costArray);
	    }
	    return wires;
	} else {
	    /*this is the most simple version, to exploit locality, do a sort of
	      two dimensional quick sort here: divide the wire list into four parts
	      corresponding to four subareas of the cicuit*/
	    LinkedList wires1, wires2;
	    wires1 = /*spawn*/computeWires(firstHalf(wires), costArray);
	    wires2 = /*spawn*/computeWires(secondHalf(wires), costArray);
	    sync();
	    LinkedList result = merge(wires1, wires2);
	    return result;
	}
    }
    
    void computeWire(Wire wire, CostArray costArray) {
	
	if ( wire.bend1_x != -1 ) {
	    costArray.ripOutWire(wire);
	}
	evaluateRoutes(wire, costArray);
	/*	System.err.println("Wire: "
			   + wire.begin_x + "," + wire.begin_y + "-"
			   + wire.end_x + "," + wire.end_y + ", bends: "
			   + wire.bend1_x + "," + wire.bend1_y + " " 
			   + wire.bend2_x + "," + wire.bend2_y);*/
	costArray.placeWire(wire);
    }

    private int min(int a, int b) {
	if (a <= b) {
	    return a;
	} else {
	    return b;
	}
    }

    private int max(int a, int b) {
	if (a >= b) {
	    return a;
	} else {
	    return b;
	}
    }

    void evaluateRoutes(Wire wire, CostArray costArray) {

	int bestRoute = Integer.MAX_VALUE;
	int currentRoute;
	int min, max;

	if (wire.begin_x == wire.end_x || wire.begin_y == wire.end_y) {
	    wire.place(wire.begin_x, wire.begin_y, wire.end_x, wire.end_y);
	}

	/* principially horizontal routes*/
	min = min(wire.begin_x, wire.end_x);
	max = max(wire.begin_x, wire.end_x);
	for (int i=min+1; i<=max; i++) {
	    currentRoute = costArray.evaluateRoute(wire,
						   i, wire.begin_y,
						   i, wire.end_y);
	    if (currentRoute < bestRoute) {
		bestRoute = currentRoute;
		wire.place(i, wire.begin_y, i, wire.end_y);
	    }
	}

	/* principially vertical routes*/
	min = min(wire.begin_y, wire.end_y);
	max = max(wire.begin_y, wire.end_y);
	for (int j=min+1; j<=max; j++) {
	    currentRoute = costArray.evaluateRoute(wire,
						   wire.begin_x, j,
						   wire.end_x, j);
	    if (currentRoute < bestRoute) {
		bestRoute = currentRoute;
		wire.place(wire.begin_x, j, wire.end_x, j);
	    }
	}
	
    }


    static void readInputFile(String inputFileName, LinkedList wires) {

	FileInputStream fis = null;
	InputStreamReader isr = null;
	Reader r = null;
	StreamTokenizer st = null;
	int label = 0;

	try {

	    fis = new FileInputStream(inputFileName);
	    isr = new InputStreamReader(fis);
	    r = new BufferedReader(isr);
	    st = new StreamTokenizer(r);

	} catch (FileNotFoundException e) {
	    System.err.println("File not found: " + inputFileName);
	    System.exit(1);
	}

	st.parseNumbers();
	st.slashSlashComments(true);
	st.slashStarComments(true);

	try {

	    if (st.nextToken() == StreamTokenizer.TT_EOF) {
		System.err.println("Invalid input file format: "
				   + "line " + st.lineno() + ": "
				   + "number of routing channels "
				   + "missing");
		System.exit(1);
	    }
	    numOfRoutingChannels = (int) st.nval + 1;
	    if (st.nextToken() == StreamTokenizer.TT_EOF) {
		System.err.println("Invalid input file format: "
				   + "line " + st.lineno() + ": "
				   + "width of routing channel "
				   + "missing");
		System.exit(1);
	    }      
	    widthOfRoutingChannels = (int) st.nval;
	    
	    while (st.nextToken() != StreamTokenizer.TT_EOF) {
		int begin_y = (int) st.nval;
		if (begin_y < 0 || begin_y >= widthOfRoutingChannels) {
		    System.err.println("Line " + st.lineno() 
				       + ": Wire coordinates outside circuit");
		    System.exit(1);
		}
		if (st.nextToken() == StreamTokenizer.TT_EOF) {
		    System.err.println("Invalid input file format: "
				       + "line " + st.lineno() + ": "
				       + "coordinate of a wire missing");
		    System.exit(1);
		}
		int begin_x = (int) st.nval;
		if (begin_x < 0 || begin_x >= numOfRoutingChannels) {
		    System.err.println("Line " + st.lineno() 
				       + ": Wire coordinates outside circuit");
		    System.exit(1);
		}
		if (st.nextToken() == StreamTokenizer.TT_EOF) {
		    System.err.println("Invalid input file format: "
				       + "line " + st.lineno() + ": "
				       + "coordinate of a wire missing");
		    System.exit(1);
		}
		int end_y = (int) st.nval;
		if (end_y < 0 || end_y >= widthOfRoutingChannels) {
		    System.err.println("Line " + st.lineno() 
				       + ": Wire coordinates outside circuit");
		    System.exit(1);
		}
		if (st.nextToken() == StreamTokenizer.TT_EOF) {
		    System.err.println("Invalid input file format: "
				       + "line " + st.lineno() + ": "
				       + "coordinate of a wire missing");
		    System.exit(1);
		}
		int end_x = (int) st.nval;
		if (end_x < 0 || end_x >= numOfRoutingChannels) {
		    System.err.println("Line " + st.lineno() 
				       + ": Wire coordinates outside circuit");
		    System.exit(1);
		}
		Wire wire = new Wire(label++, begin_x, begin_y, end_x, end_y);
		wire.place(-1, -1, -1, -1);
		wires.add(wire);
	    }

	} catch (IOException e) {
	    System.err.println("LocusRoute: Error reading input file: "
			       + e.getMessage());
	    System.exit(1);
	}
	 
	try {
	    r.close();
	    isr.close();
	    fis.close();
	} catch (IOException e) {
	    System.err.println("LocusRoute: Error closing input file: "
			       + e.getMessage());
	}	
		    
    }

    public static void main(String[] args) {

	int numIterations;
	LinkedList wires;
	int globalCost;
	CostArray costArray;
	LocusRoute locusRoute;
	
	//parse arguments
	if (args.length < 2) {
	    System.err.println("Usage: LocusRoute <num_iterations> <input_file>");
	    System.exit(1);
	}
	
	numIterations = Integer.parseInt(args[0]);
	wires = new LinkedList();

	readInputFile(args[1], wires);
	
	costArray = new CostArray(numOfRoutingChannels,
				  widthOfRoutingChannels,
				  wires.size());
	costArray.exportObject();

	locusRoute = new LocusRoute();

	System.out.println("LocusRoute: routing " + wires.size() + " wires, "
			   + "circuit size: " + numOfRoutingChannels + "x"
			   + widthOfRoutingChannels + ", iterations: "
			   + numIterations);

	for (int i=0; i<numIterations; i++) {
	    
	    wires = /*spawn*/locusRoute.computeWires(wires, costArray);
	    locusRoute.sync();
	    System.out.println("num of wires: " + wires.size());	  
	    
	    globalCost = costArray.globalCost();
	    System.out.println("LocusRoute: Global cost in iteration " 
			       + i + ": " + globalCost);
	}

	/* recalculate the cost array because the shared object might not be consistent */
	costArray.clear();
	for (int i=0; i<wires.size(); i++) {
	    Wire wire = (Wire) wires.get(i);
	    costArray.placeWire_locally(wire);
	}
	globalCost = costArray.globalCost();
	System.out.println("LocusRoute: Final global cost: " + globalCost);
	    
    }
}
    
