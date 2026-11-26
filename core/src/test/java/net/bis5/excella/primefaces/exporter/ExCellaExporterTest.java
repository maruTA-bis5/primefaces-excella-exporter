package net.bis5.excella.primefaces.exporter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.LocalTime;

import org.junit.jupiter.api.Test;

class ExCellaExporterTest {

    private final DataTableExcellaExporter exporter = new DataTableExcellaExporter();

    @Test
    void normalizesTimeStringsToExcelFractions() {
        assertEquals(Double.valueOf(0), exporter.normalizeValue("0:00"));
        assertEquals(Double.valueOf(1439d / 1440), exporter.normalizeValue("23:59"));
        assertEquals(Double.valueOf(1), exporter.normalizeValue("24:00"));
        assertEquals(Double.valueOf(1441d / 1440), exporter.normalizeValue("24:01"));
        assertEquals(Double.valueOf(1500d / 24), exporter.normalizeValue("1500:00"));
    }

    @Test
    void preservesInvalidOrUnrepresentableTimeStrings() {
        String invalidMinutes = "25:99";
        String tooLargeHour = "99999999999999999999:00";

        assertEquals(invalidMinutes, exporter.normalizeValue(invalidMinutes));
        assertEquals(tooLargeHour, exporter.normalizeValue(tooLargeHour));
    }

    @Test
    void normalizesLocalTimeAndPreservesNull() {
        LocalTime time = LocalTime.of(23, 59, 30, 123_000_000);

        assertEquals(Double.valueOf(time.toNanoOfDay() / 86_400_000_000_000d), exporter.normalizeValue(time));
        assertNull(exporter.normalizeValue(null));
    }
}
