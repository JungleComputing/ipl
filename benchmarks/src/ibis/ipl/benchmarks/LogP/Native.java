package ibis.ipl.benchmarks.LogP;

import java.util.Properties;

public final class Native {
    static {
	System.loadLibrary("Native");
    }

    public static native long timestamp();
}
