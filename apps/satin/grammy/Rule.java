// File: $id: $

public class Rule implements java.io.Serializable {
    String txt;         // The full text this rule produces.
    Rule members[];     // The constituent rules, or null.

    public Rule( byte c ){
        txt = new String( new byte[] { c } );
        members = null;
    }

    public Rule( Rule a, Rule b )
    {
        members = new Rule[] { a, b };
        txt = a.txt + b.txt;
    }
}
