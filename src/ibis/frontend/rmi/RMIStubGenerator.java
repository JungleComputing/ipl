package ibis.frontend.rmi;

import com.ibm.jikesbt.*;   

import java.util.Vector;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import ibis.util.BT_Analyzer;

class RMIStubGenerator extends RMIGenerator { 

	BT_Analyzer data;
	PrintWriter output;
	boolean verbose;

	BT_Ins putfield;

	RMIStubGenerator(BT_Analyzer data, PrintWriter output, boolean verbose) {
		this.data   = data;		
		this.output = output;
		this.verbose = verbose;
	} 

	boolean isWriteMethod(BT_Method m) { 
		BT_CodeAttribute code = m.getCode();
		BT_InsVector ins = code.ins;
		BT_Ins [] bytes = ins.toArray();

		for (int i=0;i<bytes.length;i++) { 
			if (bytes[i].opcode == BT_Ins.opc_putfield) { 
				return true;
			} 
		}

		return false;
	} 

	void methodHeader(BT_Method m, int number) { 
		
		BT_Class ret          = getReturnType(m);
		BT_ClassVector params = getParameterTypes(m);
		
		output.print("\tpublic final " + getType(ret) + " " + m.getName() + "(");
		
		for (int j=0;j<params.size();j++) { 
			output.print(getType((BT_Class) params.elementAt(j)) + " p" + j);

			if (j<params.size()-1) { 
				output.print(", ");
			} 
		}
		
		output.print(") throws java.rmi.RemoteException {\n");
	}

	void methodBody(BT_Method m, int number) { 
		
	    //boolean write = isWriteMethod(m);
	    BT_Class ret = getReturnType(m);
	    BT_ClassVector params = getParameterTypes(m);
	    
	    //if (verbose) System.out.println(m.getName() + " is a " + (write ? "write" : "read") + " method");
	    
//	    output.println("\t\t\tif (RTS.DEBUG) System.out.println(\"rmi_stub_" + data.classname + "." + m.getName() + " doing RMI call\");");	

	    if (!ret.equals(BT_Class.getVoid())) { 
		output.println("\t\t" + getInitedLocal(ret, "result") + ";");
	    }

	    output.println("\t\ttry {");
	    output.println("\t\t\tWriteMessage w = send.newMessage();");
	    output.println("\t\t\tw.writeInt(" + number + ");");
	    output.println("\t\t\tw.writeInt(stubID);");

	    for (int j=0;j<params.size();j++) { 
		output.println(writeMessageType("\t\t\t", "w", params.elementAt(j), "p" + j));
	    }
	    
	    output.println("\t\t\tw.send();");
	    output.println("\t\t\tw.finish();");

	    output.println("\t\t\tReadMessage r = reply.receive();");
	    output.println("\t\t\tif (r.readByte() == ibis.rmi.Protocol.EXCEPTION) {");
	    output.println("\t\t\t\tException e = (Exception) r.readObject();");
	    output.println("\t\t\t\tr.finish();");
	    output.println("\t\t\t\tthrow e;");
	    output.println("\t\t\t}");
	    output.println("\t\t\t//else: normal result");	    
	    if (!ret.equals(BT_Class.getVoid())) { 		
		output.println(readMessageType("\t\t\t", "result", "r", ret));
	    }
	    output.println("\t\t\tr.finish();");

	    output.println("\t\t} catch (Exception e) {");
	    output.println("\t\t\tthrow new RemoteException(\"oops\" + e);");
	    output.println("\t\t}");
	} 

	void methodTrailer(BT_Method m) { 

		BT_Class ret = getReturnType(m);

		if (!ret.equals(BT_Class.getVoid())) {       
			output.println("\t\treturn result;"); 
		} 
		output.println("\t}\n");			
	} 

	void header() { 

		Vector interfaces = data.specialInterfaces;

		if (data.packagename != null) { 
			output.println("package " + data.packagename + ";");		
			output.println();
		}

		output.println("import ibis.rmi.*;");		
		output.println("import ibis.ipl.*;");			
		output.println("import java.rmi.RemoteException;");			
		output.println();

		output.print("public final class rmi_stub_" + data.classname + " extends ibis.rmi.Stub implements ");		

		for (int i=0;i<interfaces.size();i++) { 
			output.print(getType((BT_Class) interfaces.get(i)));

			if (i<interfaces.size()-1) { 
				output.print(", ");
			}  
		}
			
		output.println(" {\n");
	} 

	void constructor() { 
		output.println("\tpublic rmi_stub_" + data.classname + "() {");
//		output.println("\t\tsuper();");
		output.println("\t}\n");
	} 

	void body(Vector methods) { 

		for (int i=0;i<methods.size();i++) { 
			BT_Method m = (BT_Method) methods.get(i);
			methodHeader(m, i);
			methodBody(m, i);
			methodTrailer(m);		
		} 
	} 
	       
	void trailer() { 
		output.println("}\n");
	} 

	void generate() { 		
		header();		
		constructor();		
		body(data.subjectSpecialMethods);
		trailer();
	} 

} 

