package net.bis5.excella.primefaces.exporter;

/**
 * Thrown by {@link DataTableExcellaExporter} and {@link TreeTableExcellaExporter} when no records found to export.
 */
public class EmptyDataException extends RuntimeException {

    public EmptyDataException() {
        super();
    }

    public EmptyDataException(String message) {
        super(message);
    }

    public EmptyDataException(Throwable cause) {
        super(cause);
    }

    public EmptyDataException(String message, Throwable cause) {
        super(message, cause);
    }

}
