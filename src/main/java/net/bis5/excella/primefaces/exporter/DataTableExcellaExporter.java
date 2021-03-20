package net.bis5.excella.primefaces.exporter;

import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.faces.component.UIComponent;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.bbreak.excella.core.SheetData;
import org.bbreak.excella.core.SheetParser;
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
import org.primefaces.component.datatable.DataTable;
import org.primefaces.component.datatable.export.DataTableExporter;
import org.primefaces.component.export.ExportConfiguration;
import org.primefaces.util.ComponentUtils;

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
        @SuppressWarnings("unchecked")
        Map<String, List<String>> dataContainer = (Map<String, List<String>>) sheet.getParam(null, DATA_CONTAINER_KEY);
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

    private void addCellValue(FacesContext context, Map<String, List<String>> dataContainer, int colIndex,
            UIColumn column) {
        String columnKey = "data" + colIndex;

        String exportValue;
        if (column.getExportFunction() != null) {
            exportValue = exportColumnByFunction(context, column);
        } else {
            List<UIComponent> components = column.getChildren();
            StringBuilder builder = new StringBuilder();
            components.stream() //
                    .map(c -> exportValue(context, c)) //
                    .map(v -> v == null ? "" : v).forEach(builder::append);
            exportValue = builder.toString();
        }

        List<String> values = dataContainer.computeIfAbsent(columnKey, ignore -> new ArrayList<>());
        values.add(exportValue);
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
        Map<String, List<String>> dataContainer = new LinkedHashMap<>();
        reportSheet.addParam(null, DATA_CONTAINER_KEY, dataContainer);

        List<String> columnHeader = exportFacet(facesContext, table, DataTableExporter.ColumnType.HEADER);

        if (config.isPageOnly()) {
            exportPageOnly(facesContext, table, reportSheet);
        } else if (config.isSelectionOnly()) {
            exportSelectionOnly(facesContext, table, reportSheet);
        } else {
            exportAll(facesContext, table, reportSheet);
        }

        List<String> columnFooter = exportFacet(facesContext, table, DataTableExporter.ColumnType.FOOTER);

        reportSheet.removeParam(null, DATA_CONTAINER_KEY);

        setExportParameters(reportSheet, columnHeader, columnFooter, dataContainer);
    }

    private void setExportParameters(ReportSheet reportSheet, List<String> columnHeader, List<String> columnFooter, Map<String, List<String>> dataContainer) {
        Object[] columnDataParams = dataContainer.keySet().stream().map(k -> "$R[]{" + k + "}").toArray();
        reportSheet.addParam(ColRepeatParamParser.DEFAULT_TAG, dataColumnsTag != null ? dataColumnsTag : DEFAULT_DATA_COLUMNS_TAG, columnDataParams);
        dataContainer.entrySet()
                .forEach(e -> reportSheet.addParam(RowRepeatParamParser.DEFAULT_TAG, e.getKey(), e.getValue().toArray()));

        reportSheet.addParam(ColRepeatParamParser.DEFAULT_TAG, headersTag != null ? headersTag : DEFAULT_HEADERS_TAG, columnHeader.toArray());
        reportSheet.addParam(ColRepeatParamParser.DEFAULT_TAG, footersTag != null ? footersTag : DEFAULT_FOOTERS_TAG, columnFooter.toArray());
        boolean removeHeader = columnHeader.isEmpty();
        boolean removeFooter = columnFooter.isEmpty();
        if (removeHeader || removeFooter) {
            listeners.add(new ReportProcessAdaptor() {
                @Override
                public void postParse(Sheet sheet, SheetParser sheetParser, SheetData sheetData) {
                    if (!Objects.equals(sheetData.getSheetName(), reportSheet.getSheetName())) {
                        return;
                    }
                    if (removeHeader) {
                        // XXX 最初の行がフッターとは限らないだろう
                        sheet.removeRow(sheet.getRow(0));
                    }
                    if (removeFooter) {
                        // XXX 最終行がフッターとは限らないだろう
                        sheet.removeRow(sheet.getRow(sheet.getLastRowNum()));
                    }
                }
            });
        }

        reportBook.addReportSheet(reportSheet);
    }

    private List<String> exportFacet(FacesContext context, DataTable table, DataTableExporter.ColumnType columnType) {
        List<String> facetColumns = new ArrayList<>();

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
