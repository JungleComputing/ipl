package ibis.ipl.impl.stacking.cache;

import ibis.ipl.IbisIdentifier;
import ibis.ipl.ReceivePortIdentifier;

public class CacheReceivePortIdentifier implements ReceivePortIdentifier {

    IbisIdentifier ii;
    String name;

    CacheReceivePortIdentifier(IbisIdentifier ii, String name) {
        this(name, ii);
    }

    CacheReceivePortIdentifier(String name, IbisIdentifier ii) {
        if (name == null) {
            throw new NullPointerException("name is null in "
                    + "CacheReceivePortIdentifier");
        }
        if (ii == null) {
            throw new NullPointerException("Ibis identifier is null in "
                    + "CacheReceivePortIdentifier");
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
        hash = 41 * hash + (this.ii != null ? this.ii.hashCode() : 0);
        hash = 41 * hash + (this.name != null ? this.name.hashCode() : 0);
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
        if (!(o instanceof ReceivePortIdentifier)) {
            return false;
        }
        ReceivePortIdentifier other = (ReceivePortIdentifier) o;
        
        return this.name().equals(other.name()) && 
                this.ibisIdentifier().equals(other.ibisIdentifier());
    }
    
    @Override
    public String toString(){
        return ("(CacheReceivePortIdentifier: name = \"" + name
                + "\", ibis = \"" + ii + "\")");
    }
}
