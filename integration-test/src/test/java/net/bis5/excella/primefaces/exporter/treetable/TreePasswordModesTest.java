package net.bis5.excella.primefaces.exporter.treetable;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Path;

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
class TreePasswordModesTest extends AbstractPrimePageTest {

    @Test
    void exportPageOnly(Page page) throws IOException {
        page.exportPage.getRoot().click();
        assertMaskedRows("tree-password-page.xlsx", 2);
    }

    @Test
    void exportSelectionOnly(Page page) throws IOException {
        page.firstRow.click();
        page.exportSelection.getRoot().click();
        assertMaskedRows("tree-password-selection.xlsx", 1);
    }

    private void assertMaskedRows(String fileName, int expectedRows) throws IOException {
        Path localFile = Download.downloadFileToLocal(fileName);
        try (Workbook workbook = WorkbookFactory.create(localFile.toFile(), null, true)) {
            Sheet sheet = workbook.getSheetAt(0);
            assertEquals(expectedRows + 2, sheet.getPhysicalNumberOfRows());
            assertEquals("String", sheet.getRow(expectedRows + 1).getCell(0).getStringCellValue());
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
                    () -> assertEquals(expectedValue, row.getCell(6).getStringCellValue())
                );
            }
        }
    }

    public static class Page extends AbstractPrimePage {
        @FindBy(id = "form:exportPage")
        CommandLink exportPage;

        @FindBy(id = "form:exportSelection")
        CommandLink exportSelection;

        @FindBy(id = "form:tbl_node_0")
        WebElement firstRow;

        @Override
        public String getLocation() {
            return "ui/data/treetable/passwordModes.xhtml";
        }
    }
}
