package ibis.group;

/**
 * The {@link ReplyPersonalizer} class can be extended by the user to create a
 * specific reply personalizer. The default one just copies.
 */
public class ReplyPersonalizer implements java.io.Serializable { 

    /**
     * Personalize method for booleans.
     *
     * @param in the result of the method invocation
     * @param out the array of personalized results
     */
    public void personalize(boolean in, boolean [] out) {
	for (int i=0;i<out.length;i++) out[i] = in;
    }

    /**
     * See {@link #personalize(boolean,boolean[])} but for bytes.
     */
    public void personalize(byte in, byte [] out) {
	for (int i=0;i<out.length;i++) out[i] = in;
    }

    /**
     * See {@link #personalize(boolean,boolean[])} but for shorts.
     */
    public void personalize(short in, short [] out) { 
	for (int i=0;i<out.length;i++) out[i] = in;
    }

    /**
     * See {@link #personalize(boolean,boolean[])} but for chars.
     */
    public void personalize(char in, char [] out) { 
	for (int i=0;i<out.length;i++) out[i] = in;
    }

    /**
     * See {@link #personalize(boolean,boolean[])} but for ints.
     */
    public void personalize(int in, int [] out) { 
	for (int i=0;i<out.length;i++) out[i] = in;
    }

    /**
     * See {@link #personalize(boolean,boolean[])} but for longs.
     */
    public void personalize(long in, long [] out) { 
	for (int i=0;i<out.length;i++) out[i] = in;
    }

    /**
     * See {@link #personalize(boolean,boolean[])} but for floats.
     */
    public void personalize(float in, float [] out) { 
	for (int i=0;i<out.length;i++) out[i] = in;
    }

    /**
     * See {@link #personalize(boolean,boolean[])} but for doubles.
     */
    public void personalize(double in, double [] out) { 
	for (int i=0;i<out.length;i++) out[i] = in;
    }

    /**
     * See {@link #personalize(boolean,boolean[])} but for Objects..
     */
    public void personalize(Object in, Object [] out) {
	for (int i=0;i<out.length;i++) out[i] = in;
    }

    /**
     * Personalize method for Exceptions. Yes, even exceptions can
     * be personalized.
     *
     * @param in the result of the method invocation
     * @param out the array of personalized results
     */
    public void personalize(Exception in, Exception [] out) { 
	for (int i=0;i<out.length;i++) out[i] = in;
    }
} 
