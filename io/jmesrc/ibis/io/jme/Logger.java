package ibis.io.jme;

/* 
 * This is a shell class for the moment since log4jme doesn't actually
 * work on JME devices.
 */
public class Logger {
	
	public Logger() {
		
	}
	
	boolean isDebugEnabled() { return false; }
	boolean isInfoEnabled() { return false; }
	void debug(String arg) {}
	void debug(String arg, Throwable e) {}
	void info(String arg) {}
	void fatal(String arg, Throwable e) {}
}
