
import ibis.gmi.GroupMember;

class Test extends GroupMember implements myGroup {

    public final static boolean DEBUG = false;

    int i;

    Object data;

    Test() {
        if (DEBUG)
            System.out.println(getRank() + ": Test()");
    }

    public void groupInit() {
        i = getRank();
        if (DEBUG)
            System.out.println(getRank() + ": Test.groupInit()");
    }

    public void put(Object o) {
        if (DEBUG)
            System.out.println(getRank() + ": Test.put()");
        data = o;
    }

    public Object get() {
        if (DEBUG)
            System.out.println(getRank() + ": Test.get()");
        return data;
    }

    public Object put_get(Object o) {
        if (DEBUG)
            System.out.println(getRank() + ": Test.put_get()");
        Main.inc_count();
        return o;
    }
}