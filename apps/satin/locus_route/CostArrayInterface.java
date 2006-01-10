import ibis.satin.WriteMethodsInterface;

public interface CostArrayInterface extends WriteMethodsInterface {

    public void placeWire(Wire wire);

    public void ripOutWire(Wire wire);

}
