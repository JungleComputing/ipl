// File: $Id$

import java.io.File;

class PrintGrammar {
    /** Private constructor to prevent instantiation of this class. */
    private PrintGrammar() {
    }

    /**
     * Allows execution of the class.
     * @param args The command-line arguments.
     */
    public static void main(String args[]) throws java.io.IOException {
        if (args.length != 1) {
            System.err.println("Usage: <compressed-text>");
            System.exit(1);
        }
        File infile = new File(args[0]);

        try {
            byte text[] = Helpers.readFile(infile);
            ByteBuffer buf = new ByteBuffer(text);
            ShortBuffer s = buf.decodeByteStream();
            SuffixArray a = new SuffixArray(s);
            a.printGrammar();
        } catch (Exception x) {
            x.printStackTrace();
        }
    }
}