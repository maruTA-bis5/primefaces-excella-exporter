package net.bis5.excella.primefaces.exporter;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;

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

/**
 * ExCella Reportsを用いてDataTableのデータを出力する{@link Exporter}実装
 */
public class DataTableExcellaExporter extends DataTableExporter implements ExCellaExporter<DataTable> {

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

    private String dataTag() {
        return dataColumnsTag != null ? dataColumnsTag : DEFAULT_DATA_COLUMNS_TAG;
    }

    @Override
    public void setExportParameters(ReportSheet reportSheet, List<String> columnHeader, List<String> columnFooter, Map<String, List<Object>> dataContainer) {
        Object[] columnDataParams = dataContainer.keySet().stream().map(k -> "$R[]{" + k + "}").toArray();
        reportSheet.addParam(ColRepeatParamParser.DEFAULT_TAG, dataTag(), columnDataParams);

        dataContainer.entrySet()
            .stream()
            .map(this::normalizeValues)
            .forEach(e -> reportSheet.addParam(RowRepeatParamParser.DEFAULT_TAG, e.getKey(), e.getValue().toArray()));

        String headersTagName = headersTag != null ? headersTag : DEFAULT_HEADERS_TAG;
        String footersTagName = footersTag != null ? footersTag : DEFAULT_FOOTERS_TAG;
        reportSheet.addParam(ColRepeatParamParser.DEFAULT_TAG, headersTagName, columnHeader.toArray());
        reportSheet.addParam(ColRepeatParamParser.DEFAULT_TAG, footersTagName, columnFooter.toArray());

        final int columnSize = columnHeader.size();

        reportBook.addReportSheet(reportSheet);

        listeners.add(new StyleUpdateListener(reportSheet, dataContainer, dataTag(), headersTagName, footersTagName, columnSize, columnDataParams));

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
