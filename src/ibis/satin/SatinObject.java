package ibis.satin;

public class SatinObject {
	public void sync() {}
	public void abort() {}

	/** Pause Satin operation. 
	    This method can optionally be called before a large sequential part in a program.
	    This will temporarily pause Satin's internal load distribution strategies to 
	    avoid communication overhead during sequential code.
	**/
	public static void pause() {
		Satin.pause();
	}

	/** Resume Satin operation. 
	    This method can optionally be called after a large sequential part in a program.
	**/
	public static void resume() {
		Satin.resume();
	}
}
