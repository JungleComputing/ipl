import java.io.*;

public final class DITree implements Externalizable {

	public static final int OBJECT_SIZE = 4*4+2*4;
	public static final int KARMI_SIZE = 4*4;

	DITree	left;
	DITree	right;
	
	int i;
	int i1;
	int i2;
	int i3;
	
	public DITree() {}

	public DITree(int size) {
		int leftSize = size / 2;
		if (leftSize > 0) {
			this.left = new DITree(leftSize);
		}
		if (size - leftSize - 1 > 0) {
			this.right = new DITree(size - leftSize - 1);
		}
	}

	public void writeExternal(ObjectOutput out)
		throws IOException {
//		System.err.print("w");
		out.writeInt(i);
		out.writeInt(i1);
		out.writeInt(i2);
		out.writeInt(i3);
		out.writeObject(left);
		out.writeObject(right);
	}

	
	public void readExternal(ObjectInput in)
		throws IOException, ClassNotFoundException {
//		System.err.print("r");
		i = in.readInt();
		i1 = in.readInt();
		i2 = in.readInt();
		i3 = in.readInt();
		left = (DITree) in.readObject();
		right = (DITree) in.readObject();
	}

}





