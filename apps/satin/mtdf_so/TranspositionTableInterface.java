
import ibis.satin.so.*;

public interface TranspositionTableInterface extends WriteMethodsInterface {
    void broadcast_store(String key, TTEntry value);
}