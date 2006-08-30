/* $Id$ */

class Wire implements java.io.Serializable {

    int label;

    int begin_x, begin_y;
    int end_x, end_y;
    int bend1_x, bend1_y;
    int bend2_x, bend2_y;

    Wire(int label, int begin_x, int begin_y, int end_x, int end_y) {
	this.label = label;
	this.begin_x = begin_x;
	this.begin_y = begin_y;
	this.end_x = end_x;
	this.end_y = end_y;
    }

    void place(int bend1_x, int bend1_y, int bend2_x, int bend2_y) {
	this.bend1_x = bend1_x;
	this.bend1_y = bend1_y;
	this.bend2_x = bend2_x;
	this.bend2_y = bend2_y;
    }
    
	
}
