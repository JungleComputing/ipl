package ibis.ipl;

// Be careful, classes implementing this interface should override the methods equals() and hashCode().
// note: if an IbisIdentifier is sent over the network, it is not guaranteed that the object on the remote
// side will be a new one. This allows the Ibis implementation to cache IbisIdentifiers.
public interface IbisIdentifier {
	public String name();
}
