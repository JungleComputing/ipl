public final class LeafObject implements TestObject {

    static final int OBJECT_SIZE = 4 * 4;

    static final int KARMI_SIZE = 4 * 4;

    int size;

    int i1;

    int i2;

    int i3;

    public LeafObject() {
    }

    public int object_size() {
        return OBJECT_SIZE;
    }

    public int payload() {
        return KARMI_SIZE;
    }

    public String id() {
        return "LeafObject";
    }

    public int num_objs() {
        return 1;
    }
}