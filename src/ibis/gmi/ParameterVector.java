package ibis.group;

// This class is used as a wrapper for a marhalstruct.
// Needed for the implementation of a "personalized group invocation".

public abstract class ParameterVector { 
         	
    public boolean done = false;
    public int set_count = 0;
    public boolean [] set;
    public int marshal_struct;

    public abstract void reset();

    public void reset(int marshal_struct) { 
	reset();
	this.marshal_struct = marshal_struct;
    } 

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

    public boolean readBoolean(int num) { 
	throw new RuntimeException("EEK: ParameterVector"); 
    }
    public byte readByte(int num) { 
	throw new RuntimeException("EEK: ParameterVector"); 
    }
    public short readShort(int num) { 
	throw new RuntimeException("EEK: ParameterVector"); 
    }
    public char readChar(int num) { 
	throw new RuntimeException("EEK: ParameterVector"); 
    }
    public int readInt(int num) { 
	throw new RuntimeException("EEK: ParameterVector"); 
    }
    public long readLong(int num) { 
	throw new RuntimeException("EEK: ParameterVector"); 
    }
    public float readFloat(int num) { 
	throw new RuntimeException("EEK: ParameterVector"); 
    }
    public double readDouble(int num) { 
	throw new RuntimeException("EEK: ParameterVector"); 
    }
    public Object readObject(int num) { 
	throw new RuntimeException("EEK: ParameterVector"); 
    }


    public void read(int num, boolean[] dest) { 
	throw new RuntimeException("EEK: ParameterVector"); 
    }
    public void read(int num, byte[] dest) { 
	throw new RuntimeException("EEK: ParameterVector"); 
    }
    public void read(int num, short[] dest) { 
	throw new RuntimeException("EEK: ParameterVector"); 
    }
    public void read(int num, char[] dest) { 
	throw new RuntimeException("EEK: ParameterVector"); 
    }
    public void read(int num, int[] dest) { 
	throw new RuntimeException("EEK: ParameterVector"); 
    }
    public void read(int num, long[] dest) { 
	throw new RuntimeException("EEK: ParameterVector"); 
    }
    public void read(int num, float[] dest) { 
	throw new RuntimeException("EEK: ParameterVector"); 
    }
    public void read(int num, double[] dest) { 
	throw new RuntimeException("EEK: ParameterVector"); 
    }
    public void read(int num, Object[] dest) { 
	throw new RuntimeException("EEK: ParameterVector"); 
    }


    public void readSubArray(int num, int offset, int size, boolean[] dest) { 
	throw new RuntimeException("EEK: ParameterVector"); 
    }
    public void readSubArray(int num, int offset, int size, byte[] dest) { 
	throw new RuntimeException("EEK: ParameterVector"); 
    }
    public void readSubArray(int num, int offset, int size, short[] dest) { 
	throw new RuntimeException("EEK: ParameterVector"); 
    }
    public void readSubArray(int num, int offset, int size, char[] dest) { 
	throw new RuntimeException("EEK: ParameterVector"); 
    }
    public void readSubArray(int num, int offset, int size, int[] dest) { 
	throw new RuntimeException("EEK: ParameterVector"); 
    }
    public void readSubArray(int num, int offset, int size, long[] dest) { 
	throw new RuntimeException("EEK: ParameterVector"); 
    }
    public void readSubArray(int num, int offset, int size, float[] dest) { 
	throw new RuntimeException("EEK: ParameterVector"); 
    }
    public void readSubArray(int num, int offset, int size, double[] dest) { 
	throw new RuntimeException("EEK: ParameterVector"); 
    }
    public void readSubArray(int num, int offset, int size, Object[] dest) { 
	throw new RuntimeException("EEK: ParameterVector"); 
    }


    public void store(int num, boolean value) { 
	throw new RuntimeException("EEK: ParameterVector"); 
    }
    public void store(int num, byte value) { 
	throw new RuntimeException("EEK: ParameterVector"); 
    }
    public void store(int num, short value) { 
	throw new RuntimeException("EEK: ParameterVector"); 
    }
    public void store(int num, char value) { 
	throw new RuntimeException("EEK: ParameterVector"); 
    }
    public void store(int num, int value) { 
	throw new RuntimeException("EEK: ParameterVector"); 
    }
    public void store(int num, long value) { 
	throw new RuntimeException("EEK: ParameterVector"); 
    }
    public void store(int num, float value) { 
	throw new RuntimeException("EEK: ParameterVector"); 
    }
    public void store(int num, double value) { 
	throw new RuntimeException("EEK: ParameterVector"); 
    }
    public void store(int num, Object value) { 
	throw new RuntimeException("EEK: ParameterVector"); 
    }

    public void store(int num, boolean[] value) { 
	throw new RuntimeException("EEK: ParameterVector"); 
    }
    public void store(int num, byte[] value) { 
	throw new RuntimeException("EEK: ParameterVector"); 
    }
    public void store(int num, short[] value) { 
	throw new RuntimeException("EEK: ParameterVector"); 
    }
    public void store(int num, char[] value) { 
	throw new RuntimeException("EEK: ParameterVector"); 
    }
    public void store(int num, int[] value) { 
	throw new RuntimeException("EEK: ParameterVector"); 
    }
    public void store(int num, long[] value) { 
	throw new RuntimeException("EEK: ParameterVector"); 
    }
    public void store(int num, float[] value) { 
	throw new RuntimeException("EEK: ParameterVector"); 
    }
    public void store(int num, double[] value) { 
	throw new RuntimeException("EEK: ParameterVector"); 
    }
    public void store(int num, Object[] value) { 
	throw new RuntimeException("EEK: ParameterVector"); 
    }
}
