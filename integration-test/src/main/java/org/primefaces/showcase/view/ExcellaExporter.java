package org.primefaces.showcase.view;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Named;

import org.primefaces.component.datatable.DataTable;
import org.primefaces.component.export.Exporter;
import org.primefaces.component.treetable.TreeTable;

import net.bis5.excella.primefaces.exporter.DataTableExcellaExporter;
import net.bis5.excella.primefaces.exporter.TreeTableExcellaExporter;

@Named
@RequestScoped
public class ExcellaExporter {

    public Exporter<DataTable> getDataTableExporter() {
        return DataTableExcellaExporter.builder().build();
    }

    public Exporter<TreeTable> getTreeTableExporter() {
        return TreeTableExcellaExporter.builder().build();
    }
}
