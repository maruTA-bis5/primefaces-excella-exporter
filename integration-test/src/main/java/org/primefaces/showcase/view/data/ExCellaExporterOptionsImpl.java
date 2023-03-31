package org.primefaces.showcase.view.data;

import org.primefaces.component.export.ExcelOptions;

import net.bis5.excella.primefaces.exporter.ExCellaExporterOptions;

public class ExCellaExporterOptionsImpl extends ExcelOptions implements ExCellaExporterOptions {

    private boolean throwExceptionWhenNoData;

    public void setThrowExceptionWhenNoData(boolean value) {
        throwExceptionWhenNoData = value;
    }

    @Override
    public boolean isThrowExceptionWhenNoData() {
        return throwExceptionWhenNoData;
    }

}
