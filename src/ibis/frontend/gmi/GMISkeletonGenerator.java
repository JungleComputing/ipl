package ibis.frontend.group;

import java.util.Vector;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import ibis.util.Analyzer;

class GMISkeletonGenerator extends GMIGenerator {

    Analyzer data;
    PrintWriter output;
    boolean verbose;

    String dest_name;

    GMISkeletonGenerator(Analyzer data, PrintWriter output, boolean verbose) {
	this.data   = data;
	this.output = output;
	this.verbose = verbose;
    }

    void header() {
	Class [] interfaces = data.subject.getInterfaces();

	if (data.packagename != null && ! data.packagename.equals("")) {
	    output.println("package " + data.packagename + ";");
	    output.println();
	}

	output.println("import ibis.group.*;");
	output.println("import ibis.ipl.*;");
	output.println("import java.lang.reflect.*;");
	output.println("import java.io.IOException;");
	output.println();

	output.println("public final class group_skeleton_" + dest_name + " extends ibis.group.GroupSkeleton {");
	output.println();
	output.println("public String toString() { return \"group_skeleton_" + dest_name + "\";}");
	output.println();
    }

    void writeResult(String spacing, Class ret) {
	String resultOpcode = getResultOpcode(ret);

	output.println(spacing + "if (ex != null) {");
	output.println(spacing + "\tw.writeByte(RESULT_EXCEPTION);");
	output.println(spacing + "\tw.writeObject(ex);");
	output.println(spacing + "} else {");
	output.println(spacing + "\tw.writeByte("+ resultOpcode +");");

	if (!ret.equals(Void.TYPE)) {
	    output.println(writeMessageType(spacing + "\t", "w", ret, "result"));
	}

	output.println(spacing + "}");
    }

    void handleResult(String spacing, Class ret) {

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
	output.println(spacing + "\t\tw = Group.unicast[cpu_rank].newMessage();");
	output.println(spacing + "\t\tw.writeByte(INVOCATION_REPLY);");
	output.println(spacing + "\t\tw.writeByte((byte) resultMode);");
	output.println(spacing + "\t\tw.writeInt(ticket);");
	output.println(spacing + "\t\tw.writeInt(myGroupRank);");
	output.println(spacing + "\t\tif (personalizer != null) {");
	output.println(spacing + "\t\t\tif (ex != null) {");
	output.println(spacing + "\t\t\t\tException[] out = new Exception[1];");
	output.println(spacing + "\t\t\t\tpersonalizer.personalize(ex, out);");
	output.println(spacing + "\t\t\t\tex = out[0];\n");
	if (! ret.equals(Void.TYPE)) {
	    output.println(spacing + "\t\t\t} else {");
	    output.println(spacing + "\t\t\t" + getType(ret) + "[] out = new " + getType(ret) + "[1];");
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
	if (! ret.equals(Void.TYPE)) {
	    output.println(spacing + "\t\t" + getType(ret) + "[] out = null;");
	}
	output.println(spacing + "\t\tif (personalizer != null) {");
	output.println(spacing + "\t\t\texcpts = new Exception[info.numInvokers];");
	if (! ret.equals(Void.TYPE)) {
	    output.println(spacing + "\t\t\tout = new " + getType(ret) + "[info.numInvokers];");
	}
	output.println(spacing + "\t\t\tif (ex != null) {");
	output.println(spacing + "\t\t\t\tpersonalizer.personalize(ex, excpts);");
	if (! ret.equals(Void.TYPE)) {
	    output.println(spacing + "\t\t\t} else {");
	    output.println(spacing + "\t\t\t\tpersonalizer.personalize(result, out);");
	}
	output.println(spacing + "\t\t\t}");
	output.println(spacing + "\t\t}");


	output.println(spacing + "\t\tfor (int i = 0; i < info.numInvokers; i++) {");
	output.println(spacing + "\t\t\tw = Group.unicast[info.participating_cpus[i]].newMessage();");
	output.println(spacing + "\t\t\tw.writeByte(INVOCATION_REPLY);");
	output.println(spacing + "\t\t\tw.writeByte((byte) resultMode);");
	output.println(spacing + "\t\t\tw.writeInt(info.stubids_tickets[i]);");
	output.println(spacing + "\t\t\tw.writeInt(myGroupRank);");
	output.println(spacing + "\t\t\tif (personalizer != null) {");
	output.println(spacing + "\t\t\t\tex = excpts[i];");
	if (! ret.equals(Void.TYPE)) {
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

    void handleMethodInvocation(String spacing, Method m, Class ret, Class [] params) {

	output.println(spacing + "\t/* Second - Extract the parameters */");

	for (int j=0;j<params.length;j++) {
	    Class temp = params[j];
	    output.println(readMessageType(spacing + "\t", "p" + j, "r", temp, true));
	}

	output.println(spacing + "\tr.finish();");
	output.println();

	output.println(spacing + "\t/* Third - Invoke the method */");

	output.println(spacing + "\ttry {");
	output.print(spacing + "\t\t");

	if (!ret.equals(Void.TYPE)) {
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

	Class ret = m.getReturnType();
	Class [] params = m.getParameterTypes();

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

	if (!ret.equals(Void.TYPE)) {
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

	if (ret.equals(Void.TYPE)) {
	    output.println(spacing + "\t\t\tcombine_void(combiner, false, cpu_rank, ex);");
	} else {
	    if (ret.isPrimitive()) {
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

    String getResultOpcode(Class ret) {

	String result = null;

	if (ret.isPrimitive()) {
	    if (ret.equals(Byte.TYPE)) {
		result = "RESULT_BYTE";
	    } else if (ret.equals(Void.TYPE)) {
		result = "RESULT_VOID";
	    } else if (ret.equals(Character.TYPE)) {
		result = "RESULT_CHAR";
	    } else if (ret.equals(Short.TYPE)) {
		result = "RESULT_SHORT";
	    } else if (ret.equals(Integer.TYPE)) {
		result = "RESULT_INT";
	    } else if (ret.equals(Long.TYPE)) {
		result = "RESULT_LONG";
	    } else if (ret.equals(Float.TYPE)) {
		result = "RESULT_FLOAT";
	    } else if (ret.equals(Double.TYPE)) {
		result = "RESULT_DOUBLE";
	    } else if (ret.equals(Boolean.TYPE)) {
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
