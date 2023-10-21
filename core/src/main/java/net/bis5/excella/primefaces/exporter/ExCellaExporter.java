package net.bis5.excella.primefaces.exporter;

import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.el.ValueExpression;
import javax.faces.component.UIComponent;
import javax.faces.component.UIParameter;
import javax.faces.component.ValueHolder;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.bbreak.excella.core.listener.PostSheetParseListener;
import org.bbreak.excella.core.listener.PreSheetParseListener;
import org.bbreak.excella.reports.exporter.ExcelExporter;
import org.bbreak.excella.reports.listener.PostBookParseListener;
import org.bbreak.excella.reports.listener.PreBookParseListener;
import org.bbreak.excella.reports.listener.ReportProcessAdaptor;
import org.bbreak.excella.reports.listener.ReportProcessListener;
import org.bbreak.excella.reports.model.ConvertConfiguration;
import org.bbreak.excella.reports.model.ReportBook;
import org.bbreak.excella.reports.model.ReportSheet;
import org.bbreak.excella.reports.processor.ReportProcessor;
import org.bbreak.excella.reports.tag.RowRepeatParamParser;
import org.primefaces.PrimeFaces;
import org.primefaces.component.api.UIColumn;
import org.primefaces.component.celleditor.CellEditor;
import org.primefaces.component.columngroup.ColumnGroup;
import org.primefaces.component.export.ExportConfiguration;
import org.primefaces.component.link.Link;
import org.primefaces.util.ComponentUtils;
import org.primefaces.util.Constants;

import net.bis5.excella.primefaces.exporter.component.ExportableComponent;
import net.bis5.excella.primefaces.exporter.convert.ExporterConverter;

// internal
interface ExCellaExporter<T> {

    String COLUMN_GROUP_MERGED_AREAS_KEY = "HEADER_MERGED_AREAS_KEY";

    URL DEFAULT_TEMPLATE_URL = ExCellaExporter.class.getResource("/DefaultTemplate.xlsx");
    String DEFAULT_TEMPLATE_SHEET_NAME = "DATA";
    String DATA_CONTAINER_KEY = "DATA_CONTAINER_KEY";

    void setTemplatePath(Path templatePath);
    Path getTemplatePath();
    void setTemplateUrl(URL templateUrl);
    URL getTemplateUrl();

    private URL getTemplateFileUrl() throws MalformedURLException {
        if (getTemplatePath() != null) {
            return getTemplatePath().toUri().toURL();
        } else if (getTemplateUrl() != null) {
            return getTemplateUrl();
        }
        return DEFAULT_TEMPLATE_URL;
    }

    void setTemplateSheetName(String templateSheetName);
    void setTemplateType(TemplateType templateType);
    TemplateType getTemplateType();
    default void addListener(ReportProcessListener listener) {
        addPreBookParseListener(listener);
        addPreSheetParseListener(listener);
        addPostSheetParseListener(listener);
        addPostBookParseListener(listener);
    };
    void addPreBookParseListener(PreBookParseListener listener);
    void addPreSheetParseListener(PreSheetParseListener listener);
    void addPostSheetParseListener(PostSheetParseListener listener);
    void addPostBookParseListener(PostBookParseListener listener);

    void applyListeners(ReportProcessor reportProcessor);
    void setReportBook(ReportBook reportBook);
    ReportBook getDocument();

    default void preExport(FacesContext context, ExportConfiguration config) {
        setReportBook(new ReportBook());

        addPreBookParseListener(this::detectTemplateType);
    }

    default void detectTemplateType(Workbook workbook, ReportBook reportBook) {
        if (workbook instanceof HSSFWorkbook) {
            setTemplateType(TemplateType.XLS);
        } else {
            setTemplateType(TemplateType.XLSX);
        }
    }

    default void postExport(FacesContext context, ExportConfiguration config) throws IOException {
        // TODO configを考慮する(何をする?)
        Path outputFile = processExport();

        try {
            writeResponse(context, outputFile, config);
        } finally {
            reset();
            Files.delete(outputFile);
        }
    }

    private Path processExport() throws IOException {
        ReportBook reportBook = getDocument();
        ReportProcessor processor = new ReportProcessor();
        applyListeners(processor);
        reportBook.setTemplateFileURL(getTemplateFileUrl());
        reportBook.setConfigurations(new ConvertConfiguration(ExcelExporter.FORMAT_TYPE));
        Path outputFile = Files.createTempFile(null, null);
        reportBook.setOutputFileName(outputFile.toString());
        try {
            processor.process(reportBook);
        } catch (Exception e) {
            throw new IllegalStateException("Unexpected exception", e); // XXX そもそもthrows Exception宣言しているのがおかしい
        } finally {
            Files.delete(outputFile);
        }
        // ExCellaが拡張子を付けるので注意
        return Paths.get(outputFile.toString() + getTemplateType().getSuffix());
    }

    private void writeResponse(FacesContext context, Path outputFile, ExportConfiguration config) throws IOException {
        if (!PrimeFaces.current().isAjaxRequest()) {
            // overwrite response header by actual information
            ExternalContext externalContext = context.getExternalContext();
            externalContext.setResponseContentType(getTemplateType().getContentType());

            externalContext.setResponseHeader("Content-disposition",
                ComponentUtils.createContentDisposition("attachment", config.getOutputFileName() + getTemplateType().getSuffix()));
        }

        OutputStream out = getOutputStream();
        Files.copy(outputFile, out); // どうせOutputStreamに吐き出すんだから一時ファイル経由したくない気持ちもありつつ
        out.flush();
    }

    OutputStream getOutputStream();

    void reset();

    String getTemplateSheetName();

    default void doExport(FacesContext facesContext, T table, ExportConfiguration config, int index) throws IOException {
        // 一度の出力で複数のテーブルが対象となった場合、このメソッドは引数のtable, indexを変えて複数回呼ばれる。
        // このExporterでは1テーブルを1シートに出力する方針とする。
        String sheetName = getTemplateSheetName() != null ? getTemplateSheetName() : DEFAULT_TEMPLATE_SHEET_NAME;
        ReportSheet reportSheet = new ReportSheet(sheetName, sheetName + "_" + index);
        Map<String, List<Object>> dataContainer = new LinkedHashMap<>();
        reportSheet.addParam(null, DATA_CONTAINER_KEY, dataContainer);

        List<String> columnHeader = exportFacet(facesContext, table, ColumnType.HEADER, reportSheet);

        if (config.isPageOnly()) {
            exportPageOnly(facesContext, table, reportSheet, config);
        } else if (config.isSelectionOnly()) {
            exportSelectionOnly(facesContext, table, reportSheet, config);
        } else {
            exportAll(facesContext, table, reportSheet, config);
        }

        List<String> columnFooter = exportFacet(facesContext, table, ColumnType.FOOTER, reportSheet);

        reportSheet.removeParam(null, DATA_CONTAINER_KEY);

        setExportParameters(reportSheet, columnHeader, columnFooter, dataContainer);
    }

    default void exportPageOnly(FacesContext context, T table, Object document, ExportConfiguration config) {
        if (config.getOptions() instanceof ExCellaExporterOptions) {
            boolean throwExceptionWhenNoData = ((ExCellaExporterOptions)config.getOptions()).isThrowExceptionWhenNoData();
            if (throwExceptionWhenNoData && getPageRows(table) == 0) {
                throw new EmptyDataException();
            }
        }

        exportPageOnly(context, table, document);
    }

    int getPageRows(T table);

    void exportPageOnly(FacesContext context, T table, Object document);

    default void exportSelectionOnly(FacesContext context, T table, Object document, ExportConfiguration config) {
        if (config.getOptions() instanceof ExCellaExporterOptions) {
            boolean throwExceptionWhenNoData = ((ExCellaExporterOptions)config.getOptions()).isThrowExceptionWhenNoData();
            if (throwExceptionWhenNoData && isSelectionEmpty(table)) {
                throw new EmptyDataException();
            }
        }
        exportSelectionOnly(context, table, document);
    }

    boolean isSelectionEmpty(T table);

    void exportSelectionOnly(FacesContext context, T table, Object document);

    default void exportAll(FacesContext context, T table, Object document, ExportConfiguration config) {
        if (config.getOptions() instanceof ExCellaExporterOptions) {
            boolean throwExceptionWhenNoData = ((ExCellaExporterOptions)config.getOptions()).isThrowExceptionWhenNoData();
            if (throwExceptionWhenNoData && getTotalRows(table) == 0) {
                throw new EmptyDataException();
            }
        }
        exportAll(context, table, document);
    }

    int getTotalRows(T table);

    void exportAll(FacesContext context, T table, Object document);

    enum ColumnType {
        HEADER("header"),
        FOOTER("footer");

        private final String facet;

        ColumnType(String facet) {
            this.facet = facet;
        }

        String facet() {
            return facet;
        }

        @Override
        public String toString() {
            return facet();
        }
    }

    List<String> exportFacet(FacesContext context, T table, ColumnType columnType, ReportSheet reportSheet);

    void setExportParameters(ReportSheet reportSheet, List<String> columnHeader, List<String> columnFooter, Map<String, List<Object>> dataContainer);

    String exportValue(FacesContext context, UIComponent component);

    default String exportUIInstructionsValue(FacesContext context, UIComponent component, String value) {
        // evaluate el expr
        ValueExpression ve = context.getApplication().getExpressionFactory().createValueExpression(context.getELContext(), value, Object.class);
        Object objValue = ve.getValue(context.getELContext());
        return objValue != null ? removeTags(String.valueOf(objValue)) : null;
    }

    /**
     * Remove invisible tags from UIInsutructions' evaluated value
     * @param value evaluated value
     * @return tag removed value
     */
    default String removeTags(String value) {
        return value.replaceAll("<[bB][rR] ?/>", "");
    }

    default void addCellValue(FacesContext context, Map<String, List<Object>> dataContainer, int colIndex,
            UIColumn column) {
        String columnKey = "data" + colIndex;

        Object exportValue;
        if (column.getExportFunction() != null) {
            exportValue = exportColumnByFunction(context, column);
        } else if (column.getChildren().size() == 1) {
            exportValue = exportObjectValue(context, column.getChildren().get(0));
        } else {
            List<UIComponent> components = column.getChildren();
            StringBuilder builder = new StringBuilder();
            components.stream() //
                    .filter(UIComponent::isRendered) //
                    .map(c -> exportValue(context, c)) //
                    .map(v -> v == null ? "" : v) //
                    .forEach(builder::append);
            exportValue = builder.toString();
        }

        List<Object> values = dataContainer.computeIfAbsent(columnKey, ignore -> new ArrayList<>());
        values.add(exportValue);
    }

    default Entry<String, List<Object>> normalizeValues(Entry<String, List<Object>> entry) {
        // excella-coreのPoiUtil#setCellValueが特定の型以外はnoopなので予め型変換しておく
        List<Object> rawValues = entry.getValue();
        List<Object> normalizedValues = rawValues.stream()
            .map(this::normalizeValue)
            .collect(Collectors.toList());
        return new AbstractMap.SimpleEntry<>(entry.getKey(), normalizedValues);
    }

    default Object normalizeValue(Object rawValue) {
        if (rawValue instanceof LocalDate) {
            LocalDate localDate = (LocalDate)rawValue;
            return Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
        }
        if (rawValue instanceof LocalDateTime) {
            LocalDateTime localDateTime = (LocalDateTime)rawValue;
            return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
        }
        if (rawValue instanceof YearMonth) {
            YearMonth yearMonth = (YearMonth)rawValue;
            return Date.from(yearMonth.atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant());
        }
        return rawValue;
    }

    String exportColumnByFunction(FacesContext context, UIColumn column);

    default Object exportObjectValue(FacesContext context, UIComponent component) {
        if (!component.isRendered()) {
            return null;
        }
        if (component instanceof Link) {
            Link link = (Link)component;
            if (link.getValue() != null) {
                return link.getValue();
            }
            List<Object> values = link.getChildren().stream()
                .filter(c -> !UIParameter.class.isAssignableFrom(c.getClass()))
                .map(c -> exportObjectValue(context, c))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
            if (values.isEmpty()) {
                return null;
            }
            if (values.size() == 1) {
                return values.get(0);
            }
            return values.stream()
                .map(Object::toString)
                .collect(Collectors.joining());
        } else if (component instanceof ValueHolder) {
            ValueHolder valueHolder = (ValueHolder)component;
            return getComponentValue(context, valueHolder);
        } else if (component instanceof CellEditor) {
            return exportObjectValue(context, component.getFacet("output"));
        }
        return exportValue(context, component);
    }

    @SuppressWarnings("unchecked")
    private Object getComponentValue(FacesContext context, ValueHolder valueHolder) {
        Object value = valueHolder.getValue();
        if (valueHolder instanceof ExportableComponent) {
            value = ((ExportableComponent)valueHolder).getExportValue();
        }
        if (value == null) {
            return null;
        }

        UIComponent component = (UIComponent)valueHolder;
        Converter<Object> converter = valueHolder.getConverter();
        if (converter == null) {
            Class<?> valueClass = value.getClass();
            converter = context.getApplication().createConverter(valueClass);
        }
        if (converter instanceof ExporterConverter){
            return converter.getAsString(context, component, value);
        }

        if (value instanceof Number) {
            String strValue = exportValue(context, component);
            if (strValue != null && strValue.endsWith("%")) {
                // percentage number output as string
                return strValue;
            }
        }
        if (value instanceof Number || value instanceof Date || value instanceof Calendar || value instanceof LocalDate || value instanceof LocalDateTime || value instanceof LocalTime || value instanceof YearMonth) {
            return value;
        }
        return value.toString();
    }

    /**
     * Returns specified column is exportable.
     * @param context current faces context
     * @param column the column
     * @return if column is exportable, returns {@code true}, otherwise {@code false}
     */
    default boolean isExportable(FacesContext context, UIColumn column) {
        boolean visibleOnly = getExportConfiguration().isVisibleOnly();
        return column.isExportable() && column.isRendered() && (!visibleOnly || column.isVisible());
    }

    ExportConfiguration getExportConfiguration();

    default String getFacetColumnText(FacesContext context, UIColumn column, ExCellaExporter.ColumnType columnType) {
        UIComponent facet = column.getFacet(columnType.facet());
        String text;
        if (columnType == ExCellaExporter.ColumnType.HEADER) {
            text = column.getExportHeaderValue() != null ? column.getExportHeaderValue() : column.getHeaderText();
        } else if (columnType == ExCellaExporter.ColumnType.FOOTER) {
            text = column.getExportFooterValue() != null ? column.getExportFooterValue() : column.getFooterText();
        } else {
            text = null;
        }

        if (text != null) {
            return (text);
        } else if (ComponentUtils.shouldRenderFacet(facet)) {
            return exportValue(context, facet);
        } else {
            return "";
        }
    }

    default String getContentType() {
        return "application/octet-stream";
    }

    default String getFileExtension() {
        String url;
        try {
            url = getTemplateFileUrl().toString();
        } catch (MalformedURLException e) {
            throw new IllegalStateException(e);
        }
        return url.substring(url.lastIndexOf("."), url.length());
    }

    default List<String> exportColumnGroup(FacesContext context, ColumnGroup columnGroup, ExCellaExporter.ColumnType columnType, ReportSheet reportSheet) {
        List<String> facetColumns = new ArrayList<>();
        context.getAttributes().put(Constants.HELPER_RENDERER, "columnGroup");

        @SuppressWarnings("unchecked")
        Set<CellRangeAddress> mergedAreas = nonNull((Set<CellRangeAddress>) reportSheet.getParam(null, COLUMN_GROUP_MERGED_AREAS_KEY + columnType), new HashSet<>());
        reportSheet.addParam(null, COLUMN_GROUP_MERGED_AREAS_KEY + columnType, mergedAreas);

        for (UIComponent child : columnGroup.getChildren()) {
            if (!child.isRendered()) {
                continue;
            }
            if (child instanceof org.primefaces.component.row.Row) {
                if (columnGroup.getChildren().size() > 1) {
                    return exportColumnGroupMultiRow(context, columnGroup, columnType, reportSheet);
                } else {
                    return exportFacetColumns(context, child.getChildren(), columnType, reportSheet);
                }
            } else if (child instanceof UIColumn) {
                return exportFacetColumns(context, columnGroup.getChildren(), columnType, reportSheet);
            } else {
                // ignore
            }
        }

        context.getAttributes().remove(Constants.HELPER_RENDERER);
        return facetColumns;
    }

    default List<String> exportColumnGroupMultiRow(FacesContext context, ColumnGroup columnGroup, ExCellaExporter.ColumnType columnType,
            ReportSheet reportSheet) {

        return exportColumnGroupMultiRow(context, columnGroup, columnType, reportSheet, 0);
    }

    default List<String> exportColumnGroupMultiRow(FacesContext context, ColumnGroup columnGroup, ExCellaExporter.ColumnType columnType,
            ReportSheet reportSheet, int beginColIndex) {

        Map</*colindex*/Integer, List<String>> headerContents = new HashMap<>();
        int rowIndex = 0;
        Set<CellRangeAddress> mergedAreas = new HashSet<>();
        reportSheet.addParam(null, COLUMN_GROUP_MERGED_AREAS_KEY + columnType, mergedAreas);

        for (UIComponent child : columnGroup.getChildren()) {
            if (!child.isRendered() || !(child instanceof org.primefaces.component.row.Row)) {
                continue;
            }
            org.primefaces.component.row.Row row = (org.primefaces.component.row.Row)child;
            int colIndex = beginColIndex;
            boolean foundExportableColumn = false;
            for (UIComponent rowChild : row.getChildren()) {
                if (!rowChild.isRendered() || !(rowChild instanceof UIColumn)) {
                    continue;
                }
                UIColumn column = (UIColumn)rowChild;
                if (!isExportable(context, column)) {
                    continue;
                }
                foundExportableColumn = true;
                while (true) {
                    var currRowIndex = rowIndex;
                    var currColIndex = colIndex;
                    boolean overlapped = mergedAreas.stream()
                        .anyMatch(a -> a.isInRange(currRowIndex, currColIndex));
                    if (!overlapped) { break; }
                    colIndex++;
                }
                List<String> columnContents = headerContents.computeIfAbsent(colIndex, c -> new ArrayList<>());
                columnContents.add(getFacetColumnText(context, column, columnType));
                if (column.getRowspan() > 1) {
                    mergedAreas.add(new CellRangeAddress(rowIndex, rowIndex + column.getRowspan() - 1, colIndex, colIndex));

                    IntStream.range(rowIndex + 1, rowIndex + column.getRowspan())
                        .forEach(i -> columnContents.add(null));
                }
                if (column.getColspan() > 1) {
                    mergedAreas.add(new CellRangeAddress(rowIndex, rowIndex, colIndex, colIndex + column.getColspan() -1));

                    IntStream.range(colIndex + 1, colIndex + column.getColspan())
                        .mapToObj(i -> headerContents.computeIfAbsent(i, c -> new ArrayList<>()))
                        .forEach(c -> c.add(null));
                    colIndex += column.getColspan() - 1;
                }
                colIndex++;
            }
            if (foundExportableColumn) {
                rowIndex++;
            }
        }
        String tagPrefix = columnType == ExCellaExporter.ColumnType.HEADER ? "header" : "footer";
        headerContents.entrySet().forEach(e -> reportSheet.addParam(RowRepeatParamParser.DEFAULT_TAG, tagPrefix + e.getKey(), e.getValue().toArray()));

        return headerContents.keySet().stream()
            .map(i -> "$R[]{" + tagPrefix + i + "}")
            .collect(Collectors.toList());
    }

    private List<String> exportFacetColumns(FacesContext context, List<UIComponent> columns, ExCellaExporter.ColumnType columnType, ReportSheet reportSheet) {
        @SuppressWarnings("unchecked")
        Set<CellRangeAddress> mergedAreas = nonNull((Set<CellRangeAddress>) reportSheet.getParam(null, COLUMN_GROUP_MERGED_AREAS_KEY + columnType), new HashSet<>());
        reportSheet.addParam(null, COLUMN_GROUP_MERGED_AREAS_KEY + columnType, mergedAreas);

        List<String> facetColumns = new ArrayList<>();

        int colIndex = -1;
        for (UIComponent child : columns) {
            UIColumn column = (UIColumn)child;
            if (!isExportable(context, column)) {
                continue;
            }
            colIndex++;
            facetColumns.add(getFacetColumnText(context, column, columnType));
            if (column.getColspan() > 1) {
                int colsToMerge = column.getColspan() - 1;
                mergedAreas.add(new CellRangeAddress(0, 0, colIndex, colIndex + colsToMerge));
                colIndex += colsToMerge;
                IntStream.range(0, colsToMerge).forEach(i -> facetColumns.add(null));
            }
        }

        return facetColumns;
    }

    default <V> V nonNull(V obj, V defaultValue) {
        return obj != null ? obj : defaultValue;
    }

}
