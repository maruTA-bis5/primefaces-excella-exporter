package net.bis5.excella.primefaces.exporter.datatable;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;

import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.primefaces.selenium.AbstractPrimePage;
import org.primefaces.selenium.AbstractPrimePageTest;
import org.primefaces.selenium.component.CommandLink;

import net.bis5.excella.primefaces.exporter.Download;
import net.bis5.excella.primefaces.exporter.TakeScreenShotAfterFailure;

@ExtendWith(TakeScreenShotAfterFailure.class)
class DataPasswordModesTest extends AbstractPrimePageTest {

    @Test
    void exportPageOnly(Page page) throws IOException {
        page.exportPage.getRoot().click();
        assertMaskedRows("password-page.xlsx", 2);
    }

    @Test
    void exportSelectionOnly(Page page) throws IOException {
        page.firstRow.click();
        page.exportSelection.getRoot().click();
        assertMaskedRows("password-selection.xlsx", 1);
    }

    @Test
    void exportColumnGroupFacets(Page page) throws IOException {
        page.exportGroupedFacets.getRoot().click();
        Path localFile = Download.downloadFileToLocal("password-grouped-facets.xlsx");
        try (Workbook workbook = WorkbookFactory.create(localFile.toFile(), null, true)) {
            Sheet sheet = workbook.getSheetAt(0);
            assertAll(
                () -> assertEquals("MASKED", sheet.getRow(0).getCell(0).getStringCellValue()),
                () -> assertEquals("MASKED", sheet.getRow(0).getCell(1).getStringCellValue()),
                () -> assertTrue(sheet.getRow(0).getCell(2).getStringCellValue().contains("$")),
                () -> assertEquals("MASKED", sheet.getRow(sheet.getLastRowNum()).getCell(0).getStringCellValue()),
                () -> assertEquals("MASKED", sheet.getRow(sheet.getLastRowNum()).getCell(1).getStringCellValue()),
                () -> assertTrue(sheet.getRow(sheet.getLastRowNum()).getCell(2).getStringCellValue().contains("$"))
            );
        }
    }

    @Test
    void exportChangingLinkValue(Page page) throws IOException {
        for (int attempt = 0; attempt < 2; attempt++) {
            page.exportChangingLinkValue.getRoot().click();
            Path localFile = Download.downloadFileToLocal("changing-link-value.xlsx");
            try (Workbook workbook = WorkbookFactory.create(localFile.toFile(), null, true)) {
                Sheet sheet = workbook.getSheetAt(0);
                assertEquals("Captured link label", sheet.getRow(1).getCell(0).getStringCellValue());
            }
        }
    }

    private void assertMaskedRows(String fileName, int expectedRows) throws IOException {
        Path localFile = Download.downloadFileToLocal(fileName);
        try (Workbook workbook = WorkbookFactory.create(localFile.toFile(), null, true)) {
            Sheet sheet = workbook.getSheetAt(0);
            assertEquals(expectedRows + 2, sheet.getPhysicalNumberOfRows());
            assertEquals("String", sheet.getRow(expectedRows + 1).getCell(0).getStringCellValue());
            assertAll(
                () -> assertEquals("MASKED", sheet.getRow(0).getCell(7).getStringCellValue()),
                () -> assertEquals("MASKED", sheet.getRow(expectedRows + 1).getCell(7).getStringCellValue()),
                () -> assertEquals("MASKED", sheet.getRow(0).getCell(8).getStringCellValue()),
                () -> assertEquals("MASKED", sheet.getRow(expectedRows + 1).getCell(8).getStringCellValue())
            );
            for (int i = 1; i <= expectedRows; i++) {
                Row row = sheet.getRow(i);
                String expectedValue = i + "-STRING value";
                assertAll(
                    () -> assertEquals(expectedValue, row.getCell(0).getStringCellValue()),
                    () -> assertEquals("MASKED", row.getCell(1).getStringCellValue()),
                    () -> assertEquals("MASKED", row.getCell(2).getStringCellValue()),
                    () -> assertEquals("MASKED", row.getCell(3).getStringCellValue()),
                    () -> assertEquals(expectedValue, row.getCell(4).getStringCellValue()),
                    () -> assertEquals("MASKED", row.getCell(5).getStringCellValue()),
                    () -> assertEquals(expectedValue, row.getCell(6).getStringCellValue()),
                    () -> assertEquals(CellType.NUMERIC, row.getCell(9).getCellType()),
                    () -> assertEquals(204.89, row.getCell(9).getNumericCellValue(), 0.001),
                    () -> assertEquals("Public link label", row.getCell(10).getStringCellValue()),
                    () -> assertEquals("MASKED", row.getCell(11).getStringCellValue()),
                    () -> assertEquals(expectedValue, row.getCell(12).getStringCellValue()),
                    () -> assertEquals("MASKED", row.getCell(13).getStringCellValue()),
                    () -> assertEquals("MASKED", row.getCell(14).getStringCellValue()),
                    () -> assertEquals("MASKED", row.getCell(15).getStringCellValue()),
                    () -> assertEquals("Hidden link label", row.getCell(16).getStringCellValue()),
                    () -> assertEquals("", row.getCell(17).getStringCellValue()),
                    () -> assertEquals("MASKED", row.getCell(18).getStringCellValue()),
                    () -> assertEquals("Public cell editor command label", row.getCell(19).getStringCellValue()),
                    () -> assertEquals("MASKED", row.getCell(20).getStringCellValue()),
                    () -> assertEquals("MASKED", row.getCell(21).getStringCellValue())
                );
            }
        }
    }

    public static class Page extends AbstractPrimePage {
        @FindBy(id = "form:exportPage")
        CommandLink exportPage;

        @FindBy(id = "form:exportSelection")
        CommandLink exportSelection;

        @FindBy(id = "form:exportGroupedFacets")
        CommandLink exportGroupedFacets;

        @FindBy(id = "form:exportChangingLinkValue")
        CommandLink exportChangingLinkValue;

        @FindBy(css = "[id='form:tbl_data'] > tr:first-child > td:first-child")
        WebElement firstRow;

        @Override
        public String getLocation() {
            return "ui/data/datatable/passwordModes.xhtml";
        }
    }
}
