package ibis.ipl.impl.stacking.sns;

import ibis.ipl.IbisIdentifier;
import ibis.ipl.Location;

import java.io.UnsupportedEncodingException;

public class SNSIbisIdentifier implements ibis.ipl.IbisIdentifier{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	IbisIdentifier base;
	SNSIbisApplicationTag tag;
	
	public SNSIbisIdentifier (IbisIdentifier base){
		this.base = base;
		
		tag = new SNSIbisApplicationTag(base.tag());
	}
	
	@Override
	public Location location() {
		return base.location();
	}

	@Override
	public String name() {
		return base.name();
	}

	@Override
	public String poolName() {
		return base.poolName();
	}

	@Override
	public byte[] tag() {
		return tag.appTag;
	}

    /**
     * Return only the tag from the application, SNS Ibis-related tag is not included
     * 
     * @return a string of application tag 
     */
	@Override
	public String tagAsString() {		
        if (tag.appTag == null) {
            return null;
        }
        try {
            return new String(tag.appTag, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("could not convert tag to string", e);
        }		
	}

	@Override
	public int compareTo(IbisIdentifier o) {
		return base.compareTo(o);
	}

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o == null) {
            return false;
        }

//Don't Compare the class        
/* 
        if (!o.getClass().equals(getClass())) {
            return false;
        }
*/
        IbisIdentifier other = (IbisIdentifier) o;
        return other.name().equals(this.name()) && other.poolName().equals(poolName());
    }

    public int hashCode() {
        return base.hashCode();
    }
	
    public String toString() {
    	return base.toString();
    }   
}
