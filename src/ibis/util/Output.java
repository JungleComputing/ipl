package ibis.util;

import java.io.*;

public class Output {

    protected static final char SPATIE = ' ';

    protected boolean printingOnStandardOutput;

    protected String lineSeparator;
    protected BufferedOutputStream out;


    protected FileOutputStream try_new_FileOutputStream (String name) {
	FileOutputStream f = null;
	try {
	    f = new FileOutputStream(name);
	} catch(IOException e) {
            System.err.println("IO exception\n" + e.toString());
	    System.exit(1);
	}
        return f;
    }


    protected String try_getProperty (String key) {
	String s = null;
	try {
	    s = System.getProperty(key);
	} catch(SecurityException e) {
	    throw new Error("security exception\n" + e.toString());
	}
	return s;
    }
	 

    public Output () {
	out = open(System.out);
	printingOnStandardOutput = true;
    }


    public Output (String name) {
	out = open(try_new_FileOutputStream(name));
	printingOnStandardOutput = false;
    }


    protected BufferedOutputStream open (OutputStream outputStream) {
	lineSeparator = try_getProperty("line.separator");
        return new BufferedOutputStream(outputStream);
    }


    public void println () {
	print(lineSeparator);
    }


    public void print (byte b) {
	print(String.valueOf((int)b));
    }


    public void print (short s) {
	print(String.valueOf((int)s));
    }


    public void print (char c) {
	print(String.valueOf(c));
    }


    public void print (int i) {
	print(String.valueOf(i));
    }


    public void print (long l) {
	print(String.valueOf(l));
    }


    public void print (float f) {
	print(String.valueOf(f));
    }


    public void print (double d) {
	print(String.valueOf(d));
    }


    public void print (boolean b) {
	print(String.valueOf(b));
    }

    public void print (Object o) {
	print(o.toString());
    }


    protected void try_write (char c) {
        try {
            out.write(c);
        } catch(IOException e) {
            System.err.println("IO exception\n" + e.toString());
            System.exit(1);
        }
    }


    protected void try_flush () {
        try {
            out.flush();
        } catch(IOException e) {
            System.err.println("IO exception\n" + e.toString());
            System.exit(1);
        }
    }


    public void print (String s) {
        int i;

        for (i = 0; i < s.length(); i++) {
	   try_write(s.charAt(i));
        }
	try_flush();
    }


    public void println (byte b) {
	println(String.valueOf((int)b));
    }


    public void println (short s) {
	println(String.valueOf((int)s));
    }


    public void println (char c) {
	println(String.valueOf(c));
    }


    public void println (int i) {
	println(String.valueOf(i));
    }


    public void println (long l) {
	println(String.valueOf(l));
    }


    public void println (float f) {
	println(String.valueOf(f));
    }


    public void println (double d) {
	println(String.valueOf(d));
    }


    public void println (boolean b) {
	println(String.valueOf(b));
    }


    public void println (Object o) {
	println(o.toString());
    }


    public void println (String s) {
	print(s + lineSeparator);
    }


    /* print + lengte-parameter */

    public void print (byte b, int lengte) {
	print(String.valueOf((int)b), lengte);
    }


    public void print (short s, int lengte) {
	print(String.valueOf((int)s), lengte);
    }


    public void print (char c, int lengte) {
	print(String.valueOf(c), lengte);
    }


    public void print (int i, int lengte) {
	print(String.valueOf(i), lengte);
    }


    public void print (long l, int lengte) {
	print(String.valueOf(l), lengte);
    }


    public void print (float f, int lengte) {
	print(String.valueOf(f), lengte);
    }


    public void print (double d, int lengte) {
	print(String.valueOf(d), lengte);
    }


    public void print (boolean b, int lengte) {
	print(String.valueOf(b), lengte);
    }


    public void print (Object o, int lengte) {
	print(o.toString(), lengte);
    }


    public void print (String s, int lengte) {
        int i;

        for (i = 0; i < lengte-s.length(); i++) {
	   try_write(SPATIE);
        }
        print(s);
    }


    /* println + lengte-parameter */

    public void println (byte b, int lengte) {
	println(String.valueOf((int)b), lengte);
    }


    public void println (short s, int lengte) {
	println(String.valueOf((int)s), lengte);
    }


    public void println (char c, int lengte) {
	println(String.valueOf(c), lengte);
    }


    public void println (int i, int lengte) {
	println(String.valueOf(i), lengte);
    }


    public void println (long l, int lengte) {
	println(String.valueOf(l), lengte);
    }


    public void println (float f, int lengte) {
	println(String.valueOf(f), lengte);
    }


    public void println (double d, int lengte) {
	println(String.valueOf(d), lengte);
    }


    public void println (boolean b, int lengte) {
	println(String.valueOf(b), lengte);
    }


    public void println (Object o, int lengte) {
	println(o.toString(), lengte);
    }


    public void println (String s, int lengte) {
	print(s, lengte);
        println();
    }


    private String nullen (int aantal) {
	String s = "";
	int i;

	for (i=0; i<aantal; i++) {
	    s += "0";
	}

	return s;
    }


    private String kommaNaarLinks (String s, int n) {
	int    aantalTientallen,
	       indexDecimalePunt,
	       aantalDecimalen;
	String tientallen,
	       decimalen,
	       extraNullen;

	// Waar staat de decimale punt?
	indexDecimalePunt = s.indexOf('.');
	if (indexDecimalePunt == -1) {
            System.err.println("no decimal point in string value of double");
	    System.exit(1);
	}

	// voor de decimale punt
	aantalTientallen = indexDecimalePunt;

	// achter de decimale punt
	aantalDecimalen = s.length() - indexDecimalePunt - 1;
    	extraNullen = nullen(n - aantalTientallen);
	decimalen = s.substring(indexDecimalePunt + 1);

        // Schuif de komma n plaatsen naar links.
        if (n < aantalTientallen) {
    	    tientallen = s.substring(0, aantalTientallen - n);
    	    decimalen = s.substring(aantalTientallen-n, aantalTientallen) +
    	                decimalen;
    	} else { // n >= aantalTientallen
    	    tientallen = "0";
    	    decimalen = s.substring(0, aantalTientallen) + decimalen;
	}

	return new String(tientallen + "." + extraNullen + decimalen);
    }


    private String kommaNaarRechts (String s, int n) {
	int    aantalTientallen,
	       indexDecimalePunt,
	       aantalDecimalen;
	String tientallen,
	       decimalen,
	       extraNullen;

	// Waar staat de decimale punt?
	indexDecimalePunt = s.indexOf('.');
	if (indexDecimalePunt == -1) {
            System.err.println("no decimal point in string value of double");
	    System.exit(1);
	}

	// voor de decimale punt
	aantalTientallen = indexDecimalePunt;
    	tientallen = s.substring(0, aantalTientallen);


	// achter de decimale punt
	aantalDecimalen = s.length() - indexDecimalePunt - 1;
	extraNullen = nullen(n - aantalDecimalen);

        // Schuif de komma n plaatsen naar rechts.
        if (n < aantalDecimalen) {
    	    tientallen +=s.substring(indexDecimalePunt+1,indexDecimalePunt+1+n);
    	    decimalen = s.substring(indexDecimalePunt+1+n);
    	} else { // n >= aantalDecimalen
    	    tientallen += s.substring(indexDecimalePunt+1);
    	    decimalen = "0";
	}

	return new String(tientallen + extraNullen + "." + decimalen);
    }


    // Levert een fixed point stringrepresentatie van een double.
    private String fixedPointNotatie (double d) {
	int     indexExponent,
		exponent = 0;
	boolean floatingPointNotatie;
	String  teken,
		s;

	// bepaal stringrepresentatie van de double-waarde
	s = String.valueOf(d);

	// Bepaal of er een exponent is.
	indexExponent = s.indexOf('E');
	floatingPointNotatie = indexExponent != -1;

	if (floatingPointNotatie) {
	    exponent = Integer.parseInt(s.substring(indexExponent + 1));
	    s = s.substring(0, indexExponent);	// verwijder exponent
	}
	
	// Geen decimale punt aanwezig; voeg er een toe.
	if (s.indexOf('.') == -1) {
	    s += ".";
	}
	
	// Klaar als string representatie niet in floating point notatie was.
	if (! floatingPointNotatie) {
	    return s;
	}

	// Bepaal teken
	if (s.charAt(0) == '-') {
	    teken = "-";
	    s = s.substring(1);			// verwijder teken
	} else {
	    teken = "";
	}

        if (exponent < 0) {
	    return teken + kommaNaarLinks(s, -exponent);
	} else if (exponent == 0) {
	    return teken + s;
	} else { // exponent > 0
	   return teken + kommaNaarRechts(s, exponent);
	}
    }


    private String afgerond (String s, int ndec) {
	int    i,
	       indexDecimalePunt,
	       aantalTientallen,
	       aantalDecimalen;
	String teken,
	       extraNullen;

	// Bepaal teken
	if (s.charAt(0) == '-') {
	    teken = "-";
	    s = s.substring(1);			// verwijder teken
	} else {
	    teken = "";
	}

	// Bepaal waar de decimale punt staat.
	if ((indexDecimalePunt = s.indexOf('.')) == -1) {
            System.err.println("no decimal point in string value of double");
	    System.exit(1);
	}

	// bepaal aantal tientallen
	aantalTientallen = indexDecimalePunt;

	// bepaal aantal decimalen
	aantalDecimalen = s.length() - indexDecimalePunt - 1;

	if (ndec < aantalDecimalen) {
	    extraNullen = "";
	    if (s.charAt(indexDecimalePunt + ndec + 1) >= '5') {
		// er moet afgerond worden.

		// gooi eerst de overbodige decimalen weg.
	        s = s.substring(0, indexDecimalePunt + ndec + 1);

		// schuif de komma ndec plaatsen naar rechts
		s = kommaNaarRechts(s, ndec);
		indexDecimalePunt += ndec;

		// het afronden
		// zoek eerst uit op welke positie het eerste cijfer staat
		// dat geen '9' is.
		for (i = indexDecimalePunt - 1;
		     i > -1 && s.charAt(i) == '9'   ; i--) {
		     // doe niets
		}

		if (i == -1) {
		    // het af te ronden getal bevat alleen negens.
		    s = "1"                             + 
			nullen(aantalTientallen + ndec) +
			".";
		} else
		if (i == 0) { // s.charAt(0) != '9'
		    s = Character.forDigit(s.charAt(0)-'0'+1, 10) +
			nullen(indexDecimalePunt-1)               +
			"."                                       ;
		} else { // s.charAt(i) != '9' && i > 0
		   s = s.substring(0, i)                         +
		       Character.forDigit(s.charAt(i)-'0'+1, 10) +
		       nullen(indexDecimalePunt-1-i)             +
		       "."                                       ;
		}
		indexDecimalePunt = s.length() - 1;

		// schuif de komma weer terug
		s = kommaNaarLinks(s, ndec);
		indexDecimalePunt -= ndec;
	    } else { // s.charAt(indexDecimalePunt + ndec + 1) < '5'
            // er moet worden afgekapt
		s = s.substring(0, indexDecimalePunt + ndec + 1);
            }
	} else if (ndec == aantalDecimalen) {
	    extraNullen = "";
	} else { // ndec > aantalDecimalen
	    extraNullen = nullen(ndec - aantalDecimalen);
	}
	
	return teken + s + extraNullen;
    }


    public void print (float f, int lengte, int aantalDecimalen) {
	print((double) f, lengte, aantalDecimalen);
    }


    public void print (double d, int lengte, int aantalDecimalen) {
	print(afgerond(fixedPointNotatie(d), aantalDecimalen), lengte);
    }


    public void println (float f, int lengte, int aantalDecimalen) {
	print(f, lengte, aantalDecimalen);
	println();
    }


    public void println (double d, int lengte, int aantalDecimalen) {
	print(d, lengte, aantalDecimalen);
	println();
    }


}

