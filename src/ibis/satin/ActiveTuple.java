package ibis.satin;

/**
 * The marker interface of active tuples. Every active tuple should implement
 * this interface. When an active tuple is added to the tuple space (as data
 * element), the handleTuple method will be invoked on all machines, including
 * the one that inserted the element.
 */
public interface ActiveTuple extends java.io.Serializable {
	/**
	 * Notifies that a new active tuple with the specified key has been added to
	 * the tuple space.
	 * 
	 * @param key
	 *            The key of the new active tuple.
	 */
	public void handleTuple(String key);
}