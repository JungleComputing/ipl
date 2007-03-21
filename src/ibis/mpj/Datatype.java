/* $Id$ */

/*
 * Created on 21.01.2005
 */
package ibis.mpj;

import java.io.*;

/**
 * Organisation of primitive and derived datatypes.
 */
public class Datatype implements Serializable {
    /** 
     * Generated
     */
    private static final long serialVersionUID = 2863490198818044959L;
    protected static final int BASE_TYPE_BYTE = 1; 
    protected static final int BASE_TYPE_CHAR = 2;
    protected static final int BASE_TYPE_SHORT = 3;
    protected static final int BASE_TYPE_BOOLEAN = 4;
    protected static final int BASE_TYPE_INT = 5;
    protected static final int BASE_TYPE_LONG = 6;
    protected static final int BASE_TYPE_FLOAT = 7;
    protected static final int BASE_TYPE_DOUBLE = 8;
    protected static final int BASE_TYPE_OBJECT = 9;   


    protected int byteSize = 0;
    protected int type;
    protected int size = 1;
    protected int lb = 0; 
    protected int ub = 0;
    protected int[] displacements;
    protected boolean hasMPJLB = false;
    protected boolean hasMPJUB = false;

    Datatype() {
        displacements = new int[1];
        displacements[0] = 1;
    }

    protected int[] getDisplacements() {
        return (this.displacements);
    }


    protected int getByteSize() {
        return (byteSize);
    }


    protected int getType() {
        return(type);
    }



    /**
     * Construct new datatype representing replication of old datatype into contiguous locations.
     * @param count  replication count
     * @return new datatype
     * @throws MPJException
     */
    public Datatype contiguous(int count) throws MPJException {

        Datatype contDatatype = new Datatype();
        contDatatype.byteSize = this.byteSize;
        contDatatype.type = this.type;
        contDatatype.size = this.size;
        contDatatype.hasMPJLB = this.hasMPJLB;
        contDatatype.hasMPJUB = this.hasMPJUB;

        contDatatype.displacements = new int[count * this.size()];


        if(count > 0) {

            int displIndex = 0 ;

            for (int i = 0 ; i < count ; i++) {
                int nextItem = i * (this.extent()-1);

                for (int j = 0; j < this.size()-1; j++) {
                    contDatatype.displacements[displIndex] = nextItem + this.displacements[j];
                    displIndex++;
                }
            }
        }

        if (contDatatype.hasMPJLB) {
            contDatatype.lb = this.lb;
        }
        else {
            contDatatype.lb = Integer.MAX_VALUE;
            for (int i=0; i < contDatatype.displacements.length; i++) {
                if (contDatatype.lb > contDatatype.displacements[i]) {
                    contDatatype.lb = contDatatype.displacements[i];
                }
            }
        }

        if (contDatatype.hasMPJUB) {
            contDatatype.ub = this.ub + ((count-1) * (this.extent()-1));
        }
        else {
            contDatatype.ub = Integer.MIN_VALUE;
            for (int i=0; i < contDatatype.displacements.length; i++) {
                if (contDatatype.ub < (contDatatype.displacements[i] + 1)) {
                    contDatatype.ub = contDatatype.displacements[i] + 1;
                }
            }
        }

        return(contDatatype);
    }



    /**
     * <strong> NOT IMPLEMENTED YET. </strong>
     * Construct new datatype representing replication of old datatype into locations that consist of equally spaced blocks.
     * 
     * @param count  number of blocks
     * @param blocklength  number of elements in each block
     * @param stride  number of elements between start of each block
     * @return new datatype
     * @throws MPJException
     */
    public Datatype vector(int count, int blocklength, int stride) throws MPJException { 

        Datatype dummy = new Datatype();

        return(dummy);

    }


    /**
     * <strong> NOT IMPLEMENTED YET. </strong>
     * Identical to vector except that the stride is expressed directly in terms of the buffer index, rather than the units of the old type.
     * 
     * @param count number of blocks
     * @param blocklength number of elements in each block
     * @param stride number of elements between start of each block
     * @return new datatype
     * @throws MPJException
     */
    public Datatype hvector(int count, int blocklength, int stride) throws MPJException {

        Datatype dummy = new Datatype();

        return(dummy);

    }


    /**
     * <strong> NOT IMPLEMENTED YET. </strong>
     * Construct new datatype representing replication of old datatype into a sequence of blocks where each block can contain a different number of copies and have a different displacement.
     * 
     * @param arrayofBlocklengths number of elements per block
     * @param arrayofDisplacements displacement of each block in units of old type
     * @return new datatype
     * @throws MPJException
     */
    public Datatype indexed(int [] arrayofBlocklengths, int [] arrayofDisplacements) throws MPJException { 

        Datatype dummy = new Datatype();

        return(dummy);

    }


    /**
     * <strong> NOT IMPLEMENTED YET. </strong>
     * Identical to indexed except that the displacements are expressed directly in terms of the buffer index, rather than the units of the old type.
     * 
     * @param arrayofBlocklengths  number of elements per block
     * @param arrayofDisplacements  displacement in buffer for each block
     * @return  new datatype
     * @throws MPJException
     */
    public Datatype hindexed(int [] arrayofBlocklengths, int [] arrayofDisplacements) throws MPJException {

        Datatype dummy = new Datatype();

        return(dummy);

    }


    /**
     * <strong> NOT IMPLEMENTED YET. </strong>
     * The most general type constructor.
     * 
     * @param arrayofBlocklengths number of elements per block
     * @param arrayofDisplacements displacement in buffer for each block
     * @param arrayOfTypes type of elements in each block
     * @return new datatype
     * @throws MPJException
     */
    public static Datatype struct(int [] arrayofBlocklengths, int [] arrayofDisplacements,
            Datatype [] arrayOfTypes) throws MPJException { 

        Datatype dummy = new Datatype();

        return(dummy);

    }


    /**
     * Returns the extent of a datatype - the difference between upper and lower bound.
     * 
     * @return datatype extent
     * @throws MPJException
     */
    public int extent() throws MPJException { 

        return(this.ub - this.lb +1);
    }

    /**
     * Find the lower bound of a datatype - the least value in its displacement sequence.
     * @return displacement of lower bound from origin
     * @throws MPJException
     */
    public int lb() throws MPJException { 
        return(lb);
    }


    /**
     * Find the upper bound of a datatype - the greatest value in its displacement sequence.
     * @return displacement of upper bound from origin
     * @throws MPJException
     */
    public int ub() throws MPJException { 
        return(ub);
    }


    /**
     * Returns the total size of a datatype - the number of buffer elements it represents.
     * @return datatype size
     * @throws MPJException
     */
    public int size() throws MPJException { 
        return(this.displacements.length);
    }


    /**
     * <strong> NOT IMPLEMENTED YET. </strong>
     * Commit a derived datatype.
     * 
     * @throws MPJException
     */
    public void commit() throws MPJException {
    	// not implemented yet
    }


    public void finalize() throws MPJException {
    	// not implemented yet
    }

}

