package ibis.ipl.impl.messagePassing;

final class PortHash {

    private final int	PORT_DATABASE_CHUNK = 32;
    private int		maxPortDataBase = 0;
    private int		allocPortDataBase = 0;
    private Object[]	portDataBase;

    void bind(int x, Object p) {
	Ibis.myIbis.checkLockOwned();

	// System.err.println("PortHash: now bind port " + x + " to Object " + p);

	if (x >= allocPortDataBase) {
	    Object[] a;

	    allocPortDataBase = x + PORT_DATABASE_CHUNK;
	    a = new Object[allocPortDataBase];
	    for (int i = 0; i < maxPortDataBase; i++) {
		a[i] = portDataBase[i];
	    }
	    portDataBase = a;
	}
	if (x >= maxPortDataBase) {
	    maxPortDataBase = x+1;
	}
	portDataBase[x] = p;
    }


    Object lookup(int x) {
	Ibis.myIbis.checkLockOwned();
	return portDataBase[x];
    }


    void unbind(int x) {
	Ibis.myIbis.checkLockOwned();
	portDataBase[x] = null;
    }

}
