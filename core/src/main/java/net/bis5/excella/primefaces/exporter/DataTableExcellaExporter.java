package net.bis5.excella.primefaces.exporter;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
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
public class DataTableExcellaExporter extends DataTableExporter<ReportBook, ExCellaExporterOptions> implements ExCellaExporter<DataTable> {

    private static final String DEFAULT_DATA_COLUMNS_TAG = "dataColumns";

    private static final String DEFAULT_HEADERS_TAG = "headers";

    private static final String DEFAULT_FOOTERS_TAG = "footers";

    private TemplateType templateType;

    private List<ReportProcessListener> listeners = new ArrayList<>();

    private ReportSheet currentSheet;

    public DataTableExcellaExporter() {
        super(new ExCellaExporterOptions(), ALL_FACETS, true);
    }

    private DataTableExcellaExporter(Builder builder) {
        super(builder.options, ALL_FACETS, true);
        setTemplatePath(builder.templatePath);
        setTemplateUrl(builder.templateUrl);
        setTemplateSheetName(builder.templateSheetName);
        setDataColumnsTag(builder.dataColumnsTag);
        setHeadersTag(builder.headersTag);
        setFootersTag(builder.footersTag);
    }

    /**
     * @deprecated Use {@link ExCellaExporterOptions}. This method will be removed in 5.0.0.
     */
    @Deprecated(forRemoval = true)
    public static Builder builder() {
        return new Builder();
    }

    /**
     * @deprecated Use {@link ExCellaExporterOptions}. This class will be removed in 5.0.0.
     */
    @Deprecated(forRemoval = true)
    public static class Builder {
        private Path templatePath;
        private URL templateUrl;
        private String templateSheetName;
        private String dataColumnsTag;
        private String headersTag;
        private String footersTag;
        private ExCellaExporterOptions options = new ExCellaExporterOptions();

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

        public Builder options(ExCellaExporterOptions options) {
            this.options = options;
            return this;
        }

        public DataTableExcellaExporter build() {
            return new DataTableExcellaExporter(this);
        }
    }

    /**
     * @deprecated Use {@link ExCellaExporterOptions}. This method will be removed in 5.0.0.
     */
    @Deprecated(forRemoval = true)
    @Override
    public void setTemplatePath(Path templatePath) {
        getExporterOptions().setTemplatePath(templatePath);
    }

    /**
     * @deprecated Use {@link ExCellaExporterOptions}. This method will be removed in 5.0.0.
     * @implNote Make this getter private in 5.0.0.
     */
    @Deprecated(forRemoval = true)
    @Override
    public Path getTemplatePath() {
        return getExporterOptions().getTemplatePath();
    }

    /**
     * @deprecated Use {@link ExCellaExporterOptions}. This method will be removed in 5.0.0.
     */
    @Deprecated(forRemoval = true)
    @Override
    public void setTemplateUrl(URL templateUrl) {
        getExporterOptions().setTemplateUrl(templateUrl);
    }

    /**
     * @deprecated Use {@link ExCellaExporterOptions}. This method will be removed in 5.0.0.
     * @implNote Make this getter private in 5.0.0.
     */
    @Deprecated(forRemoval = true)
    @Override
    public URL getTemplateUrl() {
        return getExporterOptions().getTemplateUrl();
    }

    @Override
    public void setTemplateType(TemplateType templateType) {
        this.templateType = templateType;
    }

    @Override
    public TemplateType getTemplateType() {
        return templateType;
    }

    /**
     * @deprecated Use {@link #builder()}. This method will be removed in 5.0.0.
     */
    @Deprecated(forRemoval = true)
    @Override
    public void setTemplateSheetName(String templateSheetName) {
        getExporterOptions().setTemplateSheetName(templateSheetName);
    }

    /**
     * @deprecated Use {@link ExCellaExporterOptions}. This method will be removed in 5.0.0.
     * @implNote Make this getter private in 5.0.0.
     */
    @Deprecated(forRemoval = true)
    @Override
    public String getTemplateSheetName() {
        return getExporterOptions().getTemplateSheetName();
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
    public ReportBook getDocument() {
        return document;
    }

    @Override
    public void setCurrentSheet(ReportSheet reportSheet) {
        this.currentSheet = reportSheet;
    }

    @Override
    public void preExport(FacesContext context) throws IOException {
        super.preExport(context);
        ExCellaExporter.super.preExport(context);
    }

    @Override
    public void postExport(FacesContext context) throws IOException {
        super.postExport(context);
        ExCellaExporter.super.postExport(context);
    }

    @Override
    public OutputStream os() { // change visivility
        return super.os();
    }

    @Override
    protected void exportCellValue(FacesContext context, DataTable table, UIColumn col, String text, int index) {
        Map<String, List<Object>> dataContainer = getDataContainer(currentSheet);
        addCellValue(context, dataContainer, table, index, col);
    }

    /**
     * @deprecated Use {@link #builder()}. This method will be removed in 5.0.0.
     */
    @Deprecated(forRemoval = true)
    public void setDataColumnsTag(String dataColumnsTag) {
        getExporterOptions().setDataColumnsTag(dataColumnsTag);
    }

    private String getDataColumnsTag() {
        return getExporterOptions().getDataColumnsTag();
    }

    /**
     * @deprecated Use {@link #builder()}. This method will be removed in 5.0.0.
     */
    @Deprecated(forRemoval = true)
    public void setHeadersTag(String headersTag) {
        getExporterOptions().setHeadersTag(headersTag);
    }

    private String getHeadersTag() {
        return getExporterOptions().getHeadersTag();
    }

    /**
     * @deprecated Use {@link #builder()}. This method will be removed in 5.0.0.
     */
    @Deprecated(forRemoval = true)
    public void setFootersTag(String footersTag) {
        getExporterOptions().setFootersTag(footersTag);
    }

    private String getFootersTag() {
        return getExporterOptions().getFootersTag();
    }

    @Override
    public void exportTable(FacesContext facesContext, DataTable table, int index) throws IOException {
        ExCellaExporter.super.exportTable(facesContext, table, index);
    }

    @Override
    public void exportSelectionOnly(FacesContext facesContext, DataTable table) {
        super.exportSelectionOnly(facesContext, table);
    }

    @Override
    public boolean isSelectionEmpty(DataTable table) {
        return table.getSelectedRowKeys().isEmpty();
    }

    @Override
    public void exportPageOnly(FacesContext context, DataTable table) {
        super.exportPageOnly(context, table);
    }

    @Override
    public int getPageRows(DataTable table) {
        return table.getRowsToRender();
    }

    @Override
    public void exportAll(FacesContext context, DataTable table) {
        super.exportAll(context, table);
    }

    @Override
    public int getTotalRows(DataTable table) {
        return table.getRowCount();
    }

    private String dataTag() {
        return Objects.requireNonNullElse(getDataColumnsTag(), DEFAULT_DATA_COLUMNS_TAG);
    }

    @Override
    public void setExportParameters(ReportSheet reportSheet, List<String> columnHeader, List<String> columnFooter, Map<String, List<Object>> dataContainer) {
        Object[] columnDataParams = dataContainer.keySet().stream().map(k -> "$R[]{" + k + "}").toArray();
        reportSheet.addParam(ColRepeatParamParser.DEFAULT_TAG, dataTag(), columnDataParams);

        dataContainer.entrySet()
            .stream()
            .map(this::normalizeValues)
            .forEach(e -> reportSheet.addParam(RowRepeatParamParser.DEFAULT_TAG, e.getKey(), e.getValue().toArray()));

        String headersTagName = Objects.requireNonNullElse(getHeadersTag(), DEFAULT_HEADERS_TAG);
        String footersTagName = Objects.requireNonNullElse(getFootersTag(), DEFAULT_FOOTERS_TAG);
        reportSheet.addParam(ColRepeatParamParser.DEFAULT_TAG, headersTagName, columnHeader.toArray());
        reportSheet.addParam(ColRepeatParamParser.DEFAULT_TAG, footersTagName, columnFooter.toArray());

        final int columnSize = columnHeader.size();

        getDocument().addReportSheet(reportSheet);

        listeners.add(new StyleUpdateListener(reportSheet, dataContainer, dataTag(), headersTagName, footersTagName, columnSize, columnDataParams));

    }

    @Override
    public void exportFacet(FacesContext context, DataTable table, ExCellaExporter.ColumnType columnType, ReportSheet reportSheet, List<String> facetColumns) {

        ColumnGroup group = table.getColumnGroup(columnType.facet());
        if (group != null && group.isRendered()) {
            exportColumnGroup(context, group, columnType, reportSheet, facetColumns);
            return;
        }
        if (table.getFrozenColumns() > 0) {
            ColumnGroup frozenGroup = table.getColumnGroup(columnType == ExCellaExporter.ColumnType.HEADER ? "frozenHeader" : "frozenFooter");
            ColumnGroup scrollableGroup = table.getColumnGroup(columnType == ExCellaExporter.ColumnType.HEADER ? "scrollableHeader" : "scrollableFooter");
            if (frozenGroup != null && scrollableGroup != null && frozenGroup.isRendered() && scrollableGroup.isRendered()) {
                exportFrozenScrollableGroup(context, columnType, frozenGroup, scrollableGroup, reportSheet, facetColumns);
                return;
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
            facetColumns.clear();
        }
    }


    private void exportFrozenScrollableGroup(FacesContext context, ExCellaExporter.ColumnType columnType,
            ColumnGroup frozenGroup, ColumnGroup scrollableGroup, ReportSheet reportSheet, List<String> facetColumns) {

        for (UIComponent child : frozenGroup.getChildren()) {
            if (child instanceof org.primefaces.component.row.Row) {
                if (frozenGroup.getChildren().size() > 1) {
                    exportColumnGroupMultiRow(context, frozenGroup, columnType, reportSheet, facetColumns);
                    break;
                } else {
                    exportColumnGroup(context, frozenGroup, columnType, reportSheet, facetColumns);
                }
            } else if (child instanceof UIColumn) {
                exportColumnGroup(context, frozenGroup, columnType, reportSheet, facetColumns);
            } else {
                // ignore
            }
        }

        int frozenColumns = facetColumns.size();

        for (UIComponent child : scrollableGroup.getChildren()) {
            if (child instanceof org.primefaces.component.row.Row) {
                if (scrollableGroup.getChildren().size() > 1) {
                    exportColumnGroupMultiRow(context, scrollableGroup, columnType, reportSheet, facetColumns, frozenColumns);
                    break;
                } else {
                    exportColumnGroup(context, scrollableGroup, columnType, reportSheet, facetColumns);
                }
            } else if (child instanceof UIColumn) {
                exportColumnGroup(context, scrollableGroup, columnType, reportSheet, facetColumns);
            } else {
                // ignore
            }
        }
    }

    @Override
    public void reset() {
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

    @Override
    public ExportConfiguration getExportConfiguration() {
        return exportConfiguration;
    }


    @Override
    protected ReportBook createDocument(FacesContext context) throws IOException {
        return new ReportBook();
    }

}
