
import ibis.satin.so.SharedObject;

class CostArray extends SharedObject implements CostArrayInterface {

    int heigth, width;
    int[][] array;
    int numWires;
    boolean[] wirePlaced;
    
    
    public CostArray(int heigth, int width, int numWires) {
	this.heigth = heigth;
	this.width = width;
	this.numWires = numWires;
	array = new int[heigth][width];        
	wirePlaced = new boolean[numWires];
	
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
    
    /*read method*/
    public int evaluateRoute(Wire wire, int bend1_x, int bend1_y, 
			     int bend2_x, int bend2_y) {
	int value = 0;
	int min, max;
	//1st segment: begin-bend1
	if (wire.begin_x == bend1_x) {
	    //horizontal
	    min = min(wire.begin_y, bend1_y);
	    max = max(wire.begin_y, bend1_y);
	    for (int j = min; j<= max; j++) {
		value += array[wire.begin_x][j];
	    }
	} else if (wire.begin_y == bend1_y) {
	    //vertical
	    //nothing to do for simplified version
	} else {
	    System.err.println("1Invalid bends: wire: "
			       + wire.begin_x + "," + wire.begin_y + "-"
			       + wire.end_x + "," + wire.end_y + ", bends: "
			       + bend1_x + "," + bend1_y + " " 
			       + bend2_x + "," + bend2_y);
	    System.exit(1);
	}
	//2nd segment: bend1-bend2
	if (bend1_x == bend2_x) {
	    //horizontal
	    min = min(bend1_y, bend2_y);
	    max = max(bend1_y, bend2_y);
	    for (int j = min; j<= max; j++) {
		value += array[bend1_x][j];
	    }
	} else if (bend1_y == bend2_y) {
	    //vertical
	    //nothing to do for simplified version
	} else {
	    System.err.println("2Invalid bends: wire: "
			       + wire.begin_x + "," + wire.begin_y + "-"
			       + wire.end_x + "," + wire.end_y + ", bends: "
			       + bend1_x + "," + bend1_y + " " 
			       + bend2_x + "," + bend2_y);
	    System.exit(1);
	}
	//3rd segment: bend2-end
	if (bend2_x == wire.end_x) {
	    //horizontal
	    min = min(bend2_y, wire.end_y);
	    max = max(bend2_y, wire.end_y);
	    for (int j = min; j<= max; j++) {
		value += array[bend2_x][j];
	    }
	} else if (bend2_y == wire.end_y) {
	    //vertical
	    //nothing to do for simplified version
	} else {
	    System.err.println("3Invalid bends: wire: "
			       + wire.begin_x + "," + wire.begin_y + "-"
			       + wire.end_x + "," + wire.end_y + ", bends: "
			       + bend1_x + "," + bend1_y + " " 
			       + bend2_x + "," + bend2_y);
	    System.exit(1);
	}	
	return value;
    }

    /*write method*/
    public void placeWire(Wire wire) {
	placeWire_locally(wire);
    }

    public void placeWire_locally(Wire wire) {    
	int min, max;
	//place wire only if it hasn't been placed
	if (wirePlaced[wire.label]) {
	    return;
	}
	//1st segment: begin-bend1
	if (wire.begin_x == wire.bend1_x) {
	    //horizontal
	    min = min(wire.begin_y, wire.bend1_y);
	    max = max(wire.begin_y, wire.bend1_y);
	    for (int j = min; j<=max; j++) {
		array[wire.begin_x][j] ++;
	    }
	}
	//2nd segment: bend1-bend2
	if (wire.bend1_x == wire.bend2_x) {
	    //horizontal
	    min = min(wire.bend1_y, wire.bend2_y);
	    max = max(wire.bend1_y, wire.bend2_y);	    
	    for (int j = min; j<=max; j++) {
		array[wire.bend1_x][j] ++;
	    }
	}
	//3rd segment: bend2-end
	if (wire.bend2_x == wire.end_x) {
	    //horizontal
	    min = min(wire.bend2_y, wire.end_y);
	    max = max(wire.bend2_y, wire.end_y);
	    for (int j = min; j<=max; j++) {
		array[wire.bend2_x][j] ++;
	    }
	}
	wirePlaced[wire.label] = true;
    }
    
    /*write method*/
    public void ripOutWire(Wire wire) {
	int min, max;
	//rip out wire only if it has been placed
	if (!wirePlaced[wire.label]) {
	    return;
	}
	//1st segment: begin-bend1
	if (wire.begin_x == wire.bend1_x) {
	    //horizontal
	    min = min(wire.begin_y, wire.bend1_y);
	    max = max(wire.begin_y, wire.bend1_y);
	    for (int j = min; j<=max; j++) {
		array[wire.begin_x][j] --;
	    }
	}
	//2nd segment: bend1-bend2
	if (wire.bend1_x == wire.bend2_x) {
	    //horizontal
	    min = min(wire.bend1_y, wire.bend2_y);
	    max = max(wire.bend1_y, wire.bend2_y);	    
	    for (int j = min; j<=max; j++) {
		array[wire.bend1_x][j] --;
	    }
	}
	//3rd segment: bend2-end
	if (wire.bend2_x == wire.end_x) {
	    //horizontal
	    min = min(wire.bend2_y, wire.end_y);
	    max = max(wire.bend2_y, wire.end_y);
	    for (int j = min; j<=max; j++) {
		array[wire.bend2_x][j] --;
	    }
	}
	wirePlaced[wire.label] = false;
    }
    
    /*read method*/
    public int globalCost() {	
	int globalCost = 0;
	for (int i=0; i<heigth; i++) {
	    for (int j=0; j<width; j++) {
		globalCost += array[i][j];
	    }
	}
	return globalCost;
    }

    /*remove all the wires*/
    public void clear() {
	for (int i=0; i<heigth; i++) {
	    for (int j=0; j<width; j++) {
		array[i][j] = 0;
	    }
	}

	for (int k=0; k<numWires; k++) {
	    wirePlaced[k] = false;
	}
    }

    public void print() {
	System.out.println("----------COST ARRAY----------");
	for (int i=0; i<heigth; i++) {
	    for (int j=0; j<width; j++) {
		System.out.print(array[i][j]);
	    }
	    System.out.println();
	}
    }
	    
}
