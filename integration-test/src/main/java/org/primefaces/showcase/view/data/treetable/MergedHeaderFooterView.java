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

import javax.faces.view.ViewScoped;
import javax.inject.Named;

import org.primefaces.component.export.Exporter;
import org.primefaces.component.treetable.TreeTable;

import net.bis5.excella.primefaces.exporter.TreeTableExcellaExporter;


@Named("ttMergedHfView")
@ViewScoped
public class MergedHeaderFooterView extends BasicView {

    public Exporter<TreeTable> getTreeTableExporter() {
        return TreeTableExcellaExporter.builder()
            .templateUrl(MergedHeaderFooterView.class.getResource("/PositionChangedTemplate.xlsx"))
            .build();
    }

}
