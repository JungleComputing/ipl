package ibis.frontend.repmi;

import ibis.util.BT_Analyzer;

import java.io.PrintWriter;
import java.util.Vector;

import org.apache.bcel.Constants;
import org.apache.bcel.classfile.Code;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionList;
import org.apache.bcel.generic.Type;

class RepMIStubGenerator extends RepMIGenerator { 

	BT_Analyzer data;
	PrintWriter output;
	boolean verbose;

	RepMIStubGenerator(BT_Analyzer data, PrintWriter output, boolean verbose) {
		this.data   = data;		
		this.output = output;
		this.verbose = verbose;
	} 

	boolean isWriteMethod(Method m) { 
		Code code = m.getCode();
		InstructionList il = new InstructionList(code.getCode());
		Instruction[] ins = il.getInstructions();

		for (int i=0;i<ins.length;i++) { 
			if (ins[i].getOpcode() == Constants.PUTFIELD) { 
				return true;
			} 
		}

		return false;
	} 

	void methodHeader(Method m) { 
		Type ret      = Type.getReturnType(m.getSignature());
		Type[] params = Type.getArgumentTypes(m.getSignature());
	       
		output.print("\tpublic final " + ret + " " + m.getName() + "(");
		
		for (int j=0;j<params.length;j++) { 
			output.print(params[j] + " p" + j);

			if (j<params.length-1) { 
				output.print(", ");
			} 
		}
		
		output.print(") {\n");
			
		if (!ret.equals(Type.VOID)) {
			output.print("\t\t" + ret + " result = ");
			if (ret.equals(Type.BYTE)) {
				output.println("(byte) 0;");
			} else if (ret.equals(Type.CHAR)) {
				output.println("(char) 0;");
			} else if (ret.equals(Type.SHORT)) {
				output.println("(short) 0;");
			} else if (ret.equals(Type.INT)) {
				output.println("(int) 0;");
			} else if (ret.equals(Type.LONG)) {
				output.println("(long) 0;");
			} else if (ret.equals(Type.FLOAT)) {
				output.println("(float) 0.0;");
			} else if (ret.equals(Type.DOUBLE)) {
				output.println("(double) 0.0;");
			} else if (ret.equals(Type.BOOLEAN)) {
				output.println("false;");
			} else { 
				output.println("null;");
			} 
		} 
	}

	void methodBody(Method m, int number) { 
		
		boolean write = isWriteMethod(m);
		Type ret      = Type.getReturnType(m.getSignature());
		Type[] params = Type.getArgumentTypes(m.getSignature());

		output.println("\t\ttry {");			

		if (verbose) System.out.println(m.getName() + " is a " + (write ? "write" : "read") + " method");

		if (!write) { 
			output.println("\t\t\tif (RTS.DEBUG) System.out.println(\"repmi_stub_" + data.classname + "." + m.getName() + " doing LOCAL call\");");				
			output.print("\t\t\t");				

			if (!ret.equals(Type.VOID)) { 
				output.print("result = ");			
			} 
		
			output.print("(("+ data.classname + ")localSkeleton.destination)." + m.getName() + "(");
			for (int j=0;j<params.length;j++) { 
				
				output.print("p" + j);
				
				if (j<params.length-1) { 
					output.print(", ");
				}
			}
			output.println(");");			       
			output.println();
		} else { 
			output.println("\t\t\tif (RTS.DEBUG) System.out.println(\"repmi_stub_" + data.classname + "." + m.getName() + " doing GROUP call\");");	

			output.println("\t\t\tWriteMessage w = RTS.multicast.newMessage();");
			output.println("\t\t\tw.writeByte(ibis.repmi.Protocol.INVOCATION);");
			output.println("\t\t\tw.writeInt(objectID);");
			
			output.println("\t\t\tw.writeInt(" + number + ");");
			
			for (int j=0;j<params.length;j++) { 
				output.println(writeMessageType("\t\t\t", "w", params[j], "p" + j));
			}
		
			output.println("\t\t\tw.finish();");
		}

		output.println("\t\t} catch (Exception e) {");
		output.println("\t\t\tSystem.out.println(\"OOPS : \" + e);");
		output.println("\t\t\tSystem.exit(1);");
		output.println("\t\t}");
	} 

	void methodTrailer(Method m) { 
		Type ret      = Type.getReturnType(m.getSignature());

		if (!ret.equals(Type.VOID)) {       
			output.println("\t\treturn result;"); 
		} 
		
		output.println("\t}\n");			
	} 

	void header() { 

		Vector interfaces = data.specialInterfaces;

		if (data.packagename != null && ! data.packagename.equals("")) { 
			output.println("package " + data.packagename + ";");		
			output.println();
		}

		output.println("import ibis.repmi.*;");		
		output.println("import ibis.ipl.*;");		
		output.println();

		output.print("public final class repmi_stub_" + data.classname + " extends ibis.repmi.Stub implements ");		

		for (int i=0;i<interfaces.size();i++) { 
			output.print(((JavaClass) interfaces.get(i)).getClassName());

			if (i<interfaces.size()-1) { 
				output.print(", ");
			}  
		}
			
		output.println(" {\n");
	} 

	void constructor() { 
		output.println("\tpublic repmi_stub_" + data.classname + "() {");
		output.println("\t\tsuper();");
		output.println("\t}\n");
	} 

	void body(Vector methods) { 

		for (int i=0;i<methods.size();i++) { 
			Method m = (Method) methods.get(i);
			methodHeader(m);
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
