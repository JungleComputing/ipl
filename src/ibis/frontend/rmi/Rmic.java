/* $Id$ */

package ibis.frontend.rmi;

import ibis.frontend.generic.BT_Analyzer;
import ibis.frontend.ibis.IbiscComponent;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.bcel.Repository;
import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.Constant;
import org.apache.bcel.classfile.ConstantClass;
import org.apache.bcel.classfile.ConstantNameAndType;
import org.apache.bcel.classfile.ConstantPool;
import org.apache.bcel.classfile.Field;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.LocalVariable;
import org.apache.bcel.classfile.LocalVariableTable;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ClassGen;
import org.apache.bcel.generic.ConstantPoolGen;

public class Rmic extends IbiscComponent {
    private static final String ibisRmiInterface = "ibis.rmi.Remote";

    private boolean local = true;

    private boolean printOnly = false;

    private boolean java2ibis = false;

    private JavaClass rmiInterface = null;

    // For Ibisc
    private boolean rmicEnabled = false;

    private ArrayList javaFiles = new ArrayList();

    private ArrayList classes = new ArrayList();

    private boolean first = true;

    public Rmic() {
        this(ibisRmiInterface);
    }

    private Rmic(String remoteInterface) {
        rmiInterface = Repository.lookupClass(remoteInterface);

        if (rmiInterface == null) {
            System.err.println("Class " + remoteInterface + " not found");
            System.exit(1);
        }
    }


    private String getFileName(JavaClass subject, String pkg, String name, String pre) {
        name = pre + name + ".java";
        if (!local && pkg != null && !pkg.equals("")) {
            return pkg.replace('.', File.separatorChar) + File.separator + name;
        }
        if (fromIbisc) {
            String dir = getDirectory(subject.getClassName());
            if (dir != null) {
                return dir + File.separator + name;
            }
        }
        return name;
    }

    private PrintWriter createFile(String name) throws Exception {
        File f = new File(name);

        if (!f.createNewFile()) {
            System.err.println("File " + name + " already exists!");
            System.exit(1);
        }

        FileOutputStream fileOut = new FileOutputStream(f);

        return new PrintWriter(fileOut);
    }

    public boolean processArgs(ArrayList args) {
        for (int i = 0; i < args.size(); i++) {
            String arg = (String) args.get(i);
            if (arg.equals("-rmi")) {
                rmicEnabled = true;
                args.remove(i--);
            } else if (arg.equals("-rmi-java2ibis")) {
                java2ibis = true;
                rmicEnabled = true;
                args.remove(i--);
            }
        }
        return rmicEnabled;
    }

    public void process(Iterator iter) {
        if (! rmicEnabled) {
            return;
        }

        while (iter.hasNext()) {
            JavaClass subject = (JavaClass) iter.next();
            classes.add(subject);
        }

        processClasses();
    }

    public String rewriterImpl() {
        return "BCEL";
    }

    private String sig_java2ibis(String s) {
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == 'L') {
                if (s.startsWith("java/rmi/", i + 1)) {
                    s = s.substring(0, i + 1) + "ibis" + s.substring(i + 5);
                }
                do {
                    i++;
                } while (s.charAt(i) != ';');
            }
        }
        return s;
    }

    private JavaClass cvt_java2ibis(JavaClass c) {
        ClassGen cg = new ClassGen(c);
        ConstantPoolGen cpg = cg.getConstantPool();
        ConstantPool cp = c.getConstantPool();
        boolean changed = false;
        boolean must_save = false;

        for (int i = 0; i < cp.getLength(); i++) {
            Constant co = cp.getConstant(i);
            if (co instanceof ConstantNameAndType) {
                ConstantNameAndType conap = (ConstantNameAndType) co;
                String name = conap.getName(cp);
                if (name.startsWith("java.rmi.")
                        || name.startsWith("java/rmi/")) {
                    changed = true;
                    name = "ibis" + name.substring(4);
                    conap.setNameIndex(cpg.addUtf8(name));
                }

                String sig = conap.getSignature(cp);
                if (sig.indexOf("java/rmi/") >= 0) {
                    changed = true;
                    sig = sig_java2ibis(sig);
                    conap.setSignatureIndex(cpg.addUtf8(sig));
                }
                cpg.setConstant(i, conap);
            } else if (co instanceof ConstantClass) {
                ConstantClass coc = (ConstantClass) co;
                String name = coc.getBytes(cp);

                if (name.startsWith("java/rmi/")
                        || name.startsWith("java.rmi.")) {
                    changed = true;
                    name = "ibis" + name.substring(4);
                    coc.setNameIndex(cpg.addUtf8(name));
                    cpg.setConstant(i, coc);
                }
            }
        }

        if (changed) {
            must_save = true;
            changed = false;
            Repository.removeClass(c);
            c = cg.getJavaClass();
            Repository.addClass(c);
            cg = new ClassGen(c);
            cpg = cg.getConstantPool();
        }

        Field f[] = cg.getFields();
        for (int i = 0; i < f.length; i++) {
            String sig = f[i].getSignature();
            if (sig.indexOf("java/rmi/") >= 0) {
                changed = true;
                sig = sig_java2ibis(sig);
                f[i].setSignatureIndex(cpg.addUtf8(sig));
            }
        }

        Method m[] = cg.getMethods();
        for (int i = 0; i < m.length; i++) {
            String sig = m[i].getSignature();
            if (sig.indexOf("java/rmi/") >= 0) {
                changed = true;
                sig = sig_java2ibis(sig);
                m[i].setSignatureIndex(cpg.addUtf8(sig));
            }
            LocalVariableTable lt = m[i].getLocalVariableTable();
            if (lt != null) {
                LocalVariable l[] = lt.getLocalVariableTable();
                for (int j = 0; j < l.length; j++) {
                    sig = l[j].getSignature();
                    if (sig.indexOf("java/rmi/") >= 0) {
                        changed = true;
                        sig = sig_java2ibis(sig);
                        l[j].setSignatureIndex(cpg.addUtf8(sig));
                    }
                }
            }
        }

        if (changed || must_save) {
            String className = c.getClassName();
            String classFile = className.replace('.', File.separatorChar)
                    + ".class";
            Repository.removeClass(c);
            c = cg.getJavaClass();
            /*
             * At this point, BCEL delivers an inconsistent JavaClass: there
             * exist constant_pool references in each Field, Method, Attribute,
             * and these sometimes don't refer to the new constantpool of the
             * class. However, everything seems to be good enough to dump and
             * reload.
             */
            if (rmicEnabled) {
                byte[] buf = c.getBytes();
                try {
                    c = new ClassParser(new ByteArrayInputStream(buf), classFile).parse();
                } catch(IOException e) {
                    // Should not happen
                }
                Repository.addClass(c);
                setModified(wrapper.getInfo(c));
            } else {
                try {
                    c.dump(classFile);
                    c = Repository.lookupClass(className);
                } catch(IOException e) {
                    System.err.println("got IOException " + e);
                    e.printStackTrace();
                    System.exit(1);
                }
            }
        }

        return c;
    }

    private void doCompile(String[] args) {

        int num = args.length;
        int i = 0;

        while (i < num) {
            if (false) { /* do nothing */
            } else if (args[i].equals("-n")) {
                printOnly = true;
                args[i] = args[num - 1];
                num--;
            } else if (args[i].equals("-java")) {
                args[i] = args[num - 1];
                num--;
            } else if (args[i].equals("-karmi")) {
                args[i] = args[num - 1];
                num--;
            } else if (args[i].equals("-v")) {
                verbose = true;
                args[i] = args[num - 1];
                num--;
            } else if (args[i].equals("-dir")) {
                local = false;
                args[i] = args[num - 1];
                num--;
            } else if (args[i].equals("-java2ibis")) {
                java2ibis = true;
                args[i] = args[num - 1];
                num--;
            } else if (args[i].equals("-local")) {
                local = true;
                args[i] = args[num - 1];
                num--;
            } else {
                i++;
            }
        }

        for (i = 0; i < num; i++) {
            JavaClass c = Repository.lookupClass(args[i]);
            if (c == null) {
                System.err.println("Class " + args[i] + " not found");
                System.exit(1);
            }
            classes.add(c);
        }

        processClasses();
    }

    private void processClasses() {
        if (java2ibis) {
            for (int i = 0; i < classes.size(); i++) {
                classes.set(i, cvt_java2ibis((JavaClass) classes.get(i)));
            }
        }
        if (rmicEnabled) {
            writeAll();
        }
        for (int i = 0; i < classes.size(); i++) {
            JavaClass subject = (JavaClass) classes.get(i);
            processFile(subject);
            if (rmicEnabled && javaFiles.size() > 0) {
                compile(javaFiles, subject.getClassName());
                javaFiles.clear();
            }
        }
        if (printOnly) {
            System.out.println("");
        }
    }

    private void processFile(JavaClass subject) {
        try {
            PrintWriter output;

            if (subject.isInterface()) {
                return;
            }

            if (! subject.implementationOf(rmiInterface)) {
                return;
            }

            BT_Analyzer a = new BT_Analyzer(subject, rmiInterface, verbose);
            a.start(true);

            if (a.specialInterfaces.size() == 0) {
                return;
            }

            if (printOnly) {
                if (! first) {
                    System.out.print(" ");
                } else {
                    first = false;
                }
                System.out.print(subject.getClassName() + ".class");
                return;
            }

            if (verbose) {
                System.out.println("Handling " + subject.getClassName());
            }

            String fileName = getFileName(subject, a.packagename, a.classname,
                    "rmi_stub_");
            output = createFile(fileName);
            new RMIStubGenerator(a, output).generate();
            output.close();
            javaFiles.add(fileName);

            fileName = getFileName(subject, a.packagename, a.classname,
                    "rmi_skeleton_");
            output = createFile(fileName);
            new RMISkeletonGenerator(a, output).generate();
            output.close();
            javaFiles.add(fileName);

        } catch (Exception e) {
            System.err.println("Got exception during processing of "
                    + subject.getClassName());
            System.err.println("exception is: " + e);
            e.printStackTrace();
            System.exit(1);
        }
    }

    public static void main(String[] args) {

        if (args.length == 0) {
            System.err.println("Usage : java Rmic [-n] [-v] [-java2ibis] "
                    + "[-dir | -local] classname");
            System.exit(1);
        }

        String remoteInterface = ibisRmiInterface;

        int num = args.length;
        int i = 0;

        while (i < num) {
            if (false) { /* do nothing */
            } else if (args[i].equals("-java")) {
                remoteInterface = "java.rmi.Remote";
            } else if (args[i].equals("-karmi")) {
                remoteInterface = "uka.karmi.rmi.Remote";
            }
            i++;
        }
        new Rmic(remoteInterface).doCompile(args);
    }
}
