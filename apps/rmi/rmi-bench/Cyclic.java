import java.io.*;

class Cyclic implements Serializable {

    double	d;
    int		i;
    Cyclic	next;

    Cyclic() {
    }

    Cyclic(int elts) {
	Cyclic first;
	Cyclic c;

	c = this;
	for (int i = 1; i < elts; i++) {
	    c.next = new Cyclic();
	    c = c.next;
	}
	c.next = this;
    }

    public String toString() {
	Cyclic c = this;
	String s = new String();

	do {
	    s = s + " + " + c.hashCode();
	    c = c.next;
	} while (c != this);

	return s;
    }

}
