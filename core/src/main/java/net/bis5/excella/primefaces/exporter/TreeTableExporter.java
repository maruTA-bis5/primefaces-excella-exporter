package net.bis5.excella.primefaces.exporter;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.util.Collection;
import java.util.List;

import javax.faces.FacesException;
import javax.faces.component.UIComponent;
import javax.faces.component.visit.VisitCallback;
import javax.faces.component.visit.VisitContext;
import javax.faces.component.visit.VisitResult;
import javax.faces.context.FacesContext;

import org.primefaces.component.api.UIColumn;
import org.primefaces.component.datatable.DataTable;
import org.primefaces.component.datatable.export.DataTableExporter;
import org.primefaces.component.export.ExportConfiguration;
import org.primefaces.component.export.Exporter;
import org.primefaces.component.treetable.TreeTable;
import org.primefaces.model.TreeNode;

public abstract class TreeTableExporter implements Exporter<TreeTable> {

    private OutputStream outputStream;

    static class ExportLogicDelegate extends DataTableExporter {

        @Override
        protected void exportCells(DataTable table, Object document) {
            // NOOP
        }

        @Override
        protected void doExport(FacesContext facesContext, DataTable table, ExportConfiguration config, int index)
                throws IOException {
            // NOOP
        }

        // change access modifier

        @Override
        public String exportColumnByFunction(FacesContext context, UIColumn column) {
            return super.exportColumnByFunction(context, column);
        }

        @Override
        public String getContentType() {
            // NOOP
            return null;
        }

        @Override
        public String getFileExtension() {
            // NOOP
            return null;
        }
    }
    private final ExportLogicDelegate delegate = new ExportLogicDelegate();

    protected enum ColumnType {
        HEADER("header"),
        FOOTER("footer");

        private final String facet;

        ColumnType(String facet) {
            this.facet = facet;
        }

        public String facet() {
            return facet;
        }

        @Override
        public String toString() {
            return facet;
        }
    }

    @Override
    public void export(FacesContext facesContext, List<TreeTable> tables, OutputStream outputStream,
            ExportConfiguration config) throws IOException {
        this.outputStream = outputStream;

        preExport(facesContext, config);

        int index = 0;
        for (TreeTable table : tables) {
            TreeTableVisitCallback visitCallback = new TreeTableVisitCallback(table, config ,index);
            int nbTables = visitCallback.invoke(facesContext);
            index += nbTables;
        }
        postExport(facesContext, config);

        this.outputStream = null;
    }

    protected void preExport(FacesContext context, ExportConfiguration config) {
        // NOOP
    }

    protected void postExport(FacesContext context, ExportConfiguration config) throws IOException {
        // NOOP
    }

    public void doExport(FacesContext facesContext, TreeTable target, ExportConfiguration config, int index) throws IOException {
        // TODO
    }

    private class TreeTableVisitCallback implements VisitCallback {

        private final TreeTable target;
        private final ExportConfiguration config;
        private int index;
        private int counter;

        public TreeTableVisitCallback(TreeTable target, ExportConfiguration config, int index) {
            this.target = target;
            this.config = config;
            this.index = index;
        }

        @Override
        public VisitResult visit(VisitContext context, UIComponent component) {
            if (target == component) {
                try {
                    doExport(context.getFacesContext(), target, config, index);
                    index++;
                    counter++;
                } catch (IOException e) {
                    throw new FacesException(e);
                }
            }
            return VisitResult.ACCEPT;
        }

        /**
         * Returns number of tables exported
         * @param context faces context
         * @return number of tables exporter
         */
        public int invoke(FacesContext context) {
            // ComponentUtils.invokeOnClosestIteratorParent(target, p -> {
                VisitContext visitContext = VisitContext.createVisitContext(context);
                // p.visitTree(visitContext, this);
            // }, true);
            visit(visitContext, target);

            return counter;
        }
    }

    public String exportValue(FacesContext context, UIComponent component) {
        return delegate.exportValue(context, component);
    }

    protected void exportAll(FacesContext context, TreeTable table, Object document) {
        int first = table.getFirst();

        TreeNode root = table.getValue();
        if (root == null) {
            return;
        }
        List<TreeNode> nodes = root.getChildren();
        nodes.forEach(n -> exportNode(table, document, n, 0));

        //restore
        table.setFirst(first);
        table.setRowKey(null);
    }

    protected void exportNode(TreeTable table, Object document, TreeNode node, int level) {
        table.setRowKey(node.getRowKey());
        preNodeExport(table, document, node);
        exportCells(table, document, node);
        postNodeExport(table, document, node);
        node.getChildren().forEach(n -> exportNode(table, document, n, level + 1));
    }

    protected void postNodeExport(TreeTable table, Object document, TreeNode node) {
        // NOOP
    }

    protected abstract void exportCells(TreeTable table, Object document, TreeNode node);

    protected void preNodeExport(TreeTable table, Object document, TreeNode node) {
        // NOOP
    }

    protected void exportSelectionOnly(FacesContext facesContext, TreeTable table, Object document) {
        Object selection = table.getSelection();

        if (selection == null) {
            return;
        }
        if (selection.getClass().isArray()) {
            int size = Array.getLength(selection);

            for (int i = 0; i < size; i++) {
                TreeNode node = (TreeNode)Array.get(selection, i);
                exportNode(table, document, node, 0);
            }
        } else if (Collection.class.isAssignableFrom(selection.getClass())) {
            @SuppressWarnings("unchecked")
            Collection<? extends TreeNode> nodes = (Collection<? extends TreeNode>)selection;
            nodes.forEach(n -> exportNode(table, document, n, 0));
        } else {
            assert selection instanceof TreeNode;
            TreeNode node = (TreeNode) selection;
            exportNode(table, document, node, 0);
        }
    }

    protected String exportColumnByFunction(FacesContext context, UIColumn column) {
        return delegate.exportColumnByFunction(context, column);
    }

    protected OutputStream getOutputStream() {
        return outputStream;
    }
}
