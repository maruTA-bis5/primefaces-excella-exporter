package net.bis5.excella.primefaces.exporter.datatable;

import static net.bis5.excella.primefaces.exporter.Assertions.assertCell;
import static net.bis5.excella.primefaces.exporter.Assertions.assertHeaderCell;
import static net.bis5.excella.primefaces.exporter.Assertions.assertMergedRegion;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.openqa.selenium.support.FindBy;
import org.primefaces.selenium.AbstractPrimePage;
import org.primefaces.selenium.AbstractPrimePageTest;
import org.primefaces.selenium.PrimeSelenium;
import org.primefaces.selenium.component.CommandLink;
import org.primefaces.showcase.view.data.datatable.BasicView.DataTypeCheck;
import org.primefaces.showcase.view.data.datatable.MergedHeaderFooterView;

import net.bis5.excella.primefaces.exporter.TakeScreenShotAfterFailure;
import net.bis5.excella.primefaces.exporter.ValueType;

@ExtendWith(TakeScreenShotAfterFailure.class)
class MergedHeaderFooterTest extends AbstractPrimePageTest {

    private String getBaseDir() {
        return System.getProperty("basedir");
    }

    @Test
    void exportExcellaAjax(Page page) throws EncryptedDocumentException, IOException {
        var backingBean = new MergedHeaderFooterView();
        DataTypeCheck record = backingBean.getDataTypes().get(0);

        CommandLink link = page.commandLinkAjax;
        link.click();
        PrimeSelenium.wait(1000);

        assertFileContent(record, "merged-hf-ajax.xlsx");
    }

    @Test
    void exportExcellaNonAjax(Page page) throws EncryptedDocumentException, IOException {
        var backingBean = new MergedHeaderFooterView();
        DataTypeCheck record = backingBean.getDataTypes().get(0);

        CommandLink link = page.commandLinkNonAjax;
        link.getRoot().click();

        assertFileContent(record, "merged-hf-non-ajax.xlsx");
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
                        () -> assertHeaderCell(COL_OFFSET + 0, firstCell, "colspan"),
                        () -> assertHeaderCell(COL_OFFSET + 2, secondCell, "rowspan"),
                        () -> assertMergedRegion(sheet, ROW_OFFSET + 0, COL_OFFSET + 0, ROW_OFFSET + 0, COL_OFFSET + 1),
                        () -> assertMergedRegion(sheet, ROW_OFFSET + 0, COL_OFFSET + 2, ROW_OFFSET + 1, COL_OFFSET + 2)
                    );
                }),
                () -> {
                    List<Executable> headerAssertions = new ArrayList<>();
                    for (int i = 0; i < detailHeaders.size(); i++) {
                        Cell cell = detailHeaderRow.getCell(COL_OFFSET + i);
                        String expectedHeaderValue = detailHeaders.get(i);
                        var index = i;
                        headerAssertions.add(() -> assertHeaderCell(index, cell, expectedHeaderValue));
                    }
                    assertAll("Detail Header row", headerAssertions.toArray(Executable[]::new));
                },
                () -> assertAll("Data row",
                    () -> assertCell("String cell", dataRow.getCell(COL_OFFSET + 0), CellType.STRING, ValueType.STRING, record.getStringProperty(), Cell::getStringCellValue),
                    () -> assertCell("YearMonth cell", dataRow.getCell(COL_OFFSET + 1), CellType.NUMERIC, ValueType.YEAR_MONTH, record.getYearMonthProperty().atDay(1).atStartOfDay(), Cell::getLocalDateTimeCellValue),
                    () -> assertCell("LocalDate cell", dataRow.getCell(COL_OFFSET + 2), CellType.NUMERIC, ValueType.DATE, record.getLocalDateProperty().atStartOfDay(), Cell::getLocalDateTimeCellValue)
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
    void exportExcellaAjaxSingleRow(Page page) throws EncryptedDocumentException, IOException {
        var backingBean = new MergedHeaderFooterView();
        DataTypeCheck record = backingBean.getDataTypes().get(0);

        CommandLink link = page.commandLinkAjaxSingleRow;
        link.click();
        PrimeSelenium.wait(1000);

        assertFileContentSingleRow(record, "merged-hf-ajax-sr.xlsx");
    }

    @Test
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
                    () -> assertCell("String cell", dataRow.getCell(COL_OFFSET + 0), CellType.STRING, ValueType.STRING, record.getStringProperty(), Cell::getStringCellValue),
                    () -> assertCell("YearMonth cell", dataRow.getCell(COL_OFFSET + 1), CellType.NUMERIC, ValueType.YEAR_MONTH, record.getYearMonthProperty().atDay(1).atStartOfDay(), Cell::getLocalDateTimeCellValue),
                    () -> assertCell("LocalDate cell", dataRow.getCell(COL_OFFSET + 2), CellType.NUMERIC, ValueType.DATE, record.getLocalDateProperty().atStartOfDay(), Cell::getLocalDateTimeCellValue)
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
