package ibis.frontend.repmi;

import com.ibm.jikesbt.*;   

import java.util.Vector;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import ibis.util.BT_Analyzer;

class RepMISkeletonGenerator extends RepMIGenerator { 

	BT_Analyzer data;
	PrintWriter output;
	boolean verbose;

	String dest_name;

	RepMISkeletonGenerator(BT_Analyzer data, PrintWriter output, boolean verbose) {
		this.data   = data;		
		this.output = output;
		this.verbose = verbose;
	} 
     
	void header() { 
		if (data.packagename != null) {
			output.println("package " + data.packagename + ";");		
			output.println();
		}

		output.println("import manta.repmi.*;");		
		output.println("import manta.ibis.*;");		
		output.println("import java.lang.reflect.*;");		
		output.println();
	
		output.println("public final class repmi_skeleton_" + dest_name + " extends manta.repmi.Skeleton {");
		output.println();		
	} 

	void messageHandler(Vector methods) { 

		output.println("\tpublic final void handleMessage(ReadMessage r) throws IbisException {");
		output.println();		

		output.println("\t\tint method = r.readInt();");		
		output.println();		
		
		output.println("\t\tswitch(method) {");		

		for (int i=0;i<methods.size();i++) { 
			BT_Method m = (BT_Method) methods.get(i);
			BT_Class ret = getReturnType(m);
			BT_ClassVector params = getParameterTypes(m);
			
			output.println("\t\tcase " + i + ":");
			output.println("\t\t{");		       

			output.println("\t\t\t/* First - Extract the parameters */");		       
			
			for (int j=0;j<params.size();j++) { 
				BT_Class temp = (BT_Class) params.elementAt(j);
				output.println(readMessageType("\t\t\t", getType(temp) + " p" + j, "r", temp));
			}
			
			output.println("\t\t\tr.finish();");
			output.println();
			
			output.println("\t\t\t/* Second - Invoke the method */");		       

			output.print("\t\t\t");
		
			if (!ret.equals(BT_Class.getVoid())) { 
				if (ret.isPrimitive()) {					
					output.print(getType(ret) + " result = ");
				} else { 
					output.print(getType(ret) + " result = ");
				}		
			} 

			output.print("((" + dest_name + ") destination)." + m.getName() + "(");
			
			for (int j=0;j<params.size();j++) { 
				output.print("p" + j);
				
				if (j<params.size()-1) { 
					output.print(", ");
				}
			}
			output.println(");");			       
			output.println("\t\t\tbreak;");
			output.println("\t\t}");
			output.println();
		}
		

		output.println("\t\tdefault:");
		output.println("\t\t\tSystem.err.println(\"OOPS: group_skeleton got illegal method number!\");");
		output.println("\t\t\tSystem.exit(1);");
		output.println("\t\t\tbreak;");
		
		output.println("\t\t}");
		output.println("\t}");
		output.println();			       
	} 
	       
	void trailer() { 
		output.println("}\n");
	} 
       
	void constructor(Vector methods) { 

		output.println("\tpublic repmi_skeleton_" + data.classname + "() {");

		output.println("\t\tsuper();");
		output.println();
		output.println("\t}\n");
	} 

	void generate() { 

		dest_name = data.classname;

		header();		
		constructor(data.specialMethods);
		messageHandler(data.specialMethods);		
		trailer();
	} 
} 
