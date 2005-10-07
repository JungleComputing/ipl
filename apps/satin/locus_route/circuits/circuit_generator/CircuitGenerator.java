
import java.util.Random;

public class CircuitGenerator {
    
    private static void usage() {
	System.err.println("Usage: CircuitGenerator " 
			   + "<number of routing channels> "
			   + "<width of routing channel> "
			   + "<number of wires>");
	System.exit(1);
    }
    
    public static void main(String[] args) {

	int numOfRoutingChannels = 0;
	int widthOfRoutingChannels = 0;
	int numOfWires = 0;

	Random random;
	int begin_x, begin_y, end_x, end_y;

	if (args.length < 3) {
	    usage();
	}

	try {
	    numOfRoutingChannels = Integer.parseInt(args[0]);
	    widthOfRoutingChannels = Integer.parseInt(args[1]);
	    numOfWires = Integer.parseInt(args[2]);
	} catch (NumberFormatException e) {
	    usage();
	}

	random = new Random(System.currentTimeMillis());
	//print to stdout
	System.out.println(numOfRoutingChannels + " " 
			   + widthOfRoutingChannels);
	for (int i = 0; i < numOfWires; i++) {
	    begin_x = random.nextInt(numOfRoutingChannels);
	    begin_y = random.nextInt(widthOfRoutingChannels);
	    end_x = random.nextInt(numOfRoutingChannels);
	    end_y = random.nextInt(numOfRoutingChannels);
	    //order the end-points
	    if (begin_y > end_y) {
		int temp1 = begin_y;
		begin_y = end_y;
		end_y = temp1;
		int temp2 = begin_x;
		begin_x = end_x;
		end_x = temp2;
	    }
	    System.out.println(begin_y + " " + begin_x + " "
			       + end_y + " " + end_x);
	}

    }
	
}
