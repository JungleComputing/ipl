/* $Id$ */

final class TwoOutThree extends ibis.satin.SatinObject implements
        TwoOutThreeInterface, java.io.Serializable {
    public void foo(int i) throws Done {
        throw new Done(i);
    }

    public static void main(String[] args) {
        TwoOutThree t = new TwoOutThree();
        int n = 0;
        String result = "";

        System.out.print("application result two_out_of_three result = ");

        for (int i = 0; i < 3; i++) {
            try {
                t.foo(i);
            } catch (Done d) {
                result = result + "foo res: " + d.res;
                if (++n == 2) {
                    t.abort();
                }
                return;
            }
        }
        t.sync();
        System.out.println(result);
        if (!result.equals("foo res: 2foo res: 1")) {
            System.out.println("Test failed!");
            System.exit(1);
        }
        System.out.println("Test succeeded!");
    }
}