// File: $Id$

/**
 * The Satin marker interface for the Compressor class. By implementing this
 * interface the class can be handled by the Satin divide-and-conquer parallel
 * execution framework.
 */

interface CompressorInterface extends ibis.satin.Spawnable
{
    public Backref selectBestMoveJob( byte text[], int backrefs[], int pos, int bestpos, int max_shortening, int depth, int max_depth );
    public Backref shallowEvaluateBackrefJob( final byte text[], int backpos, int pos );
}
