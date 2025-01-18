package net.bis5.excella.primefaces.exporter;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.primefaces.component.export.ExcelOptions;

import net.bis5.excella.primefaces.exporter.listener.BeforeWriteResponseListener;

/**
 * ExCella exporter options
 */
public class ExCellaExporterOptions extends ExcelOptions {

    private Path templatePath;

    public Path getTemplatePath() {
        return templatePath;
    }

    public void setTemplatePath(Path templatePath) {
        this.templatePath = templatePath;
    }

    private URL templateUrl;

    public URL getTemplateUrl() {
        return templateUrl;
    }

    public void setTemplateUrl(URL templateUrl) {
        this.templateUrl = templateUrl;
    }

    private String templateSheetName;

    public String getTemplateSheetName() {
        return templateSheetName;
    }

    public void setTemplateSheetName(String templateSheetName) {
        this.templateSheetName = templateSheetName;
    }

    private String dataColumnsTag;

    public String getDataColumnsTag() {
        return dataColumnsTag;
    }

    public void setDataColumnsTag(String dataColumnsTag) {
        this.dataColumnsTag = dataColumnsTag;
    }

    private String headersTag;

    public String getHeadersTag() {
        return headersTag;
    }

    public void setHeadersTag(String headersTag) {
        this.headersTag = headersTag;
    }

    private String footersTag;

    public String getFootersTag() {
        return footersTag;
    }

    public void setFootersTag(String footersTag) {
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
