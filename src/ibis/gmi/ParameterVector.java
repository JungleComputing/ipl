package ibis.group;

import ibis.ipl.WriteMessage;
import ibis.ipl.IbisException;

// This class is used as a wrapper for a marhalstruct. Needed for the implementation of a "personalized group invocation".

public abstract class ParameterVector { 
         	
	public boolean done = false;
	public int set_count = 0;
	public boolean [] set;

	public abstract void reset();

	public void write(int num, boolean value) { 
		throw new RuntimeException("EEK: ParameterVector"); 
	}
	public void write(int num, byte value) { 
		throw new RuntimeException("EEK: ParameterVector"); 
	}
	public void write(int num, short value) { 
		throw new RuntimeException("EEK: ParameterVector"); 
	}
	public void write(int num, char value) { 
		throw new RuntimeException("EEK: ParameterVector"); 
	}
	public void write(int num, int value) { 
		throw new RuntimeException("EEK: ParameterVector"); 
	}
	public void write(int num, long value) { 
		throw new RuntimeException("EEK: ParameterVector"); 
	}
	public void write(int num, float value) { 
		throw new RuntimeException("EEK: ParameterVector"); 
	}
	public void write(int num, double value) { 
		throw new RuntimeException("EEK: ParameterVector"); 
	}
	public void write(int num, Object value) { 
		throw new RuntimeException("EEK: ParameterVector"); 
	}


	public void write(int num, boolean[] value) { 
		throw new RuntimeException("EEK: ParameterVector"); 
	}
	public void write(int num, byte[] value) { 
		throw new RuntimeException("EEK: ParameterVector"); 
	}
	public void write(int num, short[] value) { 
		throw new RuntimeException("EEK: ParameterVector"); 
	}
	public void write(int num, char[] value) { 
		throw new RuntimeException("EEK: ParameterVector"); 
	}
	public void write(int num, int[] value) { 
		throw new RuntimeException("EEK: ParameterVector"); 
	}
	public void write(int num, long[] value) { 
		throw new RuntimeException("EEK: ParameterVector"); 
	}
	public void write(int num, float[] value) { 
		throw new RuntimeException("EEK: ParameterVector"); 
	}
	public void write(int num, double[] value) { 
		throw new RuntimeException("EEK: ParameterVector"); 
	}
	public void write(int num, Object[] value) { 
		throw new RuntimeException("EEK: ParameterVector"); 
	}


	public void writeSubArray(int num, int offset, int size, boolean[] value) { 
		throw new RuntimeException("EEK: ParameterVector"); 
	}
	public void writeSubArray(int num, int offset, int size, byte[] value) { 
		throw new RuntimeException("EEK: ParameterVector"); 
	}
	public void writeSubArray(int num, int offset, int size, short[] value) { 
		throw new RuntimeException("EEK: ParameterVector"); 
	}
	public void writeSubArray(int num, int offset, int size, char[] value) { 
		throw new RuntimeException("EEK: ParameterVector"); 
	}
	public void writeSubArray(int num, int offset, int size, int[] value) { 
		throw new RuntimeException("EEK: ParameterVector"); 
	}
	public void writeSubArray(int num, int offset, int size, long[] value) { 
		throw new RuntimeException("EEK: ParameterVector"); 
	}
	public void writeSubArray(int num, int offset, int size, float[] value) { 
		throw new RuntimeException("EEK: ParameterVector"); 
	}
	public void writeSubArray(int num, int offset, int size, double[] value) { 
		throw new RuntimeException("EEK: ParameterVector"); 
	}
	public void writeSubArray(int num, int offset, int size, Object[] value) { 
		throw new RuntimeException("EEK: ParameterVector"); 
	}

}









