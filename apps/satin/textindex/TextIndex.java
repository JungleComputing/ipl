// File: $Id$

/**
 * A parallel text indexer. Given a file and a list of directories, traverse
 * directories, and construct an index of words that occur in the files
 * in the traversed directories.
 * 
 * @author Kees van Reeuwijk
 * @version $Revision$
 */

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.TreeSet;
import java.util.regex.Pattern;

public final class TextIndex extends ibis.satin.SatinObject implements
        IndexerInterface, java.io.Serializable {
    private static final boolean traceTreeWalk = false;

    private static final Pattern nonWord = Pattern.compile("\\W+");

    /**
     * Given the name of a file to index, returns an Index entry for just
     * that file.
     * @param f The file to index.
     */
    public Index indexFile(String f) throws IOException {
        BufferedReader r;

        if (f.endsWith(".gz")) {
            r = new BufferedReader(new InputStreamReader(
                    new java.util.zip.GZIPInputStream(new FileInputStream(f))));
        } else {
            r = new BufferedReader(new FileReader(f));
        }
        TreeSet set = new TreeSet();

        while (true) {
            String s = r.readLine();
            if (s == null) {
                break;
            }
            String words[] = nonWord.split(s);
            for (int i = 0; i < words.length; i++) {
                String w = words[i];

                if (w.length() != 0) {
                    set.add(w.toLowerCase());
                }
            }
        }
        r.close();
        return new Index(f, set);
    }

    /**
     * Given a directory file, returns a list of files in that
     * directory. The list may contain null entries that should be ignored.
     */
    private static String[] buildFileList(File f) {
        String files[] = f.list();

        if (files == null) {
            return null;
        }
        for (int i = 0; i < files.length; i++) {
            // Make these files fully qualified.
            String fnm = files[i];
            if (fnm.equals("CVS") || fnm.charAt(0) == '.') {
                files[i] = null;
            } else {
                files[i] = new File(f, files[i]).toString();
            }
        }
        return files;
    }

    /**
     * Given a list of files, return an index of these files.
     * @param fl The list of files.
     */
    public Index indexFileList(String fl[]) throws IOException {
        if (fl.length == 0) {
            return null;
        }
        if (fl.length == 1) {
            String fnm = fl[0];

            if (fnm == null) {
                return null;
            }
            File f = new File(fnm);

            if (f.isDirectory()) {
                if (traceTreeWalk) {
                    System.err.println("Visiting directory " + f);
                }
                String fl1[] = buildFileList(f);
                Index res = indexFileList(fl1);
                sync();
                return res;
            }
            if (f.isFile()) {
                if (traceTreeWalk) {
                    System.err.println("Indexing plain file " + f);
                }
                return indexFile(fnm);
            }
            System.err.println("Skipping weird file " + fnm);
            return null;
        }
        // Divide and conquer...
        int mid = fl.length / 2;
        String l1[] = new String[mid];
        String l2[] = new String[fl.length - mid];

        System.arraycopy(fl, 0, l1, 0, mid);
        System.arraycopy(fl, mid, l2, 0, l2.length);
        Index ix1 = indexFileList(l1);
        Index ix2 = indexFileList(l2);
        sync();
        // Now merge them.
        if (ix1 == null) {
            // Merging is easy when one of the indices is null.
            return ix2;
        }
        ix1.merge(ix2);
        return ix1;
    }

    /**
     * Deletes the specified file or directory, and all files
     * under it.
     * @param f The file or directory to delete.
     */
    private static boolean delTree(File f) {
        boolean success = true;
        if (!f.exists()) {
            return true;
        }
        if (f.isFile()) {
            success &= f.delete();
        } else if (f.isDirectory()) {
            // First delete any files in the directory.
            String files[] = f.list();

            for (int ix = 0; ix < files.length; ix++) {
                String fnm = files[ix];

                if (fnm.charAt(0) != '.') {
                    // Skip '.', '..', and all hidden files per Unix convention.
                    File f1 = new File(f, fnm);

                    success &= delTree(f1);
                }
            }
            success &= f.delete();
        } else {
            System.err.println("Cannot delete weird file " + f);
            success = false;
        }
        return success;
    }

    /**
     * Allows execution of the class.
     * @param args The command-line arguments.
     */
    public static void main(String args[]) throws java.io.IOException {
        if (args.length < 2) {
            System.err
                    .println("Usage: <indexfile> <directory> ... <directory>");
            System.exit(1);
        }
        File ixfile = new File(args[0]);
        if (ixfile.exists() && !ixfile.isFile()) {
            System.err
                    .println("The index file exists, but is not a plain file: "
                            + ixfile);
            System.exit(1);
        }
        long startTime = System.currentTimeMillis();
        TextIndex ix = new TextIndex();
        String files[] = new String[0];

        for (int i = 1; i < args.length; i++) {
            File dir = new File(args[i]);
            if (!dir.exists()) {
                System.err.println("The directory to index does not exist: "
                        + dir);
                System.exit(1);
            }
            if (dir.isDirectory()) {
                String fl1[] = buildFileList(dir);
                if (fl1 == null) {
                    System.err.println("Cannot create a file list for " + dir);
                    System.exit(1);
                }
                String nwfiles[] = new String[files.length + fl1.length];
                System.arraycopy(files, 0, nwfiles, 0, files.length);
                System.arraycopy(fl1, 0, nwfiles, files.length, fl1.length);
                files = nwfiles;
            } else if (dir.isFile()) {
                String nwfiles[] = new String[files.length + 1];
                System.arraycopy(files, 0, nwfiles, 0, files.length);
                nwfiles[files.length] = args[i];
                files = nwfiles;
            } else {
                System.err.println("Cannot index weird file " + dir);
                System.exit(1);
            }
        }
        Index res = ix.indexFileList(files);
        ix.sync();
        FileWriter output = new FileWriter(ixfile);
        res.write(output);
        output.close();

        long endTime = System.currentTimeMillis();
        double time = ((double) (endTime - startTime)) / 1000.0;

        System.out.println("ExecutionTime: " + time);
    }
}