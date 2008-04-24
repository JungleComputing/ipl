/**
 * 
 */
package ibis.ipl.impl.mx;

import ibis.ipl.CapabilitySet;
import ibis.ipl.IbisCapabilities;
import ibis.ipl.IbisStarter;
import ibis.ipl.PortType;

/**
 * @author Timo van Kessel
 *
 */
public class MxIbisStarter extends IbisStarter {

	/**
	 * 
	 */
	public MxIbisStarter() {
		// TODO Auto-generated constructor stub
	}

	/* (non-Javadoc)
	 * @see ibis.ipl.IbisStarter#isSelectable()
	 */
	@Override
	public boolean isSelectable() {
		// TODO Auto-generated method stub
		return false;
	}

	/* (non-Javadoc)
	 * @see ibis.ipl.IbisStarter#matches(ibis.ipl.IbisCapabilities, ibis.ipl.PortType[])
	 */
	@Override
	public boolean matches(IbisCapabilities capabilities, PortType[] portTypes) {
		// TODO Auto-generated method stub
		return false;
	}

	/* (non-Javadoc)
	 * @see ibis.ipl.IbisStarter#unmatchedIbisCapabilities()
	 */
	@Override
	public CapabilitySet unmatchedIbisCapabilities() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see ibis.ipl.IbisStarter#unmatchedPortTypes()
	 */
	@Override
	public PortType[] unmatchedPortTypes() {
		// TODO Auto-generated method stub
		return null;
	}

}
