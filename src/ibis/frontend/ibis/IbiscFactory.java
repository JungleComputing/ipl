package ibis.frontend.ibis;

import java.util.Vector;

import org.apache.bcel.RepositoryObserver;

final class IbiscFactory implements RepositoryObserver {

    Vector loadList = new Vector();

    IbiscFactory() {
        // do nothing
    }

    public void notify(String className) {
        if (className.startsWith("ibis."))
            return;
        if (className.startsWith("java."))
            return;
        if (className.startsWith("sun."))
            return;
        if (className.startsWith("ibm."))
            return;
        if (className.startsWith("org.apache.bcel."))
            return;

        // System.out.println("notify " + className);

        loadList.add(className);
    }

    public Vector getList() {
        return loadList;
    }
}