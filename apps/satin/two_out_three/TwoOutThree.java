/* $Id$ */

final class TwoOutThree extends ibis.satin.SatinObject implements
        TwoOutThreeInterface, java.io.Serializable {
    public void foo() throws Done {
        throw new Done();
    }

    public static void main(String[] args) {
        TwoOutThree t = new TwoOutThree();
        String result = "";
        int n = 0;

        System.out.print("application result two_out_of_three = ");
        for (int i = 0; i < 3; i++) {
            try {
                t.foo();
            } catch (Done d) {
                result = result + "| in Catch, n is now " + n;
                if (++n == 2) {
                    t.abort();
                }
                return;
            }
        }

        t.sync();

        System.out.println(result);
        if (!result.equals("| in Catch, n is now 0| in Catch, n is now 1")) {
            System.out.println("Test failed!");
            System.exit(1);
        }
        System.out.println("Test succeeded!");
    }
}