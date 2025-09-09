package org.primefaces.showcase.view.data.treetable;

import java.io.Serializable;

import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;

import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;

@Named("ttDynamicColumnsView")
@ViewScoped
public class DynamicColumnsView implements Serializable {

    private final TreeNode<String> root = new DefaultTreeNode<>("Root", null);

    @PostConstruct
    public void init() {
        new DefaultTreeNode<>("row", root);
    }

    public TreeNode<String> getRoot() {
        return root;
    }

}
