package ibis.util;

import java.io.*;

/**
 * An Input object which enables input like in Pascal. 
 * @see InputStream
 * @see BufferedInputStream
 * @see System
 * @see Integer
 * @see String
 * @version     5.0 (November 21th, 2001)
 * @author    Matty Huntjens
 */
public class Input extends BufferedInputStream {

    protected static final char NULL_CHARACTER = '\u0000';

    protected static final int END_OF_FILE = -1,
			       CODE_NULL_CHARACTER = 0;

    protected static final int LENGTH_BYTE_MIN_VALUE  = length(Byte.MIN_VALUE),
                               LENGTH_BYTE_MAX_VALUE  = length(Byte.MAX_VALUE),
                               LENGTH_SHORT_MIN_VALUE = length(Short.MIN_VALUE),
                               LENGTH_SHORT_MAX_VALUE = length(Short.MAX_VALUE),
                               LENGTH_INT_MIN_VALUE = length(Integer.MIN_VALUE),
                               LENGTH_INT_MAX_VALUE = length(Integer.MAX_VALUE),
                               LENGTH_LONG_MIN_VALUE = length(Long.MIN_VALUE),
                               LENGTH_LONG_MAX_VALUE = length(Long.MAX_VALUE);

    protected static final int BYTE   = 0,
                               SHORT  = 1,
                               INT    = 2,
                               LONG   = 3,
                               FLOAT  = 4,
                               DOUBLE = 5;

    protected static final int OCTAL       =  8,
			       DECIMAL     = 10,
			       HEXADECIMAL = 16;

    protected static final long[] MIN_VALUE = {Byte.MIN_VALUE,
                                               Short.MIN_VALUE,
                                               Integer.MIN_VALUE,
                                               Long.MIN_VALUE
                                              },
                                  MAX_VALUE = {Byte.MAX_VALUE,
                                               Short.MAX_VALUE,
                                               Integer.MAX_VALUE,
                                               Long.MAX_VALUE
                                              };

    protected static final int[] LENGTH_MIN_VALUE = {LENGTH_BYTE_MIN_VALUE,
                                                     LENGTH_SHORT_MIN_VALUE,
                                                     LENGTH_INT_MIN_VALUE,
                                                     LENGTH_LONG_MIN_VALUE
                                                    },
                                 LENGTH_MAX_VALUE = {LENGTH_BYTE_MAX_VALUE,
                                                     LENGTH_SHORT_MAX_VALUE,
                                                     LENGTH_INT_MAX_VALUE,
                                                     LENGTH_LONG_MAX_VALUE
                                                    };

    protected static final String NAME[] = {"byte", "short", "int",
                                            "long", "float", "double"};

    protected String lineSeparator;
    protected int lengthLineSeparator;
    protected boolean endOfFile;
    protected String name;

                           /* CONSTRUCTORS */

    // Deze constructor initialiseert het Input object als lezende van
    // de InputStream in.
    public Input (InputStream in) {
	super(in);
        lineSeparator = try_getProperty("line.separator");
        lengthLineSeparator = lineSeparator.length();
        endOfFile = false;
    }


    // Deze constructor initialiseert het Input object als lezende van
    // standard input.
    public Input () {
	this(System.in);
    }


    // Deze constructor initialiseert het Input object als lezende van
    // de file met naam "name".
    public Input (String name) throws FileNotFoundException {
        this(new FileInputStream(name));
	this.name = name;
    }

                           /* FINALIZE */

    protected void finalize () {
	try_close(in);
        try_finalize();
    }

                           /* CLASS METHODS */

	public String name() {
		return name;
	}

    // retourneert het aantal characters in een number
    // Deze methode moet static zijn om in de constante declaraties gebruikt
    // te kunnen worden.
    protected static int length (long i) {
        return String.valueOf(i).length();
    }


                   /* METHODS TO HIDE TRY STATEMENTS */

    protected void try_close (InputStream in) {
        try {
           close();
        } catch (IOException e) {
            throw new Error("I/O error\n" + e);
        }
    }


    protected void try_finalize () {
        try {
           super.finalize();
        } catch (Throwable e) {
            throw new Error("throwable thrown\n" + e);
        }
    }


    protected void try_setCharAt (StringBuffer sb, int index, char ch) {
        try {
           sb.setCharAt(index, ch);
        } catch (StringIndexOutOfBoundsException e) {
            throw new Error("string index out of bounds\n" + e);
        }
    }


    protected String try_substring (String source, int beginIndex) {
        try {
             return source.substring(beginIndex);
         } catch (StringIndexOutOfBoundsException e) {
            throw new Error("string index out of bounds\n" + e);
        }
    }


    protected String try_getProperty (String key) {
        try {
            return System.getProperty(key);
        } catch (SecurityException e) {
            throw new Error("security exception\n" + e);
        }
    }


    protected void try_reset () {
        try {
            reset();
        } catch (IOException e) {
            throw new Error("IO exception\n" + e);
        }
    }


    protected boolean bufEmpty () {
	return pos >= count;
    }


    protected int try_read () {
	/* N.B. Als de methode read() de waarde END_OF_FILE retourneert, zal,
		bij inlezen van standaard invoer (het toetsenbord), een tweede 
		aanroep van read() niet tot gevolg hebben dat er weer de waarde
		END_OF_FILE geretourneerd wordt, maar dat er gewacht zal worden
		op invoer.
		Om dit te voorkomen is de boolean endOfFile ingevoerd.
	*/
        try {
	    int charCode = bufEmpty() && endOfFile? CODE_NULL_CHARACTER: read();
	    if (charCode == END_OF_FILE) {
		endOfFile = true;
		charCode = CODE_NULL_CHARACTER;
	    }
	    return charCode;
        } catch (IOException e) {
            throw new Error("IO exception\n" + e);
        }
    }

                   /* BASIC INPUT METHODS */

    // Indien readChar() wordt aangeroepen bij end-of-file,
    // leidt dit tot een foutmelding en een crash.
    // Anders leest readChar() een char in en retourneert dit.
    public char readChar () {
	if (eof()) {
	    throw new Error("end-of-file error");
        }

        return (char) try_read();
    }


    public char nextChar () {
        return nextNCharacters(1).charAt(0);
    }


    protected String nextNCharacters (int n) {
        StringBuffer s = new StringBuffer();
	int codeChar;

	mark(n);
        for (int i = 0; i < n; i++) {
	    s.append((char) try_read());
        }
        try_reset();

        return s.toString();
    }


    public boolean eoln () {
        return nextNCharacters(lengthLineSeparator).equals(lineSeparator);
    }


    public boolean eof () {
	if (! bufEmpty()) {
	    return false;
	}

	// de buffer is leeg

	if (endOfFile) {
	    return true;
	}

        mark(1);
	try_read();	// try_read() zet endOfFile op true indien end-of-file
	try_reset();

        return endOfFile;
    }


    public void readln () {
        // Skip alle characters voor de end-of-line.
        while (! eoln()) {
           readChar();
        }

        // Skip de end-of-line.
	skipCharacters(lengthLineSeparator);
    }

    public void skipWhiteSpace () {
        while (!eof() && Character.isWhitespace(nextChar())) {
            readChar();
        }
    }

    protected void skipCharacters (int n) {
        for (int i = 0; i < n; i++) {
           readChar();
        }
    }


    protected String optionalSign () {
        switch (nextChar()) {
           case '+': readChar();
                     return "";
           case '-': return String.valueOf(readChar());
           default:  return "";
        }
    }

    protected boolean isOctalDigit (char c) {
	return '0' <= c && c <= '7';
    }


    protected boolean isDecimalDigit (char c) {
	return '0' <= c && c <= '9';
    }


    protected boolean isHexadecimalDigit (char c) {
	c = Character.toUpperCase(c);
	return Character.isDigit(c) || 'A' <= c && c <= 'F';
    }


    protected String hexadecimalCardinal () {
	// skip, indien de notatie van de Java syntax gebruikt wordt, 0X of 0x
	if (nextNCharacters(2).toUpperCase().equals("0X")) {
	    skipCharacters(2);
	    if (! isHexadecimalDigit(nextChar())) {
		throw new Error ("No hexadicimal digits after 0X or 0x.");
	    }
	}

	String s = "";
        while (isHexadecimalDigit(nextChar())) {
	    s += readChar();
        }
        return s;
    }


    protected String decimalCardinal () {
	String s = "";
        while (isDecimalDigit(nextChar())) {
	    s += readChar();
        }
        return s;
    }


    protected String octalCardinal () {
	String s = "";
        while (isOctalDigit(nextChar())) {
	    s += readChar();
        }
        return s;
    }


    // Deze methode wordt alleen aangeroepen als nextChar() een cijfer is.
    protected String optionalCardinal (int radix) {
	switch (radix) {
	    case OCTAL       : return octalCardinal();
	    case DECIMAL     : return decimalCardinal();
	    case HEXADECIMAL : return hexadecimalCardinal();
            default          : throw new Error("illegal radix");
	}
    }

    protected String optionalExponent () {
        String s = "";
        if (Character.toUpperCase(nextChar()) == 'E') {
            s += String.valueOf(readChar())+optionalSign()+cardinal(DECIMAL);
        }
        return s;
    }


    protected String cardinal (int radix) {
	switch (radix) {
	    case OCTAL: 
		  if (! isOctalDigit(nextChar())) {
                      throw new Error("Cannot read octal digits.");
                  }
		  break;

	    case DECIMAL: 
		  if (! isDecimalDigit(nextChar())) {
                      throw new Error("Cannot read decimal digits.");
                  }
		  break;

	    case HEXADECIMAL: 
		  if (! isHexadecimalDigit(nextChar())) {
                      throw new Error("Cannot read hexadecimal digits.");
                  }
		  break;

            default: throw new Error("cardinal(): illegal radix");
	}

        return optionalCardinal(radix);
    }

    protected String exponent () {
        if (Character.toUpperCase(nextChar()) != 'E') {
            throw new Error("Cannot read exponent.");
        }
        return optionalExponent();
    }

    protected String decimalPoint () {
        if (nextChar() != '.') {
            throw new Error("Cannot read decimal point.");
        }
        return String.valueOf(readChar());
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
    protected boolean underflow (String s, int integerType, int radix) {
	long l;

        if (s.charAt(0) != '-') {
            return false;
        }

        // Skip possible leading zeros after the minus sign.
        // Note that from the PRE-condition follows that there is at least one
        // digit in the string after the minus sign.
        s = "-" + skipLeadingZeros(try_substring(s, 1));

	try {
	    l = Long.valueOf(s, radix).longValue();
	} catch (NumberFormatException e) {
            return true;
	}

        return l < ((long) MIN_VALUE[integerType]);
    }


    // PRE  - s contains a number n
    // POST - FALSE: n <= maxValue
    //        TRUE : n >  maxValue
    protected boolean overflow (String s, int integerType, int radix) {
	long l;

        if (s.charAt(0) == '-') {
            return false;
        }

        // Skip possible leading zeros.
        s = skipLeadingZeros(s);

	try {
	    l = Long.valueOf(s, radix).longValue();
	} catch (NumberFormatException e) {
            return true;
	}

        return l > ((long) MAX_VALUE[integerType]);
    }

    protected long stringToIntegerValue (String s, int integerType, int radix){
        if (underflow(s, integerType, radix)) {
            throw new Error(NAME[integerType] + " underflow");
        }

        if (overflow(s, integerType, radix)) {
            throw new Error(NAME[integerType] + " overflow");
        }

        return Long.parseLong(s, radix);
    }

    protected double stringToFloatingPointValue (String s, 
						 int    floatingPointType) {
        double d = 0.0;
        try { 
            d = Double.valueOf(s).doubleValue();
        } catch (NumberFormatException e) {
            throw new Error("number format exception\n" + e);
        }
        return d;
    }

    protected long readIntegerValue (int integerType, int radix) {
        skipWhiteSpace();
        String s = optionalSign() + cardinal(radix);
        return stringToIntegerValue(s, integerType, radix);
    }

    protected double readFloatingPointValue (int floatingPointType) {
        skipWhiteSpace();
        String s = optionalSign();
        if (Character.isDigit(nextChar())) {
            s += cardinal(DECIMAL);
            switch (nextChar()) {
              case '.':
                s +=decimalPoint()+optionalCardinal(DECIMAL)+optionalExponent();
                break;
              case 'e':
              case 'E':
                s += exponent();
                break;
            }
        } else {
            if (nextChar() == '.') {
                s += decimalPoint() + cardinal(DECIMAL) + optionalExponent();
            } else {
                throw new Error(NAME[floatingPointType] + " should begin " +
                                "with a digit or a decimal point"          );
            }
        }
        return stringToFloatingPointValue(s, floatingPointType);
    }

                    /* READING OCTAL INTEGER VALUES */

    public byte readOctalByte () {
        return (byte) readIntegerValue(BYTE, OCTAL);
    }

    public short readOctalShort () {
        return (short) readIntegerValue(SHORT, OCTAL);
    }

    public int readOctalInt () {
        return (int) readIntegerValue(INT, OCTAL);
    }

    public long readOctalLong () {
        return readIntegerValue(LONG, OCTAL);
    }

                    /* READING DECIMAL INTEGER VALUES */

    public byte readByte () {
        return (byte) readIntegerValue(BYTE, DECIMAL);
    }

    public short readShort () {
        return (short) readIntegerValue(SHORT, DECIMAL);
    }

    public int readInt () {
        return (int) readIntegerValue(INT, DECIMAL);
    }

    public long readLong () {
        return readIntegerValue(LONG, DECIMAL);
    }

                    /* READING HEXADECIMAL INTEGER VALUES */

    public byte readHexadecimalByte () {
        return (byte) readIntegerValue(BYTE, HEXADECIMAL);
    }

    public short readHexadecimalShort () {
        return (short) readIntegerValue(SHORT, HEXADECIMAL);
    }

    public int readHexadecimalInt () {
        return (int) readIntegerValue(INT, HEXADECIMAL);
    }

    public long readHexadecimalLong () {
        return readIntegerValue(LONG, HEXADECIMAL);
    }


                    /* READING FLOATING POINT VALUES */

    public float readFloat () {
        return (float) readFloatingPointValue(FLOAT);
    }

    public double readDouble () {
        return readFloatingPointValue(DOUBLE);
    }

}
