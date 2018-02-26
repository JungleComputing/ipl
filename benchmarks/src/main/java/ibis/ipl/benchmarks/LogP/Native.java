package ibis.ipl.benchmarks.LogP;

public final class Native {
    static {
	System.loadLibrary("Native");
    }

    public static native long timestamp();
}
