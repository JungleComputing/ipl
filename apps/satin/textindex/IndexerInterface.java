// File: $Id$

/**
 * The Satin marker interface for the Indexer class. By implementing this
 * interface the class can be handled by the Satin divide-and-conquer parallel
 * execution framework.
 */
import java.io.File;
import java.io.IOException;

interface IndexerInterface extends ibis.satin.Spawnable
{
    public void indexFile( File f, File ixF ) throws IOException;
    public void indexDirectory( File dir, File IxDir ) throws IOException;
}
