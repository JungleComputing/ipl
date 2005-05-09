/* $Id$ */
package ibis.util;

import org.apache.log4j.Logger;
import org.apache.log4j.Level;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.WriterAppender;

/**
 * Utility to obtain a log4j logger even when there is no configuration file.
 */

public class GetLogger {

    /** Layout for default. */
    private static PatternLayout layout = new PatternLayout();

    /** Appender for default. */
    private static WriterAppender appender
            = new WriterAppender(layout, System.out);

    /** Prevent instantiation of this class. */
    private GetLogger() {
    }

    /**
     * Creates a log4j logger. If no log4j configuration is found, a default
     * one is created with level WARN and console appender.
     * @param name the name of this logger.
     * @return the logger.
     */
    public static Logger getLogger(String name) {
        return getLogger(name, (Level) Level.WARN);
    }

    /**
     * Creates a log4j logger. If no log4j configuration is found, a default
     * one is created with level WARN and console appender.
     * @param cl the class that bears the name of this logger.
     * @return the logger.
     */
    public static Logger getLogger(Class cl) {
        return getLogger(cl.getName());
    }

    /**
     * Creates a log4j logger. If no log4j configuration is found, a default
     * one is created with the specified level and console appender.
     * @param name the name of this logger.
     * @param level the level of this logger.
     * @return the logger.
     */
    public static Logger getLogger(String name, Level level) {
        Logger logger = Logger.getLogger(name);
        // System.out.println("Creating logger " + name);
        
        Logger rootLogger = Logger.getRootLogger();
        if (! rootLogger.getAllAppenders().hasMoreElements()) {
            // No appenders defined for this appender.
            logger.addAppender(appender);
            logger.setLevel(level);
            // System.out.println("Setting level of logger " + name);
        }
        return logger;
    }

    /**
     * Creates a log4j logger. If no log4j configuration is found, a default
     * one is created with the specified level and console appender.
     * @param cl the class that bears the name of this logger.
     * @param level the level of this logger.
     * @return the logger.
     */
    public static Logger getLogger(Class cl, Level level) {
        return getLogger(cl.getName(), level);
    }
}
