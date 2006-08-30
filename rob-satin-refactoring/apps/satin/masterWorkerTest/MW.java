final class MW extends ibis.satin.SatinObject implements MWInterface {
    public void test(int n) {
    }

    public static void main(String[] args) {
        MW mw = new MW();

        for(int j=0; j<1000; j++) {
        System.out.println("Running MW");

        long start = System.currentTimeMillis();
        for(int i=0;i<100000; i++) {
            mw.test(i);
        }
        
        mw.sync();
        double timeSatin = (double) (System.currentTimeMillis() - start) / 1000.0;

        System.out.println("application time MW took " + timeSatin + " s");
        }
    }
}