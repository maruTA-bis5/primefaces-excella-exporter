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
package org.primefaces.showcase.view.data.datatable;

import javax.faces.view.ViewScoped;
import javax.inject.Named;

import org.primefaces.component.datatable.DataTable;
import org.primefaces.component.export.Exporter;

import net.bis5.excella.primefaces.exporter.DataTableExcellaExporter;


@Named("dtMergedHfView")
@ViewScoped
public class MergedHeaderFooterView extends BasicView {

    public Exporter<DataTable> getDataTableExporter() {
        var exporter = new DataTableExcellaExporter();

        exporter.setTemplateUrl(MergedHeaderFooterView.class.getResource("/PositionChangedTemplate.xlsx"));

        return exporter;
    }

}
