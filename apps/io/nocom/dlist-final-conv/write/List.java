import java.io.IOException;
import java.io.Serializable;

public final class List implements Serializable {

	public static final int KARMI_SIZE = 4*4;

	List next, prev;
	
	int i;
	int i1;
	int i2;
	int i3;
	
	private List(int size, List prev) { 
		if (size > 0) {
			this.prev = prev;
			this.next = new List(size-1);
		}
	} 

	public List(int size) {
		this.prev = null;
		this.next = new List(size-1, this);
	}
}





