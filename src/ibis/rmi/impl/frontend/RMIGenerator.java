/* $Id$ */

package ibis.rmi.impl.frontend;

import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ArrayType;
import org.apache.bcel.generic.BasicType;
import org.apache.bcel.generic.Type;

class RMIGenerator {

    String packageName;

    RMIGenerator(String packageName) {
        this.packageName = packageName;
    }

    public String printType(Type c) {

        if (!c.equals(Type.VOID)) {

            if (c instanceof BasicType) {

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
            } else {
                return "Object";
            }
        }
        return null;
    }

    public String getType(Type c) {

        if (c instanceof ArrayType) {
            return getType(((ArrayType) c).getElementType()) + "[]";
        }
        if (!c.equals(Type.VOID)) {

            if (c instanceof BasicType) {

                if (c.equals(Type.BYTE)) {
                    return "byte";
                } else if (c.equals(Type.CHAR)) {
                    return "char";
                } else if (c.equals(Type.SHORT)) {
                    return "short";
                } else if (c.equals(Type.INT)) {
                    return "int";
                } else if (c.equals(Type.LONG)) {
                    return "long";
                } else if (c.equals(Type.FLOAT)) {
                    return "float";
                } else if (c.equals(Type.DOUBLE)) {
                    return "double";
                } else if (c.equals(Type.BOOLEAN)) {
                    return "boolean";
                }
            }
            String name = c.toString();
            if (packageName != null && name.startsWith(packageName + ".")) {
                return name.substring(packageName.length() + 1);
            }
            return name;
        }
        return "void";
    }

    public String getInitedLocal(Type c, String name) {

        String result = null;

        if (!c.equals(Type.VOID)) {
            if (c instanceof BasicType) {
                if (c.equals(Type.BYTE)) {
                    result = "byte " + name + " = 0";
                } else if (c.equals(Type.CHAR)) {
                    result = "char " + name + " = 0";
                } else if (c.equals(Type.SHORT)) {
                    result = "short " + name + " = 0";
                } else if (c.equals(Type.INT)) {
                    result = "int " + name + " = 0";
                } else if (c.equals(Type.LONG)) {
                    result = "long " + name + " = 0";
                } else if (c.equals(Type.FLOAT)) {
                    result = "float " + name + " = 0.0";
                } else if (c.equals(Type.DOUBLE)) {
                    result = "double " + name + " = 0.0";
                } else if (c.equals(Type.BOOLEAN)) {
                    result = "boolean " + name + " = false";
                }
            } else {
                result = getType(c) + " " + name + " = null";
            }
        }

        return result;
    }

    public String writeMessageType(String pre, String message, Type c,
            String param) {
        return (pre + message + ".write" + printType(c) + "(" + param + ");");
    }

    public String readMessageType(String pre, String dest,
            String message, Type c) {
        String temp = pre + dest + " = ";

        if (!(c instanceof BasicType)) {
            temp += "(" + getType(c) + ") ";
        }

        temp += message + ".read" + printType(c) + "();";

        return temp;
    }
}
