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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.el.ValueExpression;
import javax.faces.component.UIComponent;
import javax.faces.component.ValueHolder;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.bbreak.excella.reports.exporter.ExcelExporter;
import org.bbreak.excella.reports.listener.ReportProcessAdaptor;
import org.bbreak.excella.reports.listener.ReportProcessListener;
import org.bbreak.excella.reports.model.ConvertConfiguration;
import org.bbreak.excella.reports.model.ReportBook;
import org.bbreak.excella.reports.model.ReportSheet;
import org.bbreak.excella.reports.processor.ReportProcessor;
import org.bbreak.excella.reports.tag.ColRepeatParamParser;
import org.bbreak.excella.reports.tag.RowRepeatParamParser;
import org.primefaces.component.api.DynamicColumn;
import org.primefaces.component.api.UIColumn;
import org.primefaces.component.celleditor.CellEditor;
import org.primefaces.component.columngroup.ColumnGroup;
import org.primefaces.component.datatable.DataTable;
import org.primefaces.component.datatable.export.DataTableExporter;
import org.primefaces.component.export.ExportConfiguration;
import org.primefaces.component.export.Exporter;
import org.primefaces.util.ComponentUtils;
import org.primefaces.util.Constants;

import net.bis5.excella.primefaces.exporter.convert.ExporterConverter;

/**
 * ExCella Reportsを用いてDataTableのデータを出力する{@link Exporter}実装
 */
public class DataTableExcellaExporter extends DataTableExporter {

    private static final String DEFAULT_TEMPLATE_SHEET_NAME = "DATA";

    private static final String DATA_CONTAINER_KEY = "DATA_CONTAINER_KEY";

    private static final String DEFAULT_DATA_COLUMNS_TAG = "dataColumns";

    private static final String DEFAULT_HEADERS_TAG = "headers";

    private static final String DEFAULT_FOOTERS_TAG = "footers";

    private static final URL DEFAULT_TEMPLATE_URL = DataTableExcellaExporter.class.getResource("/DefaultTemplate.xlsx");

    private static final String MERGE_TO_LEFT_MARKER = DataTableExcellaExporter.class.getCanonicalName()+".MergeToLeft";
    private static final String MERGE_TO_UP_MARKER = DataTableExcellaExporter.class.getCanonicalName()+".MergeToUp";

    private ReportBook reportBook;

    private URL templateUrl;

    private Path templatePath;

    private String templateSheetName;

    private TemplateType templateType;

    private List<ReportProcessListener> listeners = new ArrayList<>();

    private String dataColumnsTag;

    private String headersTag;

    private String footersTag;

    public void setTemplatePath(Path templatePath) {
        this.templatePath = templatePath;
    }

    public void setTemplateUrl(URL templateUrl) {
        this.templateUrl = templateUrl;
    }

    private void setTemplateType(TemplateType templateType) {
        this.templateType = templateType;
    }

    public void setTemplateSheetName(String templateSheetName) {
        this.templateSheetName = templateSheetName;
    }

    @Override
    protected void preExport(FacesContext context, ExportConfiguration config) throws IOException {
        reportBook = new ReportBook();

        listeners.add(new ReportProcessAdaptor() {
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

    @Override
    protected void exportCells(DataTable table, Object document) {
        ReportSheet sheet = (ReportSheet) document;
        Map<String, List<Object>> dataContainer = getDataContainer(sheet);
        int colIndex = 0;
        for (UIColumn column : table.getColumns()) {
            if (column instanceof DynamicColumn) {
                ((DynamicColumn) column).applyStatelessModel();
            }
            if (!(column.isRendered() && column.isExportable())) {
                continue;
            }
            addCellValue(FacesContext.getCurrentInstance(), dataContainer, colIndex++, column);
        }
    }

    private void addCellValue(FacesContext context, Map<String, List<Object>> dataContainer, int colIndex,
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
                    .map(c -> exportValue(context, c)) //
                    .map(v -> v == null ? "" : v).forEach(builder::append);
            exportValue = builder.toString();
        }

        List<Object> values = dataContainer.computeIfAbsent(columnKey, ignore -> new ArrayList<>());
        values.add(exportValue);
    }

    @Override
    public String exportValue(FacesContext context, UIComponent component) {
        String value = super.exportValue(context, component);
        if (component.getClass().getSimpleName().equals("UIInstructions")) {
            // evaluate el expr
            ValueExpression ve = context.getApplication().getExpressionFactory().createValueExpression(context.getELContext(), value, Object.class);
            return String.valueOf(ve.getValue(context.getELContext()));
        }
        return value;
    }

    @SuppressWarnings("unchecked")
    public Object exportObjectValue(FacesContext context, UIComponent component) {
        if (component instanceof ValueHolder) {
            ValueHolder valueHolder = (ValueHolder)component;
            Object value = valueHolder.getValue();
            if (value == null) {
                return null;
            }
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
            if (value instanceof Number || value instanceof Date || value instanceof Calendar || value instanceof LocalDate || value instanceof LocalDateTime) {
                return value;
            }
        } else if (component instanceof CellEditor) {
            return exportObjectValue(context, component.getFacet("output"));
        }
        return exportValue(context, component);
    }

    public void setDataColumnsTag(String dataColumnsTag) {
        this.dataColumnsTag = dataColumnsTag;
    }

    public void setHeadersTag(String headersTag) {
        this.headersTag = headersTag;
    }

    /**
     * @deprecated Use {@link #setHeadersTag(String)} instead.
     */
    @Deprecated
    public void setHedersTag(String headersTag) {
        setHeadersTag(headersTag);
    }

    public void setFootersTag(String footersTag) {
        this.footersTag = footersTag;
    }

    @Override
    protected void doExport(FacesContext facesContext, DataTable table, ExportConfiguration config, int index)
            throws IOException {
        // 一度の出力で複数のDataTableが対象となった場合、このメソッドは引数のtable, indexを変えて複数回呼ばれる。
        // このExporterでは1DataTableを1シートに出力する方針とする。
        String sheetName = templateSheetName != null ? templateSheetName : DEFAULT_TEMPLATE_SHEET_NAME;
        ReportSheet reportSheet = new ReportSheet(sheetName, sheetName + "_" + index);
        Map<String, List<Object>> dataContainer = new LinkedHashMap<>();
        reportSheet.addParam(null, DATA_CONTAINER_KEY, dataContainer);

        List<String> columnHeader = exportFacet(facesContext, table, DataTableExporter.ColumnType.HEADER, reportSheet);

        if (config.isPageOnly()) {
            exportPageOnly(facesContext, table, reportSheet);
        } else if (config.isSelectionOnly()) {
            exportSelectionOnly(facesContext, table, reportSheet);
        } else {
            exportAll(facesContext, table, reportSheet);
        }

        List<String> columnFooter = exportFacet(facesContext, table, DataTableExporter.ColumnType.FOOTER, reportSheet);

        reportSheet.removeParam(null, DATA_CONTAINER_KEY);

        setExportParameters(reportSheet, columnHeader, columnFooter, dataContainer);
    }

    public Map<String, List<Object>> getDataContainer(ReportSheet reportSheet) {
        @SuppressWarnings("unchecked")
        Map<String, List<Object>> dataContainer = (Map<String, List<Object>>) reportSheet.getParam(null, DATA_CONTAINER_KEY);
        if (dataContainer == null) {
            dataContainer = new HashMap<>();
            reportSheet.addParam(null, DATA_CONTAINER_KEY, dataContainer);
        }
        return dataContainer;
    }
    private void setExportParameters(ReportSheet reportSheet, List<String> columnHeader, List<String> columnFooter, Map<String, List<Object>> dataContainer) {
        Object[] columnDataParams = dataContainer.keySet().stream().map(k -> "$R[]{" + k + "}").toArray();
        reportSheet.addParam(ColRepeatParamParser.DEFAULT_TAG, dataColumnsTag != null ? dataColumnsTag : DEFAULT_DATA_COLUMNS_TAG, columnDataParams);
        dataContainer.entrySet()
                .forEach(e -> reportSheet.addParam(RowRepeatParamParser.DEFAULT_TAG, e.getKey(), e.getValue().toArray()));

        String headersTagName = headersTag != null ? headersTag : DEFAULT_HEADERS_TAG;
        String footersTagName = footersTag != null ? footersTag : DEFAULT_FOOTERS_TAG;
        reportSheet.addParam(ColRepeatParamParser.DEFAULT_TAG, headersTagName, columnHeader.toArray());
        reportSheet.addParam(ColRepeatParamParser.DEFAULT_TAG, footersTagName, columnFooter.toArray());

        reportBook.addReportSheet(reportSheet);

        listeners.add(new ReportProcessAdaptor() {
            @Override
            public void postBookParse(Workbook workbook, ReportBook reportBook) {
                IntStream.range(0, workbook.getNumberOfSheets())
                    .mapToObj(workbook::getSheetAt)
                    .forEach(this::mergeCells);
            }

            private void mergeCells(Sheet sheet) {
                for (Row row : sheet) {
                    for (Cell cell : row) {
                        try {
                            String value = cell.getStringCellValue();
                            if (MERGE_TO_LEFT_MARKER.equals(value)) {
                                cell.setCellValue((String)null);
                                List<CellRangeAddress> mergedRegions = sheet.getMergedRegions();
                                Optional<CellRangeAddress> leftCellMergedRegion = findMergedRegion(sheet, cell.getRowIndex(), cell.getColumnIndex() - 1);
                                leftCellMergedRegion.ifPresent(r -> sheet.removeMergedRegion(mergedRegions.indexOf(r)));
                                CellRangeAddress newMergedRegion = leftCellMergedRegion.map(r -> new CellRangeAddress(r.getFirstRow(), r.getLastRow(), r.getFirstColumn(), r.getLastColumn() + 1))
                                    .orElseGet(() -> new CellRangeAddress(cell.getRowIndex(), cell.getRowIndex(), cell.getColumnIndex() - 1, cell.getColumnIndex()));
                                sheet.addMergedRegion(newMergedRegion);
                            } else if (MERGE_TO_UP_MARKER.equals(value)) {
                                cell.setCellValue((String)null);
                                List<CellRangeAddress> mergedRegions = sheet.getMergedRegions();
                                Optional<CellRangeAddress> leftCellMergedRegion = findMergedRegion(sheet, cell.getRowIndex() - 1, cell.getColumnIndex());
                                leftCellMergedRegion.ifPresent(r -> sheet.removeMergedRegion(mergedRegions.indexOf(r)));
                                CellRangeAddress newMergedRegion = leftCellMergedRegion.map(r -> new CellRangeAddress(r.getFirstRow(), r.getLastRow() + 1, r.getFirstColumn(), r.getLastColumn()))
                                    .orElseGet(() -> new CellRangeAddress(cell.getRowIndex() - 1, cell.getRowIndex(), cell.getColumnIndex(), cell.getColumnIndex()));
                                sheet.addMergedRegion(newMergedRegion);
                            }
                        } catch (RuntimeException ignore) {
                            // noop
                        }
                    }
                }
            }

            private Optional<CellRangeAddress> findMergedRegion(Sheet sheet, int rowIndex, int colIndex) {
                return sheet.getMergedRegions().stream()
                    .filter(r -> r.isInRange(rowIndex, colIndex))
                    .findAny();
            }
        });
    }

    private List<String> exportFacet(FacesContext context, DataTable table, ColumnType columnType, ReportSheet reportSheet) {
        List<String> facetColumns = new ArrayList<>();

        ColumnGroup group = table.getColumnGroup(columnType == ColumnType.HEADER ? "header" : "footer");
        if (group != null && group.isRendered()) {
            return exportColumnGroup(context, group, columnType, reportSheet);
        }
        if (table.getFrozenColumns() > 0) {
            ColumnGroup frozenGroup = table.getColumnGroup(columnType == ColumnType.HEADER ? "frozenHeader" : "frozenFooter");
            ColumnGroup scrollableGroup = table.getColumnGroup(columnType == ColumnType.HEADER ? "scrollableHeader" : "scrollableFooter");
            if (frozenGroup != null && scrollableGroup != null && frozenGroup.isRendered() && scrollableGroup.isRendered()) {
                return exportFrozenScrollableGroup(context, columnType, frozenGroup, scrollableGroup, reportSheet);
            }
        }

        for (UIColumn column : table.getColumns()) {
            if (column instanceof DynamicColumn) {
                ((DynamicColumn)column).applyStatelessModel();
            }
            if (!column.isRendered() || !column.isExportable()) {
                continue;
            }
            facetColumns.add(getFacetColumnText(context, column, columnType));
        }
        boolean allEmpty = facetColumns.stream() //
            .filter(c -> !Objects.isNull(c)) //
            .allMatch(String::isEmpty);
        if (allEmpty) {
            return Collections.emptyList();
        }
        return facetColumns;
    }

    private List<String> exportColumnGroup(FacesContext context, ColumnGroup columnGroup, ColumnType columnType, ReportSheet reportSheet) {
        List<String> facetColumns = new ArrayList<>();
        context.getAttributes().put(Constants.HELPER_RENDERER, "columnGroup");

        for (UIComponent child : columnGroup.getChildren()) {
            if (!child.isRendered()) {
                continue;
            }
            if (child instanceof org.primefaces.component.row.Row) {
                if (columnGroup.getChildren().size() > 1) {
                    return exportColumnGroupMultiRow(context, columnGroup, columnType, reportSheet);
                }
                for (UIComponent rowChild : child.getChildren()) {
                    UIColumn column = (UIColumn) rowChild;
                    if (!column.isRendered()) {
                        continue;
                    }
                    facetColumns.add(getFacetColumnText(context, column, columnType));
                    if (column.getColspan() > 1) {
                        IntStream.rangeClosed(2, column.getColspan())
                            .forEach(x -> facetColumns.add(MERGE_TO_LEFT_MARKER));
                    }
                }
            } else if (child instanceof UIColumn) {
                UIColumn column = (UIColumn)child;
                facetColumns.add(getFacetColumnText(context, column, columnType));
                if (column.getColspan() > 1) {
                    IntStream.rangeClosed(2, column.getColspan())
                        .forEach(x -> facetColumns.add(MERGE_TO_LEFT_MARKER));
                }
            } else {
                // ignore
            }
        }

        context.getAttributes().remove(Constants.HELPER_RENDERER);
        return facetColumns;
    }

    private List<String> exportFrozenScrollableGroup(FacesContext context, ColumnType columnType,
            ColumnGroup frozenGroup, ColumnGroup scrollableGroup, ReportSheet reportSheet) {
        List<String> facetColumns = new ArrayList<>();

        for (UIComponent child : frozenGroup.getChildren()) {
            if (child instanceof org.primefaces.component.row.Row) {
                if (frozenGroup.getChildren().size() > 1) {
                    facetColumns.addAll(exportColumnGroupMultiRow(context, frozenGroup, columnType, reportSheet));
                    break;
                } else {
                    facetColumns.addAll(exportColumnGroup(context, frozenGroup, columnType, reportSheet));
                }
            } else if (child instanceof UIColumn) {
                facetColumns.addAll(exportColumnGroup(context, frozenGroup, columnType, reportSheet));
            } else {
                // ignore
            }
        }

        int frozenColumns = facetColumns.size();

        for (UIComponent child : scrollableGroup.getChildren()) {
            if (child instanceof org.primefaces.component.row.Row) {
                if (scrollableGroup.getChildren().size() > 1) {
                    facetColumns.addAll(exportColumnGroupMultiRow(context, scrollableGroup, columnType, reportSheet, frozenColumns));
                    break;
                } else {
                    facetColumns.addAll(exportColumnGroup(context, scrollableGroup, columnType, reportSheet));
                }
            } else if (child instanceof UIColumn) {
                facetColumns.addAll(exportColumnGroup(context, scrollableGroup, columnType, reportSheet));
            } else {
                // ignore
            }
        }

        return facetColumns;
    }

    private static class RowMergedArea {
        private final int rowStart;
        private final int colIndex;
        private final int rowEnd;
        private RowMergedArea(int rowStart, int colIndex, int rowspan) {
            this.rowStart= rowStart;
            this.colIndex = colIndex;
            this.rowEnd = rowStart + rowspan - 1;
        }
        public boolean isInRowspanRange(int rowIndex, int colIndex) {
            return colIndex == this.colIndex && this.rowStart <= rowIndex && rowIndex <= this.rowEnd;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof RowMergedArea)) {
                return false;
            }
            if (this == obj) {
                return true;
            }
            RowMergedArea other = (RowMergedArea)obj;
            return this.rowStart == other.rowStart && this.colIndex == other.colIndex;
        }

        @Override
        public int hashCode() {
            return Objects.hash(rowStart, colIndex);
        }
    }

    private List<String> exportColumnGroupMultiRow(FacesContext context, ColumnGroup columnGroup, ColumnType columnType,
            ReportSheet reportSheet) {

        return exportColumnGroupMultiRow(context, columnGroup, columnType, reportSheet, 0);
    }

    private List<String> exportColumnGroupMultiRow(FacesContext context, ColumnGroup columnGroup, ColumnType columnType,
            ReportSheet reportSheet, int beginColIndex) {

        Map</*colindex*/Integer, List<String>> headerContents = new HashMap<>();
        Set<RowMergedArea> rowMergedAreas = new HashSet<>();
        int rowIndex = 0;
        for (UIComponent child : columnGroup.getChildren()) {
            if (!child.isRendered() || !(child instanceof org.primefaces.component.row.Row)) {
                continue;
            }
            org.primefaces.component.row.Row row = (org.primefaces.component.row.Row)child;
            int colIndex = beginColIndex;
            for (UIComponent rowChild : row.getChildren()) {
                while (true) {
                    boolean whileBreak = true;
                    for (RowMergedArea mergedArea : rowMergedAreas) {
                        if (mergedArea.isInRowspanRange(rowIndex, colIndex)) {
                            List<String> columnContents = headerContents.computeIfAbsent(colIndex, c -> new ArrayList<>());
                            columnContents.add(MERGE_TO_UP_MARKER);
                            colIndex++;
                            whileBreak = false;
                            break;
                        }
                    }
                    if (whileBreak) {
                        break;
                    }
                }
                if (!rowChild.isRendered() || !(rowChild instanceof UIColumn)) {
                    continue;
                }
                UIColumn column = (UIColumn)rowChild;
                List<String> columnContents = headerContents.computeIfAbsent(colIndex, c -> new ArrayList<>());
                columnContents.add(getFacetColumnText(context, column, columnType));
                if (column.getRowspan() > 1) {
                    RowMergedArea mergedArea = new RowMergedArea(rowIndex, colIndex, column.getRowspan());
                    rowMergedAreas.add(mergedArea);
                }
                if (column.getColspan() > 1) {
                    IntStream.range(colIndex + 1, colIndex + column.getColspan())
                        .mapToObj(i -> headerContents.computeIfAbsent(i, c -> new ArrayList<>()))
                        .forEach(c -> c.add(MERGE_TO_LEFT_MARKER));
                    colIndex += column.getColspan() - 1;
                }
                colIndex++;
            }
            rowIndex++;
        }
        String tagPrefix = columnType == ColumnType.HEADER ? "header" : "footer";
        headerContents.entrySet().forEach(e -> reportSheet.addParam(RowRepeatParamParser.DEFAULT_TAG, tagPrefix + e.getKey(), e.getValue().toArray()));

        return headerContents.keySet().stream()
            .map(i -> "$R[]{" + tagPrefix + i + "}")
            .collect(Collectors.toList());
    }

    private String getFacetColumnText(FacesContext context, UIColumn column, DataTableExporter.ColumnType columnType) {
        UIComponent facet = column.getFacet(columnType.facet());
        String text;
        if (columnType == DataTableExporter.ColumnType.HEADER) {
            text = column.getExportHeaderValue() != null ? column.getExportHeaderValue() : column.getHeaderText();
        } else if (columnType == DataTableExporter.ColumnType.FOOTER) {
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

    @Override
    protected void postExport(FacesContext context, ExportConfiguration config) throws IOException {
        // TODO configを考慮する(何をする?)
        Path outputFile = processExport();

        try {
            writeResponse(context, outputFile, config);
        } finally {
            reset();
            Files.delete(outputFile);
        }
    }

    private void writeResponse(FacesContext context, Path outputFile, ExportConfiguration config) throws IOException {
        ExternalContext externalContext = context.getExternalContext();
        externalContext.setResponseContentType(templateType.getContentType());

        externalContext.setResponseHeader("Content-disposition",
                ComponentUtils.createContentDisposition("attachment", config.getOutputFileName() + templateType.getSuffix()));

        // TODO PF 9.0
        // addResponseCookie(context); // NOSONAR

        OutputStream out = externalContext.getResponseOutputStream();
        Files.copy(outputFile, out); // どうせOutputStreamに吐き出すんだから一時ファイル経由したくない気持ちもありつつ
        out.flush();
    }

    private void reset() {
        reportBook = null;
        listeners.clear();
    }

    private URL getTemplateFileUrl() throws MalformedURLException {
        if (templatePath != null) {
            return templatePath.toUri().toURL();
        } else if (templateUrl != null) {
            return templateUrl;
        }
        return DEFAULT_TEMPLATE_URL;
    }
    private Path processExport() throws IOException {
        ReportProcessor processor = new ReportProcessor();
        listeners.forEach(processor::addReportProcessListener);
        reportBook.setTemplateFileURL(getTemplateFileUrl());
        reportBook.setConfigurations(new ConvertConfiguration(ExcelExporter.FORMAT_TYPE));
        Path outputFile = Files.createTempFile(null, null);
        reportBook.setOutputFileName(outputFile.toString());
        try {
            processor.process(reportBook);
        } catch (Exception e) {
            throw new IllegalStateException("Unexpected exception", e); // XXX そもそもthrows Exception宣言しているのがおかしい
        }
        // ExCellaが拡張子を付けるので注意
        return Paths.get(outputFile.toString() + templateType.getSuffix());
    }

}
