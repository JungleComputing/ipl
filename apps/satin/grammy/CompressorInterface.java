// File: $Id$

/**
 * The Satin marker interface for the Compressor class. By implementing this
 * interface the class can be handled by the Satin divide-and-conquer parallel
 * execution framework.
 */

interface CompressorInterface extends ibis.satin.Spawnable {
    public SuffixArray applyFoldingStep(SuffixArray a, Step s, int levels);
}