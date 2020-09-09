package net.bis5.excella.primefaces.exporter;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.faces.component.UIComponent;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;

import org.apache.poi.ss.usermodel.Sheet;
import org.bbreak.excella.core.SheetData;
import org.bbreak.excella.core.SheetParser;
import org.bbreak.excella.core.exception.ParseException;
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

    private ReportBook reportBook;
    private String templateSheetName = "DATA";
    private static final String DATA_CONTAINER_KEY = "DATA_CONTAINER_KEY";
    private List<ReportProcessListener> listeners = new ArrayList<>();

    @Override
    protected void preExport(FacesContext context, ExportConfiguration config) throws IOException {
        reportBook = new ReportBook();
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

    @Override
    protected void doExport(FacesContext facesContext, DataTable table, ExportConfiguration config, int index)
            throws IOException {
        // 一度の出力で複数のDataTableが対象となった場合、このメソッドは引数のtable, indexを変えて複数回呼ばれる。
        // このExporterでは1DataTableを1シートに出力する方針とする。
        ReportSheet reportSheet = new ReportSheet(templateSheetName, templateSheetName + "_" + index);
        Map<String, List<String>> dataContainer = new HashMap<>();
        reportSheet.addParam(null, DATA_CONTAINER_KEY, dataContainer);

        // TODO header
        List<String> columnHeader = new ArrayList<>();

        if (config.isPageOnly()) {
            exportPageOnly(facesContext, table, reportSheet);
        } else if (config.isSelectionOnly()) {
            exportSelectionOnly(facesContext, table, reportSheet);
        } else {
            exportAll(facesContext, table, reportSheet);
        }

        // TODO footer
        List<String> columnFooter = new ArrayList<>();

        reportSheet.removeParam(null, DATA_CONTAINER_KEY);
        Object[] columnDataParams = dataContainer.keySet().stream().map(k -> "$R[]{" + k + "}").toArray();
        reportSheet.addParam(ColRepeatParamParser.DEFAULT_TAG, "dataColumns", columnDataParams);
        dataContainer.entrySet()
                .forEach(e -> reportSheet.addParam(RowRepeatParamParser.DEFAULT_TAG, e.getKey(), e.getValue().toArray()));

        reportSheet.addParam(ColRepeatParamParser.DEFAULT_TAG, "headers", columnHeader.toArray());
        reportSheet.addParam(ColRepeatParamParser.DEFAULT_TAG, "footers", columnFooter.toArray());
        boolean removeHeader = columnHeader.isEmpty();
        boolean removeFooter = columnFooter.isEmpty();
        if (removeHeader || removeFooter) {
            listeners.add(new ReportProcessAdaptor() {
                @Override
                public void postParse(Sheet sheet, SheetParser sheetParser, SheetData sheetData) throws ParseException {
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
        externalContext.setResponseContentType(detectContentType());

        externalContext.setResponseHeader("Content-disposition",
                ComponentUtils.createContentDisposition("attachment", config.getOutputFileName() + detectSuffix()));
        // TODO PF 9.0
        // addResponseCookie(context); // NOSONAR

        OutputStream out = externalContext.getResponseOutputStream();
        Files.copy(outputFile, out); // どうせOutputStreamに吐き出すんだから一時ファイル経由したくない気持ちもありつつ
        out.flush();
    }

    private String detectSuffix() {
        // TODO templatePathの拡張子から判断するのが妥当だろうか?
        return ".xlsx";
    }

    private String detectContentType() {
        // TODO templatePathの拡張子から判断するのが妥当だろうか?
        return "application/octet-stream";
    }

    private void reset() {
        reportBook = null;
    }

    private Path processExport() throws IOException {
        ReportProcessor processor = new ReportProcessor();
        reportBook.setTemplateFileURL(getClass().getResource("/DefaultTemplate.xlsx")); // TODO 変更可能にする。ExCella側がテンプレートとしてInputStreamを受け付けるのが良い気がする
        reportBook.setConfigurations(new ConvertConfiguration(ExcelExporter.FORMAT_TYPE));
        Path outputFile = Files.createTempFile(null, null);
        reportBook.setOutputFileName(outputFile.toString());
        try {
            processor.process(reportBook);
        } catch (Exception e) {
            throw new IllegalStateException("Unexpected exception", e); // XXX そもそもthrows Exception宣言しているのがおかしい
        }
        // ExCellaが拡張子を付けるので注意
        return Paths.get(outputFile.toString() + ".xlsx");
    }

}