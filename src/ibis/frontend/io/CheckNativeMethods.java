package ibis.frontend.io;

import java.io.BufferedReader;
import java.io.StreamTokenizer;
import java.io.FileReader;
import java.io.IOException;

import ibis.classfile.*;

public class CheckNativeMethods {

    private static String stripJunk(String clazz) {
	String c = new String(clazz);

	// Convert '/' to '.'
	// Strip trailing '.class'

	return c;
    }


    private static void checkNativeMethods(String clazz)
	    throws IOException {

	// System.err.println("Inspect Class " + clazz);

	clazz = stripJunk(clazz);
	ClassFile c = ClassFile.read(clazz, false, false, false);

	int ACC_NATIVE = ibis.classfile.ClassFileConstants.ACC_NATIVE;
	for (int i = 0; i < c.methods.length; i++) {
	    int flags = c.methods[i].accessFlags();
	    if ((flags & ACC_NATIVE) != 0) {
		System.out.println(clazz);
		return;
	    }
	}
    }


    private static String[] increase_one(String[] old) {
	String[] s;
	if (old == null) {
	    s = new String[1];
	} else {
	    s = new String[old.length + 1];
	    for (int i = 0; i < old.length; i++) {
		s[i] = old[i];
	    }
	}
	return s;
    }


    private static void checkNativeMethodsFromFile(String f)
	    throws IOException {
	BufferedReader in = new BufferedReader(new FileReader(f));
	StreamTokenizer tok = new StreamTokenizer(in);

	// for all classes in file {
	    // checkNativeMethods(clazz);
	// }
    }


    public static void main(String[] args) {

	String file = null;
	String[] clazz = null;
	boolean verbose = false;

	if (false) {
	    System.err.print("CheckNativeMethods arguments: ");
	    for (int i = 0; i < args.length; i++) {
		System.err.print(args[i] + " ");
	    }
	    System.err.println();
	}

	for (int i = 0; i < args.length; i++) {
	    if (false) {
	    } else if (args[i].equals("-v")) {
		verbose = true;
	    } else if (args[i].equals("-f")) {
		try {
		    checkNativeMethodsFromFile(args[++i]);
		} catch (IOException e) {
		    System.err.println("Error for arg " + args[i] + ": " + e);
		}
	    } else {
		clazz = increase_one(clazz);
		clazz[clazz.length - 1] = args[i];
	    }
	}

	for (int i = 0; i < clazz.length; i++) {
	    try {
		if (verbose) {
		    System.err.println("Check class " + clazz[i]);
		}
		checkNativeMethods(clazz[i]);
	    } catch (IOException e) {
		System.err.println("Error for class " + clazz[i] + ": " + e);
	    }
	}
    }

}
