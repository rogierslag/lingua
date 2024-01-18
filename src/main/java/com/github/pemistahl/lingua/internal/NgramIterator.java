package com.github.pemistahl.lingua.internal;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;

public class NgramIterator implements Iterator<Ngram> {
    private Ngram current;

    public NgramIterator(Ngram start) {
        this.current = start;
    }

    @Override
    public boolean hasNext() {
        return current.toString().length() > 0;
    }

    @Override
    public Ngram next() {
        if (!hasNext()) {
            throw new NoSuchElementException("No such element");
        }
        Ngram result = current;
        current = current.decrement();
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        NgramIterator that = (NgramIterator) o;
        return Objects.equals(current, that.current);
    }

    @Override
    public int hashCode() {
        return Objects.hash(current);
    }
}
