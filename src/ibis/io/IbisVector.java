package ibis.io;

public final class IbisVector { 

	public static final int INIT_SIZE = 1024;

	private Object [] array;
	private int current_size;
	
	public IbisVector() { 
		this(INIT_SIZE);
	} 

	public IbisVector(int size) { 
		array = new Object[size];
		current_size = size;
	} 

	private void double_array() { 
		Object [] temp = new Object[current_size*2];
		System.arraycopy(array, 0, temp, 0, current_size);
		array = temp;
		current_size *= 2;
	} 

	public void add(int index, Object data) { 
//		System.err.println("objects.add: index = " + index + " data = " + (data == null ? "NULL" : data.getClass().getName()));

		while (index >= current_size) { 
			double_array();
		} 
		array[index] = data;
	} 

	public Object get(int index) { 
		return array[index];
	} 

	public void clear() { 
		for (int i=0;i<current_size;i++) { 
			array[i] = null;
		}
	} 
} 
