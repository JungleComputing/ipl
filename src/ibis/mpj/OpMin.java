/* $Id$ */

/*
 * Created on 12.02.2005
 */
package ibis.mpj;

/**
 * Implementation of the operation: minimum (MPJ.MIN).
 * Only available for these datatypes: MPJ.BYTE, MPJ.CHAR, MPJ.SHORT, MPJ.BOOLEAN, MPJ.INT, MPJ.LONG, MPJ.FLOAT, MPJ.DOUBLE.
 */
public class OpMin extends Op{
    OpMin(boolean commute) throws MPJException {
        super(commute);
    }

    public void call(Object invec, int inoffset, Object inoutvec, int outoffset, int count, Datatype datatype) throws MPJException {
        if (datatype == MPJ.BYTE) {
            if (((byte[])invec).length != ((byte[])inoutvec).length) {
                return;
            }

            for (int i=0; i< count; i++) {

                byte o1 = ((byte[])invec)[i+inoffset];
                byte o2 = ((byte[])inoutvec)[i+outoffset];

                if (o1 < o2) {
                    ((byte[])inoutvec)[i+outoffset] = o1;
                }
                else {
                    ((byte[])inoutvec)[i+outoffset] = o2;
                }

            }
            return;

        }
        else if (datatype == MPJ.CHAR) {
            if (((char[])invec).length != ((char[])inoutvec).length) {
                return;
            }

            for (int i=0; i< count; i++) {

                char o1 = ((char[])invec)[i+inoffset];
                char o2 = ((char[])inoutvec)[i+outoffset];

                if (o1 < o2) {
                    ((char[])inoutvec)[i+outoffset] = o1;
                }
                else {
                    ((char[])inoutvec)[i+outoffset] = o2;
                }

            }
            return;
        }
        else if (datatype == MPJ.SHORT) {
            if (((short[])invec).length != ((short[])inoutvec).length) {
                return;
            }

            for (int i=0; i< count; i++) {

                short o1 = ((short[])invec)[i+inoffset];
                short o2 = ((short[])inoutvec)[i+outoffset];

                if (o1 < o2) {
                    ((short[])inoutvec)[i+outoffset] = o1;
                }
                else {
                    ((short[])inoutvec)[i+outoffset] = o2;
                }

            }
            return;
        }
        else if (datatype == MPJ.BOOLEAN) {
            if (((boolean[])invec).length != ((boolean[])inoutvec).length) {
                return;
            }

            for (int i=0; i< count; i++) {

                boolean o1 = ((boolean[])invec)[i+inoffset];
                boolean o2 = ((boolean[])inoutvec)[i+outoffset];

                if (!o1) {
                    ((boolean[])inoutvec)[i+outoffset] = o1;
                }
                else {
                    ((boolean[])inoutvec)[i+outoffset] = o2;
                }

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

                if (o1 < o2) {
                    ((int[])inoutvec)[i+outoffset] = o1;
                }
                else {
                    ((int[])inoutvec)[i+outoffset] = o2;
                }

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

                if (o1 < o2) {
                    ((long[])inoutvec)[i+outoffset] = o1;
                }
                else {
                    ((long[])inoutvec)[i+outoffset] = o2;
                }

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

                if (o1 < o2) {
                    ((float[])inoutvec)[i+outoffset] = o1;
                }
                else {
                    ((float[])inoutvec)[i+outoffset] = o2;
                }

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

                if (o1 < o2) {
                    ((double[])inoutvec)[i+outoffset] = o1;
                }
                else {
                    ((double[])inoutvec)[i+outoffset] = o2;
                }

            }
            return;
        }

        throw new MPJException("Operation does not support this Datatype");
    }
}
