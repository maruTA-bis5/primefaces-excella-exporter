package net.bis5.excella.primefaces.exporter;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellAddress;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellUtil;
import org.bbreak.excella.core.SheetData;
import org.bbreak.excella.core.SheetParser;
import org.bbreak.excella.reports.listener.ReportProcessAdaptor;
import org.bbreak.excella.reports.listener.ReportProcessListener;
import org.bbreak.excella.reports.model.ReportBook;
import org.bbreak.excella.reports.model.ReportSheet;
import org.bbreak.excella.reports.tag.ColRepeatParamParser;
import org.bbreak.excella.reports.tag.RowRepeatParamParser;
import org.primefaces.component.api.DynamicColumn;
import org.primefaces.component.api.UIColumn;
import org.primefaces.component.columngroup.ColumnGroup;
import org.primefaces.component.datatable.DataTable;
import org.primefaces.component.datatable.export.DataTableExporter;
import org.primefaces.component.export.ExportConfiguration;
import org.primefaces.component.export.Exporter;
import org.primefaces.util.Constants;

/**
 * ExCella Reportsを用いてDataTableのデータを出力する{@link Exporter}実装
 */
public class DataTableExcellaExporter extends DataTableExporter implements ExCellaExporter<DataTable> {

    private static final String COLUMN_GROUP_MERGED_AREAS_KEY = "HEADER_MERGED_AREAS_KEY";

    private static final String DEFAULT_DATA_COLUMNS_TAG = "dataColumns";

    private static final String DEFAULT_HEADERS_TAG = "headers";

    private static final String DEFAULT_FOOTERS_TAG = "footers";

    private ReportBook reportBook;

    private URL templateUrl;

    private Path templatePath;

    private String templateSheetName;

    private TemplateType templateType;

    private List<ReportProcessListener> listeners = new ArrayList<>();

    private String dataColumnsTag;

    private String headersTag;

    private String footersTag;

    /**
     * @deprecated Use {@link #builder()}
     */
    @Deprecated(forRemoval = true)
    public DataTableExcellaExporter() {
        // deprecated
    }

    private DataTableExcellaExporter(Builder builder) {
        this.templatePath = builder.templatePath;
        this.templateUrl = builder.templateUrl;
        this.templateSheetName = builder.templateSheetName;
        this.dataColumnsTag = builder.dataColumnsTag;
        this.headersTag = builder.headersTag;
        this.footersTag = builder.footersTag;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Path templatePath;
        private URL templateUrl;
        private String templateSheetName;
        private String dataColumnsTag;
        private String headersTag;
        private String footersTag;

        public Builder templatePath(Path templatePath) {
            this.templatePath = templatePath;
            return this;
        }

        public Builder templateUrl(URL templateUrl) {
            this.templateUrl = templateUrl;
            return this;
        }

        public Builder templateSheetName(String templateSheetName) {
            this.templateSheetName = templateSheetName;
            return this;
        }

        public Builder dataColumnsTag(String dataColumnsTag) {
            this.dataColumnsTag = dataColumnsTag;
            return this;
        }

        public Builder headersTag(String headersTag) {
            this.headersTag = headersTag;
            return this;
        }

        public Builder footersTag(String footersTag) {
            this.footersTag = footersTag;
            return this;
        }

        public DataTableExcellaExporter build() {
            return new DataTableExcellaExporter(this);
        }
    }

    @Override
    public void setTemplatePath(Path templatePath) {
        this.templatePath = templatePath;
    }

    @Override
    public Path getTemplatePath() {
        return templatePath;
    }

    @Override
    public void setTemplateUrl(URL templateUrl) {
        this.templateUrl = templateUrl;
    }

    @Override
    public URL getTemplateUrl() {
        return templateUrl;
    }

    @Override
    public void setTemplateType(TemplateType templateType) {
        this.templateType = templateType;
    }

    @Override
    public TemplateType getTemplateType() {
        return templateType;
    }

    @Override
    public void setTemplateSheetName(String templateSheetName) {
        this.templateSheetName = templateSheetName;
    }

    @Override
    public String getTemplateSheetName() {
        return templateSheetName;
    }

    @Override
    public void addListener(ReportProcessListener listener) {
        listeners.add(listener);
    }

    @Override
    public List<ReportProcessListener> getListeners() {
        return listeners;
    }

    @Override
    public void setReportBook(ReportBook reportBook) {
        this.reportBook = reportBook;
    }

    @Override
    public ReportBook getDocument() {
        return reportBook;
    }

    @Override
    public void preExport(FacesContext context, ExportConfiguration exportConfiguration) {
        ExCellaExporter.super.preExport(context, exportConfiguration);
    }

    @Override
    public void postExport(FacesContext context, ExportConfiguration exportConfiguration) throws IOException {
        ExCellaExporter.super.postExport(context, exportConfiguration);
    }

    @Override
    public OutputStream getOutputStream() {
        return super.getOutputStream();
    }

    @Override
    protected void exportCells(DataTable table, Object document) {
        ReportSheet sheet = (ReportSheet) document;
        Map<String, List<Object>> dataContainer = getDataContainer(sheet);
        int colIndex = 0;
        for (UIColumn column : getExportableColumns(table)) {
            if (column instanceof DynamicColumn) {
                ((DynamicColumn) column).applyStatelessModel();
            }
            if (!isExportable(FacesContext.getCurrentInstance(), column)) {
                continue;
            }
            addCellValue(FacesContext.getCurrentInstance(), dataContainer, colIndex++, column);
        }
    }

    @Override
    public String exportColumnByFunction(FacesContext context, UIColumn column) {
        return super.exportColumnByFunction(context, column);
    }

    @Override
    public String exportValue(FacesContext context, UIComponent component) {
        String value = super.exportValue(context, component);
        if (component.getClass().getSimpleName().equals("UIInstructions")) {
            return exportUIInstructionsValue(context, component, value);
        }
        return value;
    }

    private Map<ValueType, CellStyle> styles;

    public void setDataColumnsTag(String dataColumnsTag) {
        this.dataColumnsTag = dataColumnsTag;
    }

    public void setHeadersTag(String headersTag) {
        this.headersTag = headersTag;
    }

    public void setFootersTag(String footersTag) {
        this.footersTag = footersTag;
    }

    @Override
    public void doExport(FacesContext facesContext, DataTable table, ExportConfiguration config, int index)
            throws IOException {
        ExCellaExporter.super.doExport(facesContext, table, config, index);
    }

    @Override
    public void exportSelectionOnly(FacesContext facesContext, DataTable table, Object document) {
        super.exportSelectionOnly(facesContext, table, document);
    }

    @Override
    public boolean isSelectionEmpty(DataTable table) {
        return table.getSelectedRowKeys().isEmpty();
    }

    @Override
    public void exportPageOnly(FacesContext context, DataTable table, Object document) {
        super.exportPageOnly(context, table, document);
    }

    @Override
    public int getPageRows(DataTable table) {
        return table.getRowsToRender();
    }

    @Override
    public void exportAll(FacesContext context, DataTable table, Object document) {
        super.exportAll(context, table, document);
    }

    @Override
    public int getTotalRows(DataTable table) {
        return table.getRowCount();
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

    private <T> T nonNull(T obj, T defaultValue) {
        return obj != null ? obj : defaultValue;
    }

    private String dataTag() {
        return dataColumnsTag != null ? dataColumnsTag : DEFAULT_DATA_COLUMNS_TAG;
    }

    @Override
    public void setExportParameters(ReportSheet reportSheet, List<String> columnHeader, List<String> columnFooter, Map<String, List<Object>> dataContainer) {
        Object[] columnDataParams = dataContainer.keySet().stream().map(k -> "$R[]{" + k + "}").toArray();
        reportSheet.addParam(ColRepeatParamParser.DEFAULT_TAG, dataTag(), columnDataParams);

        Map<String, ValueType> valueTypes = detectValueTypes(dataContainer);
        dataContainer.entrySet()
            .stream()
            .map(this::normalizeValues)
            .forEach(e -> reportSheet.addParam(RowRepeatParamParser.DEFAULT_TAG, e.getKey(), e.getValue().toArray()));
        int repeatRows = dataContainer.values().stream()
            .mapToInt(List::size)
            .max()
            .orElse(1);

        String headersTagName = headersTag != null ? headersTag : DEFAULT_HEADERS_TAG;
        String footersTagName = footersTag != null ? footersTag : DEFAULT_FOOTERS_TAG;
        reportSheet.addParam(ColRepeatParamParser.DEFAULT_TAG, headersTagName, columnHeader.toArray());
        reportSheet.addParam(ColRepeatParamParser.DEFAULT_TAG, footersTagName, columnFooter.toArray());

        final int columnSize = columnHeader.size();

        @SuppressWarnings("unchecked")
        Set<CellRangeAddress> headerMergedAreas = nonNull((Set<CellRangeAddress>) reportSheet.getParam(null, COLUMN_GROUP_MERGED_AREAS_KEY + "header"), new HashSet<>());
        @SuppressWarnings("unchecked")
        Set<CellRangeAddress> footerMergedAreas = nonNull((Set<CellRangeAddress>) reportSheet.getParam(null, COLUMN_GROUP_MERGED_AREAS_KEY + "footer"), new HashSet<>());
        reportSheet.removeParam(null, COLUMN_GROUP_MERGED_AREAS_KEY + "header");
        reportSheet.removeParam(null, COLUMN_GROUP_MERGED_AREAS_KEY + "footer");

        reportBook.addReportSheet(reportSheet);

        listeners.add(new ReportProcessAdaptor() {

            private CellAddress headerPosition;

            private CellAddress dataPosition;

            private CellAddress footerPosition;

            private int headerSize;

            @Override
            public void preBookParse(Workbook workbook, ReportBook reportBook) {
                styles = ValueType.initStyles(workbook);
            }

            private void setHeaderPosition(CellAddress address) {
                headerPosition = address;
            }

            private void setDataPosition(CellAddress address) {
                dataPosition = address;
            }

            private int dataRowOffset(int row) {
                return Math.max(headerSize - 1, 0) + (dataPosition != null ? row + dataPosition.getRow() : row);
            }

            private int dataColOffset(int col) {
                return dataPosition != null ? col + dataPosition.getColumn()  : col;
            }

            private void setFooterPosition(CellAddress address) {
                footerPosition = address;
            }

            @Override
            public void preParse(Sheet sheet, SheetParser sheetParser) {
                String headerTag = ColRepeatParamParser.DEFAULT_TAG + "{" + headersTagName + "}";
                String footerTag = ColRepeatParamParser.DEFAULT_TAG + "{" + footersTagName + "}";
                String dataTag = ColRepeatParamParser.DEFAULT_TAG + "{" + dataTag() + "}";
                StreamSupport.stream(sheet.spliterator(), false)
                    .map(Row::spliterator)
                    .flatMap(s -> StreamSupport.stream(s, false))
                    .filter(c -> c.getCellType() == CellType.STRING)
                    .forEach(c -> {
                        if (headerTag.equals(c.getStringCellValue())) {
                            setHeaderPosition(c.getAddress());
                        } else if (footerTag.equals(c.getStringCellValue())) {
                            setFooterPosition(c.getAddress());
                        } else if (dataTag.equals(c.getStringCellValue())) {
                            setDataPosition(c.getAddress());
                        }
                    });
            }

            @Override
            public void postParse(Sheet sheet, SheetParser sheetParser, SheetData sheetData) {
                if (dataContainer.isEmpty() || !sheetData.getSheetName().equals(reportSheet.getSheetName())) {
                    return;
                }
                IntStream.range(0, columnSize)
                    .mapToObj(i -> "header" + i)
                    .map(t -> reportSheet.getParam(RowRepeatParamParser.DEFAULT_TAG, t))
                    .filter(Objects::nonNull)
                    .map(Object[].class::cast)
                    .mapToInt(a -> a.length)
                    .max()
                    .ifPresentOrElse(s -> headerSize = s, () -> headerSize = 1);
                for (Entry<String, ValueType> entry : valueTypes.entrySet()) {
                    String columnTag = getColumnTag(entry.getKey());
                    ValueType valueType = entry.getValue();
                    if (valueType == null) {
                        continue;
                    }
                    CellStyle style = styles.get(valueType);
                    int colIndex = Arrays.asList(columnDataParams).indexOf(columnTag);
                    IntStream.range(dataRowOffset(0), dataRowOffset(repeatRows))
                        .mapToObj(sheet::getRow)
                        .filter(Objects::nonNull)
                        .map(r -> r.getCell(dataColOffset(colIndex)))
                        .filter(Objects::nonNull)
                        .forEach(c -> setCellStyle(c, style));
                }
            }

            private void setCellStyle(Cell cell, CellStyle style) {
                CellUtil.setCellStyleProperty(cell, CellUtil.DATA_FORMAT, style.getDataFormat());
            }
            private String getColumnTag(String key) {
                return "$R[]{" + key + "}";
            }

            @Override
            public void postBookParse(Workbook workbook, ReportBook reportBook) {
                IntStream.range(0, workbook.getNumberOfSheets())
                    .mapToObj(workbook::getSheetAt)
                    .forEach(this::mergeCells);
            }

            private void mergeCells(Sheet sheet) {
                int headerOffset = headerPosition == null ? 0 : headerPosition.getRow();
                headerMergedAreas.forEach(a -> mergeCell(sheet, headerPosition, headerOffset, a));

                int footerOffset = footerPosition == null ? 0 : dataRowOffset(repeatRows) + footerPosition.getRow() - dataPosition.getRow() - 1;
                footerMergedAreas.forEach(a -> mergeCell(sheet, footerPosition, footerOffset, a));
            }

            private void mergeCell(Sheet sheet, CellAddress beginPosition, int rowOffset, CellRangeAddress area) {
                if (beginPosition == null) {
                    return;
                }
                if (beginPosition.equals(CellAddress.A1) && rowOffset <= 0) {
                    sheet.addMergedRegion(area);
                    return;
                }
                int colOffset = beginPosition.getColumn();

                var rangeToMerge = new CellRangeAddress(rowOffset + area.getFirstRow(), rowOffset + area.getLastRow(), colOffset + area.getFirstColumn(), colOffset + area.getLastColumn());
                sheet.addMergedRegion(rangeToMerge);
            }
        });
    }

    @Override
    public List<String> exportFacet(FacesContext context, DataTable table, ExCellaExporter.ColumnType columnType, ReportSheet reportSheet) {
        List<String> facetColumns = new ArrayList<>();

        ColumnGroup group = table.getColumnGroup(columnType.facet());
        if (group != null && group.isRendered()) {
            return exportColumnGroup(context, group, columnType, reportSheet);
        }
        if (table.getFrozenColumns() > 0) {
            ColumnGroup frozenGroup = table.getColumnGroup(columnType == ExCellaExporter.ColumnType.HEADER ? "frozenHeader" : "frozenFooter");
            ColumnGroup scrollableGroup = table.getColumnGroup(columnType == ExCellaExporter.ColumnType.HEADER ? "scrollableHeader" : "scrollableFooter");
            if (frozenGroup != null && scrollableGroup != null && frozenGroup.isRendered() && scrollableGroup.isRendered()) {
                return exportFrozenScrollableGroup(context, columnType, frozenGroup, scrollableGroup, reportSheet);
            }
        }

        for (UIColumn column : getExportableColumns(table)) {
            if (column instanceof DynamicColumn) {
                ((DynamicColumn)column).applyStatelessModel();
            }
            if (!isExportable(context, column)) {
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

    private List<String> exportColumnGroup(FacesContext context, ColumnGroup columnGroup, ExCellaExporter.ColumnType columnType, ReportSheet reportSheet) {
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

    private List<String> exportFrozenScrollableGroup(FacesContext context, ExCellaExporter.ColumnType columnType,
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

    private List<String> exportColumnGroupMultiRow(FacesContext context, ColumnGroup columnGroup, ExCellaExporter.ColumnType columnType,
            ReportSheet reportSheet) {

        return exportColumnGroupMultiRow(context, columnGroup, columnType, reportSheet, 0);
    }

    private List<String> exportColumnGroupMultiRow(FacesContext context, ColumnGroup columnGroup, ExCellaExporter.ColumnType columnType,
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

    @Override
    public void reset() {
        reportBook = null;
        listeners.clear();
    }

    @Override
    public String getContentType() {
        return ExCellaExporter.super.getContentType();
    }

    @Override
    public String getFileExtension() {
        return ExCellaExporter.super.getFileExtension();
    }

}
