package ibis.frontend.rmi;

import ibis.util.BT_Analyzer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.IOException;
import java.util.Vector;

import org.apache.bcel.Repository;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Constant;
import org.apache.bcel.classfile.ConstantClass;
import org.apache.bcel.classfile.ConstantNameAndType;
import org.apache.bcel.classfile.ConstantPool;
import org.apache.bcel.generic.ClassGen;
import org.apache.bcel.generic.ConstantPoolGen;

class Main {
	static boolean local = true;

	public static String getFileName(String pkg, String name, String pre) {
		if (! local && pkg != null && ! pkg.equals("")) {
		    return pkg.replace('.', File.separatorChar) +
			    File.separator + pre + name + ".java";
		}
		return pre + name + ".java";
	}

	public static PrintWriter createFile(String name) throws Exception {

		File f = new File(name);

		if (!f.createNewFile()) {
			System.err.println("File " + name + " already exists!");
			System.exit(1);
		}

		FileOutputStream fileOut = new FileOutputStream(f);

		return new PrintWriter(fileOut);
	}

	public static String sig_java2ibis(String s) {
		for (int i = 0; i < s.length(); i++) {
			if (s.charAt(i) == 'L') {
				if (s.startsWith("java/rmi/", i+1)) {
					s = s.substring(0, i+1) + "ibis" + s.substring(i+5);
				}
				do {
					i++;
				} while (s.charAt(i) != ';');
			}
		}
		return s;
	}

	public static JavaClass cvt_java2ibis(JavaClass c) {
		ClassGen cg = new ClassGen(c);
		ConstantPoolGen cpg = cg.getConstantPool();
		ConstantPool cp = c.getConstantPool();
		boolean changed = false;

		for (int i = 0; i < cp.getLength(); i++) {
			Constant co = cp.getConstant(i);
			if (co instanceof ConstantNameAndType) {
				ConstantNameAndType conap = (ConstantNameAndType) co;
				String name = conap.getName(cp);
				if (name.startsWith("java.rmi.")) {
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

				System.out.println("Got ConstantNameAndType with name " + name + " and signature " + sig);
				cpg.setConstant(i, conap);
			}
			else if (co instanceof ConstantClass) {
				ConstantClass coc = (ConstantClass) co;
				String name = coc.getBytes(cp);

				if (name.startsWith("java/rmi/")) {
					changed = true;
					name = "ibis" + name.substring(4);
					coc.setNameIndex(cpg.addUtf8(name));
					cpg.setConstant(i, coc);
				}

				System.out.println("Got ConstantClass with name " + name);
			}
		}

		if (changed) {
			Repository.removeClass(c);
			c = cg.getJavaClass();
			Repository.addClass(c);
			String classname = c.getClassName();
			String classfile = classname.replace('.', java.io.File.separatorChar) + ".class";
			try {
				c.dump(classfile);
			} catch(IOException e) {
				System.err.println("got IOException " + e);
				e.printStackTrace();
				System.exit(1);
			}
		}

		return c;
	}

	public static void main(String [] args) {

		Vector classes = new Vector();
		boolean verbose = false;
		boolean java2ibis = false;
		JavaClass rmiInterface = null;

		if (args.length == 0) {
			System.err.println("Usage : java Main [-v] [-java2ibis] [-dir | -local] classname");
			System.exit(1);
		}

		int num = args.length;
		int i = 0;

		while (i<num) {
			if (args[i].equals("-v")) {
				verbose = true;
				args[i] = args[num-1];
				num--;
			} else if (args[i].equals("-dir")) {
				local = false;
				args[i] = args[num-1];
				num--;
			} else if (args[i].equals("-java2ibis")) {
				java2ibis = true;
				args[i] = args[num-1];
				num--;
			} else if (args[i].equals("-local")) {
				local = true;
				args[i] = args[num-1];
				num--;
			} else {
				i++;
			}
		}

		rmiInterface = Repository.lookupClass("ibis.rmi.Remote");

		if (rmiInterface == null) {
			System.err.println("Class ibis.rmi.Remote not found");
			System.exit(1);
		}

		for (i=0;i<num;i++) {
			JavaClass c = Repository.lookupClass(args[i]);
			if (c == null) {
				System.err.println("Class " + args[i] + " not found");
				System.exit(1);
			}
			classes.addElement(c);
		}

		if (java2ibis) {
			for (i=0;i<classes.size();i++) {
				classes.setElementAt(cvt_java2ibis((JavaClass) classes.get(i)), i);
			}
		}

		for (i=0;i<classes.size();i++) {

			try {
				PrintWriter output;
				JavaClass subject = (JavaClass) classes.get(i);

				BT_Analyzer a = new BT_Analyzer(subject, rmiInterface, verbose);
				a.start();

				if (a.specialInterfaces.size() == 0) {
				    continue;
				}

				if (verbose) {
					System.out.println("Handling " + subject.getClassName());
				}

				output = createFile(getFileName(a.packagename, a.classname, "rmi_stub_"));
				new RMIStubGenerator(a, output, verbose).generate();
				output.flush();

				output = createFile(getFileName(a.packagename, a.classname, "rmi_skeleton_"));
				new RMISkeletonGenerator(a, output, verbose).generate();
				output.flush();

			} catch (Exception e) {
				System.err.println("Main got exception " + e);
				e.printStackTrace();
				System.exit(1);
			}

		}
	}
}
