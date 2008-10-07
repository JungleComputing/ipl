/* $Id$ */

package ibis.io.jme;

import ibis.io.SerializationInput;

/** 
 * The <code>ObjectInput</code> is a rename of the SerializationInput
 * interface in order to support replacement of classes using Sun
 * serialization with just a change in packages since Sun serialization
 * has an <code>ObjectInput</code> interface on it's 
 * <code>ObjectStream*</code> classes.
 **/

public interface ObjectInput extends SerializationInput {
}
