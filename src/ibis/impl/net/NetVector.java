package ibis.impl.net;

/**
 * The NetIbis 'reliability' driver.
 */
public class NetVector {

    private final static int INCREMENT_DATABASE = 16;

    private int n;

    private int alloc;

    private Object[] data;

    public int add(Object x) {
        if (n == alloc) {
            alloc += INCREMENT_DATABASE;
            Object[] new_data = new Object[alloc];
            if (n != 0) {
                System.arraycopy(data, 0, new_data, 0, n);
            }
            data = new_data;
        }
        data[n] = x;
        return n++;
    }

    public Object get(int i) {
        if (i < 0 || i >= n) {
            return null;
        }
        return data[i];
    }

    public void delete(Object x) {
        for (int i = 0; i < n; i++) {
            if (x.equals(data[i])) {
                data[i] = null;
                break;
            }
        }
    }

    public int size() {
        return n;
    }

}