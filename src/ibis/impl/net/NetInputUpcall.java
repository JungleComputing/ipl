package ibis.impl.net;

import java.io.IOException;

public interface NetInputUpcall {
    public void inputUpcall(NetInput input, Integer spn) throws IOException;
}