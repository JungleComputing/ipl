public final class DList implements TestObject {

    static final int OBJECT_SIZE = 4 * 4 + 2 * 4;

    static final int KARMI_SIZE = 4 * 4;

    static final int LEN = 1023;

    DList next, prev;

    int size;

    int i1;

    int i2;

    int i3;

    private DList(int size, DList prev) {
        if (size > 0) {
            this.prev = prev;
            this.next = new DList(size - 1, this);
        }
        this.size = size;
    }

    public DList() {
        this.prev = null;
        this.next = new DList(LEN - 1, this);
    }

    public int object_size() {
        return size * OBJECT_SIZE;
    }

    public int payload() {
        return size * KARMI_SIZE;
    }

    public String id() {
        return "DList of " + size + " DList objects";
    }

    public int num_objs() {
        return size;
    }
}

