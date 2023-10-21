package net.bis5.excella.primefaces.exporter;

import java.util.ArrayList;
import java.util.List;

import org.bbreak.excella.core.listener.PostSheetParseListener;
import org.bbreak.excella.core.listener.PreSheetParseListener;
import org.bbreak.excella.reports.listener.PostBookParseListener;
import org.bbreak.excella.reports.listener.PreBookParseListener;
import org.bbreak.excella.reports.processor.ReportProcessor;

class ListenerHolder {

    private final List<PreBookParseListener> preBookParseListeners = new ArrayList<>();
    private final List<PreSheetParseListener> preSheetParseListeners = new ArrayList<>();
    private final List<PostSheetParseListener> postSheetParseListeners = new ArrayList<>();
    private final List<PostBookParseListener> postBookParseListeners = new ArrayList<>();

    void addPreBookParseListener(PreBookParseListener listener) {
        preBookParseListeners.add(listener);
    }

    void addPreSheetParseListener(PreSheetParseListener listener) {
        preSheetParseListeners.add(listener);
    }

    void addPostSheetParseListener(PostSheetParseListener listener) {
        postSheetParseListeners.add(listener);
    }

    void addPostBookParseListener(PostBookParseListener listener) {
        postBookParseListeners.add(listener);
    }

    void applyListeners(ReportProcessor processor) {
        preBookParseListeners.forEach(processor::addListener);
        preSheetParseListeners.forEach(processor::addListener);
        postSheetParseListeners.forEach(processor::addListener);
        postBookParseListeners.forEach(processor::addListener);
    }
}
