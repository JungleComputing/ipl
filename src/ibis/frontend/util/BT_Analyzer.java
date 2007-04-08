/* $Id$ */

package ibis.frontend.util;

import java.util.Vector;

import org.apache.bcel.Repository;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;

public class BT_Analyzer {
    boolean verbose;

    public JavaClass specialInterface;

    public JavaClass subject;

    public String classname;

    public String packagename;

    public Vector<JavaClass> specialInterfaces;

    public Vector<Method> specialMethods;

    public Vector<Method> subjectSpecialMethods;

    public BT_Analyzer(JavaClass subject, JavaClass specialInterface,
            boolean verbose) {
        this.subject = subject;
        this.specialInterface = specialInterface;
        this.verbose = verbose;

        if (verbose) {
            System.out.println("BT_Analyzer looking for "
                    + specialInterface.getClassName() + " in "
                    + subject.getClassName());
        }
    }

    private boolean compareMethods(Method m1, Method m2) {
        return m1.getSignature().equals(m2.getSignature())
                && m1.getName().equals(m2.getName());
    }

    public boolean isSpecial(Method m1) {
        if (specialMethods == null) {
            return false;
        }

        for (int i = 0; i < specialMethods.size(); i++) {
            Method m2 = specialMethods.get(i);
            if (compareMethods(m1, m2)) {
                return true;
            }
        }

        return false;
    }

    public boolean hasSpecialMethods() {
        if (specialMethods == null) {
            return false;
        }
        return specialMethods.size() > 0;
    }

    private void addSpecialMethod(Vector<Method> sm, Method m) {
        for (int i = 0; i < sm.size(); i++) {
            if (compareMethods(m, sm.get(i))) {
                // it is already in the vector
                return;
            }
        }

        sm.addElement(m);
    }

    private void findSpecialMethods(JavaClass si, Vector<Method> sm) {
        Method[] methods = si.getMethods();

        for (int i = 0; i < methods.length; i++) {
            if (!methods[i].getName().equals("<clinit>")) {
                addSpecialMethod(sm, methods[i]);
            }
        }
    }

    Vector<Method> findSpecialMethods(Vector<JavaClass> si) {
        Vector<Method> mi = new Vector<Method>();

        for (int i = 0; i < si.size(); i++) {
            findSpecialMethods(si.get(i), mi);
        }

        return mi;
    }

    private void findSubjectSpecialMethod(Method[] sub, Method special,
            Vector<Method> dest) {

        for (int i = 0; i < sub.length; i++) {

            Method temp = sub[i];

            if (compareMethods(special, temp)) {
                // System.out.println(temp.fullName() + " equals "
                //         + special.fullName());
                dest.add(temp);
            }
        }
    }

    Vector<Method> findSubjectSpecialMethods() {
        Vector<Method> temp = new Vector<Method>();
        Method v[] = subject.getMethods();

        for (int i = 0; i < specialMethods.size(); i++) {
            findSubjectSpecialMethod(v, specialMethods.get(i), temp);
        }

        return temp;
    }

    boolean findSpecialInterfaces(String inter, Vector<JavaClass> si) {
        boolean result = false;
        JavaClass interf = Repository.lookupClass(inter);

        if (interf == null) {
            System.err.println("interface " + inter + " not found.");
            System.exit(1);
        }

        if (inter.equals(specialInterface.getClassName())) {
            result = true;
        } else {
            String[] interfaces = interf.getInterfaceNames();

            for (int i = 0; i < interfaces.length; i++) {
                result |= findSpecialInterfaces(interfaces[i], si);
            }
        }

        if (result) {
            if (!si.contains(inter)) {
                si.addElement(interf);
            }
        }

        return result;
    }

    Vector<JavaClass> findSpecialInterfaces() {
        Vector<JavaClass> si = new Vector<JavaClass>();

        if (!subject.isClass()) {
            findSpecialInterfaces(subject.getClassName(), si);
        } else {
            String[] interfaces = subject.getInterfaceNames();

            for (int i = 0; i < interfaces.length; i++) {
                if (verbose) {
                    System.out.println(subject.getClassName() + " implements "
                            + interfaces[i]);
                }
                findSpecialInterfaces(interfaces[i], si);
            }
        }

        return si;
    }

    void findSpecialInterfaces2(JavaClass inter, Vector<JavaClass> si) {
//        boolean result = false;

        JavaClass[] interfaces = inter.getAllInterfaces();

        for (int i = 0; i < interfaces.length; i++) {
            if (! si.contains(interfaces[i])) {
                si.addElement(interfaces[i]);
            }
        }

        if (! si.contains(inter)) {
            si.addElement(inter);
        }
    }

    Vector<JavaClass> findSpecialInterfaces2() {
        Vector<JavaClass> si = new Vector<JavaClass>();

        if (!subject.isClass()) {
            if (subject.implementationOf(specialInterface)) {
                findSpecialInterfaces2(subject, si);
            }
        } else {
            JavaClass[] interfaces = subject.getInterfaces();

            for (int i = 0; i < interfaces.length; i++) {
                if (interfaces[i].implementationOf(specialInterface)) {
                    findSpecialInterfaces2(interfaces[i], si);
                }
            }
        }

        return si;
    }

    public void start(boolean rmi_like) {

        String temp = subject.getClassName();
        packagename = subject.getPackageName();
        classname = temp.substring(temp.lastIndexOf('.') + 1);

        if (rmi_like) {
            specialInterfaces = findSpecialInterfaces2();
        } else {
            specialInterfaces = findSpecialInterfaces();
        }

        if (specialInterfaces.size() == 0) {
            if (verbose) {
                System.out.println("Class " + subject.getClassName()
                        + " does not implement "
                        + specialInterface.getClassName());
            }

            return;
        }

        if (verbose) {
            System.out.println(specialInterfaces.size()
                    + " special interfaces found in " + subject.getClassName());

            for (int i = 0; i < specialInterfaces.size(); i++) {
                System.out.println("\t"
                        + specialInterfaces.elementAt(i).getClassName());
            }
        }

        specialMethods = findSpecialMethods(specialInterfaces);

        if (verbose) {
            System.out.println(specialMethods.size()
                    + " special methods found in " + subject.getClassName());

            for (int i = 0; i < specialMethods.size(); i++) {
                System.out.println("\t"
                        + specialMethods.get(i).toString());
            }
        }

        subjectSpecialMethods = findSubjectSpecialMethods();
    }
}
