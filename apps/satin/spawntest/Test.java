/* $Id$ */

class Test extends ibis.satin.SatinObject implements TestInterface,
        java.io.Serializable {

    public Test() {
    }

    public int spawn_test(int depth) {
        if (depth <= 0) {
            return 0;
        }
        int res = spawn_test(depth - 1);
        sync();
        return 1 + res;
    }

    public int spawn100(int depth) {
        int[] x = new int[100];

        for (int i = 0; i < 100; i++) {
            x[i] = spawn_test(depth);
        }
        sync();
        return x[42];
    }
}