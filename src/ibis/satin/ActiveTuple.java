package ibis.satin;

/** Active tuples should implement this interface.
 When an active tuple is added to the tuple space (as data element),
 the handleTuple method will be invoked on all machines, except the
 one that inserted the element.  the inserted tuple key is passed as
 parameter.
**/
public interface ActiveTuple extends java.io.Serializable {
	public void handleTuple(String key);
}
