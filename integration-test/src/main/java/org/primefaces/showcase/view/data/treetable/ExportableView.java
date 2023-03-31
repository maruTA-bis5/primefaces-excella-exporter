/*
 * Copyright 2009-2014 PrimeTek.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.primefaces.showcase.view.data.treetable;

import java.io.Serializable;

import javax.annotation.PostConstruct;
import javax.faces.view.ViewScoped;
import javax.inject.Named;

import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;
import org.primefaces.showcase.view.data.treetable.BasicView.DataTypeCheck;
import org.primefaces.showcase.view.data.treetable.BasicView.EvenOddNode;
@Named("ttExportableView")
@ViewScoped
public class ExportableView implements Serializable {

    public TreeNode<DataTypeCheck> getRoot() {
        return root;
    }

    public void setRoot(TreeNode<DataTypeCheck> root) {
        this.root = root;
    }

    private TreeNode<DataTypeCheck> root = new DefaultTreeNode<>();

    @PostConstruct
    public void initialize() {
        TreeNode<DataTypeCheck> parent = createTreeNode("P1-", root, false);
        createTreeNode("C1-", parent, true);
        parent.setExpanded(true);
    }

    private TreeNode<DataTypeCheck> createTreeNode(String namePrefix, TreeNode<DataTypeCheck> parent, boolean even) {
        return new EvenOddNode<>(new DataTypeCheck(namePrefix), parent, even);
    }

}
