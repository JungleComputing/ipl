/* $Id$ */

/*
 * Created on 12.02.2005
 */
package ibis.mpj;

/**
 * Implementation of the operation: sum (MPJ.SUM).
 * Only available for these datatypes: MPJ.SHORT, MPJ.INT, MPJ.LONG, MPJ.FLOAT, MPJ.DOUBLE.
 */

public class OpSum extends Op {
    OpSum(boolean commute) throws MPJException {
        super(commute);
    }

    public void call(Object invec, int inoffset, Object inoutvec, int outoffset, int count, Datatype datatype) throws MPJException { 
        if (datatype == MPJ.SHORT) {
            if (((short[])invec).length != ((short[])inoutvec).length) {
                return;
            }

            for (int i=0; i< count; i++) {

                short o1 = ((short[])invec)[i+inoffset];
                short o2 = ((short[])inoutvec)[i+outoffset];

                ((short[])inoutvec)[i+outoffset] = (short)(o1 + o2);
            }
            return;
        }
        else if (datatype == MPJ.INT)  {
            if (((int[])invec).length != ((int[])inoutvec).length) {
                return;
            }

            for (int i=0; i< count; i++) {

                int o1 = ((int[])invec)[i+inoffset];
                int o2 = ((int[])inoutvec)[i+outoffset];

                ((int[])inoutvec)[i] = o1 + o2;
            }
            return;
        }
        else if (datatype == MPJ.LONG) {
            if (((long[])invec).length != ((long[])inoutvec).length) {
                return;
            }

            for (int i=0; i< count; i++) {

                long o1 = ((long[])invec)[i+inoffset];
                long o2 = ((long[])inoutvec)[i+outoffset];

                ((long[])inoutvec)[i+outoffset] = o1 + o2;
            }
            return;
        }
        else if (datatype == MPJ.FLOAT) {				
            if (((float[])invec).length != ((float[])inoutvec).length) {
                return;
            }

            for (int i=0; i< count; i++) {

                float o1 = ((float[])invec)[i+inoffset];
                float o2 = ((float[])inoutvec)[i+outoffset];

                ((float[])inoutvec)[i+outoffset] = o1 + o2;
            }
            return;
        }
        else if (datatype == MPJ.DOUBLE) {
            if (((double[])invec).length != ((double[])inoutvec).length) {
                return;
            }

            for (int i=0; i< count; i++) {

                double o1 = ((double[])invec)[i+inoffset];
                double o2 = ((double[])inoutvec)[i+outoffset];

                ((double[])inoutvec)[i+outoffset] = o1 + o2;
            }
            return;
        }

        throw new MPJException("Operation does not support this Datatype");

    }
}
