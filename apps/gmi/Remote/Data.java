import java.io.*;

class Data implements Serializable { 

	double value;
	Data next;

	Data(double value, Data next) { 
		this.value = value;
		this.next  = next;
	} 
}
