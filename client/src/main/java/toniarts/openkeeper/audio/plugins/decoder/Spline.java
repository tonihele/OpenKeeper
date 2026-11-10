/* Copyright (C) 2003-2014 Michael Scheerer. All Rights Reserved. */

/*
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 */
package toniarts.openkeeper.audio.plugins.decoder;

/**
 * The
 * <code>Spline</code> class converts a number array of the length n either into
 * a double array of the length m or into a function within the parameter range
 * from 0 to 1. The algorithm based on the work of Robert Sedgewick and Donald
 * E. Knuth.
 *
 *
 * @author Michael Scheerer
 */
public final class Spline {

    private double[] x;
    private int[] yInt;
    private float[] yFloat;
    private double[] yDouble;
    private double[] u;
    private double[] d;
    private double[] w;
    private double[] p;
    private int N;
    private int i;
    private int j;
    private boolean done;
    private boolean initDone;
    private boolean error;

    /**
     * Resets the
     * <code>Spline</code> class and frees all system resources including the
     * setted number arrays, which are bounded to this object.
     */
    public void close() {
        x = null;
        yInt = null;
        yFloat = null;
        yDouble = null;
        u = null;
        d = null;
        w = null;
        p = null;
        initDone = false;
        error = false;
    }

    /**
     * Converts a number array of the length n into a double array of the length
     * m. The method returns null if something goes wrong. After return this
     * object is closed.
     *
     * @param a The source int array with a minimum size of 3
     * @param m The length of the target array with a minimum value of 3
     * @return The target array
     */
    public double[] getArray(int[] a, int m) {
        if (m < 3) {
            close();
            return null;
        }

        double[] array = new double[m];

        for (j = 0; j < m; j++) {
            double inc = j / (double) (m - 1);

            array[j] = getValue(a, inc);
            if (error) {
                close();
                return null;
            }
        }
        close();
        return array;
    }

    /**
     * Converts a number array of the length n into a double array of the length
     * m. The method returns null if something goes wrong. After return this
     * object is closed.
     *
     * @param a The source float array with a minimum size of 3
     * @param m The length of the target array with a minimum value of 3
     * @return The target array
     */
    public double[] getArray(float[] a, int m) {
        if (m < 3) {
            close();
            return null;
        }

        double[] array = new double[m];

        for (j = 0; j < m; j++) {
            double inc = j / (double) (m - 1);

            array[j] = getValue(a, inc);
            if (error) {
                close();
                return null;
            }
        }
        close();
        return array;
    }

    /**
     * Converts a number array of the length n into a double array of the length
     * m. The method returns null if something goes wrong. After return this
     * object is closed.
     *
     * @param a The source double array with a minimum size of 3
     * @param m The length of the target array with a minimum value of 3
     * @return The target array
     */
    public double[] getArray(double[] a, int m) {
        if (m < 3) {
            close();
            return null;
        }

        double[] array = new double[m];

        for (j = 0; j < m; j++) {
            double inc = j / (double) (m - 1);

            array[j] = getValue(a, inc);
            if (error) {
                close();
                return null;
            }
        }
        close();
        return array;
    }

    /**
     * Gets the function within the parameter range from 0 to 1. The method
     * returns 0 if something goes wrong.
     *
     * @param a The source int array with a minimum size of 3
     * @param v The parameter value within the range from 0 to 1
     * @return The function value
     */
    public double getValue(int[] a, double v) {
        setYInput(a);
        return process(v);
    }

    /**
     * Gets the function within the parameter range from 0 to 1. The method
     * returns 0 if something goes wrong.
     *
     * @param a The source float array with a minimum size of 3
     * @param v The parameter value within the range from 0 to 1
     * @return The function value
     */
    public double getValue(float[] a, double v) {
        setYInput(a);
        return process(v);
    }

    /**
     * Gets the function within the parameter range from 0 to 1. The method
     * returns 0 if something goes wrong.
     *
     * @param a The source double array with a minimum size of 3
     * @param v The parameter value within the range from 0 to 1
     * @return The function value
     */
    public double getValue(double[] a, double v) {
        setYInput(a);
        return process(v);
    }

    private void makeSpline() {
        if (x == null) {
            error = true;
            return;
        }
        for (i = 0; i < N; i++) {
            x[i] = i / (double) (N - 1);
        }
        for (i = 1; i < N - 1; i++) {
            d[i] = 2 * (x[i + 1] - x[i - 1]);
        }
        for (i = 0; i < N - 1; i++) {
            u[i] = x[i + 1] - x[i];
        }
        if (yInt != null) {
            for (i = 1; i < N - 1; i++) {
                w[i] = 6 * ((yInt[i + 1] - yInt[i]) / u[i] - (yInt[i] - yInt[i - 1]) / u[i - 1]);
            }
        } else if (yFloat != null) {
            for (i = 1; i < N - 1; i++) {
                w[i] = 6 * ((yFloat[i + 1] - yFloat[i]) / u[i] - (yFloat[i] - yFloat[i - 1]) / u[i - 1]);
            }
        } else {
            for (i = 1; i < N - 1; i++) {
                w[i] = 6 * ((yDouble[i + 1] - yDouble[i]) / u[i] - (yDouble[i] - yDouble[i - 1]) / u[i - 1]);
            }
        }
        p[0] = 0;
        p[N - 1] = 0;
        for (i = 1; i < N - 2; i++) {
            w[i + 1] -= w[i] * u[i] / d[i];
            d[i + 1] -= u[i] * u[i] / d[i];
        }
        for (i = N - 2; i > 0; i--) {
            p[i] = (w[i] - u[i] * p[i + 1]) / d[i];
        }
    }

    private static double f(double x) {
        return x * x * x - x;
    }

    private void init(int i) {
        if (initDone) {
            return;
        }
        if (i < 3) {
            error = true;
            return;
        }
        done = false;
        N = i;
        x = new double[N];
        u = new double[N];
        d = new double[N];
        w = new double[N];
        p = new double[N];
        initDone = true;
    }

    private void setYInput(int[] v) {
        if (yInt != v) {
            close();
        }
        yInt = v;
        init(v.length);
    }

    private void setYInput(float[] v) {
        if (yFloat != v) {
            close();
        }
        yFloat = v;
        init(v.length);
    }

    private void setYInput(double[] v) {
        if (yDouble != v) {
            close();
        }
        yDouble = v;
        init(v.length);
    }

    private double process(double v) {
        double t;

        if (x == null || yInt == null && yFloat == null && yDouble == null) {
            return 0;
        }

        if (!done) {
            makeSpline();
            done = true;
        }
        int i = 0;

        while (v > x[i + 1]) {
            i++;
        }

        t = (v - x[i]) / u[i];

        if (yInt != null) {
            return t * yInt[i + 1] + (1 - t) * yInt[i] + u[i] * u[i] * (f(t) * p[i + 1] + f(1 - t) * p[i]) / 6.0;
        } else if (yFloat != null) {
            return t * yFloat[i + 1] + (1 - t) * yFloat[i] + u[i] * u[i] * (f(t) * p[i + 1] + f(1 - t) * p[i]) / 6.0;
        }

        return t * yDouble[i + 1] + (1 - t) * yDouble[i] + u[i] * u[i] * (f(t) * p[i + 1] + f(1 - t) * p[i]) / 6.0;
    }
}
