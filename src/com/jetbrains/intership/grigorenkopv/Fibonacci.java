package com.jetbrains.intership.grigorenkopv;

import java.math.BigInteger;

public class Fibonacci {
    private Fibonacci() {
    }

    private final static BigInteger[][] BASE_MATRIX = {
            {BigInteger.ZERO, BigInteger.ONE},
            {BigInteger.ONE, BigInteger.ONE}
    };

    private static BigInteger[][] mul(BigInteger[][] a, BigInteger[][] b) {
        return new BigInteger[][]{
                {a[0][0].multiply(b[0][0]).add(a[0][1].multiply(b[1][0])), a[0][0].multiply(b[0][1]).add(a[0][1].multiply(b[1][1]))},
                {a[1][0].multiply(b[0][0]).add(a[1][1].multiply(b[1][0])), a[1][0].multiply(b[0][1]).add(a[1][1].multiply(b[1][1]))}
        };
    }

    private static BigInteger[][] pow(BigInteger[][] a, long k) {
        if (k == 1) {
            return a;
        } else if (k == 2) {
            return mul(a, a);
        } else {
            if (k % 2 == 1) {
                return mul(pow(a, k - 1), BASE_MATRIX);
            } else {
                return pow(pow(a, k / 2), 2);
            }
        }
    }

    public static BigInteger getNth(long n) {
        if (n < 0) {
            final BigInteger positive = getNth(-n);
            return n % 2 == 0 ? positive.negate() : positive;
        } else if (n < 2) {
            return BASE_MATRIX[0][(int) n];
        } else {
            return pow(BASE_MATRIX, n - 1)[1][1];
        }
    }
}
