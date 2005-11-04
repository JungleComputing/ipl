
package ibis.frontend.satin.so;

import ibis.frontend.generic.BT_Analyzer;
import ibis.frontend.generic.RunJavac;

import java.io.*;
import java.util.Vector;

import org.apache.bcel.Constants;
import org.apache.bcel.Repository;
import org.apache.bcel.classfile.*;
import org.apache.bcel.generic.*;

/** The Satin Shared Objects compiler and bytecode rewriter.
 *  This class creates SOInvocationRecords for write methods
 *  of shared objects. It also rewrite the bytecode of each 
 *  write method and inserts communication code
 */

class SatinSOc {

    static ClassGen classGen;
    static InstructionFactory insFactory;
    static ConstantPoolGen cpg;

    static boolean verbose = false;
    static boolean keep = false;
    static boolean local = true;

    // @TODO: add a $rewritten$ field, so that we don't rewrite the 
    // class twice

    public static void main(String[] args) {

        Vector classes = new Vector();
	Vector classList = new Vector();

        for (int i = 0; i < args.length; i++) {
            if (!args[i].startsWith("-")) {
                classList.add(args[i]);
            } else if (args[i].equals("-v")) {
                verbose = true;
            } else if (args[i].equals("-verbose")) {
                verbose = true;
            } else if (args[i].equals("-no-verbose")) {
                verbose = false;
            } else if (args[i].equals("-keep")) {
                keep = true;
            } else if (args[i].equals("-no-keep")) {
                keep = false;
            } else if (args[i].equals("-dir")) {
                local = false;
            } else if (args[i].equals("-local")) {
                local = true;
            } else {
                usage();
            }
        }

        if (classList.size() == 0) {
            usage();
        }

        JavaClass writeMethodsInterface = 
	    Repository.lookupClass("ibis.satin.so.WriteMethodsInterface");

        if (writeMethodsInterface == null) {
            System.err.println("Class WriteMethodInterface not found");
            System.exit(1);
        }

	//lookup classes from the argument list
        for (int i = 0; i < classList.size(); i++) {
            JavaClass c = Repository.lookupClass((String)classList.get(i));
            if (c == null) {
                System.err.println("Class " + ((String)classList.get(i)) + " not found");
                System.exit(1);
            }
            classes.addElement(c);
        }

	//for each class in the argument list..
        for (int i = 0; i < classes.size(); i++) {

            try {

//                PrintWriter output;
                JavaClass subjectClass = (JavaClass) classes.get(i);

                if (subjectClass.isInterface()) {
                    continue;
                }

		//check if it derives from the ibis.satin.so.SharedObject class
		if (!Repository.instanceOf(subjectClass, 
					   "ibis.satin.so.SharedObject")) {
		    continue;
		}

                String className = subjectClass.getClassName();
                String packageName = subjectClass.getPackageName();
                String classNameNoPackage;
                if (packageName != null && ! packageName.equals("")) {
                    classNameNoPackage = className.substring(className.lastIndexOf('.')+1, className.length());
                } else {
                    classNameNoPackage = className;
                }

 		classGen = new ClassGen(subjectClass);
		insFactory = new InstructionFactory(classGen);
		cpg = classGen.getConstantPool();

                if (verbose) {
                    System.out.println("Handling " + className);
                }

		if (classGen.containsField("$SOrewritten$")!=null) {
		    System.err.println("Class " + className 
				       + " is already rewritten");
		    continue;
		}
		
		BT_Analyzer a = new BT_Analyzer(subjectClass, 
						writeMethodsInterface, verbose);
		a.start(false);


		//		Vector methods = a.subjectSpecialMethods;
		Method[] methods = subjectClass.getMethods();

		if (methods == null) {
		    continue;
		}
		    
		//add the $SOrewritten$ field
		classGen.addField(new FieldGen(Constants.ACC_STATIC, Type.BOOLEAN,
					    "$SOrewritten$", cpg).getField());		

		//add things to the constant pool
		/*		int getSatin = cpg.addMethodref("ibis.satin.impl.Satin",
				       "getSatin",
				       "()Libis/satin/impl/Satin");
		int broadcastSOInvocation = 
		    cpg.addMethodref("ibis.satin.impl.Satin",
				     "broadcastSOInvocation",
				     "(Libis/satin/impl/SOInvocationRecord)V");*/
	    
		//rewrite methods
		for (int j = 0; j < methods.length; j ++) {		    
		    Method method = methods[j];		    
		    //		    System.err.println(method.toString());
		    if (a.isSpecial(method)) {
			//change the name of the method to so_local_methodName
			//create a new methodName with the following body
			//Satin.getSatin().broadcastSOInvocation(new SOInvRecord())
			//return so_local_methodName
			rewriteMethod(method, classNameNoPackage, packageName);


		    }
		    /*	    if(method.getName().equals("<init>")) {
			System.err.println("rewriting constructor");
			rewriteConstructor(method, subjectClass.getClassName());
			}*/

		}

		JavaClass newSubjectClass = classGen.getJavaClass();
		Repository.removeClass(subjectClass);
		Repository.addClass(newSubjectClass);

		//dump the class
                String dst;
                if (local) {
                    dst = className.substring(className.lastIndexOf('.')+1);
                } else {
                    dst = className.replace('.', java.io.File.separatorChar);
                }
                dst = dst + ".class";
		newSubjectClass.dump(dst);

		//generate so invocation records
		for (int j = 0; j < methods.length; j ++) {
		    Method method = methods[j];
		    if (a.isSpecial(method)) {
			//generate an SOInvocationRecord for this method
			writeSOInvocationRecord(method, classNameNoPackage,
                                packageName);

			compile(SOInvocationRecordFileBase(method, classNameNoPackage, packageName));
		    }
		}

            } catch (Exception e) {
                System.err.println("Got exception during processing of "
                        + ((JavaClass) classes.get(i)).getClassName());
                System.err.println("exception is: " + e);
                e.printStackTrace();
                System.exit(1);
            }
        }

    }

    public static void usage() {
        System.err.println("Usage : java SatinSOc [[-no]-verbose] [[-no]-keep] "
			   + "[-dir|-local] "
			   + "<classname>*");
        System.exit(1);
    }

    private static void writeSOInvocationRecord(Method m, String clname,
            String pnam) throws java.io.IOException {
        String name = SOInvocationRecordFileBase(m, clname, pnam);
        int i;

        FileOutputStream f = new FileOutputStream(name + ".java");
        BufferedOutputStream b = new BufferedOutputStream(f);
        DollarFilter b2 = new DollarFilter(b);
        PrintStream out = new PrintStream(b2);

        if (verbose) {
            System.err.println("Generating inv rec for method: " + m.getName()
			       + " with signature: " + m.getSignature());
        }

        /* Copied from MethodTable.java; I have no clue why
           m.getArgumentTypes is not used*/
        Type[] params = Type.getArgumentTypes(m.getSignature());
        String[] params_types_as_names = new String[params.length];

        for (i = 0; i < params.length; i++ ) {
            if (params[i] instanceof ObjectType) {
                String clnam = ((ObjectType) params[i]).getClassName();
                if (!Repository.implementationOf(clnam, 
                                                 "java.io.Serializable")) {
                    System.err.println(clname
                               + ": write method"
                               + " with non-serializable parameter type "
                               + clnam);
                    System.err.println(clname
                               + ": all parameters of a write method"
                               + " must be serializable.");
                    System.exit(1);
                }
            }
            params_types_as_names[i] = params[i].toString();
        }

        if (pnam != null && ! pnam.equals("")) {
            out.println("package " + pnam + ";");
        }

        out.println("import ibis.satin.so.*;\n");
        out.println("import ibis.satin.impl.*;\n");

        name = SOInvocationRecordClassName(m, clname, pnam);

        out.println("public final class " + name 
                + " extends SOInvocationRecord {");

        //fields
        for (i = 0; i < params_types_as_names.length; i++ ) {
            out.println("\t" + params_types_as_names[i] + " param" 
                        + i + ";");
        }
        out.println();

        //constructor
        out.print("\tpublic " + name + "(String objectId, ");
        for (i = 0; i < params_types_as_names.length-1; i++) {
            out.print(params_types_as_names[i] + " param" + i + ", ");
        }
        out.println(params_types_as_names[i] + " param" + i + ") {");
        out.println("\t\tsuper(objectId);");
        for (i = 0; i < params_types_as_names.length; i++) {
            //		if (params[i] instanceof BasicType) {
                out.println("\t\tthis.param" + i + " = param" + i + ";");
                /*		} else {
                //copy the parameter
                out.println("\t\tthis.param" + i + " = (" + params_types_as_names[i]
                            + ") cloneObject(param" + i + ");");
                            }*/
        }
        out.println("\t}\n");

        //invoke method
        out.println("\tpublic void invoke(SharedObject object) {");
        out.println("\t\t" + clname + " obj = (" + clname + ") object;");
        out.println("\t\ttry{");
        out.print("\t\t\tobj.so_local_" + m.getName() + "(");
        for (i = 0; i < params_types_as_names.length-1; i++ ) {
            out.print("param" + i + ", ");
        }
        out.println("param" + i + ");");
        out.println("\t\t} catch (Throwable t) {");
        out.println("\t\t\t/* exceptions will be only thrown at the originating node*/");
        out.println("\t\t}");
        out.println("\t}");
        out.println();
        out.println("}");
        out.close();
    }

    public static String getFileBase(String pkg, String name, String pre,
            String post) {
        if (!local && pkg != null && !pkg.equals("")) {
            return pkg.replace('.', File.separatorChar) + File.separator + pre
                    + name + post;
        }
        return pre + name + post;
    }

    static String SOInvocationRecordName(Method m, String clnam, String pnam) {
        if (pnam != null && ! pnam.equals("")) {
            return pnam + ".Satin_" + clnam
                    + "_" + do_mangle(m) + "_SOInvocationRecord";
        }
        return "Satin_" + clnam + "_" + do_mangle(m)
                + "_InvocationRecord";
    }

    static String SOInvocationRecordFileBase(Method m, String clnam,
            String pnam) {
        return getFileBase(pnam, clnam + "_" + do_mangle(m), "Satin_",
                "_SOInvocationRecord");
    }

    static String SOInvocationRecordClassName(Method m, String clnam,
            String pnam) {
        return getFileBase(null, clnam + "_" + do_mangle(m), "Satin_",
                "_SOInvocationRecord");
    }

    static void removeFile(String name) {
        if (verbose) {
            System.out.println("removing " + name);
        }

        try {
            File f = new File(name);
            f.delete();
        } catch (Exception e) {
            System.err.println("Warning: could not remove " + name);
        }
    }

    static void compile(String name) {
        String filename = name + ".java";
        if (! RunJavac.runJavac(new String[] { "-g" },
                    new String[] {filename}, verbose)) {
            System.exit(1);
        }

        Repository.lookupClass(name);

        if (!keep) { // remove generated files 
            removeFile(filename);
        }
    }

    static void rewriteMethod(Method m, String clnam, String pnam) {
       MethodGen origMethodGen;
       MethodGen newMethodGen;
       InstructionList newMethodInsList;
       Type[] arguments;
       Type[] objectId_and_arguments;
       Type returnType;
       int oldAccessFlags;
       int monitorVarAddr;
       String classname = clnam;

       if (pnam != null && ! pnam.equals("")) {
           classname = pnam + "." + clnam;
       }

       if (verbose) {
           System.err.println("Rewriting method: " + m.getName() 
                              + " with signature: " + m.getSignature());
        }

       //prefix the original method name with so_local
       origMethodGen = new MethodGen(m, classname, cpg);
       origMethodGen.setName("so_local_" + m.getName());
       origMethodGen.setMaxStack();
       origMethodGen.setMaxLocals();
       oldAccessFlags = origMethodGen.getAccessFlags();
       origMethodGen.setAccessFlags(0x0001); //public
       classGen.removeMethod(m);
       classGen.addMethod(origMethodGen.getMethod());
       
       //create the new method with the following body:
       //Satin.getSatin().
       //broadcastSOInvocation(new Satin_methodname_SOInvocationRecord(params))
       //synchronized(Satin.getSatin()) {
       //return so_local_methodname(params);
       //}

	newMethodInsList = new InstructionList();
	//broadcast 
	newMethodInsList.append(insFactory.createInvoke("ibis.satin.impl.Satin",
						     "getSatin",
						     new ObjectType("ibis.satin.impl.Satin"),
						     new Type[] {},
						     Constants.INVOKESTATIC));
	newMethodInsList.append(insFactory.createNew(SOInvocationRecordName(m, clnam, pnam)));
	newMethodInsList.append(InstructionFactory.createDup(1));
	newMethodInsList.append(new ALOAD(0));
	newMethodInsList.append(insFactory.createGetField(classname,
						       "objectId",
						       Type.STRING));
	arguments = m.getArgumentTypes();			     
	int k1 = 0;
	for (int k = 0; k < arguments.length; k++) {
	    newMethodInsList.append(InstructionFactory.createLoad(arguments[k], k1+1));
	    k1 += arguments[k].getSize();
	}

	objectId_and_arguments = new Type[arguments.length+1];
	objectId_and_arguments[0] = Type.STRING;
	for (int k = 0; k < arguments.length; k++) {
	    objectId_and_arguments[k+1] = arguments[k];
	}

	newMethodInsList.append(insFactory.createInvoke(SOInvocationRecordName(m, clnam, pnam), 
						   "<init>",
						   Type.VOID,
						   objectId_and_arguments,
						   Constants.INVOKESPECIAL));
	newMethodInsList.append(insFactory.createInvoke("ibis.satin.impl.Satin",
						    "broadcastSOInvocation",
						    Type.VOID,
						    new Type[] 
	    {new ObjectType("ibis.satin.impl.SOInvocationRecord")},
						    Constants.INVOKEVIRTUAL));
	//enter the monitor	
	monitorVarAddr = 1;
	for (int k = 0; k < arguments.length; k++) {
	    monitorVarAddr += arguments[k].getSize();
	}
	newMethodInsList.append(insFactory.createInvoke("ibis.satin.impl.Satin",
							 "getSatin",
							 new ObjectType("ibis.satin.impl.Satin"),
							 new Type[] {},
							 Constants.INVOKESTATIC));
	newMethodInsList.append(InstructionFactory.createDup(1));
	newMethodInsList.append(new ASTORE(monitorVarAddr));
	newMethodInsList.append(new MONITORENTER());
	//call the object method
	InstructionHandle from1 = newMethodInsList.append(new ALOAD(0));
	k1 = 0;
	for (int k = 0; k < arguments.length; k++) {
	    newMethodInsList.append(InstructionFactory.createLoad(arguments[k], k1+1));
	    k1 += arguments[k].getSize();
	}
	returnType = m.getReturnType();
	newMethodInsList.append(insFactory.createInvoke(classname,
						     "so_local_" + m.getName(),
						     returnType,
						     arguments,
						     Constants.INVOKEVIRTUAL));
	//exit the monitor
	newMethodInsList.append(new ALOAD(monitorVarAddr));
	newMethodInsList.append(new MONITOREXIT());
	//return statement
	InstructionHandle to1 = newMethodInsList.append(InstructionFactory.createReturn(returnType));
	//exception handlers
	InstructionHandle from2 = newMethodInsList.append(new ASTORE(monitorVarAddr+1));
	newMethodInsList.append(new ALOAD(monitorVarAddr));
	newMethodInsList.append(new MONITOREXIT());
	InstructionHandle to2 = newMethodInsList.append(new ALOAD(monitorVarAddr+1));
	newMethodInsList.append(new ATHROW());

	newMethodGen = new MethodGen(oldAccessFlags,
				     returnType,
				     arguments,
				     origMethodGen.getArgumentNames(),
				     m.getName(),
				     classname,
				     newMethodInsList,
				     cpg);
 
	newMethodGen.addExceptionHandler(from1, to1, from2, null);
	newMethodGen.addExceptionHandler(from2, to2, from2, null);
	newMethodGen.setMaxStack();
	newMethodGen.setMaxLocals();
		    
	classGen.addMethod(newMethodGen.getMethod());
	 	
    }

   static void rewriteConstructor(Method m, String clnam) {

       MethodGen methodGen = new MethodGen(m, clnam, cpg);
       InstructionList il = methodGen.getInstructionList();

       InstructionHandle ret_ih = il.getEnd();

       //add 'Satin.getSatin().broadcastSharedObject(this);' at the end
       //of the constructor

       il.insert(ret_ih, 
		 insFactory.createInvoke("ibis.satin.impl.Satin",
					 "getSatin",
					 new ObjectType("ibis.satin.impl.Satin"),
					 new Type[] {},
					 Constants.INVOKESTATIC));
       il.insert(ret_ih, new ALOAD(0));
       il.insert(ret_ih,
		 insFactory.createInvoke("ibis.satin.impl.Satin",
					 "broadcastSharedObject",
					 Type.VOID,
					 new Type[] {new ObjectType("ibis.satin.so.SharedObject")},
					 Constants.INVOKEVIRTUAL));

       System.err.println("il length: " + il.getLength());

       methodGen.setInstructionList(il);
       methodGen.setMaxStack();
       methodGen.setMaxLocals();
       classGen.removeMethod(m);
       classGen.addMethod(methodGen.getMethod());
       
   }

    static String do_mangle(Method m) {
        return do_mangle(m.getName(), m.getSignature());
    }

    static String do_mangle(StringBuffer s) {
        // OK, now sanitize parameters
        int i = 0;
        while (i < s.length()) {
            switch (s.charAt(i)) {
            case '$':
            case '.':
            case '/':
                s.setCharAt(i, '_');
                break;

            case '_':
                s.replace(i, i + 1, "_1");
                break;

            case ';':
                s.replace(i, i + 1, "_2");
                break;

            case '[':
                s.replace(i, i + 1, "_3");
                break;

            default:
                break;
            }
            i++;
        }
        return s.toString();
    }

    static String do_mangle(String name, String sig) {
        StringBuffer s = new StringBuffer(sig);
        name = do_mangle(new StringBuffer(name));

        int open = sig.indexOf("(");
        if (open == -1) {
            return name;
        }
        s.delete(0, open + 1);

        sig = s.toString();

        int close = sig.indexOf(")");
        if (close == -1) {
            return name;
        }
        s.delete(close, s.length());

        return name + "__" + do_mangle(s);
    }

    private static class DollarFilter extends FilterOutputStream {
	DollarFilter(OutputStream out) {
	    super(out);
	}

	public void write(int b) throws IOException {
	    if (b == '$') {
		super.write('.');
	    } else {
		super.write(b);
	    }
	}
    }

	    
}
