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


import java.util.NoSuchElementException;
import java.util.Objects;

public class Ngram implements Comparable<Ngram> {
    private final String value;

    public Ngram(String value) {
        if (value.length() < 0 || value.length() > 5) {
            throw new IllegalArgumentException("Length of ngram '" + value + "' is not in range 0..5");
        }
        this.value = value;
    }

    public String getValue() {
        return this.value;
    }

    @Override
    public String toString() {
        return value;
    }

    @Override
    public int compareTo(Ngram other) {
        return Integer.compare(this.value.length(), other.value.length());
    }

    public NgramRange rangeOfLowerOrderNgrams() {
        return new NgramRange(this, new Ngram(String.valueOf(this.value.charAt(0))));
    }

    public Ngram decrement() {
        if (value.length() == 0) {
            throw new IllegalStateException("Zerogram is ngram type of lowest order and can not be decremented");
        }
        return new Ngram(value.substring(0, value.length() - 1));
    }

    @Deprecated
    public Ngram dec() {
        return decrement();
    }

    public static String getNgramNameByLength(int ngramLength) {
        switch (ngramLength) {
            case 1:
                return "unigram";
            case 2:
                return "bigram";
            case 3:
                return "trigram";
            case 4:
                return "quadrigram";
            case 5:
                return "fivegram";
            default:
                throw new IllegalArgumentException("Ngram length " + ngramLength + " is not in range 1..5");
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Ngram ngram = (Ngram) o;
        return Objects.equals(value, ngram.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}




