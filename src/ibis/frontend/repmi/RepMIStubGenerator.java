package ibis.frontend.repmi;

import com.ibm.jikesbt.*;   

import java.util.Vector;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import ibis.util.BT_Analyzer;

class RepMIStubGenerator extends RepMIGenerator { 

	BT_Analyzer data;
	PrintWriter output;
	boolean verbose;

	BT_Ins putfield;

	RepMIStubGenerator(BT_Analyzer data, PrintWriter output, boolean verbose) {
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

	void methodHeader(BT_Method m) { 
		
		BT_Class ret          = getReturnType(m);
		BT_ClassVector params = getParameterTypes(m);
	       
		output.print("\tpublic final " + getType(ret) + " " + m.getName() + "(");
		
		for (int j=0;j<params.size();j++) { 
			output.print(getType((BT_Class) params.elementAt(j)) + " p" + j);

			if (j<params.size()-1) { 
				output.print(", ");
			} 
		}
		
		output.print(") {\n");
			
		if (!ret.equals(BT_Class.getVoid())) { 
			
			if (ret.isPrimitive()) {
				
				output.print("\t\t" + getType(ret) + " result = ");
				
				if (ret.equals(BT_Class.getByte())) { 
					output.println("(byte) 0;");
				} else if (ret.equals(BT_Class.getChar())) { 
					output.println("(char) 0;");
				} else if (ret.equals(BT_Class.getShort())) {
					output.println("(short) 0;");
				} else if (ret.equals(BT_Class.getInt())) {
					output.println("(int) 0;");
				} else if (ret.equals(BT_Class.getLong())) {
					output.println("(long) 0;");
				} else if (ret.equals(BT_Class.getFloat())) {
					output.println("(float) 0.0;");
				} else if (ret.equals(BT_Class.getDouble())) {
					output.println("(double) 0.0;");
				} else if (ret.equals(BT_Class.getBoolean())) {
					output.println("false;");
				} 					       
			} else { 
				output.println("\t\t" + getType(ret) + " result = null;");
			} 
		
		} 
	}

	void methodBody(BT_Method m, int number) { 
		
		boolean write = isWriteMethod(m);
		BT_Class ret = getReturnType(m);
		BT_ClassVector params = getParameterTypes(m);

		output.println("\t\ttry {");			

		if (verbose) System.out.println(m.getName() + " is a " + (write ? "write" : "read") + " method");

		if (!write) { 
			output.println("\t\t\tif (RTS.DEBUG) System.out.println(\"repmi_stub_" + data.classname + "." + m.getName() + " doing LOCAL call\");");				
			output.print("\t\t\t");				

			if (!ret.equals(BT_Class.getVoid())) { 
				output.print("result = ");			
			} 
		
			output.print("(("+ data.classname + ")localSkeleton.destination)." + m.getName() + "(");
			for (int j=0;j<params.size();j++) { 
				
				output.print("p" + j);
				
				if (j<params.size()-1) { 
					output.print(", ");
				}
			}
			output.println(");");			       
			output.println();
		} else { 
			output.println("\t\t\tif (RTS.DEBUG) System.out.println(\"repmi_stub_" + data.classname + "." + m.getName() + " doing GROUP call\");");	

			output.println("\t\t\tWriteMessage w = RTS.multicast.newMessage();");
			output.println("\t\t\tw.writeByte(manta.repmi.Protocol.INVOCATION);");
			output.println("\t\t\tw.writeInt(objectID);");
			
			output.println("\t\t\tw.writeInt(" + number + ");");
			
			for (int j=0;j<params.size();j++) { 
				output.println(writeMessageType("\t\t\t", "w", params.elementAt(j), "p" + j));
			}
		
			output.println("\t\t\tw.send();");
			output.println("\t\t\tw.finish();");
		}

		output.println("\t\t} catch (Exception e) {");
		output.println("\t\t\tSystem.out.println(\"OOPS : \" + e);");
		output.println("\t\t\tSystem.exit(1);");
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

		output.println("import manta.repmi.*;");		
		output.println("import manta.ibis.*;");		
		output.println();

		output.print("public final class repmi_stub_" + data.classname + " extends manta.repmi.Stub implements ");		

		for (int i=0;i<interfaces.size();i++) { 
			output.print(getType((BT_Class) interfaces.get(i)));

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
			BT_Method m = (BT_Method) methods.get(i);
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
