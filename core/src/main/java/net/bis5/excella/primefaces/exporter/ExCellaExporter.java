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

import javax.el.MethodExpression;
import javax.el.ValueExpression;
import javax.faces.component.UIComponent;
import javax.faces.component.UIParameter;
import javax.faces.component.ValueHolder;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.bbreak.excella.reports.exporter.ExcelExporter;
import org.bbreak.excella.reports.listener.ReportProcessAdaptor;
import org.bbreak.excella.reports.listener.ReportProcessListener;
import org.bbreak.excella.reports.model.ConvertConfiguration;
import org.bbreak.excella.reports.model.ReportBook;
import org.bbreak.excella.reports.model.ReportSheet;
import org.bbreak.excella.reports.processor.ReportProcessor;
import org.bbreak.excella.reports.tag.RowRepeatParamParser;
import org.primefaces.component.api.UIColumn;
import org.primefaces.component.api.UITable;
import org.primefaces.component.celleditor.CellEditor;
import org.primefaces.component.columngroup.ColumnGroup;
import org.primefaces.component.export.ColumnValue;
import org.primefaces.component.export.ExportConfiguration;
import org.primefaces.component.export.ExporterUtils;
import org.primefaces.component.link.Link;
import org.primefaces.util.Constants;
import org.primefaces.util.FacetUtils;
import org.primefaces.util.LangUtils;

import net.bis5.excella.primefaces.exporter.component.ExportableComponent;
import net.bis5.excella.primefaces.exporter.convert.ExporterConverter;

// internal
interface ExCellaExporter<T extends UITable<?>> {

    String COLUMN_GROUP_MERGED_AREAS_KEY = "HEADER_MERGED_AREAS_KEY";

    URL DEFAULT_TEMPLATE_URL = ExCellaExporter.class.getResource("/DefaultTemplate.xlsx");
    String DEFAULT_TEMPLATE_SHEET_NAME = "DATA";
    String DATA_CONTAINER_KEY = "DATA_CONTAINER_KEY";

    /**
     * @deprecated use each exporter's builder(). This method will be removed in 5.0.0.
     */
    @Deprecated(forRemoval = true)
    void setTemplatePath(Path templatePath);

    /**
     * @deprecated Use {@link ExCellaExporterOptions}. This method will be removed in 5.0.0.
     * @implNote Make this getter private in 5.0.0.
     */
    @Deprecated(forRemoval = true)
    Path getTemplatePath();
    /**
     * @deprecated use each exporter's builder(). This method will be removed in 5.0.0.
     */
    @Deprecated(forRemoval = true)
    void setTemplateUrl(URL templateUrl);
    /**
     * @deprecated Use {@link ExCellaExporterOptions}. This method will be removed in 5.0.0.
     * @implNote Make this getter private in 5.0.0.
     */
    @Deprecated(forRemoval = true)
    URL getTemplateUrl();

    private URL getTemplateFileUrl() throws MalformedURLException {
        if (getTemplatePath() != null) {
            return getTemplatePath().toUri().toURL();
        } else if (getTemplateUrl() != null) {
            return getTemplateUrl();
        }
        return DEFAULT_TEMPLATE_URL;
    }

    /**
     * @deprecated use each exporter's builder(). This method will be removed in 5.0.0.
     */
    @Deprecated(forRemoval = true)
    void setTemplateSheetName(String templateSheetName);
    void setTemplateType(TemplateType templateType);
    TemplateType getTemplateType();
    void addListener(ReportProcessListener listener);
    List<ReportProcessListener> getListeners();
    ReportBook getDocument();
    void setCurrentSheet(ReportSheet reportSheet);

    default void preExport(FacesContext context) throws IOException {
        addListener(new ReportProcessAdaptor() {
            @Override
            public void preBookParse(Workbook workbook, ReportBook reportBook) {
                if (workbook instanceof HSSFWorkbook) {
                    setTemplateType(TemplateType.XLS);
                } else {
                    setTemplateType(TemplateType.XLSX);
                }
            }
        });
    }

    default void postExport(FacesContext context) throws IOException {
        // TODO configを考慮する(何をする?)
        Path outputFile = processExport();

        try {
            writeResponse(outputFile);
        } finally {
            reset();
            Files.delete(outputFile);
        }
    }

    private Path processExport() throws IOException {
        ReportBook reportBook = getDocument();
        ReportProcessor processor = new ReportProcessor();
        getListeners().forEach(processor::addReportProcessListener);
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

    private void writeResponse(Path outputFile) throws IOException {
        OutputStream out = os();
        Files.copy(outputFile, out); // どうせOutputStreamに吐き出すんだから一時ファイル経由したくない気持ちもありつつ
        out.flush();
    }

    OutputStream os();

    void reset();

    /**
     * @deprecated Use {@link ExCellaExporterOptions}. This method will be removed in 5.0.0.
     * @implNote Make this getter private in 5.0.0.
     */
    @Deprecated(forRemoval = true)
    String getTemplateSheetName();

    default void exportTable(FacesContext facesContext, T table, int index) throws IOException {
        // 一度の出力で複数のテーブルが対象となった場合、このメソッドは引数のtable, indexを変えて複数回呼ばれる。
        // このExporterでは1テーブルを1シートに出力する方針とする。
        String sheetName = getTemplateSheetName() != null ? getTemplateSheetName() : DEFAULT_TEMPLATE_SHEET_NAME;
        ReportSheet reportSheet = new ReportSheet(sheetName, sheetName + "_" + index);
        setCurrentSheet(reportSheet);
        Map<String, List<Object>> dataContainer = new LinkedHashMap<>();
        reportSheet.addParam(null, DATA_CONTAINER_KEY, dataContainer);

        List<Object> columnHeader = new ArrayList<>();
        exportFacet(facesContext, table, ColumnType.HEADER, reportSheet, columnHeader);

        if (getExportConfiguration().isPageOnly()) {
            exportPageOnly(facesContext, table, getExportConfiguration());
        } else if (getExportConfiguration().isSelectionOnly()) {
            exportSelectionOnly(facesContext, table, getExportConfiguration());
        } else {
            exportAll(facesContext, table, getExportConfiguration());
        }

        List<Object> columnFooter = new ArrayList<>();
        exportFacet(facesContext, table, ColumnType.FOOTER, reportSheet, columnFooter);

        reportSheet.removeParam(null, DATA_CONTAINER_KEY);

        setExportParameters(reportSheet, columnHeader, columnFooter, dataContainer);
    }

    default void exportPageOnly(FacesContext context, T table, ExportConfiguration config) {
        if (config.getOptions() instanceof ExCellaExporterOptions) {
            boolean throwExceptionWhenNoData = ((ExCellaExporterOptions)config.getOptions()).isThrowExceptionWhenNoData();
            if (throwExceptionWhenNoData && getPageRows(table) == 0) {
                throw new EmptyDataException();
            }
        }

        exportPageOnly(context, table);
    }

    int getPageRows(T table);

    void exportPageOnly(FacesContext context, T table);

    default void exportSelectionOnly(FacesContext context, T table, ExportConfiguration config) {
        if (config.getOptions() instanceof ExCellaExporterOptions) {
            boolean throwExceptionWhenNoData = ((ExCellaExporterOptions)config.getOptions()).isThrowExceptionWhenNoData();
            if (throwExceptionWhenNoData && isSelectionEmpty(table)) {
                throw new EmptyDataException();
            }
        }
        exportSelectionOnly(context, table);
    }

    boolean isSelectionEmpty(T table);

    void exportSelectionOnly(FacesContext context, T table);

    default void exportAll(FacesContext context, T table, ExportConfiguration config) {
        if (config.getOptions() instanceof ExCellaExporterOptions) {
            boolean throwExceptionWhenNoData = ((ExCellaExporterOptions)config.getOptions()).isThrowExceptionWhenNoData();
            if (throwExceptionWhenNoData && getTotalRows(table) == 0) {
                throw new EmptyDataException();
            }
        }
        exportAll(context, table);
    }

    int getTotalRows(T table);

    void exportAll(FacesContext context, T table);

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

    void exportFacet(FacesContext context, T table, ColumnType columnType, ReportSheet reportSheet, List<Object> cellValues);

    void setExportParameters(ReportSheet reportSheet, List<Object> columnHeader, List<Object> columnFooter, Map<String, List<Object>> dataContainer);

    default String exportValue(FacesContext context, UIComponent component) {
        String value = ExporterUtils.getComponentValue(context, component);
        if (component.getClass().getSimpleName().equals("UIInstructions")) {
            return exportUIInstructionsValue(context, component, value);
        }
        return value;
    }


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

    default void addCellValue(FacesContext context, Map<String, List<Object>> dataContainer, T table, int colIndex,
            UIColumn column) {
        String columnKey = "data" + colIndex;

        Object exportValue;
        if (column.getChildren().size() == 1) {
            exportValue = exportObjectValue(context, column.getChildren().get(0));
        } else {
            exportValue = getColumnValue(context, table, column, true).getValue();
        }

        List<Object> values = dataContainer.computeIfAbsent(columnKey, ignore -> new ArrayList<>());
        values.add(exportValue);
    }

    // clone of ExporterUtils#getColumnValue
    default ColumnValue getColumnValue(FacesContext context, T table, UIColumn column, boolean joinComponents) {
        if (column.getExportValue() != null) {
            return ColumnValue.of(column.getExportValue());
        }
        else if (column.getExportFunction() != null) {
            MethodExpression exportFunction = column.getExportFunction();
            return ColumnValue.of(exportFunction.invoke(context.getELContext(), new Object[]{column}));
        }
        else if (LangUtils.isNotBlank(column.getField())) {
            String value = table.getConvertedFieldValue(context, column);
            return ColumnValue.of(value);
        }
        else {
            return ColumnValue.of(column.getChildren()
                    .stream()
                    .filter(UIComponent::isRendered)
                    .map(c -> exportValue(context, c)) // modified: use exportValue instead of ExporterUtils.getColumnValue
                    .filter(LangUtils::isNotBlank)
                    .limit(!joinComponents ? 1 : column.getChildren().size())
                    .collect(Collectors.joining(Constants.SPACE)));
        }
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

    default String exportColumnByFunction(FacesContext context, UIColumn column) {
        MethodExpression exportFunction = column.getExportFunction();
        return (String) exportFunction.invoke(context.getELContext(), new Object[]{column});
    }

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

    default ExCellaExporterOptions getExporterOptions() {
        return Objects.requireNonNullElse((ExCellaExporterOptions)getExportConfiguration().getOptions(), new ExCellaExporterOptions());
    }

    default Object getFacetColumnValue(FacesContext context, UIColumn column, ExCellaExporter.ColumnType columnType) {
        UIComponent facet = column.getFacet(columnType.facet());
        Object value;
        if (columnType == ExCellaExporter.ColumnType.HEADER) {
            value = column.getExportHeaderValue() != null ? column.getExportHeaderValue() : column.getHeaderText();
        } else if (columnType == ExCellaExporter.ColumnType.FOOTER) {
            value = column.getExportFooterValue() != null ? column.getExportFooterValue() : column.getFooterText();
        } else {
            value = null;
        }

        if (value != null) {
            return (value);
        } else if (FacetUtils.shouldRenderFacet(facet)) {
            return exportValue(context, facet);
        } else {
            return "";
        }
    }

    default String getContentType() {
        return "application/octet-stream";
    }

    default String getFileExtension() {
        // .xls is a legacy file type. so, we use .xlsx as default.
        return ".xlsx";
    }

    default void exportColumnGroup(FacesContext context, ColumnGroup columnGroup, ExCellaExporter.ColumnType columnType, ReportSheet reportSheet, List<Object> facetColumns) {
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
                    exportColumnGroupMultiRow(context, columnGroup, columnType, reportSheet, facetColumns);
                } else {
                    exportFacetColumns(context, child.getChildren(), columnType, reportSheet, facetColumns);
                }
            } else if (child instanceof UIColumn) {
                exportFacetColumns(context, columnGroup.getChildren(), columnType, reportSheet, facetColumns);
            } else {
                // ignore
            }
        }

        context.getAttributes().remove(Constants.HELPER_RENDERER);
    }

    default void exportColumnGroupMultiRow(FacesContext context, ColumnGroup columnGroup, ExCellaExporter.ColumnType columnType,
            ReportSheet reportSheet, List<Object> facetColumns) {

        exportColumnGroupMultiRow(context, columnGroup, columnType, reportSheet, facetColumns, 0);
    }

    default void exportColumnGroupMultiRow(FacesContext context, ColumnGroup columnGroup, ExCellaExporter.ColumnType columnType,
            ReportSheet reportSheet, List<Object> facetColumns, int beginColIndex) {

        Map</*colindex*/Integer, List<Object>> headerContents = new HashMap<>();
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
                List<Object> columnContents = headerContents.computeIfAbsent(colIndex, c -> new ArrayList<>());
                columnContents.add(getFacetColumnValue(context, column, columnType));
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

        headerContents.keySet().stream()
            .map(i -> "$R[]{" + tagPrefix + i + "}")
            .collect(Collectors.toCollection(() -> facetColumns));
    }

    private void exportFacetColumns(FacesContext context, List<UIComponent> columns, ExCellaExporter.ColumnType columnType, ReportSheet reportSheet, List<Object> facetColumns) {
        @SuppressWarnings("unchecked")
        Set<CellRangeAddress> mergedAreas = nonNull((Set<CellRangeAddress>) reportSheet.getParam(null, COLUMN_GROUP_MERGED_AREAS_KEY + columnType), new HashSet<>());
        reportSheet.addParam(null, COLUMN_GROUP_MERGED_AREAS_KEY + columnType, mergedAreas);

        int colIndex = -1;
        for (UIComponent child : columns) {
            UIColumn column = (UIColumn)child;
            if (!isExportable(context, column)) {
                continue;
            }
            colIndex++;
            facetColumns.add(getFacetColumnValue(context, column, columnType));
            if (column.getColspan() > 1) {
                int colsToMerge = column.getColspan() - 1;
                mergedAreas.add(new CellRangeAddress(0, 0, colIndex, colIndex + colsToMerge));
                colIndex += colsToMerge;
                IntStream.range(0, colsToMerge).forEach(i -> facetColumns.add(null));
            }
        }
    }

    default <V> V nonNull(V obj, V defaultValue) {
        return obj != null ? obj : defaultValue;
    }

    default Map<String, List<Object>> getDataContainer(ReportSheet reportSheet) {
        @SuppressWarnings("unchecked")
        Map<String, List<Object>> dataContainer = (Map<String, List<Object>>) reportSheet.getParam(null, DATA_CONTAINER_KEY);
        if (dataContainer == null) {
            dataContainer = new HashMap<>();
            reportSheet.addParam(null, DATA_CONTAINER_KEY, dataContainer);
        }
        return dataContainer;
    }

}
