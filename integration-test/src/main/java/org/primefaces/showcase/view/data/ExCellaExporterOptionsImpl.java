package org.primefaces.showcase.view.data;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.primefaces.component.export.ExcelOptions;

import net.bis5.excella.primefaces.exporter.ExCellaExporterOptions;
import net.bis5.excella.primefaces.exporter.listener.BeforeWriteResponseListener;

public class ExCellaExporterOptionsImpl extends ExcelOptions implements ExCellaExporterOptions {

    private boolean throwExceptionWhenNoData;

    public void setThrowExceptionWhenNoData(boolean value) {
        throwExceptionWhenNoData = value;
    }

    @Override
    public boolean isThrowExceptionWhenNoData() {
        return throwExceptionWhenNoData;
    }

    private final List<BeforeWriteResponseListener> beforeWriteResponseListeners = new ArrayList<>();

    @Override
    public void addBeforeWriteResponseListener(BeforeWriteResponseListener listener) {
        beforeWriteResponseListeners.add(listener);
    }

    @Override
    public void removeBeforeWriteResponseListener(BeforeWriteResponseListener listener) {
        beforeWriteResponseListeners.remove(listener);
    }

    @Override
    public void forEachBeforeWriteResponseListener(ConsumerThrowsIOException<BeforeWriteResponseListener> consumer)
            throws IOException {
        for (BeforeWriteResponseListener listener : beforeWriteResponseListeners) {
            consumer.accept(listener);
        }
    }

}
