package net.bis5.excella.primefaces.exporter;

import java.io.IOException;

import net.bis5.excella.primefaces.exporter.listener.BeforeWriteResponseListener;

/**
 * ExCella exporter options
 */
public interface ExCellaExporterOptions {

    /**
     * If returns {@code true}, Exporter throws {@link EmptyDataException} when no data is exported.
     * @return throw exception when returns {@code true}, otherwise export file normally.
     */
    boolean isThrowExceptionWhenNoData();

    /**
     * Add {@link BeforeWriteResponseListener} to execute before writing response.
     * @param listener the listener to add
     */
    void addBeforeWriteResponseListener(BeforeWriteResponseListener listener);

    /**
     * Remove {@link BeforeWriteResponseListener}.
     * @param listener the listener to remove
     */
    void removeBeforeWriteResponseListener(BeforeWriteResponseListener listener);

    /**
     * (internal api) Execute all {@link BeforeWriteResponseListener} before writing response.
     * @param consumer the consumer to accept each listener
     * @throws IOException if an I/O error occurs
     */
    void forEachBeforeWriteResponseListener(ConsumerThrowsIOException<BeforeWriteResponseListener> consumer) throws IOException;

    /**
     * (internal api) Consumer with IOException.
     * @param <T> the type of the input to the operation
     */
    public interface ConsumerThrowsIOException<T> {
        void accept(T t) throws IOException;
    }
}
