package net.bis5.excella.primefaces.listener;

import java.util.logging.Logger;

import javax.faces.FacesException;
import javax.faces.application.Application;
import javax.faces.context.FacesContext;
import javax.faces.context.FacesContextFactory;
import javax.faces.event.AbortProcessingException;
import javax.faces.event.SystemEvent;
import javax.faces.event.SystemEventListener;
import javax.faces.lifecycle.Lifecycle;

import org.primefaces.component.datatable.DataTable;
import org.primefaces.component.export.DataExporters;
import org.primefaces.component.treetable.TreeTable;

import net.bis5.excella.primefaces.exporter.DataTableExcellaExporter;
import net.bis5.excella.primefaces.exporter.TreeTableExcellaExporter;

/**
 * Register Excella Exporter to PrimeFaces.
 * <p>Note: This {@link FacesContextFactory} will return {@link FacesContext} using wrapped {@link FacesContextFactory}.</p>
 */
public class ExportersRegisterListener extends FacesContextFactory {

    private static final String EXPORTER_TYPE = "excella";

    private boolean registered = false;

    private void registerExporters() {
        if (registered) {
            return;
        }
        DataExporters.register(DataTable.class, DataTableExcellaExporter.class, EXPORTER_TYPE);
        DataExporters.register(TreeTable.class, TreeTableExcellaExporter.class, EXPORTER_TYPE);
        registered = true;
    }

    public ExportersRegisterListener() {
        super();
    }

    public ExportersRegisterListener(FacesContextFactory wrapped) {
        super(wrapped);
    }

    @Override
    public FacesContext getFacesContext(Object context, Object request, Object response, Lifecycle lifecycle)
            throws FacesException {

        var facesContext = getWrapped().getFacesContext(context, request, response, lifecycle);
        registerExporters();
        return facesContext;
    }

}
