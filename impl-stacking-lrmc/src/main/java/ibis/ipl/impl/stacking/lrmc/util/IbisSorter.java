/*
 * Copyright 2010 Vrije Universiteit
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ibis.ipl.impl.stacking.lrmc.util;

import ibis.ipl.IbisIdentifier;
import ibis.ipl.Location;

import java.util.Arrays;
import java.util.Comparator;

public class IbisSorter implements Comparator<IbisIdentifier> {

    // General sorter to use when no cluster order is preferred.
    private static final IbisSorter sorter = new IbisSorter("unknown", null);

    private final String preferredCluster;
    private final String preferredName;

    private IbisSorter(String preferredCluster, String preferredName) {
        this.preferredCluster = preferredCluster;
        this.preferredName = preferredName;
    }

    public static void sort(IbisIdentifier[] ids) {
        sort(ids, 0, ids.length);
    }

    public static void sort(IbisIdentifier local, IbisIdentifier[] ids) {
        sort(local, ids, 0, ids.length);
    }

    public static void sort(IbisIdentifier[] ids, int from, int to) {
        Arrays.sort(ids, from, to, sorter);
    }

    public static void sort(IbisIdentifier local, IbisIdentifier[] ids,
            int from, int to) {
        /*
         * IbisSorter tmp = sorter;
         * 
         * if (!local.equals(sorter.preferredName) ||
         * !local.getLocation().cluster().equals(sorter.preferredCluster)) { tmp =
         * new IbisSorter(local.getLocation().cluster(), local); }
         * 
         * Arrays.sort(ids, from, to, tmp);
         */
        IbisIdentifier[] tmp = new IbisIdentifier[(to - from) + 1];
        tmp[0] = local;

        System.arraycopy(ids, from, tmp, 1, to - from);

        Arrays.sort(tmp, sorter);

        int index = 0;

        for (int i = 0; i < tmp.length; i++) {
            if (tmp[i].equals(local)) {
                index = i;
                break;
            }
        }

        System.arraycopy(tmp, index + 1, ids, from, tmp.length - (index + 1));
        System.arraycopy(tmp, 0, ids, from + tmp.length - index - 1, index);
    }

    // Returns the index of the first character that is different in the two
    // Strings. Thus, the higher the number returned, the longer the prefix that
    // the two Strings share.
    private static int firstDifference(String s1, String s2) {

        // first, make sure that we s1 is the shortest string.
        if (s1.length() > s2.length()) {
            String tmp = s1;
            s1 = s2;
            s2 = tmp;
        }

        for (int i = 0; i < s1.length(); i++) {
            if (s1.charAt(i) != s2.charAt(i)) {
                return i;
            }
        }

        return s1.length();
    }

    public int compare(IbisIdentifier id1, IbisIdentifier id2) {

        Location cluster1 = id1.location().getParent();
        Location cluster2 = id2.location().getParent();

        if (cluster1.equals(cluster2)) {
            // The clusters are identical, so the order depends completely
            // on the names.
            // 
            // For SMP awareness, we assume that the identifiers of two ibises
            // on an SMP machine are 'closer' that the identifiers of two ibises
            // on different machines. This way, the identifiers will
            // automatically be sorted 'pair-wise' (or quad/oct/etc, depending
            // on the number of ibises that share the SMP machines).
            // 
            // One aditional problem is that we want the ibises that share the
            // machine with the sender to be first (which isn't that simple!).

            if (preferredName == null) {
                return id1.location().toString().compareTo(
                        id2.location().toString());
            } else {
                // Figure out if one of the two strings has a longer prefix
                // in common with 'preferredName'. Note that this will result
                // in the lenght of the string only if the IbisIdentifier
                // actually contains the 'preferredName'. Therefore, this
                // IbisIdentifier will end up at the first position of the
                // array, which is exactly what we want.
                int d1 = firstDifference(preferredName, id1.location()
                        .toString());
                int d2 = firstDifference(preferredName, id2.location()
                        .toString());

                // If both have the same distance, we sort them alphabetically.
                // Otherwise, we prefer the one that is closest to
                // 'preferredName', since these may actually be located on the
                // same machine.
                if (d1 == d2) {
                    return id1.location().toString().compareTo(
                            id2.location().toString());
                } else if (d1 <= d2) {
                    return 1;
                } else {
                    return -1;
                }
            }
        }

        // The clusters are different. If one of the two is equal to the
        // preferredCluster, we want that one to win. Otherwise, we just return
        // the 'natural order'.
        if (cluster1.equals(preferredCluster)) {
            return -1;
        }

        if (cluster2.equals(preferredCluster)) {
            return 1;
        }

        return cluster1.compareTo(cluster2);
    }
}
