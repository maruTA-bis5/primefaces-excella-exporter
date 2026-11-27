package net.bis5.excella.primefaces.exporter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    void defaultsToExportingPasswordValues() {
        ExCellaExporterOptions options = new ExCellaExporterOptions();

        assertTrue(options.isExportPassword());
        assertEquals("", options.getPasswordReplacement());
    }

    @Test
    void configuresPasswordExportAndReplacement() {
        ExCellaExporterOptions options = new ExCellaExporterOptions();

        options.setExportPassword(false);
        options.setPasswordReplacement("***");

        assertFalse(options.isExportPassword());
        assertEquals("***", options.getPasswordReplacement());
    }

    @Test
    void rejectsNullPasswordReplacement() {
        ExCellaExporterOptions options = new ExCellaExporterOptions();

        NullPointerException exception = assertThrows(NullPointerException.class,
                () -> options.setPasswordReplacement(null));

        assertEquals("passwordReplacement must not be null", exception.getMessage());
    }
}
