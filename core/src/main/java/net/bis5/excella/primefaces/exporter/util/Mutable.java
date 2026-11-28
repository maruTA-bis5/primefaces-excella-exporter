package net.bis5.excella.primefaces.exporter.util;

import org.jspecify.annotations.Nullable;

/**
 * [internal] A mutable wrapper of a value.
 *
 * @param <T> the type of the value
 */
public class Mutable<T extends @Nullable Object> {
    private T value;

    public Mutable(T value) {
        this.value = value;
    }

    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        this.value = value;
    }
}
