import java.io.Serializable;

public class IntRow implements Serializable{

    int[] row;
    int maxSize, size;

    IntRow() {
	row = new int[0];
	maxSize = 0;
	size = 0;
    }

    IntRow(int number){
	row = new int[number];
	maxSize = number;
	size = 0;
    }
     
    public synchronized int length() {
	return size;
    } 

    public void add(int i){
	row[size] = i;
	size++;
    }

    public void reset(IntRow array) {
	this.maxSize = array.maxSize;
	this.row = new int[maxSize];
	this.size = array.size;
	for(int i = 0; i < size; i++){
	    this.row[i] = array.row[i];
	}
    }

    static IntRow generateArray(int number) {
	IntRow temp;
	temp = new IntRow(number);
	for(int i = 0; i < number; i++){ 
	  temp.row[i] = (int) Math.rint((Integer.MAX_VALUE * Math.random() * 0.25));
	  temp.size++;	    
	}
	return temp;
    }

    public synchronized int[] get() {
	return row;
    }

    //returns array from begin to end -1 
    public IntRow subIntRow(int begin, int end) {
	IntRow sub = new IntRow(end - begin);
	for(int i = begin; i < end; i++){
	    sub.row[i - begin] = this.row[i];
	}
	return sub;
    }
    
    //returns array from begin to end -1 
    public int[] subArray(int begin, int end){	
    	int[] sub = new int[end - begin];
    	for(int i = begin; i < end; i++){
	    sub[i - begin] = this.row[i];
    	}
    	return sub;
    }
    
}






