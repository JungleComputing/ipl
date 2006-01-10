
import ibis.satin.WriteMethodsInterface;

public interface TranspositionTableInterface extends WriteMethodsInterface {
    void broadcast_store(String key, TTEntry value);
}
