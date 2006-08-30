import ibis.satin.Inlet;

interface SpawnOverheadInterface extends ibis.satin.Spawnable {
    public void spawn();
    public void spawnWithException() throws Inlet;
}
