package net.bis5.excella.primefaces.exporter;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.faces.component.UIComponent;
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

public class TreeTableExcellaExporter extends TreeTableExporter implements ExCellaExporter<TreeTable> {

    private String templateSheetName;

    private static final String TREE_LEVEL_KEY = "TREE_LEVEL_KEY";

    private static final String DEFAULT_DATA_COLUMNS_TAG = "dataColumns";

    private static final String DEFAULT_HEADERS_TAG = "headers";

    private static final String DEFAULT_FOOTERS_TAG = "footers";

    private String dataColumnsTag;

    private String headersTag;

    private String footersTag;

    private final List<ReportProcessListener> listeners = new ArrayList<>();

    private ReportBook reportBook;

    private TemplateType templateType;

    private URL templateUrl;

    private Path templatePath;

    /**
     * @deprecated Use {@link #builder()}
     */
    @Deprecated(forRemoval = true)
    public TreeTableExcellaExporter() {
        // deprecated
    }

    private TreeTableExcellaExporter(Builder builder) {
        this.templatePath = builder.templatePath;
        this.templateUrl = builder.templateUrl;
        this.templateSheetName = builder.templateSheetName;
        this.dataColumnsTag = builder.dataColumnsTag;
        this.headersTag = builder.headersTag;
        this.footersTag = builder.footersTag;
        this.templateUrl = builder.templateUrl;
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

        public TreeTableExcellaExporter build() {
            return new TreeTableExcellaExporter(this);
        }
    }

    public void setDataColumnsTag(String tag) {
        dataColumnsTag = tag;
    }

    public void setHeadersTag(String tag) {
        headersTag = tag;
    }

    public void setFootersTag(String tag) {
        footersTag = tag;
    }

    @Override
    public void setTemplateType(TemplateType type) {
        templateType = type;
    }

    @Override
    public TemplateType getTemplateType() {
        return templateType;
    }

    @Override
    public void setTemplatePath(Path path) {
        templatePath = path;
    }

    @Override
    public Path getTemplatePath() {
        return templatePath;
    }

    @Override
    public void setTemplateUrl(URL url) {
        templateUrl = url;
    }

    @Override
    public URL getTemplateUrl() {
        return templateUrl;
    }

    public void setTemplateSheetName(String name) {
        templateSheetName = name;
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
    public void preExport(FacesContext context, ExportConfiguration config) {
        ExCellaExporter.super.preExport(context, config);
    }

    @Override
    public void postExport(FacesContext context, ExportConfiguration config) throws IOException {
        ExCellaExporter.super.postExport(context, config);
    }

    @Override
    public OutputStream getOutputStream() {
        return super.getOutputStream();
    }

    @Override
    public void reset() {
        reportBook = null;
        listeners.clear();
    }

    @Override
    public void doExport(FacesContext facesContext, TreeTable table, ExportConfiguration config, int index)
            throws IOException {
        ExCellaExporter.super.doExport(facesContext, table, config, index);
    }

    @Override
    public void exportSelectionOnly(FacesContext context, TreeTable table, Object document) {
        super.exportSelectionOnly(context, table, document);
    }

    @Override
    public boolean isSelectionEmpty(TreeTable table) {
        return StringUtil.isEmpty(table.getSelectedRowKeysAsString());
    }

    @Override
    public void exportPageOnly(FacesContext context, TreeTable table, Object document) {
        super.exportPageOnly(context, table, document);
    }

    @Override
    public int getPageRows(TreeTable table) {
        int first = table.getFirst();
        int rows = table.getRows();
        return first + rows;
    }

    @Override
    public void exportAll(FacesContext context, TreeTable table, Object document) {
        super.exportAll(context, table, document);
    }

    @Override
    public int getTotalRows(TreeTable table) {
        TreeNode<?> root = table.getValue();
        return getTreeRowCount(root) - 1;
    }

    private String dataTag() {
        return nonNull(dataColumnsTag, DEFAULT_DATA_COLUMNS_TAG);
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

        String headersTagName = nonNull(headersTag, DEFAULT_HEADERS_TAG);
        String footersTagName = nonNull(footersTag, DEFAULT_FOOTERS_TAG);
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

        reportBook.addReportSheet(reportSheet);
    }

    @Override
    public List<String> exportFacet(FacesContext context, TreeTable table, ExCellaExporter.ColumnType columnType, ReportSheet reportSheet) {
        List<String> facetColumns = new ArrayList<>();

        ColumnGroup group = table.getColumnGroup(columnType.facet());
        if (group != null && group.isRendered()) {
            return exportColumnGroup(context, group, columnType, reportSheet);
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
    protected void exportRow(TreeTable table, Object document) {
        // exportRow(TreeTable, Object) is called for selectionOnly mode.
        Map<String, Object> requestMap = FacesContext.getCurrentInstance().getExternalContext().getRequestMap();
        TreeNode<?> node = (TreeNode<?>) requestMap.get(table.getVar());
        int level = 0;
        while(true) {
            if (node.getParent() == null) {
                break;
            }
            node = node.getParent();
            level++;
        }

        ReportSheet sheet = (ReportSheet) document;
        @SuppressWarnings("unchecked")
        Map<String, List<Object>> dataContainer = (Map<String, List<Object>>) sheet.getParam(null, DATA_CONTAINER_KEY);
        dataContainer.computeIfAbsent(TREE_LEVEL_KEY, ignore -> new ArrayList<>()).add(level);

        super.exportRow(table, document);
    }

    @Override
    protected void exportCells(TreeTable table, Object document) {
        Map<String, Object> requestMap = FacesContext.getCurrentInstance().getExternalContext().getRequestMap();
        // patch for PrimeFaces#9310
        String var = table.getVar(); // NOSONAR
        Object origVar = requestMap.get(var);
        if (origVar instanceof TreeNode) {
            requestMap.put(var, ((TreeNode<?>)origVar).getData());
        }

        String nodeVar = table.getNodeVar();
        Object origNodeVar = null;
        if (nodeVar != null) {
            origNodeVar = requestMap.get(nodeVar);
            if (origNodeVar == null && origVar instanceof TreeNode) {
                // May be exporting selection only mode
                requestMap.put(nodeVar, origVar);
            }
        }

        ReportSheet sheet = (ReportSheet) document;

        @SuppressWarnings("unchecked")
        Map<String, List<Object>> dataContainer = (Map<String, List<Object>>) sheet.getParam(null, DATA_CONTAINER_KEY);
        int colIndex = 0;
        for (UIColumn column : getExportableColumns(table)) {
            if (column instanceof DynamicColumn) {
                ((DynamicColumn)column).applyStatelessModel();
            }
            if (!isExportable(FacesContext.getCurrentInstance(), column)) {
                continue;
            }
            addCellValue(FacesContext.getCurrentInstance(), dataContainer, colIndex++, column);
        }

        if (nodeVar != null && origNodeVar == null) {
            requestMap.remove(nodeVar);
        }
        if (var != null && origVar != null) {
            requestMap.put(var, origVar);
        }
    }

    @Override
    public String exportValue(FacesContext context, UIComponent component) {
        String value = super.exportValue(context, component);
        if (component.getClass().getSimpleName().equals("UIInstructions")) {
            return exportUIInstructionsValue(context, component, value);
        }
        return value;
    }

    @Override
    public String exportColumnByFunction(FacesContext context, UIColumn column) {
        return super.exportColumnByFunction(context, column);
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
        return ExCellaExporter.super.getContentType();
    }

    @Override
    public String getFileExtension() {
        return ExCellaExporter.super.getFileExtension();
    }

}
