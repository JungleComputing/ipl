package ibis.satin;

/**
 * Describes the local variables and parameters of a method invoking
 * a spawnable method.
 * The Satin frontend generates a subclass of this class for each
 * caller of a spawnable method.
 */
abstract public class LocalRecord {
	/**
	 * Deals with an exception or error which is raised by the Satin
	 * invocation described by the parameters.
	 * This method gets called when a Satin job, executed locally,
	 * throws an exception or error.
	 * @param spawnId the identification of the spawned Satin invocation.
	 * @param t the exception or error thrown by this invocation.
	 * @param parent the invocation record describing this invocation.
	 */
	abstract public void handleException(int spawnId, Throwable t, InvocationRecord parent);
}
