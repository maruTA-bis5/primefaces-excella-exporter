package net.bis5.excella.primefaces.exporter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import org.jspecify.annotations.Nullable;

public final class PasswordExportAssertions {

    private PasswordExportAssertions() {
    }

    public static void assertExport(Workbook workbook, String prefix, int dataRowCount,
            boolean masked, String replacement, String separator) {
        Sheet sheet = workbook.getSheetAt(0);
        assertEquals(dataRowCount + 1, sheet.getLastRowNum(), "exported row count is incorrect");

        assertText(sheet.getRow(0), 0, passwordValue("header-password", masked, replacement));
        assertText(sheet.getRow(0), 1, "Field password");
        assertText(sheet.getRow(0), 2, "Ordinary header");
        assertText(sheet.getRow(0), 16, "Nested ordinary text and password");
        if (masked) {
            assertText(sheet.getRow(0), 15, "wrapped-header" + separator + replacement);
        } else {
            assertHtmlPanelGroupString(sheet.getRow(0), 15);
        }

        for (int rowIndex = 1; rowIndex <= dataRowCount; rowIndex++) {
            String rowSuffix = Integer.toString(rowIndex);
            String textValue = prefix + "-text-" + rowSuffix;
            String rawPassword = passwordValue(rawPasswordForRow(prefix, rowIndex), masked, replacement);
            Row row = sheet.getRow(rowIndex);

            assertText(row, 0, rawPassword);
            assertText(row, 1, rawPassword);
            assertText(row, 2, textValue);
            assertText(row, 3, combinedValue(textValue, rawPassword, separator));
            assertText(row, 4, masked ? replacement : null);
            assertText(row, 5, rawPassword);
            assertText(row, 6, rawPassword);
            assertText(row, 7, rawPassword);
            assertText(row, 8, masked ? combinedValue(textValue, replacement, separator) : null);
            assertText(row, 9, null);
            assertText(row, 10, null);
            assertText(row, 11, masked ? null : rawPassword);
            assertText(row, 12, null);
            assertText(row, 13, masked ? "export-value" : rawPassword);
            assertText(row, 14, masked ? "export-function" : rawPassword);
            assertText(row, 15, textValue);
            assertText(row, 16, masked ? combinedValue(textValue, replacement, separator) : null);
        }

        Row footer = sheet.getRow(dataRowCount + 1);
        assertText(footer, 0, passwordValue("footer-password", masked, replacement));
        assertText(footer, 1, null);
        assertText(footer, 2, "Ordinary footer");
        assertText(footer, 16, null);
        if (masked) {
            assertText(footer, 15, "wrapped-footer" + separator + replacement);
        } else {
            assertHtmlPanelGroupString(footer, 15);
        }
    }

    private static @Nullable String rawPasswordForRow(String prefix, int rowIndex) {
        switch (rowIndex) {
        case 1:
            return prefix + "-password-1";
        case 2:
            return "";
        case 3:
            return null;
        default:
            throw new IllegalArgumentException("Unexpected row index: " + rowIndex);
        }
    }

    private static @Nullable String passwordValue(@Nullable String rawPassword, boolean masked, String replacement) {
        return masked ? replacement : rawPassword;
    }

    private static String combinedValue(String text, @Nullable String password, String separator) {
        return password == null ? text : text + separator + password;
    }

    private static void assertText(Row row, int columnIndex, @Nullable String expected) {
        Cell cell = row == null ? null : row.getCell(columnIndex);
        if (expected == null || expected.isEmpty()) {
            assertTrue(cell == null
                    || cell.getCellType() == CellType.BLANK
                    || (cell.getCellType() == CellType.STRING && cell.getStringCellValue().isEmpty()),
                    () -> "Cell[" + columnIndex + "] should be blank");
            return;
        }

        assertNotNull(cell, () -> "Cell[" + columnIndex + "] is missing");
        assertEquals(CellType.STRING, cell.getCellType(), "cell type is incorrect");
        assertEquals(expected, cell.getStringCellValue(), "Cell[" + columnIndex + "] value is incorrect");
    }

    private static void assertHtmlPanelGroupString(Row row, int columnIndex) {
        Cell cell = row == null ? null : row.getCell(columnIndex);
        assertNotNull(cell, () -> "Cell[" + columnIndex + "] is missing");
        assertEquals(CellType.STRING, cell.getCellType(), "cell type is incorrect");
        assertTrue(cell.getStringCellValue().contains("HtmlPanelGroup@"),
                () -> "Cell[" + columnIndex + "] should contain HtmlPanelGroup@");
    }
}
