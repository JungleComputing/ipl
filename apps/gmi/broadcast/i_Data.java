
import ibis.gmi.*;

interface i_Data extends GroupInterface {
    void done(double result);

    void foo();

    void invokeRep();

    void bar();

    void barrier();
}