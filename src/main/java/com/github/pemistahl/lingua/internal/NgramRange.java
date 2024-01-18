package com.github.pemistahl.lingua.internal;

import java.util.Iterator;
import java.util.Objects;

public class NgramRange implements Iterable<Ngram> {
    private final Ngram start;
    private final Ngram endInclusive;

    public NgramRange(Ngram start, Ngram endInclusive) {
        if (start.compareTo(endInclusive) < 0) {
            throw new IllegalArgumentException("'" + start + "' must be of higher order than '" + endInclusive + "'");
        }
        this.start = start;
        this.endInclusive = endInclusive;
    }

    public boolean contains(Ngram value) {
        return value.compareTo(endInclusive) >= 0 && value.compareTo(start) <= 0;
    }

    @Override
    public Iterator<Ngram> iterator() {
        return new NgramIterator(start);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        NgramRange ngrams = (NgramRange) o;
        return Objects.equals(start, ngrams.start) && Objects.equals(endInclusive, ngrams.endInclusive);
    }

    @Override
    public int hashCode() {
        return Objects.hash(start, endInclusive);
    }
}
