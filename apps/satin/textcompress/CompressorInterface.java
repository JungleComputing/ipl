// File: $Id$

/**
 * The Satin marker interface for the Compressor class. By implementing this
 * interface the class can be handled by the Satin divide-and-conquer parallel
 * execution framework.
 */
import java.io.File;
import java.io.IOException;

interface CompressorInterface extends ibis.satin.Spawnable
{
    public int evaluateMove( byte text[], int pos, CompressContext ctx, int move, int depth );
}
