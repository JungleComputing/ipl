/* $Id$ */

package ibis.impl.net;

import java.io.FileInputStream;
import java.io.IOException;

public class NetFile {
    String filename = null;

    FileInputStream f = null;

    int line = 0;

    public NetFile(String filename) throws IOException {
        this.filename = filename;

        f = new FileInputStream(filename);
    }

    public String readline() throws IOException {
        String s = "";
        int i = 0;
        char c = 0;

        do {
            i = f.read();
            if (i < 0) {
                if (s.equals("")) {
                    return null;
                }
                break;               
            }

            c = (char) i;
            s += c;
        } while (c != '\n');

        return s;
    }

    public int lineNumber() {
        return line;
    }

    /*
     * May need some tuning on Windows systems.
     */
    public String chomp(String s) {
        int l = s.length();
        if (l > 0 && s.charAt(l - 1) == '\n') {
            if (l > 1) {
                return s.substring(0, l - 2);
            }
            return "";
        }

        return s;
    }

    public String cleanSpaces(String s) {
        String ns = "";
        int l = s.length();
        int i = 0;

        for (i = 0; i < l; i++) {
            char c = s.charAt(i);

            if (!Character.isWhitespace(c)) {
                break;
            }
        }

        out: while (true) {

            in1: while (true) {

                if (i >= l) {
                    break out;
                }

                char c = s.charAt(i);
                if (Character.isWhitespace(c)) {
                    i++;
                    break in1;
                }

                ns += c;
                i++;
            }

            in2: while (true) {
                if (i >= l) {
                    break out;
                }
                char c = s.charAt(i);

                if (!Character.isWhitespace(c)) {
                    ns += " " + c;
                    i++;
                    break in2;
                }

                i++;
            }
        }

        return ns;
    }
}

