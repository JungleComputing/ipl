// File: $Id$

/**
 * The Satin marker interface for the Indexer class. By implementing this
 * interface the class can be handled by the Satin divide-and-conquer parallel
 * execution framework.
 */

import java.io.File;
import java.io.IOException;

interface IndexerInterface extends ibis.satin.Spawnable {
    public Index indexFileList(String fl[]) throws IOException;
}