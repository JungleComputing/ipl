/* $Id$ */

class OthelloTag extends Tag {
        public static final int SIZE = 4;

        private int[] value = new int[SIZE];

        OthelloTag(byte[] pos) {
            for (int i = 0; i < pos.length; i++)
              value[i/16] = (value[i/16] << 2) | pos[i];
        }

        public int hashCode() {
            int result = 0;

            for (int i = 0; i < SIZE; i++)
                result ^= value[i] >> (i * 3 + i / 2);

            return result;
        }

        public boolean equals(Object o) {
            if (!(o instanceof OthelloTag)) return false;

            OthelloTag tag = (OthelloTag)o;

            for (int i = 0; i < SIZE; i++)
                if (value[i] != tag.value[i]) return false;

            return true;
        }

        public boolean equals(int[] array, int index) {
            for (int i = 0; i < SIZE; i++)
                if (value[i] != array[index + i]) return false;

            return true;
        }

        public void store(int[] array, int index) {
            System.arraycopy(value, 0, array, index, SIZE);
        }
}
