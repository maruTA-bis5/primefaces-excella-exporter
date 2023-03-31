package net.bis5.excella.primefaces.exporter;

/**
 * ExCella exporter options
 */
public interface ExCellaExporterOptions {

    /**
     * If returns {@code true}, Exporter throws {@link EmptyDataException} when no data is exported.
     * @return throw exception when returns {@code true}, otherwise export file normally.
     */
    boolean isThrowExceptionWhenNoData();

}
