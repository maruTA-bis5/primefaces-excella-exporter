package net.bis5.excella.primefaces.exporter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

class ExCellaExporterOptionsTest {

    @Test
    void dataFormatOverridesSurviveSerialization() throws IOException, ClassNotFoundException {
        ExCellaExporterOptions options = new ExCellaExporterOptions();
        options.setOverrideDataType(ValueType.INTEGER, (short) 42);
        options.setOverrideDataType(ValueType.STRING, "@");

        byte[] serializedOptions;
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                ObjectOutputStream output = new ObjectOutputStream(bytes)) {
            output.writeObject(options);
            output.flush();
            serializedOptions = bytes.toByteArray();
        }

        ExCellaExporterOptions restoredOptions;
        try (ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(serializedOptions))) {
            restoredOptions = (ExCellaExporterOptions) input.readObject();
        }

        try (Workbook workbook = new XSSFWorkbook()) {
            assertEquals((short) 42, restoredOptions.getOverrideDataFormat(ValueType.INTEGER, workbook));
            assertEquals(workbook.createDataFormat().getFormat("@"),
                    restoredOptions.getOverrideDataFormat(ValueType.STRING, workbook));
        }
    }

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
