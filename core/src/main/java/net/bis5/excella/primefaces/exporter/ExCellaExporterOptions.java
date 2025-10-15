package net.bis5.excella.primefaces.exporter;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.primefaces.component.export.ExcelOptions;

import org.jspecify.annotations.Nullable;

import net.bis5.excella.primefaces.exporter.listener.BeforeWriteResponseListener;

/**
 * ExCella exporter options
 */
public class ExCellaExporterOptions extends ExcelOptions {

    private @Nullable Path templatePath;

    public @Nullable Path getTemplatePath() {
        return templatePath;
    }

    public void setTemplatePath(@Nullable Path templatePath) {
        this.templatePath = templatePath;
    }

    private @Nullable URL templateUrl;

    public @Nullable URL getTemplateUrl() {
        return templateUrl;
    }

    public void setTemplateUrl(@Nullable URL templateUrl) {
        this.templateUrl = templateUrl;
    }

    private @Nullable String templateSheetName;

    public @Nullable String getTemplateSheetName() {
        return templateSheetName;
    }

    public void setTemplateSheetName(@Nullable String templateSheetName) {
        this.templateSheetName = templateSheetName;
    }

    private @Nullable String dataColumnsTag;

    public @Nullable String getDataColumnsTag() {
        return dataColumnsTag;
    }

    public void setDataColumnsTag(@Nullable String dataColumnsTag) {
        this.dataColumnsTag = dataColumnsTag;
    }

    private @Nullable String headersTag;

    public @Nullable String getHeadersTag() {
        return headersTag;
    }

    public void setHeadersTag(@Nullable String headersTag) {
        this.headersTag = headersTag;
    }

    private @Nullable String footersTag;

    public @Nullable String getFootersTag() {
        return footersTag;
    }

    public void setFootersTag(@Nullable String footersTag) {
        this.footersTag = footersTag;
    }

    private boolean throwExceptionWhenNoData;

    public void setThrowExceptionWhenNoData(boolean value) {
        throwExceptionWhenNoData = value;
    }

    /**
     * If returns {@code true}, Exporter throws {@link EmptyDataException} when no data is exported.
     * @return throw exception when returns {@code true}, otherwise export file normally.
     */
    public boolean isThrowExceptionWhenNoData() {
        return throwExceptionWhenNoData;
    }

    private final List<BeforeWriteResponseListener> beforeWriteResponseListeners = new ArrayList<>();

    /**
     * Add {@link BeforeWriteResponseListener} to execute before writing response.
     * @param listener the listener to add
     */
    public void addBeforeWriteResponseListener(BeforeWriteResponseListener listener) {
        beforeWriteResponseListeners.add(listener);
    }

    /**
     * Remove {@link BeforeWriteResponseListener}.
     * @param listener the listener to remove
     */
    public void removeBeforeWriteResponseListener(BeforeWriteResponseListener listener) {
        beforeWriteResponseListeners.remove(listener);
    }

    /**
     * (internal api) Execute all {@link BeforeWriteResponseListener} before writing response.
     * @param consumer the consumer to accept each listener
     * @throws IOException if an I/O error occurs
     */
    public void forEachBeforeWriteResponseListener(ConsumerThrowsIOException<BeforeWriteResponseListener> consumer) throws IOException {
        for (BeforeWriteResponseListener listener : beforeWriteResponseListeners) {
            consumer.accept(listener);
        }
    }

    /**
     * (internal api) Consumer with IOException.
     * @param <T> the type of the input to the operation
     */
    public interface ConsumerThrowsIOException<T> {
        void accept(T t) throws IOException;
    }
}
