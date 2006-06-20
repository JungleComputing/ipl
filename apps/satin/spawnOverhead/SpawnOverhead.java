import ibis.satin.Inlet;

class SpawnOverhead extends ibis.satin.SatinObject implements
        SpawnOverheadInterface {

    static final int SPAWNS_PER_SYNC = 10;
    static final int COUNT = 1000000;
    
    Inlet inlet = new Inlet();
    
    public void spawn() {
    }

    public void spawnWithException() throws Inlet {
        throw inlet;
    }

    void spawnTest() {
        long start = System.currentTimeMillis();
        for (int j = 0; j < COUNT; j++) {
            for (int i = 0; i < SPAWNS_PER_SYNC; i++) {
                spawn();
            }
            sync();
        }
        long end = System.currentTimeMillis();
        double timeSatin = (double) (end - start) / 1000.0;
        double cost =  ((double) (end - start) * 1000.0) / (SPAWNS_PER_SYNC * COUNT);
        
        System.out.println("spawn = " + timeSatin + " s, time/spawn = " + cost + " us/spawn" );
    }

    void abortTest() {
        long start = System.currentTimeMillis();
        for (int j = 0; j < COUNT; j++) {
            for (int i = 0; i < SPAWNS_PER_SYNC; i++) {
                spawn();
            }
            abort();
            sync();
        }
        long end = System.currentTimeMillis();
        double timeSatin = (double) (end - start) / 1000.0;
        double cost =  ((double) (end - start) * 1000.0) / (SPAWNS_PER_SYNC * COUNT);
        
        System.out.println("spawn = " + timeSatin + " s, time/spawn = " + cost + " us/spawn+abort" );
    }

    void inletTest() {
        long start = System.currentTimeMillis();
        for (int j = 0; j < COUNT; j++) {
            for (int i = 0; i < SPAWNS_PER_SYNC; i++) {
                try {
                    spawnWithException();
                } catch (Inlet e) {
                    return;
                    // ignore
                }
            }
            sync();
        }
        long end = System.currentTimeMillis();
        double timeSatin = (double) (end - start) / 1000.0;
        double cost =  ((double) (end - start) * 1000.0) / (SPAWNS_PER_SYNC * COUNT);
        
        System.out.println("spawn = " + timeSatin + " s, time/spawn = " + cost + " us/spawn+inlet" );
    }

    public static void main(String[] args) {
        SpawnOverhead f = new SpawnOverhead();

        System.out.println("Running spawn test");
        for(int i=0;i<10; i++) {
            f.spawnTest();
        }

        System.out.println("Running abort test");
        for(int i=0;i<10; i++) {
            f.abortTest();
        }

        System.out.println("Running inlet test");
        for(int i=0;i<10; i++) {
            f.inletTest();
        }
    }
}
