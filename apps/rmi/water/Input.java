import java.io.InputStream;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.FileNotFoundException;

/**
 * The Input object enables input like in Pascal. 
 * @see InputStream
 * @see BufferedInputStream
 * @see System
 * @see Integer
 * @see String
 * @version     2.1 (October 28th, 1997)
 * @author    Matty Huntjens
 */
public class Input {

            /* FIELD VARIABLES */

        /* CLASS VARIABLES */

    /* FINAL VARIABLES */
    protected static final char NULL_CHARACTER      = '\u0000';
    protected static final char LINE_FEED_CHARACTER = '\n';

    protected static final byte  BYTE_MIN_VALUE        = -128;
    protected static final byte  BYTE_MAX_VALUE        =  127;
    protected static final int   LENGTH_BYTE_MIN_VALUE = length(BYTE_MIN_VALUE);
    protected static final int   LENGTH_BYTE_MAX_VALUE = length(BYTE_MAX_VALUE);

    protected static final short SHORT_MIN_VALUE      = -32768;
    protected static final short SHORT_MAX_VALUE      =  32767;
    protected static final int LENGTH_SHORT_MIN_VALUE = length(SHORT_MIN_VALUE);
    protected static final int LENGTH_SHORT_MAX_VALUE = length(SHORT_MAX_VALUE);

    protected static final int LENGTH_INT_MIN_VALUE = length(Integer.MIN_VALUE);
    protected static final int LENGTH_INT_MAX_VALUE = length(Integer.MAX_VALUE);
                          
    protected static final int LENGTH_LONG_MIN_VALUE = length(Long.MIN_VALUE);
    protected static final int LENGTH_LONG_MAX_VALUE = length(Long.MAX_VALUE);

    protected static final byte BYTE   = 0;
    protected static final byte SHORT  = 1;
    protected static final byte INT    = 2;
    protected static final byte LONG   = 3;
    protected static final byte FLOAT  = 4;
    protected static final byte DOUBLE = 5;

    protected static final long MIN_VALUE[] = {BYTE_MIN_VALUE, 
                                               SHORT_MIN_VALUE,
                                               Integer.MIN_VALUE,
                                               Long.MIN_VALUE
                                              };
    protected static final long MAX_VALUE[] = {BYTE_MAX_VALUE, 
                                               SHORT_MAX_VALUE,
                                               Integer.MAX_VALUE,
                                               Long.MAX_VALUE
                                              };
    protected static final int LENGTH_MIN_VALUE[] = {LENGTH_BYTE_MIN_VALUE,
                                                     LENGTH_SHORT_MIN_VALUE,
                                                     LENGTH_INT_MIN_VALUE,
                                                     LENGTH_LONG_MIN_VALUE
                                                    };
    protected static final int LENGTH_MAX_VALUE[] = {LENGTH_BYTE_MAX_VALUE,
                                                     LENGTH_SHORT_MAX_VALUE,
                                                     LENGTH_INT_MAX_VALUE,
                                                     LENGTH_LONG_MAX_VALUE
                                                    };
    protected static final String NAME[] = {"byte", "short", "int",
                                            "long", "float", "double"};

    /* INSTANCE VARIABLES */

    /* STATE VARIABLES */
    protected String lineSeparator;
    protected int lengthLineSeparator;

    protected InputStream in;

    protected boolean readingFromStandardInput;
    protected boolean endOfFile = false;

                           /* CONSTRUCTORS */

    // Deze constructor initialiseert het Input object als lezende van
    // de file met naam "name".
    public Input (String name) {
        reset(name);
    }


    // Deze constructor initialiseert het Input object als lezende van
    // standard input.
    public Input () {
        standardInput();
    }


    protected void finalize () {
	if (! readingFromStandardInput) {
	    try_close(in);
	}
        try_finalize();
    }

                        /* INSTANCE METHODS */

    /* PROTECTED METHODS */

    // retourneert het aantal characters in een number
    protected static int length (long i) {
        return String.valueOf(i).length();
    }

    protected void try_close (InputStream in) {
        try {
           in.close();
        } catch (IOException e) {
            System.err.println("I/O error\n" + e.toString());
            System.exit(1);
        }
    }

    /*
    protected void try_finalize (Object in) {
        try {
           in.finalize();
        } catch (Throwable e) {
            System.err.println("exception thrown\n" + e.toString());
            System.exit(1);
        }
    }
    */

    protected void try_finalize () {
        try {
           super.finalize();
        } catch (Throwable e) {
            System.err.println("exception thrown\n" + e.toString());
            System.exit(1);
        }
    }
    
    protected void try_setCharAt (StringBuffer sb, int index, char ch) {
        try {
           sb.setCharAt(index, ch);
        } catch (StringIndexOutOfBoundsException e) {
            System.err.println("string index out of bounds\n" + e.toString());
            System.exit(1);
        }
    }

    protected String try_substring (String source, int beginIndex) {
        String s = null;
        try {
             s = source.substring(beginIndex);
         } catch(StringIndexOutOfBoundsException e) {
            System.err.println("string index out of bounds\n" + e.toString());
            System.exit(1);
        }
        return s;
    }

    protected String try_getProperty (String key) {
        String s = null;
        try {
            s = new String(System.getProperty(key));
        } catch(SecurityException e) {
            System.err.println("security exception\n" + e.toString());
            System.exit(1);
        }
        return s;
    }

    protected FileInputStream try_new_FileInputStream (String name) {
        FileInputStream f = null;
        try {
            f = new FileInputStream(name);
        } catch(FileNotFoundException e) {
            System.err.println("File not found exception\n" + e.toString());
            System.exit(1);
        }
        return f;
    }

    // Bij lezen van een file staat tussen ieder tweetal opeenvolgende regels
    // een line separator. Deze is systeemafhankelijk en kan worden opgevraagd.
    void reset (String name) {
//	in = try_new_FileInputStream(name);
        in = new BufferedInputStream(try_new_FileInputStream(name));
        readingFromStandardInput = false;
        lineSeparator = try_getProperty("line.separator");
        lengthLineSeparator = lineSeparator.length();
    }

    // Bij lezen van standard input staat tussen ieder tweetal opeenvolgende 
    // regels een line separator. Deze is systeemonafhankelijk en bestaat uit 
    // het '\n' char.
    public void standardInput () {
        in = new BufferedInputStream(System.in);
        readingFromStandardInput = true;
        lineSeparator = String.valueOf(LINE_FEED_CHARACTER);
        lengthLineSeparator = lineSeparator.length();
    }

    protected int try_read () {
        int i = 0;
        try {
            i = in.read();
        } catch(IOException e) {
            System.err.println("IO exception\n" + e.toString());
            System.exit(1);
        }
        return i;
    }

    // getChar() verschilt van readChar() in het feit dat getChar() bij
    // end-of-file geen crash genereert, maar het null character retourneert.
    protected char getChar () {
        int i = endOfFile ? 0 : try_read();
        if (i == -1) {
            endOfFile = true;
            i = 0;      // code null character
        }
        return (char)i;
    }

    // Indien readChar() wordt aangeroepen bij end-of-file, leidt dit tot een 
    // melding op standard error en een crash, anders leest readChar() een 
    // char in en retourneert dit.
    public char readChar () {
        if (eof()) {
            System.err.println("end-of-file error");
            System.err.flush();
            System.exit(1);
        }

        return getChar();
    }

    public char nextChar () {
        return eoln() ? LINE_FEED_CHARACTER : nextNCharacters(1).charAt(0);
    }

    protected void try_reset () {
        try {
            in.reset();
        } catch(IOException e) {
            System.err.println("IO exception\n" + e.toString());
            System.exit(1);
        }
    }

    protected String nextNCharacters (int n) {
        StringBuffer s = new StringBuffer();
	boolean      saveEndOfFile = endOfFile;
        in.mark(n);
        for (int i = 0; i < n; i++) {
               s.append(getChar());
        }
        try_reset();
	endOfFile = saveEndOfFile;
        return s.toString();
    }

    public boolean eoln () {
        return nextNCharacters(lengthLineSeparator).equals(lineSeparator);
    }

    public boolean eof () {
        in.mark(1);
        endOfFile = try_read() == -1;
        try_reset();
        return endOfFile;
    }

    public void readln () {
        // Skip alle characters voor de end-of-line.
        while (! eoln()) {
           readChar();
        }

        // Skip de end-of-line.
        for (int i = 0; i < lengthLineSeparator; i++) {
           readChar();
        }
    }

    protected void skipWhiteSpace () {
        while (Character.isWhitespace(nextChar())) {
            readChar();
        }
    }

    protected String optionalSign () {
        switch (nextChar()) {
           case '+': getChar();
                     break;
           case '-': return String.valueOf(getChar());
           default:  break;
        }

	return "";
    }

    protected String optionalCardinal () {
	String s = "";
        while (Character.isDigit(nextChar())) {
	    s += getChar();
        }
        return s;
    }

    protected String optionalExponent () {
        String s = "";
        if (Character.toUpperCase(nextChar()) == 'E') {
            s += String.valueOf(getChar()) + optionalSign() + cardinal();
        }
        return s;
    }

    protected String cardinal () {
        if (! Character.isDigit(nextChar())) {
	  int c = nextChar();
	    System.out.println("tried char '" + c + "'.");
            System.err.println("Cannot read digits.");
            System.exit(1);
        }
        return optionalCardinal();
    }

    protected String exponent () {
        if (Character.toUpperCase(nextChar()) != 'E') {
            System.err.println("Cannot read exponent.");
            System.exit(1);
        }
        return optionalExponent();
    }

    protected String decimalPoint () {
        if (nextChar() != '.') {
            System.err.println("Cannot read decimal point.");
            System.exit(1);
        }
        return String.valueOf(getChar());
    }

    protected String skipLeadingZeros (String s) {
        int i;
        // Zoek een character ongelijk aan '0' in s[0 .. lengte-2]
        for (i = 0; i < s.length() - 1; i++) {
            if (s.charAt(i) != '0') {
                break;
            }
        }
        return s.substring(i, s.length());

    }

    // PRE  - s contains a number n
    // POST - FALSE: n >= minValue
    //        TRUE : n <  minValue
    protected boolean underflow (String s, byte integerType) {
        if (s.charAt(0) != '-') {
            return false;
        }

        // Skip possible leading zeros after the minus sign.
        // Note that from the PRE-condition follows that there is at least one
        // digit in the string after the minus sign.
        s = "-" + skipLeadingZeros(try_substring(s, 1));

        if (s.length() > LENGTH_MIN_VALUE[integerType]) {
            return true;
        }

        if ((s.length() == LENGTH_MIN_VALUE[integerType]) && 
            (s.compareTo(String.valueOf(MIN_VALUE[integerType])) > 0)) {
            return true;
        }

        return false;
    }

    // PRE  - s contains a number n
    // POST - FALSE: n <= maxValue
    //        TRUE : n >  maxValue
    protected boolean overflow (String s, byte integerType) {
        if (s.charAt(0) == '-') {
            return false;
        }

        // Skip possible leading zeros.
        s = skipLeadingZeros(s);


        if (s.length() > LENGTH_MAX_VALUE[integerType]) {
            return true;
        }

        if ((s.length() == LENGTH_MAX_VALUE[integerType]) && 
            (s.compareTo(String.valueOf(MAX_VALUE[integerType])) > 0)) {
            return true;
        }

        return false;
    }

    protected long stringToIntegerValue (String s, byte integerType) {
        if (underflow(s, integerType)) {
            System.err.println(NAME[integerType] + " underflow");
            System.exit(1);
        }

        if (overflow(s, integerType)) {
            System.err.println(NAME[integerType] + " overflow");
            System.exit(1);
        }

        return Long.parseLong(s);
    }

    protected double stringToFloatingPointValue (String s, 
                                                 byte   floatingPointType) {
        double d = 0.0;
        try { 
            d = Double.valueOf(s).doubleValue();
        } catch(NumberFormatException e) {
            System.err.println("number format exception\n" + e.toString());
            System.exit(1);
        }
        return d;
    }

    protected long readIntegerValue (byte integerType) {
        skipWhiteSpace();
        String s = optionalSign() + cardinal();
        return stringToIntegerValue(s, integerType);
    }

    protected double readFloatingPointValue (byte floatingPointType) {
        skipWhiteSpace();
        String s = optionalSign();
        if (Character.isDigit(nextChar())) {
            s += cardinal();
            switch (nextChar()) {
              case '.':
                s += decimalPoint() + optionalCardinal() + optionalExponent();
                break;
              case 'e':
              case 'E':
                s += exponent();
                break;
            }
        } else {
            if (nextChar() == '.') {
                s += decimalPoint() + cardinal() + optionalExponent();
            } else {
                System.err.println(NAME[floatingPointType] + " should begin " +
                                   "with a digit or a decimal point"          );
                System.exit(1);
            }
        }
        return stringToFloatingPointValue(s, floatingPointType);
    }

    public byte readByte () {
        return (byte)readIntegerValue(BYTE);
    }

    public short readShort () {
        return (short)readIntegerValue(SHORT);
    }

    public int readInt () {
        return (int)readIntegerValue(INT);
    }

    public long readLong () {
        return readIntegerValue(LONG);
    }

    public float readFloat () {
        return (float)readFloatingPointValue(FLOAT);
    }

    public double readDouble () {
        return readFloatingPointValue(DOUBLE);
    }

}
