package ibis.ipl.impl.messagePassing;

class PortHash {

    private final int	PORT_DATABASE_CHUNK = 32;
    private int		maxPortDataBase = 0;
    private int		allocPortDataBase = 0;
    private Object[]	portDataBase;

    void bind(int x, Object p) {
	// already locked: synchronized (ibis.ipl.impl.messagePassing.Ibis.myIbis)
	// ibis.ipl.impl.messagePassing.Ibis.myIbis.checkLockOwned();

	if (x >= allocPortDataBase) {
	    Object[] a;

	    allocPortDataBase = x + PORT_DATABASE_CHUNK;
	    a = new Object[allocPortDataBase];
	    for (int i = 0; i < maxPortDataBase; i++) {
		a[i] = portDataBase[i];
	    }
	    portDataBase = a;
	}
	if (x > maxPortDataBase) {
	    maxPortDataBase = x;
	}
	portDataBase[x] = p;
    }


    Object lookup(int x) {
	// ibis.ipl.impl.messagePassing.Ibis.myIbis.checkLockOwned();
	return portDataBase[x];
    }


    void unbind(int x) {
	// ibis.ipl.impl.messagePassing.Ibis.myIbis.checkLockOwned();
	portDataBase[x] = null;
    }

}
