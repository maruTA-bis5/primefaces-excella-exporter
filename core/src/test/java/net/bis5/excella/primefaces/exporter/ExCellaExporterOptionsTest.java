package net.bis5.excella.primefaces.exporter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ExCellaExporterOptionsTest {

    @Test
    void defaultsColumnValueSeparatorToSingleSpace() {
        ExCellaExporterOptions options = new ExCellaExporterOptions();

        assertEquals(" ", options.getColumnValueSeparator());
    }

    @Test
    void rejectsNullColumnValueSeparator() {
        ExCellaExporterOptions options = new ExCellaExporterOptions();

        assertThrows(NullPointerException.class, () -> options.setColumnValueSeparator(null));
    }

    @Test
    void allowsEmptyColumnValueSeparator() {
        ExCellaExporterOptions options = new ExCellaExporterOptions();

        options.setColumnValueSeparator("");

        assertEquals("", options.getColumnValueSeparator());
    }
}
