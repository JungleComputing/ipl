/*************************************************************************
 *  Compilation:  javac FFT.java
 *  Execution:    java FFT N
 *  Dependencies: Complex.java
 *
 *  Compute the FFT and inverse FFT of a length N complex sequence.
 *  Bare bones implementation that runs in O(N log N) time.
 *
 *  Limitations
 *  -----------
 *   * assumes N is a power of 2
 *   * not the most memory efficient algorithm
 *  
 *************************************************************************/

public final class FFT extends ibis.satin.SatinObject implements FFTInterface,
        java.io.Serializable {

    // compute the FFT of x[], assuming its length is a power of 2
    public Complex[] fft(Complex[] x) {
        int N = x.length;
        Complex[] y = new Complex[N];

        // base case
        if (N == 1) {
            y[0] = x[0];
            return y;
        }

        // radix 2 Cooley-Tukey FFT
        if (N % 2 != 0)
            throw new RuntimeException("N is not a power of 2");
        Complex[] even = new Complex[N / 2];
        Complex[] odd = new Complex[N / 2];
        for (int k = 0; k < N / 2; k++)
            even[k] = x[2 * k];
        for (int k = 0; k < N / 2; k++)
            odd[k] = x[2 * k + 1];

        Complex[] q = fft(even);
        Complex[] r = fft(odd);
        sync();

        for (int k = 0; k < N / 2; k++) {
            double kth = -2 * k * Math.PI / N;
            Complex wk = new Complex(Math.cos(kth), Math.sin(kth));
            y[k] = q[k].plus(wk.times(r[k]));
            y[k + N / 2] = q[k].minus(wk.times(r[k]));
        }
        return y;
    }

    // compute the inverse FFT of x[], assuming its length is a power of 2
    public Complex[] ifft(Complex[] x) {
        int N = x.length;

        // take conjugate
        for (int i = 0; i < N; i++)
            x[i] = x[i].conjugate();

        // compute forward FFT
        Complex[] y = fft(x);

        // take conjugate again
        for (int i = 0; i < N; i++)
            y[i] = y[i].conjugate();

        // divide by N
        for (int i = 0; i < N; i++)
            y[i] = y[i].times(1.0 / N);

        return y;

    }

    // compute the convolution of x and y
    public Complex[] convolve(Complex[] x, Complex[] y) {
        if (x.length != y.length)
            throw new RuntimeException("Dimensions don't agree");
        int N = x.length;

        // compute FFT of each sequence
        Complex[] a = fft(x);
        Complex[] b = fft(y);

        // point-wise multiply
        Complex[] c = new Complex[N];
        for (int i = 0; i < N; i++)
            c[i] = a[i].times(b[i]);

        // compute inverse FFT
        return ifft(c);
    }

    // test client
    public static void main(String[] args) {
        int N = Integer.parseInt(args[0]);

        System.err.println("FFT start");

        Complex[] x = new Complex[N];

        // original data
        for (int i = 0; i < N; i++) {
            x[i] = new Complex(i, 0);
        }

        FFT fft = new FFT();

        /*
         for (int i = 0; i < N; i++)
         System.out.println(x[i]);
         System.out.println();
         */
        // FFT of original data
        Complex[] y = fft.fft(x);
        fft.sync();

        System.err.println("FFT done");

        /*
         for (int i = 0; i < N; i++)
         System.out.println(y[i]);
         System.out.println();
         */
        // take inverse FFT
        //        Complex[] z = ifft(y);
        /*
         for (int i = 0; i < N; i++)
         System.out.println(z[i]);
         System.out.println();
         */
        // convolution of x with itself
        //        Complex[] c = convolve(x, x);
        /*
         for (int i = 0; i < N; i++)
         System.out.println(c[i]);
         */
    }

}