package ibis.frontend.group;

import java.util.Vector;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import ibis.util.Analyzer;

class GMIParameterVectorGenerator extends GMIGenerator { 
    
    Analyzer data;
    PrintWriter output;
    boolean verbose;
    String name;

    GMIParameterVectorGenerator(Analyzer data, PrintWriter output, boolean verbose) {
	this.data    = data;		
	this.output  = output;
	this.verbose = verbose;
	name = "group_parameter_vector_" + data.subject.getName() + "_";
    } 

    void header(Method m) { 

	Class [] params = m.getParameterTypes();
	String name = this.name + m.getName();

	output.println("final class " + name + " extends ibis.group.ParameterVector {\n");	
	output.println("\tprivate final static int TOTAL_PARAMS = " + params.length + ";\n");	

	/* Parameter fields */
	for (int j=0;j<params.length;j++) { 
	    
	    if (j == 0) { 
		output.println("\t/* Parameters */");
	    }									
	    
	    output.println("\t" + getType(params[j]) + " p" + j + ";");
	    
	    if (params[j].isArray() || 
	        params[j].getName().equals("java.lang.Object") || 
	        params[j].getName().equals("java.lang.Cloneable") ||
	        params[j].getName().equals("java.lang.Serializable")) { 
		/* we may write a sub array here !!! */					
		output.println("\tboolean p" + j + "_subarray = false;");
		output.println("\tint p" + j + "_offset, p" + j + "_size;");
	    } 
	    
	    if (j == params.length-1) { 
		output.println();
	    } 			
	}

	/* Constructor */
	output.println("\t" + name + "() {");
	output.println("\t\tset = new boolean[" + params.length + "];");
	output.println("\t\treset();");
	output.println("\t}\n");

	/* Method that creates a parameter vector of the same type. */
	output.println("\tpublic final ParameterVector getVector() {");
	output.println("\t\treturn new " + name + "();");
	output.println("\t}\n");

	/* Reset method. */
	output.println("\tpublic final void reset() {");
	output.println("\t\tsuper.reset();");
	for (int j=0;j<params.length;j++) { 
	    if (params[j].isArray() || 
	        params[j].getName().equals("java.lang.Object") || 
	        params[j].getName().equals("java.lang.Cloneable") ||
	        params[j].getName().equals("java.lang.Serializable")) { 
		output.println("\t\tp" + j + "_subarray = false;");
	    } 
	}
	output.println("\t\tset_count = 0;");
	output.println("\t\tdone = false;");
	output.println("\t\tfor (int i=0;i<" +params.length+ ";i++) set[i] = false;");

	output.println("\t}\n");
    }


    void writeMethod(Class param, int [] numbers, int count) { 

	if (param.isPrimitive()) {
	    output.println("\tpublic final void write(int num, " + getType(param) + " value) {");
	}
	else {
	    output.println("\tpublic final void write(int num, " + printType(param) + " value) {");
	}
	
	output.println("\t\tif (set[num]) throw new RuntimeException(\"Parameter \" + num + \" already set!\");");	

	if (count >= 2) { 
	    output.println("\t\tswitch (num) {");		
	    for (int i=0;i<count;i++) { 
		output.println("\t\tcase " + numbers[i] + ":");		
		if (param.isPrimitive()) {
		    output.println("\t\t\tp" + numbers[i] + " = value;");		
		}
		else {
		    output.println("\t\t\tp" + numbers[i] + " = (" + getType(param) + ") value;");		
		}
		output.println("\t\t\tbreak;");		
	    }
	    output.println("\t\tdefault:");		
	    output.println("\t\t\tthrow new RuntimeException(\"Illegal parameter number or type\");");		
	    output.println("\t\t}");		
	} else { 
	    output.println("\t\tif (num == " + numbers[0] + ") {");
	    if (param.isPrimitive()) {
		output.println("\t\t\tp" + numbers[0] + " = value;");		
	    }
	    else {
		output.println("\t\t\tp" + numbers[0] + " = (" + getType(param) + ") value;");		
	    }
	    output.println("\t\t} else {");
	    output.println("\t\t\tthrow new RuntimeException(\"Illegal parameter number or type\");");	
	    output.println("\t\t}");
	}
	output.println("\t\tset[num] = true;");
	output.println("\t\tif (++set_count == TOTAL_PARAMS) done = true;");		
	output.println("\t}\n");
    } 

    void readMethod(Class param, int [] numbers, int count) { 

	if (param.isPrimitive()) {
	    output.println("\tpublic final " + getType(param) + " read" + printType(param) + "(int num) {");
	}
	else {
	    output.println("\tpublic final Object read" + printType(param) + "(int num) {");
	}
	
	output.println("\t\tif (! set[num]) throw new RuntimeException(\"Parameter \" + num + \" not yet set!\");");	

	if (count >= 2) { 
	    output.println("\t\tswitch (num) {");		
	    for (int i=0;i<count;i++) { 
		output.println("\t\tcase " + numbers[i] + ":");		
		output.println("\t\t\treturn p" + numbers[i] + ";");		
	    }
	    output.println("\t\tdefault:");		
	    output.println("\t\t\tthrow new RuntimeException(\"Illegal parameter number or type\");");		
	    output.println("\t\t}");		
	} else { 
	    output.println("\t\tif (num == " + numbers[0] + ") {");
	    output.println("\t\t\treturn p" + numbers[0] + ";");		
	    output.println("\t\t} else {");
	    output.println("\t\t\tthrow new RuntimeException(\"Illegal parameter number or type\");");	
	    output.println("\t\t}");
	}
	output.println("\t}\n");
    } 

    void writeParametersMethod(Class[] params) {
	output.println("\tpublic void writeParameters(WriteMessage w) throws IbisIOException {");
	for (int i = 0; i < params.length; i++) {
	    if (params[i].isArray()) {
		output.println("\t\tif (p" + i + "_subarray) {");
		output.println("\t\t\tw.writeArray(p"+ i + ", p" + i + "_offset, p" + i + "_size);");
		output.println("\t\t} else {");
		output.println(writeMessageType("\t\t\t", "w", params[i], "p" + i));
		output.println("\t\t}");
	    }
	    else {
		output.println(writeMessageType("\t\t", "w", params[i], "p" + i));
	    }
	}
	output.println("\t}\n");
    }

    void readParametersMethod(Class[] params, Method m) {
	String name = this.name + m.getName();
	output.println("\tpublic ParameterVector readParameters(ReadMessage r) throws IbisIOException {");
	output.println("\t\t" + name + " p = new " + name + "();");
	for (int i = 0; i < params.length; i++) {
	    output.println(readMessageType("\t\t", "p.p" + i, "r", params[i], true));
	    output.println("\t\tp.set[" + i + "] = true;");
	}
	output.println("\t\tdone = true;");
	output.println("\t\treturn p;");
	output.println("\t}\n");
    }

    void writeSubArrayMethod(Class param, int [] numbers, int count) { 

	output.println("\tpublic void writeSubArray(int num, int offset, int size, " + getType(param) + " value) {");
	
	output.println("\t\tif (set[num]) throw new RuntimeException(\"Parameter \" + num + \" already set!\");");	

	if (count >= 2) { 
	    output.println("\t\tswitch (num) {");		
	    for (int i=0;i<count;i++) { 
		output.println("\t\tcase " + numbers[i] + ":");		
		output.println("\t\t\tp" + numbers[i] + " = value;");
		output.println("\t\t\tp" + numbers[i] + "_subarray = true;");
		output.println("\t\t\tp" + numbers[i] + "_offset = offset;");
		output.println("\t\t\tp" + numbers[i] + "_size = size;");
		output.println("\t\t\tbreak;");		
	    }
	    output.println("\t\tdefault:");		
	    output.println("\t\t\tthrow new RuntimeException(\"Illegal parameter number or type\");");		
	    output.println("\t\t}");		
	} else { 
	    output.println("\t\tif (num == " + numbers[0] + ") {");
	    output.println("\t\t\tp" + numbers[0] + " = value;");		
	    output.println("\t\t\tp" + numbers[0] + "_subarray = true;");
	    output.println("\t\t\tp" + numbers[0] + "_offset = offset;");
	    output.println("\t\t\tp" + numbers[0] + "_size = size;");
	    output.println("\t\t} else {");
	    output.println("\t\t\tthrow new RuntimeException(\"Illegal parameter number or type\");");	
	    output.println("\t\t}");
	}
	output.println("\t\tset[num] = true;");
	output.println("\t\tif (++set_count == TOTAL_PARAMS) done = true;");		
	output.println("\t}\n");
    } 

    void writeSubObjectArrayMethod(Class [] param, int [] numbers, int count) { 

	output.println("\tpublic void writeSubArray(int num, int offset, int size, Object [] value) {");
	
	output.println("\t\tif (set[num]) throw new RuntimeException(\"Parameter \" + num + \" already set!\");");	

	if (count >= 2) { 
	    output.println("\t\tswitch (num) {");		
	    for (int i=0;i<count;i++) { 
		output.println("\t\tcase " + numbers[i] + ":");		
		output.println("\t\t\t\tp" + numbers[i] + " = (" + getType(param[i]) + ") value;");	
		output.println("\t\t\tp_" + numbers[i] + "_subarray = true;");
		output.println("\t\t\tp_" + numbers[i] + "_offset = offset;");
		output.println("\t\t\tp_" + numbers[i] + "_size = size;");
		output.println("\t\t\tbreak;");		
	    }
	    output.println("\t\tdefault:");		
	    output.println("\t\t\tthrow new RuntimeException(\"Illegal parameter number or type\");");		
	    output.println("\t\t}");		
	} else { 
	    output.println("\t\tif (num == " + numbers[0] + ") {");
	    output.println("\t\t\tp" + numbers[0] + " = (" + getType(param[0]) + ") value;");	
	    output.println("\t\t\tp_" + numbers[0] + "_subarray = true;");
	    output.println("\t\t\tp_" + numbers[0] + "_offset = offset;");
	    output.println("\t\t\tp_" + numbers[0] + "_size = size;");
	    output.println("\t\t} else {");
	    output.println("\t\t\tthrow new RuntimeException(\"Illegal parameter number or type\");");	
	    output.println("\t\t}");
	}
	output.println("\t\tset[num] = true;");
	output.println("\t\tif (++set_count == TOTAL_PARAMS) done = true;");		
	output.println("\t}\n");
    } 

    void body(Method m) { 

	Class ret       = m.getReturnType();
	Class [] params = m.getParameterTypes();

	writeParametersMethod(params);

	readParametersMethod(params, m);

	if (params.length > 0) { 
				
	    /* Parameter write methods */
	    Class params_to_write = null;

	    Class [] object_params = new Class[params.length];
	    Class [] temp_params = new Class[params.length];
	    System.arraycopy(params, 0, temp_params, 0, params.length);

	    int [] param_numbers = new int[params.length];
	    int count = 0;

	    for (int j=0;j<params.length;j++) {
		if (!params[j].isPrimitive()) { 
		    object_params[count] = params[j];
		    param_numbers[count] = j;
		    count++;
		}
	    } 
			
	    for (int j=0;j<temp_params.length;j++) { 			
		if (j == 0) { 
		    output.println("\t/* Methods to write/read the parameters */");
		}									

		if (temp_params[j] != null) { 
		    count = 0;		
		    params_to_write = temp_params[j];
		    param_numbers[count++] = j; 
		    temp_params[j] = null;

		    for (int i=j+1;i<temp_params.length;i++) { 
			if (params_to_write.equals(temp_params[i])) { 
			    param_numbers[count++] = i; 
			    temp_params[i] = null;
			} 
		    }
		    writeMethod(params_to_write, param_numbers, count);
		    readMethod(params_to_write, param_numbers, count);
		}
	    }

	    params_to_write = null;
	    System.arraycopy(params, 0, temp_params, 0, params.length);
	    count = 0;
	
	    for (int j=0;j<temp_params.length;j++) { 			
		if (j == 0) { 
		    output.println("\t/* Methods to write sub arrays */");
		}									

		if (temp_params[j] != null) { 
		    if (temp_params[j].isArray() && (temp_params[j].getComponentType().isPrimitive())) { 
			count = 0;		
			params_to_write = temp_params[j];
			param_numbers[count++] = j; 
			temp_params[j] = null;
			
			for (int i=j+1;i<temp_params.length;i++) { 
			    if (params_to_write.equals(temp_params[i])) { 
				param_numbers[count++] = i; 
				temp_params[i] = null;
			    } 
			}
			writeSubArrayMethod(params_to_write, param_numbers, count);
		    } else { 
			temp_params[j] = null;
		    }
		}
	    }
	    
	    params_to_write = null;
	    System.arraycopy(params, 0, temp_params, 0, params.length);
	    count = 0;
	
	    for (int j=0;j<temp_params.length;j++) { 			
		if (j == 0) { 
		    output.println("\t/* Methods to write sub object arrays */");
		}									

		if (temp_params[j].isArray() && (!temp_params[j].getComponentType().isPrimitive())) { 
		    param_numbers[count] = j; 
		    object_params[count] = temp_params[j];
		    count++;
		}
	    }

	    if (count > 0) { 
		writeSubObjectArrayMethod(object_params, param_numbers, count);
	    } 
	}
    } 
           
    void trailer(Method m) {
        output.println("}\n");
    } 

      
    void generate() { 
	output.println("import ibis.group.ParameterVector;\n");
	output.println("import ibis.ipl.IbisIOException;");
	output.println("import ibis.ipl.WriteMessage;");
	output.println("import ibis.ipl.ReadMessage;\n");
	for (int i=0;i<data.specialMethods.size();i++) { 
	    Method m = (Method) data.specialMethods.get(i);			
	
	    header(m);
	    body(m);
	    trailer(m);
	} 
    } 
} 
