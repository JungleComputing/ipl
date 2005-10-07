import ibis.satin.so.WriteMethodsInterface;

public interface CostArrayInterface extends WriteMethodsInterface {

    public void placeWire(Wire wire);

    public void ripOutWire(Wire wire);

}
