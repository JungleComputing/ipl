/* $Id$ */

package ibis.gmi;

/**
 * The {@link ReplyPersonalizer} class can be extended by the user to create a
 * specific reply personalizer. The default one just copies.
 */
public class ReplyPersonalizer implements java.io.Serializable {

    /** Creates a copying <code>ReplyPersonalizer</code>.
    public ReplyPersonalizer() {
    }

    /**
     * Personalize method for booleans.
     *
     * @param in the result of the method invocation
     * @param out the array of personalized results
     */
    public void personalize(boolean in, boolean[] out) {
        for (int i = 0; i < out.length; i++)
            out[i] = in;
    }

    /**
     * Personalize method for bytes.
     *
     * @param in the result of the method invocation
     * @param out the array of personalized results
     */
    public void personalize(byte in, byte[] out) {
        for (int i = 0; i < out.length; i++)
            out[i] = in;
    }

    /**
     * Personalize method for shorts.
     *
     * @param in the result of the method invocation
     * @param out the array of personalized results
     */
    public void personalize(short in, short[] out) {
        for (int i = 0; i < out.length; i++)
            out[i] = in;
    }

    /**
     * Personalize method for chars.
     *
     * @param in the result of the method invocation
     * @param out the array of personalized results
     */
    public void personalize(char in, char[] out) {
        for (int i = 0; i < out.length; i++)
            out[i] = in;
    }

    /**
     * Personalize method for ints.
     *
     * @param in the result of the method invocation
     * @param out the array of personalized results
     */
    public void personalize(int in, int[] out) {
        for (int i = 0; i < out.length; i++)
            out[i] = in;
    }

    /**
     * Personalize method for longs.
     *
     * @param in the result of the method invocation
     * @param out the array of personalized results
     */
    public void personalize(long in, long[] out) {
        for (int i = 0; i < out.length; i++)
            out[i] = in;
    }

    /**
     * Personalize method for floats.
     *
     * @param in the result of the method invocation
     * @param out the array of personalized results
     */
    public void personalize(float in, float[] out) {
        for (int i = 0; i < out.length; i++)
            out[i] = in;
    }

    /**
     * Personalize method for doubles.
     *
     * @param in the result of the method invocation
     * @param out the array of personalized results
     */
    public void personalize(double in, double[] out) {
        for (int i = 0; i < out.length; i++)
            out[i] = in;
    }

    /**
     * Personalize method for Objects
     *
     * @param in the result of the method invocation
     * @param out the array of personalized results
     */
    public void personalize(Object in, Object[] out) {
        for (int i = 0; i < out.length; i++)
            out[i] = in;
    }

    /**
     * Personalize method for Exceptions. Yes, even exceptions can
     * be personalized.
     *
     * @param in the result of the method invocation
     * @param out the array of personalized results
     */
    public void personalize(Exception in, Exception[] out) {
        for (int i = 0; i < out.length; i++)
            out[i] = in;
    }
}
