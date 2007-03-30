/* $Id$ */

/*
 * Created on 21.01.2005
 */
package ibis.mpj;

/**
 * MPJ Exception.
 */
public class MPJException extends Throwable {

    /** 
     * Generated
     */
    private static final long serialVersionUID = -6740661058871456218L;

    /**
     * Constructs an <code>MPJException</code> with no specified
     * detail message.
     */
    public MPJException() {
        super();
    }

    /**
     * Constructs an <code>MPJException</code> with the specified
     * detail message.
     * @param s the detail message
     */
    public MPJException(String s) {
        super(s);
    }

    /**
     * Constructs an <code>MPJException</code> with the specified
     * detail message and cause.
     * @param s the detail message
     * @param e the cause
     */
    public MPJException(String s, Throwable e) {
        super(s, e);
    }

}
