
import java.io.Serializable;

public final class Tree implements Serializable {
    public Tree(int j) {
        int k = j / 2;
        if (k > 0)
            left = new Tree(k);
        if (j - k - 1 > 0)
            right = new Tree(j - k - 1);
    }

    public static final int PAYLOAD = 16;

    Tree left;

    Tree right;

    int i;

    int i1;

    int i2;

    int i3;
}