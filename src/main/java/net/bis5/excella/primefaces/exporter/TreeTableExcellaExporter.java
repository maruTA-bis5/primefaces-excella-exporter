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
import java.util.stream.Collectors;

import javax.faces.component.UIComponent;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellUtil;
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
import org.primefaces.component.export.ExportConfiguration;
import org.primefaces.component.treetable.TreeTable;
import org.primefaces.model.TreeNode;
import org.primefaces.util.ComponentUtils;

public class TreeTableExcellaExporter extends TreeTableExporter {

    private String templateSheetName;

    private static final String DEFAULT_TEMPLATE_SHEET_NAME = "DATA";

    private static final String DATA_CONTAINER_KEY = "DATA_CONTAINER_KEY";

    private static final String MAX_LEVEL_KEY = "MAX_LEVEL_KEY";

    private static final String TREE_LEVEL_KEY = "TREE_LEVEL_KEY";

    private static final String DEFAULT_DATA_COLUMNS_TAG = "dataColumns";

    private static final String DEFAULT_HEADERS_TAG = "headers";

    private static final String DEFAULT_FOOTERS_TAG = "footers";

    private static final URL DEFAULT_TEMPLATE_URL = TreeTableExcellaExporter.class.getResource("/DefaultTemplate.xlsx");

    private String dataColumnsTag;

    private String headersTag;

    private String footersTag;

    private final List<ReportProcessListener> listeners = new ArrayList<>();

    private ReportBook reportBook;

    private TemplateType templateType;

    private URL templateUrl;

    private Path templatePath;

    public void setDataColumnsTag(String tag) {
        dataColumnsTag = tag;
    }

    public void setHeadersTag(String tag) {
        headersTag = tag;
    }

    public void setFootersTag(String tag) {
        footersTag = tag;
    }

    public void setTemplateType(TemplateType type) {
        templateType = type;
    }

    public void setTemplatePath(Path path) {
        templatePath = path;
    }

    public void setTemplateUrl(URL url) {
        templateUrl = url;
    }

    public void setTemplateSheetName(String name) {
        templateSheetName = name;
    }

    @Override
    protected void preExport(FacesContext context, ExportConfiguration config) {
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

    private void reset() {
        reportBook = null;
        listeners.clear();
    }

    private URL getTemplateFileUrl() throws MalformedURLException {
        return nonNull(templatePath.toUri().toURL(), nonNull(templateUrl, DEFAULT_TEMPLATE_URL));
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

    @Override
    public void doExport(FacesContext facesContext, TreeTable table, ExportConfiguration config, int index)
            throws IOException {
        // 一度の出力で複数のTreeTableが対象となった場合、このメソッドは引数のtable, indexを変えて複数回呼ばれる。
        // このExporterでは1TreeTableを1シートに出力する方針とする。
        String sheetName = nonNull(templateSheetName, DEFAULT_TEMPLATE_SHEET_NAME);
        ReportSheet reportSheet = new ReportSheet(sheetName, sheetName + "_" + index);
        Map<String, List<String>> dataContainer = new LinkedHashMap<>();
        reportSheet.addParam(null, DATA_CONTAINER_KEY, dataContainer);

        List<String> columnHeader = exportFacet(facesContext, table, TreeTableExporter.ColumnType.HEADER);

        if (config.isSelectionOnly()) {
            exportSelectionOnly(facesContext, table, reportSheet);
        } else {
            exportAll(facesContext, table, reportSheet);
        }

        List<String> columnFooter = exportFacet(facesContext, table, TreeTableExporter.ColumnType.FOOTER);

        reportSheet.removeParam(null, DATA_CONTAINER_KEY);

        setExportParameters(reportSheet, columnHeader, columnFooter, dataContainer);
    }

    private void setExportParameters(ReportSheet reportSheet, List<String> columnHeader, List<String> columnFooter,
            Map<String, List<String>> dataContainer) {

        List<Integer> levels = nonNull(dataContainer.remove(TREE_LEVEL_KEY), Collections.<String>emptyList())
            .stream()
            .map(Integer::valueOf)
            .collect(Collectors.toList());
        Object[] columnDataParams = dataContainer.keySet().stream().map(k -> "$R[]{" + k + "}").toArray();
        reportSheet.addParam(ColRepeatParamParser.DEFAULT_TAG, nonNull(dataColumnsTag, DEFAULT_DATA_COLUMNS_TAG), columnDataParams);
        dataContainer.entrySet().forEach(e -> reportSheet.addParam(RowRepeatParamParser.DEFAULT_TAG, e.getKey(), e.getValue().toArray()));

        reportSheet.addParam(ColRepeatParamParser.DEFAULT_TAG, nonNull(headersTag, DEFAULT_HEADERS_TAG), columnHeader.toArray());
        reportSheet.addParam(ColRepeatParamParser.DEFAULT_TAG, nonNull(footersTag, DEFAULT_FOOTERS_TAG), columnFooter.toArray());
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

        listeners.add(new ReportProcessAdaptor() {
            @Override
            public void postParse(Sheet sheet, SheetParser sheetParser, SheetData sheetData) throws org.bbreak.excella.core.exception.ParseException {
                if (!reportSheet.getSheetName().equals(sheetData.getSheetName())) {
                    return;
                }
                int lastRowNum = sheet.getLastRowNum();

                for (int i = 1; i < lastRowNum; i++) {
                    int level = levels.get(i-1);
                    Row row = sheet.getRow(i);
                    if (row == null) {
                        continue;
                    }
                    Cell indexCell = row.getCell(0);
                    if (indexCell != null) {
                        CellUtil.setCellStyleProperty(indexCell, CellUtil.INDENTION, (short)level);
                    }
                }
            }
        });

        reportBook.addReportSheet(reportSheet);
    }

    private <T> T nonNull(T obj, T defaultValue) {
        return obj != null ? obj : defaultValue;
    }

    @Override
    protected void exportNode(TreeTable table, Object document, TreeNode node, int level) {
        ReportSheet reportSheet = (ReportSheet) document;
        int maxLevel = nonNull((Integer)reportSheet.getParam(null, MAX_LEVEL_KEY), 0);
        maxLevel = Math.max(maxLevel, level);
        reportSheet.addParam(null, MAX_LEVEL_KEY, maxLevel);

        @SuppressWarnings("unchecked")
        Map<String, List<String>> dataContainer = (Map<String, List<String>>) reportSheet.getParam(null, DATA_CONTAINER_KEY);
        dataContainer.computeIfAbsent(TREE_LEVEL_KEY, ignore -> new ArrayList<>())
            .add(String.valueOf(level));

        super.exportNode(table, document, node, level);
    }

    private List<String> exportFacet(FacesContext context, TreeTable table, ColumnType columnType) {
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

    private String getFacetColumnText(FacesContext context, UIColumn column, ColumnType columnType) {
        UIComponent facet = column.getFacet(columnType.facet());
        String text;
        if (columnType == TreeTableExporter.ColumnType.HEADER) {
            text = column.getExportHeaderValue() != null ? column.getExportHeaderValue() : column.getHeaderText();
        } else if (columnType == TreeTableExporter.ColumnType.FOOTER) {
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
    protected void exportCells(TreeTable table, Object document, TreeNode node) {
        ReportSheet sheet = (ReportSheet) document;

        @SuppressWarnings("unchecked")
        Map<String, List<String>> dataContainer = (Map<String, List<String>>) sheet.getParam(null, DATA_CONTAINER_KEY);
        int colIndex = 0;
        for (UIColumn column : table.getColumns()) {
            if (column instanceof DynamicColumn) {
                ((DynamicColumn)column).applyStatelessModel();
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
            components.stream()
                .map(c -> exportValue(context, c))
                .map(v -> v == null ? "" : v)
                .forEach(builder::append);
            exportValue = builder.toString();
        }

        List<String> values = dataContainer.computeIfAbsent(columnKey, ignore -> new ArrayList<>());
        values.add(exportValue);
    }

}
