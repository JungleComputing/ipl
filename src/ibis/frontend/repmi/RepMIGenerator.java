package ibis.frontend.repmi;

import org.apache.bcel.generic.BasicType;
import org.apache.bcel.generic.Type;

class RepMIGenerator {

    private static String printType(Type c) {
        if (!c.equals(Type.VOID)) {
            if (c.equals(Type.BYTE)) {
                return "Byte";
            } else if (c.equals(Type.CHAR)) {
                return "Char";
            } else if (c.equals(Type.SHORT)) {
                return "Short";
            } else if (c.equals(Type.INT)) {
                return "Int";
            } else if (c.equals(Type.LONG)) {
                return "Long";
            } else if (c.equals(Type.FLOAT)) {
                return "Float";
            } else if (c.equals(Type.DOUBLE)) {
                return "Double";
            } else if (c.equals(Type.BOOLEAN)) {
                return "Boolean";
            }
            return "Object";
        }
        return null;
    }

    public static String writeMessageType(String pre, String message, Type c,
            String param) {
        return (pre + message + ".write" + printType(c) + "(" + param + ");");
    }

    public static String readMessageType(String pre, String dest,
            String message, Type c) {
        String temp = pre + dest + " = ";

        if (!(c instanceof BasicType)) {
            temp += "(" + c + ") ";
        }

        temp += message + ".read" + printType(c) + "();";

        return temp;
    }
}
