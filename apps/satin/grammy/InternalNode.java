// File: $Id$
//
// An internal node in the suffix tree.

import java.io.PrintStream;

class InternalNode extends Node {
    Node child;

    InternalNode( int start, int len, int dist )
    {
        super( start, len, dist );
        child = null;
    }

    /** Creates the root node. */
    InternalNode()
    {
        super( 0, 0, 0 );
    }

    protected void add( short text[], int start, int length, int pos )
        throws VerificationException
    {
        if( traceAdding ){
            System.out.println( "Adding [" + SuffixTree.buildString( text, start, length ) + "] @" + pos );
        }

        Node i = null;      // Insertion point.
        Node p = child;
        boolean match = false;

        while( p != null )
        {
            if( text[start] == text[p.start] ){
                // Node p starts the same as the text we want to
                // add, so we'll have to do some kind of merge.
                match = true;
                int l = Math.min( length, p.length );
                int n = 1;

                while( n<l ){
                    if( text[start+n] != text[p.start+n] ){
                        break;
                    }
                    n++;
                }
                if( n<p.length ){
                    // We must split up this node.
                    InternalNode nw = new InternalNode( p.start, n, p.dist );
                    nw.child = p;
                    nw.sister = p.sister;
                    nw.parent = p.parent;
                    p.sister = null;
                    p.start += n;
                    p.length -= n;
                    p.dist += n;
                    p.parent = nw;
                    if( i == null ){
                        child = nw;
                    }
                    else {
                        i.sister = nw;
                    }
                    nw.add( text, start+n, length-n, pos );
                }
                else {
                    // This entire node matches.
                    p.add( text, start+p.length, length-p.length, pos );
                }
                break;
            }
            if( text[start] < text[p.start] ){
                // We have walked past the insertion point, stop.
                break;
            }
            i = p;
            p = p.sister;
        }

        if( !match ){
            // We must insert our text in the list of children at
            // insertion point 'i', or in front if i is null.
            Node newChild = new LeafNode( start, length, this.dist+this.length, pos );
            newChild.sister = p;
            newChild.parent = this;

            if( i == null ){
                child = newChild;
            }
            else {
                i.sister = newChild;
            }
        }
    }

    protected void print( PrintStream stream, short text[], int indent )
    {
        for( int i=0; i<indent; i++ ){
            stream.print( ' ' );
        }
        if( start>=0 ){
            stream.println( "[" + SuffixTree.buildString( text, start, length ) + "]" );
        }
        Node n = child;

        while( n != null ){
            n.print( stream, text, indent+1 );
            n = n.sister;
        }
    }

    protected Node getLongestRepeat()
    {
        Node p = child;

        Node max = null;
        int l = 0;
        while( p != null ){
            if( p instanceof InternalNode ){
                Node r = p.getLongestRepeat();

                if( r != null ){
                    int l1 = r.dist+length;

                    if( max == null || l<l1 ){
                        l = l1;
                        max = r;
                        System.out.println( max + " is now longest repeat" );
                    }
                }
            }
            p = p.sister;
        }

        if( max == null ){
            max = this;
        }
        System.out.println( "Returning " + max + " as longest repeat" );
        return max;
    }

    public void test() throws VerificationException
    {
        Node p = child;

        if( p == null ){
            throw new VerificationException( "Internal node " + this + " has no children" );
        }
        if( p.sister == null ){
            throw new VerificationException( "Internal node " + this + " has only one child" );
        }
        // TODO: make sure no two branches start with the same character.
        while( p != null ){
            p.test();
            p = p.sister;
        }
    }
}
