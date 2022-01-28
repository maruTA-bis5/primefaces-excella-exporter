package net.bis5.excella.primefaces.exporter;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
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
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.el.ValueExpression;
import javax.faces.component.UIComponent;
import javax.faces.component.UIParameter;
import javax.faces.component.ValueHolder;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;

import org.apache.commons.math3.util.Pair;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
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
import org.primefaces.PrimeFaces;
import org.primefaces.component.api.DynamicColumn;
import org.primefaces.component.api.UIColumn;
import org.primefaces.component.celleditor.CellEditor;
import org.primefaces.component.export.ExportConfiguration;
import org.primefaces.component.link.Link;
import org.primefaces.component.treetable.TreeTable;
import org.primefaces.component.treetable.export.TreeTableExporter;
import org.primefaces.model.TreeNode;
import org.primefaces.util.ComponentUtils;

import net.bis5.excella.primefaces.exporter.convert.ExporterConverter;

public class TreeTableExcellaExporter extends TreeTableExporter {

    private String templateSheetName;

    private static final String DEFAULT_TEMPLATE_SHEET_NAME = "DATA";

    private static final String DATA_CONTAINER_KEY = "DATA_CONTAINER_KEY";

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
        return nonNull(templatePath != null ? templatePath.toUri().toURL() : null, nonNull(templateUrl, DEFAULT_TEMPLATE_URL));
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
        if (!PrimeFaces.current().isAjaxRequest()) {
            // overwrite response header by actual information
            ExternalContext externalContext = context.getExternalContext();
            externalContext.setResponseContentType(templateType.getContentType());

            externalContext.setResponseHeader("Content-disposition",
                ComponentUtils.createContentDisposition("attachment", config.getOutputFileName() + templateType.getSuffix()));
        }

        OutputStream out = getOutputStream();
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
        Map<String, List<Object>> dataContainer = new LinkedHashMap<>();
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

    private Map<String, ValueType> detectValueTypes(Map<String, List<Object>> dataContainer) {
        Map<String, ValueType> valueTypes = new HashMap<>();
        for(Entry<String, List<Object>> entry : dataContainer.entrySet()) {
            String key = entry.getKey();
            List<Object> values = entry.getValue();
            ValueType type = detectValueType(values);
            valueTypes.put(key, type);
        }
        return valueTypes;
    }

    private Entry<String, List<Object>> normalizeValues(Entry<String, List<Object>> entry) {
        // excella-coreのPoiUtil#setCellValueが特定の型以外はnoopなので予め型変換しておく
        List<Object> rawValues = entry.getValue();
        List<Object> normalizedValues = rawValues.stream()
            .map(this::normalizeValue)
            .collect(Collectors.toList());
        return new AbstractMap.SimpleEntry<>(entry.getKey(), normalizedValues);
    }

    protected Object normalizeValue(Object rawValue) {
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

    private void setExportParameters(ReportSheet reportSheet, List<String> columnHeader, List<String> columnFooter,
            Map<String, List<Object>> dataContainer) {

        List<Integer> levels = nonNull(dataContainer.remove(TREE_LEVEL_KEY), Collections.<Object>emptyList())
            .stream()
            .map(Integer.class::cast)
            .collect(Collectors.toList());
        Object[] columnDataParams = dataContainer.keySet().stream().map(k -> "$R[]{" + k + "}").toArray();
        reportSheet.addParam(ColRepeatParamParser.DEFAULT_TAG, nonNull(dataColumnsTag, DEFAULT_DATA_COLUMNS_TAG), columnDataParams);

        Map<String, ValueType> valueTypes = detectValueTypes(dataContainer);
        dataContainer.entrySet()
            .stream()
            .map(this::normalizeValues)
            .forEach(e -> reportSheet.addParam(RowRepeatParamParser.DEFAULT_TAG, e.getKey(), e.getValue().toArray()));

        reportSheet.addParam(ColRepeatParamParser.DEFAULT_TAG, nonNull(headersTag, DEFAULT_HEADERS_TAG), columnHeader.toArray());
        reportSheet.addParam(ColRepeatParamParser.DEFAULT_TAG, nonNull(footersTag, DEFAULT_FOOTERS_TAG), columnFooter.toArray());
        listeners.add(new ReportProcessAdaptor() {
            @Override
            public void preBookParse(Workbook workbook, ReportBook reportBook) {
                initStyles(workbook);
            }

            @Override
            public void postParse(Sheet sheet, SheetParser sheetParser, SheetData sheetData) throws org.bbreak.excella.core.exception.ParseException {
                if (!reportSheet.getSheetName().equals(sheetData.getSheetName())) {
                    return;
                }

                for (int i = 1; i <= levels.size(); i++) {
                    int level = levels.get(i-1);
                    Row row = sheet.getRow(i);
                    if (row == null) {
                        continue;
                    }
                    Cell indexCell = row.getCell(0);
                    if (indexCell != null) {
                        CellUtil.setCellStyleProperty(indexCell, CellUtil.INDENTION, (short)level - 1);
                    }
                }

                if (dataContainer.isEmpty()) {
                    return;
                }
                Object[] headers = (Object[]) reportSheet.getParam(RowRepeatParamParser.DEFAULT_TAG, "header0");
                int headerSize = 1;
                if (headers != null) {
                    headerSize = headers.length;
                }
                for (Entry<String, ValueType> entry : valueTypes.entrySet()) {
                    String columnTag = getColumnTag(entry.getKey());
                    ValueType valueType = entry.getValue();
                    if (valueType == null) {
                        continue;
                    }
                    CellStyle style = styles.get(valueType);
                    int colIndex = Arrays.asList(columnDataParams).indexOf(columnTag);
                    IntStream.rangeClosed(headerSize, nonNull(dataContainer.get(entry.getKey()), Collections.emptyList()).size())
                        .mapToObj(sheet::getRow)
                        .filter(Objects::nonNull)
                        .map(r -> r.getCell(colIndex))
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

        });

        reportBook.addReportSheet(reportSheet);
    }

    public enum ValueType {
        YEAR_MONTH, DATE, DATE_TIME, TIME, DECIMAL, INTEGER
    }

    private Map<ValueType, CellStyle> styles = new EnumMap<>(ValueType.class);

    private void initStyles(Workbook workbook) {
        DataFormat dataFormat = workbook.createDataFormat();

        CellStyle yearMonthStyle = workbook.createCellStyle();
        yearMonthStyle.setDataFormat(dataFormat.getFormat("yyyy/m"));
        styles.put(ValueType.YEAR_MONTH, yearMonthStyle);

        CellStyle dateStyle = workbook.createCellStyle();
        dateStyle.setDataFormat((short)0xe);
        styles.put(ValueType.DATE, dateStyle);

        CellStyle dateTimeStyle = workbook.createCellStyle();
        dateTimeStyle.setDataFormat((short)0x16);
        styles.put(ValueType.DATE_TIME, dateTimeStyle);

        CellStyle timeStyle = workbook.createCellStyle();
        timeStyle.setDataFormat((short)0x14);
        styles.put(ValueType.TIME, timeStyle);

        CellStyle decimalStyle = workbook.createCellStyle();
        decimalStyle.setDataFormat((short)0x4);
        styles.put(ValueType.DECIMAL, decimalStyle);

        CellStyle integerStyle = workbook.createCellStyle();
        integerStyle.setDataFormat((short)0x3);
        styles.put(ValueType.INTEGER, integerStyle);
    }

    private final Pattern timePattern = Pattern.compile("^[0-9]+:[0-9][0-9]$");

    private ValueType detectValueType(List<Object> values) {
        Set<ValueType> types = values.stream()
            .map(this::detectValueType)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

        if (types.isEmpty()) {
            return null;
        }
        if (types.contains(ValueType.INTEGER) && types.contains(ValueType.DECIMAL)) {
            return ValueType.DECIMAL;
        }
        if (types.contains(ValueType.DATE) && types.contains(ValueType.DATE_TIME)) {
            return ValueType.DATE_TIME;
        }
        return types.iterator().next();
    }

    protected ValueType detectValueType(Object value) {
        if (value instanceof LocalDateTime || (value instanceof Date && hasTime((Date)value)) || (value instanceof Calendar && hasTime((Calendar)value))) {
            return ValueType.DATE_TIME;
        }
        if (value instanceof LocalDate || value instanceof Date || value instanceof Calendar) {
            return ValueType.DATE;
        }
        if (value instanceof LocalTime || (value instanceof String && timePattern.matcher((String)value).matches())) {
            return ValueType.TIME;
        }
        if (value instanceof YearMonth) {
            return ValueType.YEAR_MONTH;
        }
        if (value instanceof Number) {
            if (value instanceof Long || value instanceof Integer) {
                return ValueType.INTEGER;
            } else if (value instanceof BigDecimal) {
                BigDecimal bigDecimal = (BigDecimal)value;
                if (bigDecimal.compareTo(BigDecimal.valueOf(bigDecimal.longValue())) == 0) {
                    return ValueType.INTEGER;
                } else {
                    return ValueType.DECIMAL;
                }
            } else {
                return ValueType.DECIMAL;
            }
        }
        return null;
    }

    private boolean hasTime(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        return hasTime(cal);
    }

    private boolean hasTime(Calendar cal) {
        return cal.get(Calendar.HOUR_OF_DAY) != 0 && cal.get(Calendar.MINUTE) != 0 && cal.get(Calendar.SECOND) != 0;
    }

    private <T> T nonNull(T obj, T defaultValue) {
        return obj != null ? obj : defaultValue;
    }

    protected List<String> exportFacet(FacesContext context, TreeTable table, ColumnType columnType) {
        List<String> facetColumns = new ArrayList<>();

        for (UIColumn column : table.getColumns()) {
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

    protected boolean isExportable(FacesContext context, UIColumn column) {
        return column.isRendered() && column.isExportable();
    }

    protected String getFacetColumnText(FacesContext context, UIColumn column, ColumnType columnType) {
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
    protected void exportRow(FacesContext context, TreeTable table, Object document, int rowIndex) {
        Pair<TreeNode<?>, Integer> currentNode = traverseTreeNode(table.getValue(), rowIndex);
        Map<String, Object> requestMap = context.getExternalContext().getRequestMap();
        String nodeVar = table.getNodeVar();
        Object origNodeVar = null;
        if (nodeVar != null) {
            origNodeVar = requestMap.get(nodeVar);
            requestMap.put(nodeVar, currentNode.getFirst());
        }

        super.exportRow(context, table, document, rowIndex);

        ReportSheet sheet = (ReportSheet) document;
        @SuppressWarnings("unchecked")
        Map<String, List<Object>> dataContainer = (Map<String, List<Object>>) sheet.getParam(null, DATA_CONTAINER_KEY);
        dataContainer.computeIfAbsent(TREE_LEVEL_KEY, ignore -> new ArrayList<>())
            .add(currentNode.getValue());

        if (nodeVar != null) {
            if (origNodeVar != null) {
                requestMap.put(nodeVar, origNodeVar);
            } else {
                requestMap.remove(nodeVar);
            }
        }
    }

    @Override
    protected void exportCells(TreeTable table, Object document) {
        ReportSheet sheet = (ReportSheet) document;

        @SuppressWarnings("unchecked")
        Map<String, List<Object>> dataContainer = (Map<String, List<Object>>) sheet.getParam(null, DATA_CONTAINER_KEY);
        int colIndex = 0;
        for (UIColumn column : table.getColumns()) {
            if (column instanceof DynamicColumn) {
                ((DynamicColumn)column).applyStatelessModel();
            }
            if (!isExportable(FacesContext.getCurrentInstance(), column)) {
                continue;
            }
            addCellValue(FacesContext.getCurrentInstance(), dataContainer, colIndex++, column);
        }

    }

    protected void addCellValue(FacesContext context, Map<String, List<Object>> dataContainer, int colIndex,
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
            components.stream()
                .filter(UIComponent::isRendered)
                .map(c -> exportValue(context, c))
                .map(v -> v == null ? "" : v)
                .forEach(builder::append);
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
            Object objValue = ve.getValue(context.getELContext());
            return objValue != null ? String.valueOf(ve.getValue(context.getELContext())) : null;
        }
        return value;
    }

    public Object exportObjectValue(FacesContext context, UIComponent component) {
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

    protected static Pair<TreeNode<?>, Integer> traverseTreeNode(TreeNode<?> node, int dataRowIndex) {
        return Objects.requireNonNull(traverseTreeNode(node, new MutableInt(dataRowIndex + 1), 0), () -> "Node for dataRowIndex " + dataRowIndex + " is not found");
    }

    protected static Pair<TreeNode<?>, Integer> traverseTreeNode(TreeNode<?> node, MutableInt rowIndex, int level) {

        int index = rowIndex.getValue();
        rowIndex.decrement();
        if (index <= 0) {
            return Pair.create(node, level);
        }

        if (node.getChildren() != null) {
            Pair<TreeNode<?>, Integer> returnNode = null;
            for (TreeNode<?> childNode : node.getChildren()) {
                returnNode = traverseTreeNode(childNode, rowIndex, level + 1);
                if (returnNode != null) {
                    break;
                }
            }
            return returnNode;
        }
        else {
            return null;
        }

    }

    private static class MutableInt {

        private int value;

        public MutableInt(int value) {
            super();
            this.value = value;
        }

        public int getValue() {
            return this.value;
        }

        public void decrement() {
            value--;
        }
    }

    @Override
    public String getContentType() {
        return "application/octet-stream";
    }

    @Override
    public String getFileExtension() {
        String url;
        try {
            url = getTemplateFileUrl().toString();
        } catch (MalformedURLException e) {
            throw new IllegalStateException(e);
        }
        return url.substring(url.lastIndexOf("."), url.length());
    }

}
