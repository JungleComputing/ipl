// File: $Id$

/**
 * Maintains a fixed-length list of compression steps in order of decreasing
 * gain.
 */

public class StepList {
    private Step steps[];
    private int ix;

    public StepList( int n )
    {
        steps = new Step[n];
    }

    /**
     * Given a compression step, adds it to the list, possibly
     * removing an existing one that is less interesting.
     */
    public void add( Step s )
    {
        int gain = s.getGain();
        if( ix<steps.length ){
            steps[ix++] = s;
        }
        else {
            int i = steps.length-1;

            if( steps[i].getGain()>=gain ){
                // The new one does not even improve on the worst
                // one in the list. Go away.
                return;
            }
            steps[i] = s;
        }

        // Now move the entry up until it has reached its rightful
        // position.
        int pos = ix;
        while( ix>1 ){
            ix--;
            if( steps[ix].getGain()>steps[ix-1].getGain() ){
                // Move the entry up.
                Step tmp = steps[ix];
                steps[ix] = steps[ix-1];
                steps[ix-1] = tmp;
            }
            else {
                break;
            }
        }
    }

    /**
     * Returns the top step, or null if there isn't one.
     */
    public Step getBestStep()
    {
        if( ix == 0 ){
            return null;
        }
        return steps[0];
    }
}
