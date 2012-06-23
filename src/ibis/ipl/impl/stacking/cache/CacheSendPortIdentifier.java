package ibis.ipl.impl.stacking.cache;

import ibis.ipl.IbisIdentifier;
import ibis.ipl.SendPortIdentifier;

public class CacheSendPortIdentifier implements SendPortIdentifier {

    IbisIdentifier ii;
    String name;
    
    CacheSendPortIdentifier(String name, IbisIdentifier ii) {
        this(ii, name);
    }

    CacheSendPortIdentifier(IbisIdentifier ii, String name) {
        if (name == null) {
            throw new NullPointerException("name is null in "
                    + "CacheSendPortIdentifier");
        }
        if (ii == null) {
            throw new NullPointerException("Ibis identifier is null in "
                    + "CacheSendPortIdentifier");
        }
        this.ii = ii;
        this.name = name;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public IbisIdentifier ibisIdentifier() {
        return ii;
    }
    
    @Override
    public int hashCode() {
        int hash = 7;
        hash = 53 * hash + (this.ii != null ? this.ii.hashCode() : 0);
        hash = 53 * hash + (this.name != null ? this.name.hashCode() : 0);
        return hash;
    }
    
    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        if (o == this) {
            return true;
        }
        if (!(o instanceof SendPortIdentifier)) {
            return false;
        }
        SendPortIdentifier other = (SendPortIdentifier) o;
        
        return this.name().equals(other.name()) && 
                this.ibisIdentifier().equals(other.ibisIdentifier());
    }
    
    @Override
    public String toString() {
        return ("(CacheSendPortIdentifier: name = \"" + name
                + "\", ibis = \"" + ii + "\")");
    }
}
