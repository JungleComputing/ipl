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

public class LevenshteinDistance {

    private static int minimum(int a, int b, int c) {
        int min = a;

        if (b < min) {
            min = b;
        }

        if (c < min) {
            min = c;
        }
        return min;
    }

    public static int distance(String s, String t) {
        int cost; // cost

        // Step 1
        int n = s.length();
        int m = t.length();

        if (n == 0) {
            return m;
        }

        if (m == 0) {
            return n;
        }

        int[][] d = new int[n + 1][m + 1];

        // Step 2
        for (int i = 0; i <= n; i++) {
            d[i][0] = i;
        }

        for (int j = 0; j <= m; j++) {
            d[0][j] = j;
        }

        // Step 3
        for (int i = 1; i <= n; i++) {

            char s_i = s.charAt(i - 1);

            // Step 4
            for (int j = 1; j <= m; j++) {
                char t_j = t.charAt(j - 1);

                // Step 5

                if (s_i == t_j) {
                    cost = 0;
                } else {
                    cost = 1;
                }

                // Step 6
                d[i][j] = minimum(d[i - 1][j] + 1, d[i][j - 1] + 1,
                        d[i - 1][j - 1] + cost);
            }
        }

        // Step 7
        return d[n][m];
    }
}