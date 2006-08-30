/* $Id$ */


import java.io.Serializable;

class PivotElt implements Serializable {
    double norm;

    double max_over_max_cols;

    int index;

    int cols;

    int max_cols;
}