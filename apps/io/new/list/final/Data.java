import java.io.Serializable;
import java.io.IOException;

public final class Data implements Serializable { 

	public static final int OBJECT_SIZE = 4*4+4;

	int v1, v2, v3, v4;
        Data next;

	public Data(int value, Data next) { 
                v1 = v2 = v3 = v4 = value;
                this.next  = next;
        } 
} 
