package org.primefaces.showcase.view;

import javax.enterprise.context.RequestScoped;
import javax.inject.Named;

import org.primefaces.component.datatable.DataTable;
import org.primefaces.component.export.Exporter;
import org.primefaces.component.treetable.TreeTable;

import net.bis5.excella.primefaces.exporter.DataTableExcellaExporter;
import net.bis5.excella.primefaces.exporter.TreeTableExcellaExporter;

@Named
@RequestScoped
public class ExcellaExporter {

    public Exporter<DataTable> getDataTableExporter() {
        return new DataTableExcellaExporter();
    }

    public Exporter<TreeTable> getTreeTableExporter() {
        return new TreeTableExcellaExporter();
    }
}
