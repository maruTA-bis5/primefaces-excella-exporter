package net.bis5.excella.primefaces.exporter.datatable;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.CellRangeAddress;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.support.FindBy;
import org.primefaces.selenium.AbstractPrimePage;
import org.primefaces.selenium.AbstractPrimePageTest;
import org.primefaces.selenium.PrimeSelenium;
import org.primefaces.selenium.component.CommandLink;
import org.primefaces.showcase.view.data.datatable.MergedHeaderFooterView;
import org.primefaces.showcase.view.data.datatable.BasicView.DataTypeCheck;

import net.bis5.excella.primefaces.exporter.DataTableExcellaExporter.ValueType;
import net.bis5.excella.primefaces.exporter.TakeScreenShotAfterFailure;

@ExtendWith(TakeScreenShotAfterFailure.class)
class MergedHeaderFooterTest extends AbstractPrimePageTest {


    private String getBaseDir() {
        return System.getProperty("basedir");
    }

    private <T> void assertCell(String description, Cell cell, CellType expectedType, T expectedValue, Function<Cell, T> actualValueMapper) {
        assertAll(description,
            () -> assertEquals(expectedType, cell.getCellType(), "cell type"),
            () -> assertEquals(expectedValue, actualValueMapper.apply(cell), "cell value")
        );
    }

    private <T> void assertCellFormat(String description, Cell cell, ValueType cellValueType) {
        Workbook workbook = cell.getRow().getSheet().getWorkbook();
        short expectedDataFormat = cellValueType.getFormat(workbook);

        CellStyle cellStyle = cell.getCellStyle();
        assertEquals(expectedDataFormat, cellStyle.getDataFormat(), description + " CellStyle.dataFormat");
    }

    @Test
    void exportExcellaAjax(Page page) throws EncryptedDocumentException, IOException {
        MergedHeaderFooterView backingBean = new MergedHeaderFooterView();
        DataTypeCheck record = backingBean.getDataTypes().get(0);

        CommandLink link = page.commandLinkAjax;
        link.click();
        PrimeSelenium.wait(1000);

        assertFileContent(record, "merged-hf-ajax.xlsx");
    }

    @Test
    void exportExcellaNonAjax(Page page) throws EncryptedDocumentException, IOException {
        MergedHeaderFooterView backingBean = new MergedHeaderFooterView();
        DataTypeCheck record = backingBean.getDataTypes().get(0);

        CommandLink link = page.commandLinkNonAjax;
        link.getRoot().click();

        assertFileContent(record, "merged-hf-non-ajax.xlsx");
    }

    private void assertMergedRegion(Sheet sheet, int fromRowIndex, int fromColIndex, int toRowIndex, int toColIndex) {
        CellRangeAddress expectedRange = new CellRangeAddress(fromRowIndex, toRowIndex, fromColIndex, toColIndex);
        List<CellRangeAddress> mergedRegions = sheet.getMergedRegions();
        assertTrue(mergedRegions.contains(expectedRange), () -> "Cell range [" + expectedRange + "] is not merged. merged regions: " + mergedRegions);
    }

    private static final int ROW_OFFSET = 2;
    private static final int COL_OFFSET = 1;
    private void assertFileContent(DataTypeCheck record, String outputFileName) throws EncryptedDocumentException, IOException {
        try (Workbook workbook = WorkbookFactory.create(new File(getBaseDir()+"/docker-compose/downloads/" + outputFileName), null, true)) {
            Sheet sheet = workbook.getSheetAt(0);
            List<String> detailHeaders = Arrays.asList("headerText B-1", "headerText B-2");

            Row groupedHeaderRow = sheet.getRow(ROW_OFFSET + 0);
            Row detailHeaderRow = sheet.getRow(ROW_OFFSET + 1);
            Row dataRow = sheet.getRow(ROW_OFFSET + 2);
            Row totalFooterRow = sheet.getRow(ROW_OFFSET + 3);
            Row eosRow = sheet.getRow(ROW_OFFSET + 4);
            assertAll(
                () -> assertAll("Grouped Header row", () -> {
                    Cell firstCell = groupedHeaderRow.getCell(COL_OFFSET + 0);
                    Cell secondCell = groupedHeaderRow.getCell(COL_OFFSET + 2);
                    assertAll(
                        () -> assertEquals("colspan", firstCell.getStringCellValue()),
                        () -> assertEquals("rowspan", secondCell.getStringCellValue()),
                        () -> assertMergedRegion(sheet, ROW_OFFSET + 0, COL_OFFSET + 0, ROW_OFFSET + 0, COL_OFFSET + 1),
                        () -> assertMergedRegion(sheet, ROW_OFFSET + 0, COL_OFFSET + 2, ROW_OFFSET + 1, COL_OFFSET + 2)
                    );
                }),
                () -> assertAll("Detail Header row", () -> {
                    for (int i = 0; i < detailHeaders.size(); i++) {
                        Cell cell = detailHeaderRow.getCell(COL_OFFSET + i);
                        assertEquals(CellType.STRING, cell.getCellType());
                        assertEquals(detailHeaders.get(i), cell.getStringCellValue());
                    }
                }),
                () -> assertAll("Data row",
                    () -> assertCell("String cell", dataRow.getCell(COL_OFFSET + 0), CellType.STRING, record.getStringProperty(), Cell::getStringCellValue),
                    () -> assertCell("YearMonth cell", dataRow.getCell(COL_OFFSET + 1), CellType.NUMERIC, record.getYearMonthProperty().atDay(1).atStartOfDay(), Cell::getLocalDateTimeCellValue),
                    () -> assertCellFormat("YearMonth cell data format", dataRow.getCell(COL_OFFSET + 1), ValueType.YEAR_MONTH),
                    () -> assertCell("LocalDate cell", dataRow.getCell(COL_OFFSET + 2), CellType.NUMERIC, record.getLocalDateProperty().atStartOfDay(), Cell::getLocalDateTimeCellValue),
                    () -> assertCellFormat("LocalDate cell data format", dataRow.getCell(COL_OFFSET + 2), ValueType.DATE)
                ),
                () -> assertAll("Grouped Footer row",
                    () -> assertMergedRegion(sheet, ROW_OFFSET + 3, COL_OFFSET + 0, ROW_OFFSET + 3, COL_OFFSET + 1),
                    () -> assertEquals("Foot left", totalFooterRow.getCell(COL_OFFSET + 0).getStringCellValue()),
                    () -> assertEquals("Foot right", totalFooterRow.getCell(COL_OFFSET + 2).getStringCellValue())
                ),
                () -> assertAll("EOS row",
                    () -> assertMergedRegion(sheet, ROW_OFFSET + 4, COL_OFFSET + 0, ROW_OFFSET + 4, COL_OFFSET + 2),
                    () -> assertEquals("EOS", eosRow.getCell(COL_OFFSET + 0).getStringCellValue())
                )
            );
        }
    }

    @Test
    @Disabled("#56")
    void exportExcellaAjaxSingleRow(Page page) throws EncryptedDocumentException, IOException {
        var backingBean = new MergedHeaderFooterView();
        DataTypeCheck record = backingBean.getDataTypes().get(0);

        CommandLink link = page.commandLinkAjaxSingleRow;
        link.click();
        PrimeSelenium.wait(1000);

        assertFileContentSingleRow(record, "merged-hf-ajax-sr.xlsx");
    }

    @Test
    @Disabled("#56")
    void exportExcellaNonAjaxSingleRow(Page page) throws EncryptedDocumentException, IOException {
        var backingBean = new MergedHeaderFooterView();
        DataTypeCheck record = backingBean.getDataTypes().get(0);

        CommandLink link = page.commandLinkNonAjaxSingleRow;
        link.getRoot().click();

        assertFileContentSingleRow(record, "merged-hf-non-ajax-sr.xlsx");
    }

    private void assertFileContentSingleRow(DataTypeCheck record, String outputFileName) throws EncryptedDocumentException, IOException {
        try (Workbook workbook = WorkbookFactory.create(new File(getBaseDir()+"/docker-compose/downloads/" + outputFileName), null, true)) {
            Sheet sheet = workbook.getSheetAt(0);

            Row groupedHeaderRow = sheet.getRow(ROW_OFFSET + 0);
            Row dataRow = sheet.getRow(ROW_OFFSET + 1);
            Row groupedFooterRow = sheet.getRow(ROW_OFFSET + 2);
            assertAll(
                () -> assertAll("Grouped Header row", () -> {
                    Cell firstCell = groupedHeaderRow.getCell(COL_OFFSET + 0);
                    Cell secondCell = groupedHeaderRow.getCell(COL_OFFSET + 2);
                    assertAll(
                        () -> assertEquals("colspan", firstCell.getStringCellValue()),
                        () -> assertEquals("col", secondCell.getStringCellValue()),
                        () -> assertMergedRegion(sheet, ROW_OFFSET + 0, COL_OFFSET + 0, ROW_OFFSET + 0, COL_OFFSET + 1)
                    );
                }),
                () -> assertAll("Data row",
                    () -> assertCell("String cell", dataRow.getCell(COL_OFFSET + 0), CellType.STRING, record.getStringProperty(), Cell::getStringCellValue),
                    () -> assertCell("YearMonth cell", dataRow.getCell(COL_OFFSET + 1), CellType.NUMERIC, record.getYearMonthProperty().atDay(1).atStartOfDay(), Cell::getLocalDateTimeCellValue),
                    () -> assertCellFormat("YearMonth cell data format", dataRow.getCell(COL_OFFSET + 1), ValueType.YEAR_MONTH),
                    () -> assertCell("LocalDate cell", dataRow.getCell(COL_OFFSET + 2), CellType.NUMERIC, record.getLocalDateProperty().atStartOfDay(), Cell::getLocalDateTimeCellValue),
                    () -> assertCellFormat("LocalDate cell data format", dataRow.getCell(COL_OFFSET + 2), ValueType.DATE)
                ),
                () -> assertAll("Grouped Footer row",
                    () -> assertMergedRegion(sheet, ROW_OFFSET + 2, COL_OFFSET + 0, ROW_OFFSET + 2, COL_OFFSET + 2),
                    () -> assertEquals("colspan3", groupedFooterRow.getCell(COL_OFFSET + 0).getStringCellValue())
                )
            );
        }
    }

    public static class Page extends AbstractPrimePage {

        @FindBy(id = "form:excellaExportNonAjax")
        CommandLink commandLinkNonAjax;

        @FindBy(id = "form:excellaExportAjax")
        CommandLink commandLinkAjax;

        @FindBy(id = "form:excellaExportNonAjaxSingleRow")
        CommandLink commandLinkNonAjaxSingleRow;

        @FindBy(id = "form:excellaExportAjaxSingleRow")
        CommandLink commandLinkAjaxSingleRow;

        @Override
        public String getLocation() {
            return "ui/data/datatable/mergedHeaderFooter.xhtml";
        }

    }

}
