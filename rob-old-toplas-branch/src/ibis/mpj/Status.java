/* $Id$ */

/*
 * Created on 21.01.2005
 */
package ibis.mpj;

/**
 * Holds status information of requests and prequests.
 */
public class Status {

    private int index;
    private int source;
    private int tag;
    private int count;
    private int size;


    /**
     * @return source of the received message
     * @throws MPJException
     */
    public int getSource() throws MPJException {
        return source;	
    }

    protected void setSource(int src) {
        source = src;
    }


    /**
     * @return tag of the received message
     * @throws MPJException
     */
    public int getTag() throws MPJException {
        return tag;
    }

    protected void setTag(int tag) {
        this.tag = tag;
    }

    /**
     * This method con be applied to any status object returned by Request.waitAny, Request.testAny,
     * Request.waitSome, or Request.testSome.
     * @return index of the operation that completed
     * @throws MPJException
     */
    public int getIndex() throws MPJException {
        return index;
    }

    protected void setIndex(int index) {
        this.index = index;
    }

    public int getSize() throws MPJException {
        return size;
    }

    protected void setSize(int size) {
        this.size = size;
    }

    /**
     * @param datatype datatype of each item in receive buffer
     * @return number of received entries
     * @throws MPJException
     */

    public int getCount(Datatype datatype) throws MPJException{ 
        if (datatype == null) {
            return(this.count);
        }
        return (this.count * datatype.extent());
    }

    protected void setCount(int count) {
        this.count = count;
    }

    public boolean testCancelled() throws MPJException { 
        return(false);
    } 


    public int getElements(Datatype datatype) throws MPJException { 
        return(0);
    }
}
