package ibis.ipl;

import java.io.PrintStream;
import java.io.PrintWriter;

public class IbisIOException extends java.io.IOException {
	// this space was intensionally left blank, but is now taken...

	private Throwable original;

	public IbisIOException(String name) {
		super(name);
	}

	public IbisIOException(Throwable t) {
		super("" + t);
	}

	public IbisIOException(String name, Throwable e) {
		super(name);
		original = e;
	}
	
	public String getMessage() { 

		String result = super.getMessage();

		if (original != null) { 

			if (original.getMessage() != null) { 				
				result += "\n" + original.getMessage();
			}
		}
		
		return result;
	}

	public void printStackTrace() { 
		if (original != null) { 
			original.printStackTrace();
		}
		super.printStackTrace();
	}

	public void printStackTrace(PrintStream s) { 
		if (original != null) { 
			original.printStackTrace(s);
		}
		super.printStackTrace(s);
	}

	public void printStackTrace(PrintWriter s) { 
		if (original != null) { 
			original.printStackTrace(s);		
		}
		super.printStackTrace(s);
	}
	
	public String toString() { 
		
		if (original != null) { 		
			return super.toString() + "\n" + original.toString();
		} else { 
			return super.toString();
		}		
	} 
}
