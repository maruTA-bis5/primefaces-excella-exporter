package net.bis5.excella.primefaces.exporter;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.faces.context.FacesContext;

import org.apache.commons.math3.util.Pair;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellUtil;
import org.bbreak.excella.core.SheetData;
import org.bbreak.excella.core.SheetParser;
import org.bbreak.excella.core.util.StringUtil;
import org.bbreak.excella.reports.listener.ReportProcessListener;
import org.bbreak.excella.reports.model.ReportBook;
import org.bbreak.excella.reports.model.ReportSheet;
import org.bbreak.excella.reports.tag.ColRepeatParamParser;
import org.bbreak.excella.reports.tag.RowRepeatParamParser;
import org.primefaces.component.api.DynamicColumn;
import org.primefaces.component.api.UIColumn;
import org.primefaces.component.columngroup.ColumnGroup;
import org.primefaces.component.export.ExportConfiguration;
import org.primefaces.component.treetable.TreeTable;
import org.primefaces.component.treetable.export.TreeTableExporter;
import org.primefaces.model.TreeNode;
import org.primefaces.util.LangUtils;

public class TreeTableExcellaExporter extends TreeTableExporter<ReportBook, ExCellaExporterOptions> implements ExCellaExporter<TreeTable> {

    private static final String TREE_LEVEL_KEY = "TREE_LEVEL_KEY";

    private static final String DEFAULT_DATA_COLUMNS_TAG = "dataColumns";

    private static final String DEFAULT_HEADERS_TAG = "headers";

    private static final String DEFAULT_FOOTERS_TAG = "footers";

    private final List<ReportProcessListener> listeners = new ArrayList<>();

    private TemplateType templateType;

    private ReportSheet currentSheet;

    public TreeTableExcellaExporter() {
        super(new ExCellaExporterOptions(), ALL_FACETS, true);
    }

    private TreeTableExcellaExporter(Builder builder) {
        super(builder.options, ALL_FACETS, true);
        setTemplatePath(builder.templatePath);
        setTemplateUrl(builder.templateUrl);
        setTemplateSheetName(builder.templateSheetName);
        setDataColumnsTag(builder.dataColumnsTag);
        setHeadersTag(builder.headersTag);
        setFootersTag(builder.footersTag);
    }

    /**
     * @deprecated Use {@link ExCellaExporterOptions}. This constructor will be removed in 5.0.0.
     */
    @Deprecated(forRemoval = true)
    public static Builder builder() {
        return new Builder();
    }

    /**
     * @deprecated Use {@link ExCellaExporterOptions}. This constructor will be removed in 5.0.0.
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

        public TreeTableExcellaExporter build() {
            return new TreeTableExcellaExporter(this);
        }
    }

    /**
     * @deprecated Use {@link #builder()}. This method will be removed in 5.0.0.
     */
    @Deprecated(forRemoval = true)
    public void setDataColumnsTag(String tag) {
        getExporterOptions().setDataColumnsTag(tag);
    }

    private String getDataColumnsTag() {
        return getExporterOptions().getDataColumnsTag();
    }

    /**
     * @deprecated Use {@link #builder()}. This method will be removed in 5.0.0.
     */
    @Deprecated(forRemoval = true)
    public void setHeadersTag(String tag) {
        getExporterOptions().setHeadersTag(tag);
    }

    private String getHeadersTag() {
        return getExporterOptions().getHeadersTag();
    }

    /**
     * @deprecated Use {@link #builder()}. This method will be removed in 5.0.0.
     */
    @Deprecated(forRemoval = true)
    public void setFootersTag(String tag) {
        getExporterOptions().setFootersTag(tag);
    }

    private String getFootersTag() {
        return getExporterOptions().getFootersTag();
    }

    @Override
    public void setTemplateType(TemplateType type) {
        templateType = type;
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
    public void setTemplatePath(Path path) {
        getExporterOptions().setTemplatePath(path);
    }

    /**
     * @deprecated Use {@link #builder()}. This method will be removed in 5.0.0.
     * @implNote Make this getter private in 5.0.0.
     */
    @Deprecated(forRemoval = true)
    @Override
    public Path getTemplatePath() {
        return getExporterOptions().getTemplatePath();
    }

    /**
     * @deprecated Use {@link #builder()}. This method will be removed in 5.0.0.
     */
    @Deprecated(forRemoval = true)
    @Override
    public void setTemplateUrl(URL url) {
        getExporterOptions().setTemplateUrl(url);
    }

    /**
     * @deprecated Use {@link #builder()}. This method will be removed in 5.0.0.
     * @implNote Make this getter private in 5.0.0.
     */
    @Deprecated(forRemoval = true)
    @Override
    public URL getTemplateUrl() {
        return getExporterOptions().getTemplateUrl();
    }

    /**
     * @deprecated Use {@link #builder()}. This method will be removed in 5.0.0.
     */
    @Deprecated(forRemoval = true)
    public void setTemplateSheetName(String name) {
        getExporterOptions().setTemplateSheetName(name);
    }

    /**
     * @deprecated Use {@link #builder()}. This method will be removed in 5.0.0.
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
    public OutputStream os() { // change visibility
        return super.os();
    }

    @Override
    public void reset() {
        listeners.clear();
    }

    @Override
    public void exportTable(FacesContext facesContext, TreeTable table, int index) throws IOException {
        ExCellaExporter.super.exportTable(facesContext, table, index);
    }

    // copied from TreeTableExporter
    @Override
    public void exportSelectionOnly(FacesContext context, TreeTable table) {
        Object selection = table.getSelection();
        String var = table.getVar(); // NOSONAR
        String nodeVar = table.getNodeVar(); // added

        if (selection != null) {
            List<String> selectedRowKeys = Arrays.asList(table.getSelectedRowKeysAsString().split(",")); // added
            Map<String, Object> requestMap = context.getExternalContext().getRequestMap();

            if (selection.getClass().isArray()) {
                int size = Array.getLength(selection);

                for (int i = 0; i < size; i++) {
                    // begin: modified
                    var data = Array.get(selection, i);
                    requestMap.put(var, data);
                    var node = findNodeByRowKey(table.getValue(), selectedRowKeys.get(i));
                    if (LangUtils.isNotEmpty(nodeVar)) {
                        requestMap.put(nodeVar, node);
                    }
                    exportSelectedRow(context, table, node);
                    // end: modified
                }
            }
            else if (Collection.class.isAssignableFrom(selection.getClass())) {
                // begin: modified
                var selectionList = new ArrayList<Object>((Collection<?>)selection);
                for (int i = 0; i < selectionList.size(); i++) {
                    Object obj = selectionList.get(i);
                    var node = findNodeByRowKey(table.getValue(), selectedRowKeys.get(i));
                    if (LangUtils.isNotEmpty(nodeVar)) {
                        requestMap.put(nodeVar, node);
                    }
                    // end: modified

                    if (obj instanceof TreeNode) {
                        requestMap.put(var, node.getData());
                    }
                    else {
                        requestMap.put(var, obj);
                    }
                    exportSelectedRow(context, table, node); // modified
                }
            }
            else {
                var node = findNodeByRowKey(table.getValue(), selectedRowKeys.get(0));
                requestMap.put(var, selection);
                if (LangUtils.isNotEmpty(nodeVar)) {
                    requestMap.put(nodeVar, node);
                }
                exportSelectedRow(context, table, node); // modified
            }
        }
    }


    private void exportSelectedRow(FacesContext context, TreeTable table, TreeNode<?> node) {
        int level = 0;
        TreeNode<?> parent = node.getParent();
        while (parent != null) {
            level++;
            parent = parent.getParent();
        }
        exportRow(context, table, node, level);
    }

    @Override
    public boolean isSelectionEmpty(TreeTable table) {
        return StringUtil.isEmpty(table.getSelectedRowKeysAsString());
    }

    @Override
    public void exportPageOnly(FacesContext context, TreeTable table) {
        super.exportPageOnly(context, table);
    }

    @Override
    public int getPageRows(TreeTable table) {
        int first = table.getFirst();
        int rows = table.getRows();
        return first + rows;
    }

    @Override
    public void exportAll(FacesContext context, TreeTable table) {
        super.exportAll(context, table);
    }

    @Override
    public int getTotalRows(TreeTable table) {
        TreeNode<?> root = table.getValue();
        return getTreeRowCount(root) - 1;
    }

    private String dataTag() {
        return nonNull(getDataColumnsTag(), DEFAULT_DATA_COLUMNS_TAG);
    }

    @Override
    public void setExportParameters(ReportSheet reportSheet, List<String> columnHeader, List<String> columnFooter,
            Map<String, List<Object>> dataContainer) {

        List<Integer> levels = nonNull(dataContainer.remove(TREE_LEVEL_KEY), Collections.<Object>emptyList())
            .stream()
            .map(Integer.class::cast)
            .collect(Collectors.toList());
        Object[] columnDataParams = dataContainer.keySet().stream().map(k -> "$R[]{" + k + "}").toArray();
        reportSheet.addParam(ColRepeatParamParser.DEFAULT_TAG, dataTag(), columnDataParams);

        dataContainer.entrySet()
            .stream()
            .map(this::normalizeValues)
            .forEach(e -> reportSheet.addParam(RowRepeatParamParser.DEFAULT_TAG, e.getKey(), e.getValue().toArray()));

        int columnSize = columnHeader.size();

        String headersTagName = nonNull(getHeadersTag(), DEFAULT_HEADERS_TAG);
        String footersTagName = nonNull(getFootersTag(), DEFAULT_FOOTERS_TAG);
        reportSheet.addParam(ColRepeatParamParser.DEFAULT_TAG, headersTagName, columnHeader.toArray());
        reportSheet.addParam(ColRepeatParamParser.DEFAULT_TAG, footersTagName, columnFooter.toArray());
        listeners.add(new StyleUpdateListener(reportSheet, dataContainer, dataTag(), headersTagName, footersTagName, columnSize, columnDataParams) {
            @Override
            public void postParse(Sheet sheet, SheetParser sheetParser, SheetData sheetData) {
                super.postParse(sheet, sheetParser, sheetData);

                if (!reportSheet.getSheetName().equals(sheetData.getSheetName())) {
                    return;
                }

                if (dataContainer.isEmpty()) {
                    return;
                }

                for (int i = 1; i <= levels.size(); i++) {
                    int level = levels.get(i-1);
                    Row row = sheet.getRow(dataRowOffset(i-1));
                    if (row == null) {
                        continue;
                    }
                    Cell indexCell = row.getCell(dataColOffset(0));
                    if (indexCell != null) {
                        CellUtil.setCellStyleProperty(indexCell, CellUtil.INDENTION, (short)level - 1);
                    }
                }
            }
        });

        getDocument().addReportSheet(reportSheet);
    }

    @Override
    public void exportFacet(FacesContext context, TreeTable table, ExCellaExporter.ColumnType columnType, ReportSheet reportSheet, List<String> facetColumns) {
        ColumnGroup group = table.getColumnGroup(columnType.facet());
        if (group != null && group.isRendered()) {
            exportColumnGroup(context, group, columnType, reportSheet, facetColumns);
            return;
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

    @Override
    protected void exportRow(FacesContext context, TreeTable table, int rowIndex) {
        Map<String, Object> requestMap = context.getExternalContext().getRequestMap();
        TreeNode<?> currentNode = traverseTreeNode(table.getValue(), rowIndex).getKey();
        var nodeVar = table.getNodeVar();
        if (LangUtils.isNotEmpty(nodeVar)) {
            requestMap.put(nodeVar, currentNode);
        }

        int level = 0;
        TreeNode<?> node = currentNode;
        while(true) {
            if (node.getParent() == null) {
                break;
            }
            node = node.getParent();
            level++;
        }

        exportRow(context, table, currentNode, level, rowIndex);

        if (LangUtils.isNotEmpty(nodeVar)) {
            requestMap.remove(nodeVar);
        }
    }

    private void exportRow(FacesContext context, TreeTable table, TreeNode<?> node, int level) {
        exportRow(context, table, node, level, -1);
    }

    private void exportRow(FacesContext context, TreeTable table, TreeNode<?> node, int level, int rowIndex) {
        Map<String, List<Object>> dataContainer = getDataContainer(currentSheet);
        dataContainer.computeIfAbsent(TREE_LEVEL_KEY, ignore -> new ArrayList<>()).add(level);

        if (rowIndex == -1) {
            // selectionOnly mode - detect rowIndex
            rowIndex = resolveRowIndex(table, node);
        }

        super.exportRow(context, table, rowIndex);
    }

    private int resolveRowIndex(TreeTable table, TreeNode<?> node) {
        TreeNode<?> root = table.getValue();
        var rowIndex = new MutableInt(-2);
        resolveRowIndex(root, node, rowIndex);

        return rowIndex.getValue() - 1;
    }

    private void resolveRowIndex(TreeNode<?> currentNode, TreeNode<?> targetNode, MutableInt rowIndex) {
        if (currentNode.getRowKey().equals(targetNode.getRowKey())) {
            return;
        }
        if (currentNode.getChildren() != null) {
            for (TreeNode<?> childNode : currentNode.getChildren()) {
                rowIndex.increment();
                resolveRowIndex(childNode, targetNode, rowIndex);
            }
        }
    }

    protected static TreeNode<?> findNodeByRowKey(TreeNode<?> node, String rowKey) {
        if (node.getRowKey().equals(rowKey)) {
            return node;
        }
        if (node.getChildren() != null) {
            for (TreeNode<?> childNode : node.getChildren()) {
                TreeNode<?> foundNode = findNodeByRowKey(childNode, rowKey);
                if (foundNode != null) {
                    return foundNode;
                }
            }
        }
        throw new IllegalArgumentException("Node for rowKey " + rowKey + " is not found");
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

        public void increment() {
            value++;
        }
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
    public void setCurrentSheet(ReportSheet reportSheet) {
        this.currentSheet = reportSheet;
    }

    @Override
    public ExportConfiguration getExportConfiguration() {
        return exportConfiguration;
    }

    @Override
    protected void exportCellValue(FacesContext context, TreeTable table, UIColumn col, String text, int index) {
        Map<String, List<Object>> dataContainer = getDataContainer(currentSheet);
        addCellValue(context, dataContainer, table, index, col);
    }

    @Override
    protected ReportBook createDocument(FacesContext context) throws IOException {
        return new ReportBook();
    }

}
