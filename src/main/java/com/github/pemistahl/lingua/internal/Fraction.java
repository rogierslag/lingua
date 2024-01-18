/*
 * Copyright © 2018-today Peter M. Stahl pemistahl@gmail.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either expressed or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.pemistahl.lingua.internal;

import java.util.Objects;

public class Fraction extends Number implements Comparable<Fraction> {

    private int numerator;
    private int denominator;

    public Fraction(int numerator, int denominator) {
        int[] reduced = reduceToLowestTerms(numerator, denominator);
        this.numerator = reduced[0];
        this.denominator = reduced[1];
    }

    @Override
    public int compareTo(Fraction other) {
        long n0d = (long) numerator * other.denominator;
        long d0n = (long) denominator * other.numerator;
        return Long.compare(n0d, d0n);
    }

    public Double toDouble() {
        return ((double) numerator) / ((double) denominator);
    }
    @Override
    public String toString() {
        return numerator + "/" + denominator;
    }

    @Override
    public int intValue() {
        return toDouble().intValue();
    }

    @Override
    public long longValue() {
        return toDouble().longValue();
    }

    @Override
    public float floatValue() {
        return toDouble().floatValue();
    }

    @Override
    public double doubleValue() {
        return (double) numerator / denominator;
    }

    private int[] reduceToLowestTerms(int numerator, int denominator) {
        if (denominator == 0) {
            throw new ArithmeticException("Zero denominator in fraction " + numerator + "/" + denominator);
        }
        if (numerator == 0) {
            return new int[]{0, 1};
        }

        int gcd = greatestCommonDenominator(numerator, denominator);
        return new int[]{numerator / gcd, denominator / gcd};
    }

    private int greatestCommonDenominator(int a, int b) {
        a = Math.abs(a);
        b = Math.abs(b);
        return greatestCommonDivisor(a, b);
    }

    private int greatestCommonDivisor(int a, int b) {
        while (b != 0) {
            int temp = b;
            b = a % b;
            a = temp;
        }
        return a;
    }

    private int numberOfTrailingZeros(int i) {
        if (i == 0) return 32;
        int num = 0;
        while ((i & 1) == 0) {
            num++;
            i >>>= 1;
        }
        return num;
    }

    private int abs(int x) {
        return (x < 0) ? -x : x;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Fraction fraction = (Fraction) o;
        return numerator == fraction.numerator && denominator == fraction.denominator;
    }

    @Override
    public int hashCode() {
        return Objects.hash(numerator, denominator);
    }

}

