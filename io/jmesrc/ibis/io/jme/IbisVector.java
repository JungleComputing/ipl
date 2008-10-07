/* $Id$ */

package ibis.io.jme;

public final class IbisVector {

    public static final int INIT_SIZE = 64;

    private static final int INCREMENT_FACTOR = 4;

    private Object[] array;

    private int current_size;

    private int maxfill;

    public IbisVector() {
        this(INIT_SIZE);
    }

    public IbisVector(int size) {
        array = new Object[size];
        current_size = size;
        maxfill = 0;
    }

    private final void double_array() {
        int new_size = current_size * INCREMENT_FACTOR;
        Object[] temp = new Object[new_size];
        // System.arraycopy(array, 0, temp, 0, current_size);
        System.arraycopy(array, 0, temp, 0, maxfill);
        array = temp;
        current_size = new_size;
    }

    public final void add(int index, Object data) {
        // System.err.println("objects.add: index = " + index + " data = "
        //         + (data == null ? "NULL" : data.getClass().getName()));

        while (index >= current_size) {
            double_array();
        }
        array[index] = data;
        if (index >= maxfill) {
            maxfill = index + 1;
        }
    }

    public final Object get(int index) {
        return array[index];
    }

    public final void clear() {
        for (int i = 0; i < maxfill; i++) {
            array[i] = null;
        }
        maxfill = 0;
    }
}
