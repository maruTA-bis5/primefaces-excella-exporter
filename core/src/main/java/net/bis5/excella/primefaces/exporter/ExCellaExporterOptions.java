package net.bis5.excella.primefaces.exporter;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import org.apache.poi.ss.usermodel.Workbook;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.primefaces.component.export.ExcelOptions;
import org.primefaces.util.Constants;

import net.bis5.excella.primefaces.exporter.listener.BeforeWriteResponseListener;

/**
 * ExCella exporter options
 */
@NullMarked
public class ExCellaExporterOptions extends ExcelOptions {

    private final Map<ValueType, Function<Workbook, Short>> overrideDataTypeForValueType = new EnumMap<>(ValueType.class);

    private @Nullable Path templatePath;

    public @Nullable Path getTemplatePath() {
        return templatePath;
    }

    public void setTemplatePath(Path templatePath) {
        this.templatePath = templatePath;
    }

    private @Nullable URL templateUrl;

    public @Nullable URL getTemplateUrl() {
        return templateUrl;
    }

    public void setTemplateUrl(URL templateUrl) {
        this.templateUrl = templateUrl;
    }

    private @Nullable String templateSheetName;

    public @Nullable String getTemplateSheetName() {
        return templateSheetName;
    }

    public void setTemplateSheetName(String templateSheetName) {
        this.templateSheetName = templateSheetName;
    }

    private @Nullable String dataColumnsTag;

    public @Nullable String getDataColumnsTag() {
        return dataColumnsTag;
    }

    public void setDataColumnsTag(String dataColumnsTag) {
        this.dataColumnsTag = dataColumnsTag;
    }

    private String columnValueSeparator = Constants.SPACE;

    /**
     * Returns the separator used between component values exported into the same column cell.
     * The default is a single space.
     *
     * @return the column value separator
     */
    public String getColumnValueSeparator() {
        return columnValueSeparator;
    }

    /**
     * Sets the separator used between component values exported into the same column cell.
     * The default is a single space. An empty string is allowed and used as-is.
     *
     * @param columnValueSeparator the separator to use
     * @throws NullPointerException if {@code columnValueSeparator} is {@code null}
     */
    public void setColumnValueSeparator(String columnValueSeparator) {
        this.columnValueSeparator = Objects.requireNonNull(columnValueSeparator, "columnValueSeparator must not be null");
    }

    private boolean exportPassword = true;

    /**
     * Returns whether values from PrimeFaces Password components are exported as-is.
     * The default is {@code true} for backward compatibility in 6.x and will change in a future version.
     * Set this property explicitly when relying on a particular behavior.
     *
     * @return whether Password values are exported
     */
    public boolean isExportPassword() {
        return exportPassword;
    }

    /**
     * Sets whether values from PrimeFaces Password components are exported as-is.
     * When {@code false}, Password values are replaced with {@link #getPasswordReplacement()}.
     * Set this property explicitly when relying on a particular behavior.
     *
     * @param exportPassword whether Password values are exported as-is
     */
    public void setExportPassword(boolean exportPassword) {
        this.exportPassword = exportPassword;
    }

    private String passwordReplacement = "";

    /**
     * Returns the replacement string used when Password values are not exported.
     * The default is an empty string.
     *
     * @return the Password replacement string
     */
    public String getPasswordReplacement() {
        return passwordReplacement;
    }

    /**
     * Sets the replacement string used when Password values are not exported.
     *
     * @param passwordReplacement the replacement string to use
     * @throws NullPointerException if {@code passwordReplacement} is {@code null}
     */
    public void setPasswordReplacement(String passwordReplacement) {
        this.passwordReplacement = Objects.requireNonNull(passwordReplacement, "passwordReplacement must not be null");
    }

    private @Nullable String headersTag;

    public @Nullable String getHeadersTag() {
        return headersTag;
    }

    public void setHeadersTag(String headersTag) {
        this.headersTag = headersTag;
    }

    private @Nullable String footersTag;

    public @Nullable String getFootersTag() {
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

    /**
     * Override the default data format for a value type.
     * @param valueType the value type to override
     * @param dataFormat the workbook data format index to use
     */
    public void setOverrideDataType(ValueType valueType, short dataFormat) {
        Objects.requireNonNull(valueType, "valueType must not be null");
        overrideDataTypeForValueType.put(valueType, w -> dataFormat);
    }

    /**
     * Override the default data format for a value type with a format string.
     * The format string is resolved against the workbook being exported.
     * @param valueType the value type to override
     * @param dataFormat the Excel data format string, or {@code null} to clear the override
     */
    public void setOverrideDataType(ValueType valueType, @Nullable String dataFormat) {
        Objects.requireNonNull(valueType, "valueType must not be null");
        if (dataFormat == null) {
            overrideDataTypeForValueType.remove(valueType);
        } else {
            overrideDataTypeForValueType.put(valueType,
                    w -> w.getCreationHelper().createDataFormat().getFormat(dataFormat));
        }
    }

    @Nullable Short getOverrideDataFormat(ValueType valueType, Workbook workbook) {
        Function<Workbook, Short> dataFormat = overrideDataTypeForValueType.get(valueType);
        return dataFormat == null ? null : dataFormat.apply(workbook);
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
