/* $Id$ */

package ibis.frontend.repmi;

import ibis.frontend.generic.BT_Analyzer;

import java.io.PrintWriter;
import java.util.Vector;

import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.Type;

class RepMISkeletonGenerator extends RepMIGenerator {

    BT_Analyzer data;

    PrintWriter output;

    String dest_name;

    RepMISkeletonGenerator(BT_Analyzer data, PrintWriter output) {
        this.data = data;
        this.output = output;
    }

    void header() {
        if (data.packagename != null && !data.packagename.equals("")) {
            output.println("package " + data.packagename + ";");
            output.println();
        }

        output.println("import ibis.repmi.*;");
        output.println("import ibis.ipl.*;");
        output.println("import java.lang.reflect.*;");
        output.println("import java.io.IOException;");
        output.println();

        output.println("public final class repmi_skeleton_" + dest_name
                + " extends ibis.repmi.Skeleton {");
        output.println();
    }

    void messageHandler(Vector methods) {

        output.println("\tpublic final void handleMessage(ReadMessage r) "
                + "throws IOException {");
        output.println();

        output.println("\t\tint method = r.readInt();");
        output.println();

        output.println("\t\tswitch(method) {");

        for (int i = 0; i < methods.size(); i++) {
            Method m = (Method) methods.get(i);
            Type ret = Type.getReturnType(m.getSignature());
            Type[] params = Type.getArgumentTypes(m.getSignature());

            output.println("\t\tcase " + i + ":");
            output.println("\t\t{");

            output.println("\t\t\t/* First - Extract the parameters */");

            for (int j = 0; j < params.length; j++) {
                output.println(readMessageType("\t\t\t", params[j] + " p" + j,
                        "r", params[j]));
            }

            output.println("\t\t\tr.finish();");
            output.println();

            output.println("\t\t\t/* Second - Invoke the method */");

            output.print("\t\t\t");

            if (!ret.equals(Type.VOID)) {
                output.print(ret + " result = ");
            }

            output.print("((" + dest_name + ") destination)." + m.getName()
                    + "(");

            for (int j = 0; j < params.length; j++) {
                output.print("p" + j);

                if (j < params.length - 1) {
                    output.print(", ");
                }
            }
            output.println(");");
            output.println("\t\t\tbreak;");
            output.println("\t\t}");
            output.println();
        }

        output.println("\t\tdefault:");
        output.println("\t\t\tSystem.err.println(\"OOPS: group_skeleton got "
                + "illegal method number!\");");
        output.println("\t\t\tSystem.exit(1);");
        output.println("\t\t\tbreak;");

        output.println("\t\t}");
        output.println("\t}");
        output.println();
    }

    void trailer() {
        output.println("}\n");
    }

    void constructor() {

        output.println("\tpublic repmi_skeleton_" + data.classname + "() {");

        output.println("\t\tsuper();");
        output.println();
        output.println("\t}\n");
    }

    void generate() {

        dest_name = data.classname;

        header();
        constructor();
        messageHandler(data.specialMethods);
        trailer();
    }
}
