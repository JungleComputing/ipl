/* $Id$ */

public class B extends A implements java.io.Serializable {
    int j;
    transient Object tO;
    transient int tI;

    B() {
    }

    B(int i) {
	super(i);
	j = i;
    }
}
