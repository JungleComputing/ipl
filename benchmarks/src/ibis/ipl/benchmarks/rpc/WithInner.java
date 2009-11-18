package ibis.ipl.benchmarks.rpc;

/* $Id$ */

class WithInner implements java.io.Serializable {

    private static final long serialVersionUID = -6251059778220583067L;

    static int count;

    class Inner implements java.io.Serializable {
        private static final long serialVersionUID = 6101579331738333255L;
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