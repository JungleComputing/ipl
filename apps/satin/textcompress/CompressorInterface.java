// File: $Id$

/**
 * The Satin marker interface for the Compressor class. By implementing this
 * interface the class can be handled by the Satin divide-and-conquer parallel
 * execution framework.
 */

interface CompressorInterface extends ibis.satin.Spawnable
{
    public Backref selectBestMoveJob( byte text[], int backrefs[], int pos, int bestpos, int depth, int max_depth );
}
