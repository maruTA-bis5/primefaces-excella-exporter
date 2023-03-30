package net.bis5.excella.primefaces.exporter;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.function.Function;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;

public class Assertions {

    public static void assertHeaderCell(int colIndex, Cell cell, String expectedValue) {
        assertCell("Column[" + colIndex+ "]: " + expectedValue, cell, CellType.STRING, ValueType.STRING, expectedValue, Cell::getStringCellValue);
    }

    public static <T> void assertCell(String description, Cell cell, CellType expectedCellType, ValueType expectedValueType, T expectedValue, Function<Cell, T> actualValueMapper) {
        var workbook = cell.getRow().getSheet().getWorkbook();
        String expectedDataFormat = workbook.createDataFormat().getFormat(expectedValueType.getFormat(workbook));
        String actualDataFormat = cell.getCellStyle().getDataFormatString();

        assertAll(description,
            () -> assertEquals(expectedCellType, cell.getCellType(), "cell type is incorrect"),
            () -> assertEquals(expectedValue, actualValueMapper.apply(cell), "cell value is incorrect"),
            () -> assertEquals(expectedDataFormat, actualDataFormat, "data format is incorrect")
        );
    }

    public static void assertMergedRegion(Sheet sheet, int fromRowIndex, int fromColIndex, int toRowIndex, int toColIndex) {
        CellRangeAddress expectedRange = new CellRangeAddress(fromRowIndex, toRowIndex, fromColIndex, toColIndex);
        List<CellRangeAddress> mergedRegions = sheet.getMergedRegions();
        assertTrue(mergedRegions.contains(expectedRange), () -> "Cell range [" + expectedRange + "] is not merged. merged regions: " + mergedRegions);
    }

    public static void assertBlankCell(String description, Cell cell) {
        assertEquals(CellType.BLANK, cell.getCellType(), "Cell is not blank");
    }
}
