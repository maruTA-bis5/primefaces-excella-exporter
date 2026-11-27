package org.primefaces.showcase.view.data;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

import javax.faces.view.ViewScoped;
import javax.inject.Named;

import org.primefaces.component.api.UIColumn;
import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;

import net.bis5.excella.primefaces.exporter.ExCellaExporterOptions;

@Named("passwordExportView")
@ViewScoped
public class PasswordExportView implements Serializable {

    private final List<PasswordRow> dataRows = Arrays.asList(
            new PasswordRow("data-1", "data-password-1", "data-text-1"),
            new PasswordRow("data-2", "", "data-text-2"),
            new PasswordRow("data-3", null, "data-text-3"));

    private PasswordRow dataSelection;

    private final TreeNode<PasswordRow> treeRoot = new DefaultTreeNode<>();

    private List<TreeNode<PasswordRow>> treeSelection;

    public PasswordExportView() {
        new DefaultTreeNode<>(new PasswordRow("tree-1", "tree-password-1", "tree-text-1"), treeRoot);
        new DefaultTreeNode<>(new PasswordRow("tree-2", "", "tree-text-2"), treeRoot);
        new DefaultTreeNode<>(new PasswordRow("tree-3", null, "tree-text-3"), treeRoot);
    }

    public List<PasswordRow> getDataRows() {
        return dataRows;
    }

    public PasswordRow getDataSelection() {
        return dataSelection;
    }

    public void setDataSelection(PasswordRow dataSelection) {
        this.dataSelection = dataSelection;
    }

    public TreeNode<PasswordRow> getTreeRoot() {
        return treeRoot;
    }

    public List<TreeNode<PasswordRow>> getTreeSelection() {
        return treeSelection;
    }

    public void setTreeSelection(List<TreeNode<PasswordRow>> treeSelection) {
        this.treeSelection = treeSelection;
    }

    public ExCellaExporterOptions getMaskedOptions() {
        ExCellaExporterOptions options = new ExCellaExporterOptions();
        options.setExportPassword(false);
        options.setColumnValueSeparator(" | ");
        return options;
    }

    public ExCellaExporterOptions getCustomReplacementOptions() {
        ExCellaExporterOptions options = getMaskedOptions();
        options.setPasswordReplacement("MASKED");
        return options;
    }

    public String passwordExportFunction(UIColumn column) {
        return "export-function";
    }

    public static class PasswordRow implements Serializable {
        private final String id;
        private final String password;
        private final String text;

        PasswordRow(String id, String password, String text) {
            this.id = id;
            this.password = password;
            this.text = text;
        }

        public String getId() {
            return id;
        }

        public String getPassword() {
            return password;
        }

        public String getText() {
            return text;
        }
    }
}
