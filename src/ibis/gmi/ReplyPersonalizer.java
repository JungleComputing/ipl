package ibis.group;

public class ReplyPersonalizer { 
    public void personalize(boolean in, boolean [] out) {
	for (int i=0;i<out.length;i++) out[i] = in;
    }
    public void personalize(byte in, byte [] out) {
	for (int i=0;i<out.length;i++) out[i] = in;
    }
    public void personalize(short in, short [] out) { 
	for (int i=0;i<out.length;i++) out[i] = in;
    }
    public void personalize(int in, int [] out) { 
	for (int i=0;i<out.length;i++) out[i] = in;
    }
    public void personalize(long in, long [] out) { 
	for (int i=0;i<out.length;i++) out[i] = in;
    }
    public void personalize(float in, float [] out) { 
	for (int i=0;i<out.length;i++) out[i] = in;
    }
    public void personalize(double in, double [] out) { 
	for (int i=0;i<out.length;i++) out[i] = in;
    }
    public void personalize(Object in, Object [] out) {
	for (int i=0;i<out.length;i++) out[i] = in;
    }
    public void personalize(Exception in, Exception [] out) { 
	for (int i=0;i<out.length;i++) out[i] = in;
    }
} 
