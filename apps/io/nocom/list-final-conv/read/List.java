import java.io.IOException;
import java.io.Serializable;

public final class List implements Serializable {

	public static final int KARMI_SIZE = 4*4;

	List next;
	
	int i;
	int i1;
	int i2;
	int i3;
	
	public List(int size) {
		if (size > 0) {
			this.next = new List(size-1);
		}
	}
}





