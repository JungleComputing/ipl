/* $Id$ */

class WithInner implements java.io.Serializable {

    static int count;

    class Inner implements java.io.Serializable {
        int x;

        Inner() {
            x = WithInner.this.x + 333;
        }
    }

    int x;

    Inner inner = new Inner();

    WithInner() {
        x = count++;
    }

    public String toString() {
        return "x=" + x + "-inner.x=" + inner.x;
    }
}