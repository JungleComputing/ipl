package ibis.satin;

/* must be different from InvocationRecord, we don't want to serialize the parameters again. */
public abstract class ReturnRecord implements java.io.Serializable {
	protected int stamp;
	public Throwable eek = null;

	protected ReturnRecord(Throwable eek) {
		this.eek = eek;
	}

	public abstract void assignTo(InvocationRecord r);
}
