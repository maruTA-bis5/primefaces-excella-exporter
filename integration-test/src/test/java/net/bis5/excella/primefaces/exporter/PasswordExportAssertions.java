package net.bis5.excella.primefaces.exporter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

public final class PasswordExportAssertions {

    private PasswordExportAssertions() {
    }

    public static void assertExport(Workbook workbook, String prefix, int dataRowCount,
            boolean masked, String replacement, String separator) {
        Sheet sheet = workbook.getSheetAt(0);
        assertEquals(dataRowCount + 1, sheet.getLastRowNum(), "exported row count is incorrect");

        assertText(sheet.getRow(0), 0, passwordValue("header-password", masked, replacement));
        assertText(sheet.getRow(0), 1, "Ordinary header");
        assertText(sheet.getRow(0), 12,
                "wrapped-header" + separator + passwordValue("nested-header-password", masked, replacement));

        for (int rowIndex = 1; rowIndex <= dataRowCount; rowIndex++) {
            String rowSuffix = Integer.toString(rowIndex);
            String textValue = prefix + "-text-" + rowSuffix;
            String rawPassword = passwordValue(rawPasswordForRow(prefix, rowIndex), masked, replacement);
            Row row = sheet.getRow(rowIndex);

            assertText(row, 0, rawPassword);
            assertText(row, 1, textValue);
            assertText(row, 2, combinedValue(textValue, rawPassword, separator));
            assertText(row, 3, rawPassword);
            assertText(row, 4, rawPassword);
            assertText(row, 5, rawPassword);
            assertText(row, 6, rawPassword);
            assertText(row, 7, combinedValue(textValue, rawPassword, separator));
            assertText(row, 8, null);
            assertText(row, 9, null);
            assertText(row, 10, "export-value");
            assertText(row, 11, "export-function");
            assertText(row, 12, textValue);
        }

        Row footer = sheet.getRow(dataRowCount + 1);
        assertText(footer, 0, passwordValue("footer-password", masked, replacement));
        assertText(footer, 1, "Ordinary footer");
        assertText(footer, 12,
                "wrapped-footer" + separator + passwordValue("nested-footer-password", masked, replacement));
    }

    private static String rawPasswordForRow(String prefix, int rowIndex) {
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

    private static String passwordValue(String rawPassword, boolean masked, String replacement) {
        return masked ? replacement : rawPassword;
    }

    private static String combinedValue(String text, String password, String separator) {
        return password == null ? text : text + separator + password;
    }

    private static void assertText(Row row, int columnIndex, String expected) {
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
}
