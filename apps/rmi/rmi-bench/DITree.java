import java.io.*;

public final class DITree implements Serializable {

    // double	d;
    int		i;
    int		i1;
    int		i2;
    int		i3;
    DITree	left;
    DITree	right;

    DITree() {
    }

    DITree(int size) {
	int leftSize = size / 2;
	if (leftSize > 0) {
	    this.left = new DITree(leftSize);
	}
	if (size - leftSize - 1 > 0) {
	    this.right = new DITree(size - leftSize - 1);
	}
    }

}
