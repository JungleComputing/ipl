package ibis.frontend.gmi;

import ibis.util.BT_Analyzer;

import java.io.PrintWriter;
import java.util.Vector;

import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ArrayType;
import org.apache.bcel.generic.BasicType;
import org.apache.bcel.generic.Type;

class GMISkeletonGenerator extends GMIGenerator {

    BT_Analyzer data;
    PrintWriter output;

    String dest_name;

    GMISkeletonGenerator(BT_Analyzer data, PrintWriter output) {
	this.data   = data;
	this.output = output;
    }

    void header() {
	if (data.packagename != null && ! data.packagename.equals("")) {
	    output.println("package " + data.packagename + ";");
	    output.println();
	}

	output.println("import ibis.gmi.*;");
	output.println("import ibis.ipl.*;");
	output.println("import java.lang.reflect.*;");
	output.println("import java.io.IOException;");
	output.println();

	output.println("public final class group_skeleton_" + dest_name + " extends ibis.gmi.GroupSkeleton {");
	output.println();
	output.println("public String toString() { return \"group_skeleton_" + dest_name + "\";}");
	output.println();
    }

    void writeResult(String spacing, Type ret) {
	String resultOpcode = getResultOpcode(ret);

	output.println(spacing + "if (ex != null) {");
	output.println(spacing + "\tw.writeByte(RESULT_EXCEPTION);");
	output.println(spacing + "\tw.writeObject(ex);");
	output.println(spacing + "} else {");
	output.println(spacing + "\tw.writeByte("+ resultOpcode +");");

	if (!ret.equals(Type.VOID)) {
	    output.println(writeMessageType(spacing + "\t", "w", ret, "result"));
	}

	output.println(spacing + "}");
    }

    void handleResult(String spacing, Type ret) {

	output.println(spacing + "switch (resultMode) {");

	/* discard the result */
	output.println(spacing + "case ReplyScheme.R_DISCARD:");
	output.println(spacing + "\tbreak;");
	output.println();

	output.println(spacing + "case ReplyScheme.R_RETURN:");
	output.println(spacing + "case ReplyScheme.R_COMBINE_BINOMIAL:");
	output.println(spacing + "\tif (invocationMode != InvocationScheme.I_SINGLE && root_object != myGroupRank) {");
	output.println(spacing + "\t\tbreak;");
	output.println(spacing + "\t}");

	output.println(spacing + "case ReplyScheme.R_COMBINE_FLAT:");
	output.println(spacing + "case ReplyScheme.R_FORWARD:");

	output.println(spacing + "\tif (info == null) {");
	output.println(spacing + "\t\tw = Group.unicast(cpu_rank).newMessage();");
	output.println(spacing + "\t\tw.writeByte(INVOCATION_REPLY);");
	output.println(spacing + "\t\tw.writeByte((byte) resultMode);");
	output.println(spacing + "\t\tw.writeInt(ticket);");
	output.println(spacing + "\t\tw.writeInt(myGroupRank);");
	output.println(spacing + "\t\tif (personalizer != null) {");
	output.println(spacing + "\t\t\tif (ex != null) {");
	output.println(spacing + "\t\t\t\tException[] out = new Exception[1];");
	output.println(spacing + "\t\t\t\tpersonalizer.personalize(ex, out);");
	output.println(spacing + "\t\t\t\tex = out[0];\n");
	if (! ret.equals(Type.VOID)) {
	    output.println(spacing + "\t\t\t} else {");
	    if (ret instanceof ArrayType) {
		output.println(spacing + "\t\t\t" + getType(ret) + "[] out = new " + getArrayType((ArrayType) ret, "1") + "[];");
	    }
	    else {
		output.println(spacing + "\t\t\t" + getType(ret) + "[] out = new " + getType(ret) + "[1];");
	    }
	    output.println(spacing + "\t\t\t\tpersonalizer.personalize(result, out);");
	    output.println(spacing + "\t\t\t\tresult = out[0];");
	}
	output.println(spacing + "\t\t\t}");
	output.println(spacing + "\t\t}");

	writeResult(spacing + "\t\t", ret);

	output.println(spacing + "\t\tw.send();");
	output.println(spacing + "\t\tw.finish();");
	output.println(spacing + "\t} else {");
	output.println(spacing + "\t\tException[] excpts = null;");
	if (! ret.equals(Type.VOID)) {
	    output.println(spacing + "\t\t" + getType(ret) + "[] out = null;");
	}
	output.println(spacing + "\t\tif (personalizer != null) {");
	output.println(spacing + "\t\t\texcpts = new Exception[info.numInvokers];");
	if (! ret.equals(Type.VOID)) {
	    if (ret instanceof ArrayType) {
		output.println(spacing + "\t\t\tout = new " + getArrayType((ArrayType) ret, "info.numInvokers") + "[];");
	    }
	    else {
		output.println(spacing + "\t\t\tout = new " + getType(ret) + "[info.numInvokers];");
	    }
	}
	output.println(spacing + "\t\t\tif (ex != null) {");
	output.println(spacing + "\t\t\t\tpersonalizer.personalize(ex, excpts);");
	if (! ret.equals(Type.VOID)) {
	    output.println(spacing + "\t\t\t} else {");
	    output.println(spacing + "\t\t\t\tpersonalizer.personalize(result, out);");
	}
	output.println(spacing + "\t\t\t}");
	output.println(spacing + "\t\t}");


	output.println(spacing + "\t\tfor (int i = 0; i < info.numInvokers; i++) {");
	output.println(spacing + "\t\t\tw = Group.unicast(info.participating_cpus[i]).newMessage();");
	output.println(spacing + "\t\t\tw.writeByte(INVOCATION_REPLY);");
	output.println(spacing + "\t\t\tw.writeByte((byte) resultMode);");
	output.println(spacing + "\t\t\tw.writeInt(info.stubids_tickets[i]);");
	output.println(spacing + "\t\t\tw.writeInt(myGroupRank);");
	output.println(spacing + "\t\t\tif (personalizer != null) {");
	output.println(spacing + "\t\t\t\tex = excpts[i];");
	if (! ret.equals(Type.VOID)) {
	    output.println(spacing + "\t\t\t\tresult = out[i];");
	}
	output.println(spacing + "\t\t\t}");
	writeResult(spacing + "\t\t\t", ret);
	output.println(spacing + "\t\t\tw.send();");
	output.println(spacing + "\t\t\tw.finish();");
	output.println(spacing + "\t\t}");

	output.println(spacing + "\t}");
	output.println(spacing + "\tbreak;");
	output.println();

	/* the default case */
	output.println(spacing + "default:");
	output.println(spacing + "\tSystem.err.println(\"OOPS: group_skeleton got illegal resultMode number!\");");
	output.println(spacing + "\tSystem.exit(1);");
	output.println(spacing + "\tbreak;");
	output.println();

	output.println(spacing + "}");
	output.println();
    }

    void handleMethodInvocation(String spacing, Method m, Type ret, Type [] params) {

	output.println(spacing + "\t/* Second - Extract the parameters */");

	for (int j=0;j<params.length;j++) {
	    Type temp = params[j];
	    if (temp instanceof BasicType) {
		output.println(readMessageType(spacing + "\t", "p" + j, "r", temp, true));
	    }
	    else {
		output.println(spacing + "\ttry {");
		output.println(readMessageType(spacing + "\t\t", "p" + j, "r", temp, true));
		output.println(spacing + "\t} catch(ClassNotFoundException e) {");
		output.println(spacing + "\t\tthrow new RuntimeException(\"class not found exception \" + e);");
		output.println(spacing + "\t}");
	    }
	}

	output.println(spacing + "\tr.finish();");
	output.println();

	output.println(spacing + "\t/* Third - Invoke the method */");

	output.println(spacing + "\ttry {");
	output.print(spacing + "\t\t");

	if (!ret.equals(Type.VOID)) {
	    output.print("result = ");
	}

	output.print("((" + dest_name + ") destination)." + m.getName() + "(");

	for (int j=0;j<params.length;j++) {
	    output.print("p" + j);
	    if (j < params.length-1) {
		output.print(", ");
	    }
	}

	output.println(");");

	output.println(spacing + "\t} catch (Exception e) {");
	output.println(spacing + "\t\tex = e;");
	output.println(spacing + "\t}");
    }

    void methodHandler(String spacing, Method m) {

	Type ret = m.getReturnType();
	Type [] params = m.getArgumentTypes();

	output.print(spacing + "private final void GMI_" + m.getName() + "(int invocationMode, int resultMode, ReadMessage r) throws IbisException, IOException {");
	output.println();

	output.println(spacing + "\tint cpu_rank = 0;");
	output.println(spacing + "\tint root_object = 0;");
	output.println(spacing + "\tint ticket = 0;");
	output.println(spacing + "\tBinomialCombiner combiner = null;");
	output.println(spacing + "\tWriteMessage w;");
	output.println(spacing + "\tException ex = null;");
	output.println(spacing + "\tCombinedInvocationInfo info = null;");
	output.println(spacing + "\tReplyPersonalizer personalizer = null;");

	for (int j=0;j<params.length;j++) {
	    output.println(spacing + "\t" + getInitedLocal(params[j], "p" + j) + ";");
	}

	if (!ret.equals(Type.VOID)) {
	    output.println(spacing + "\t" + getInitedLocal(ret, "result") + ";");
	}

	output.println();

	output.println(spacing + "\t/* First - Read additional data */");
	output.println(spacing + "\tif (invocationMode >= InvocationScheme.I_COMBINED) {");
	output.println(spacing + "\t\ttry {");
	output.println(spacing + "\t\t\tinfo = (CombinedInvocationInfo) r.readObject();");
	output.println(spacing + "\t\t} catch(ClassNotFoundException e) {");
	output.println(spacing + "\t\t\tthrow new Error(\"Expected CombinedInvocationInfo\", e);");
	output.println(spacing + "\t\t}");
	output.println(spacing + "\t}");

	output.println(spacing + "\tif (resultMode >= ReplyScheme.R_PERSONALIZED) {");
	output.println(spacing + "\t\tresultMode -= ReplyScheme.R_PERSONALIZED;");
	output.println(spacing + "\t\ttry {");
	output.println(spacing + "\t\t\tpersonalizer = (ReplyPersonalizer) r.readObject();");
	output.println(spacing + "\t\t} catch(ClassNotFoundException e) {");
	output.println(spacing + "\t\t\tthrow new Error(\"Expected ReplyPersonalizer\", e);");
	output.println(spacing + "\t\t}");
	output.println(spacing + "\t}");

	output.println(spacing + "\tswitch (resultMode) {");
	output.println(spacing + "\tcase ReplyScheme.R_COMBINE_BINOMIAL:");
	output.println(spacing + "\t\ttry {");
	output.println(spacing + "\t\t\tcombiner = (BinomialCombiner) r.readObject();");
	output.println(spacing + "\t\t} catch(ClassNotFoundException e) {");
	output.println(spacing + "\t\t\tthrow new Error(\"Expected BinomialCombiner\", e);");
	output.println(spacing + "\t\t}");
	output.println(spacing + "\t\t/* fall through */");
	output.println(spacing + "\tcase ReplyScheme.R_COMBINE_FLAT:");
	output.println(spacing + "\tcase ReplyScheme.R_FORWARD:");
	output.println(spacing + "\tcase ReplyScheme.R_RETURN:");
	output.println(spacing + "\t\tif (info == null) {");
	output.println(spacing + "\t\t\tcpu_rank = r.readInt();");
	output.println(spacing + "\t\t\tticket = r.readInt();");
	output.println(spacing + "\t\t}");
	output.println(spacing + "\t\t/* fall through */");
	output.println(spacing + "\tcase ReplyScheme.R_DISCARD:");
	output.println(spacing + "\t\tbreak;");
	output.println(spacing + "\t}");

	handleMethodInvocation(spacing, m, ret, params);
	output.println();

	output.println(spacing + "/* Fourth - Handle the result */");

//	TODO: call personalizer if present.

	output.println(spacing + "\tif (combiner != null) {");
	output.println(spacing + "\t\t/* call combiner here */");
	output.println(spacing + "\t\ttry {");

	if (ret.equals(Type.VOID)) {
	    output.println(spacing + "\t\t\tcombine_void(combiner, false, cpu_rank, ex);");
	} else {
	    if (ret instanceof BasicType) {
		output.println(spacing + "\t\t\tresult = combine_" +
		           getType(ret) + "(combiner, false, cpu_rank, result, ex);");
	    } else {
		output.println(spacing + "\t\t\tresult = (" + getType(ret) + ") combine_Object(combiner, false, cpu_rank, result, ex);");
	    }
	}

	output.println(spacing + "\t\t\tex = null;");
	output.println(spacing + "\t\t} catch (Exception e) {");
	output.println(spacing + "\t\t\tex = e;");
	output.println(spacing + "\t\t}");
	output.println(spacing + "\t}");

	output.println();
	handleResult(spacing + "\t", ret);

	output.println(spacing + "}");
	output.println();
    }

    String getResultOpcode(Type ret) {

	String result = null;

	if (ret instanceof BasicType) {
	    if (ret.equals(Type.BYTE)) {
		result = "RESULT_BYTE";
	    } else if (ret.equals(Type.VOID)) {
		result = "RESULT_VOID";
	    } else if (ret.equals(Type.CHAR)) {
		result = "RESULT_CHAR";
	    } else if (ret.equals(Type.CHAR)) {
		result = "RESULT_SHORT";
	    } else if (ret.equals(Type.INT)) {
		result = "RESULT_INT";
	    } else if (ret.equals(Type.LONG)) {
		result = "RESULT_LONG";
	    } else if (ret.equals(Type.FLOAT)) {
		result = "RESULT_FLOAT";
	    } else if (ret.equals(Type.DOUBLE)) {
		result = "RESULT_DOUBLE";
	    } else if (ret.equals(Type.BOOLEAN)) {
		result = "RESULT_BOOLEAN";
	    }
	} else {
	    result = "RESULT_OBJECT";
	}

	return result;
    }



    void messageHandler(String spacing, Vector methods) {

	output.println(spacing + "public final void handleMessage(int invocationMode, int resultMode, ReadMessage r) throws IbisException, IOException {");
	output.println();
	output.println(spacing + "\tint method = r.readInt();");

	output.println(spacing + "\tswitch(method) {");

	for (int i=0;i<methods.size();i++) {
	    Method m = (Method) methods.get(i);
	    output.println(spacing + "\tcase " + i + ":");
	    output.println(spacing + "\t\tGMI_" + m.getName() + "(invocationMode, resultMode, r);");
	    output.println(spacing + "\t\tbreak;");
	}

	output.println(spacing + "\tdefault:");
	output.println(spacing + "\t\tSystem.err.println(\"OOPS: group_skeleton got illegal method number!\");");
	output.println(spacing + "\t\tSystem.exit(1);");
	output.println(spacing + "\t\tbreak;");

	output.println(spacing + "\t}");
	output.println(spacing + "}");
    }

    void trailer() {
	output.println("}\n");
    }

    void constructor(String spacing, Vector methods) {

	output.println(spacing + "public group_skeleton_" + data.classname + "() {}");
    }

    void body(String spacing, Vector methods) {
	for (int i=0;i<methods.size();i++) {
	    methodHandler(spacing, (Method) methods.get(i));
	}
    }

    void generate() {
	dest_name = data.classname;
	header();
	constructor("\t", data.specialMethods);
	messageHandler("\t", data.specialMethods);
	body("\t", data.specialMethods);
	trailer();
    }
}
