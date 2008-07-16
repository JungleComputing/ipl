/* $Id$ */

package ibis.compile;

import java.io.IOException;

import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.verifier.VerificationResult;
import org.apache.bcel.verifier.Verifier;
import org.apache.bcel.verifier.VerifierFactory;


/**
 * BCEL implementation of the <code>ClassInfo</code> interface.
 */
public class BCELClassInfo implements ClassInfo {

    private JavaClass cl;

    /**
     * Create a BCELClassInfo object for a given class.
     * 
     * @param cl Class to create the info object for.
     */
    public BCELClassInfo(JavaClass cl) {
        this.cl = cl;
    }

    public String getClassName() {
        return cl.getClassName();
    }

    public Object getClassObject() {
        return cl;
    }

    void setClassObject(JavaClass cl) {
        this.cl = cl;
    }

    public void dump(String fileName) throws IOException {
        cl.dump(fileName);
    }

    public byte[] getBytes() {
        return cl.getBytes();
    }

    public boolean doVerify() {
        Verifier verf = VerifierFactory.getVerifier(cl.getClassName());
        boolean verification_failed = false;

        VerificationResult res = verf.doPass1();
        if (res.getStatus() == VerificationResult.VERIFIED_REJECTED) {
            System.out.println("Ibisc: Verification pass 1 failed.");
            System.out.println(res.getMessage());
            verification_failed = true;
        } else {
            res = verf.doPass2();
            if (res.getStatus() == VerificationResult.VERIFIED_REJECTED) {
                System.out.println("Ibisc: Verification pass 2 failed.");
                System.out.println(res.getMessage());
                verification_failed = true;
            } else {
                Method[] cMethods = cl.getMethods();
                for (int i = 0; i < cMethods.length; i++) {
                    res = verf.doPass3a(i);
                    if (res.getStatus() 
                            == VerificationResult.VERIFIED_REJECTED) {
                        System.out.println("Ibisc: Verification pass 3a failed "
                                + "for method " + cMethods[i].getName());
                        System.out.println(res.getMessage());
                        verification_failed = true;
                    } else {
                        res = verf.doPass3b(i);
                        if (res.getStatus() 
                                == VerificationResult.VERIFIED_REJECTED) {
                            System.out.println("Ibisc: Verification pass 3b "
                                    + "failed for method "
                                    + cMethods[i].getName());
                            System.out.println(res.getMessage());
                            verification_failed = true;
                        }
                    }
                }
            }
        }
        return !verification_failed;
    }
}
