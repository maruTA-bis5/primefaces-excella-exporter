package net.bis5.excella.primefaces.exporter.datatable;

import static net.bis5.excella.primefaces.exporter.Assertions.assertCell;
import static net.bis5.excella.primefaces.exporter.Assertions.assertExportArea;
import static net.bis5.excella.primefaces.exporter.Assertions.assertMergedRegion;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.IntStream;

import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.support.FindBy;
import org.primefaces.selenium.AbstractPrimePage;
import org.primefaces.selenium.AbstractPrimePageTest;
import org.primefaces.selenium.PrimeSelenium;
import org.primefaces.selenium.component.CommandLink;
import org.primefaces.showcase.view.data.datatable.DynamicColumnsView;

import net.bis5.excella.primefaces.exporter.Download;
import net.bis5.excella.primefaces.exporter.TakeScreenShotAfterFailure;
import net.bis5.excella.primefaces.exporter.ValueType;

@ExtendWith(TakeScreenShotAfterFailure.class)
class DynamicColumnsTest extends AbstractPrimePageTest {

    @Test
    void exportExcellaAjaxRepeat(Page page) throws EncryptedDocumentException, IOException {
        var backingBean = new DynamicColumnsView();
        backingBean.init();
        List<String> rows = backingBean.getRows();

        CommandLink link = page.commandLinkAjaxRepeat;
        link.click();
        PrimeSelenium.wait(1000);

        assertFileContent(rows, "dynamic-repeat-ajax.xlsx");
    }

    @Test
    void exportExcellaNonAjaxRepeat(Page page) throws EncryptedDocumentException, IOException {
        var backingBean = new DynamicColumnsView();
        backingBean.init();
        List<String> rows = backingBean.getRows();

        CommandLink link = page.commandLinkNonAjaxRepeat;
        link.getRoot().click();

        assertFileContent(rows, "dynamic-repeat-non-ajax.xlsx");
    }

    @Test
    void exportExcellaAjaxColumns(Page page) throws EncryptedDocumentException, IOException {
        var backingBean = new DynamicColumnsView();
        backingBean.init();
        List<String> rows = backingBean.getRows();

        CommandLink link = page.commandLinkAjaxColumns;
        link.click();
        PrimeSelenium.wait(1000);

        assertFileContent(rows, "dynamic-columns-ajax.xlsx");
    }

    @Test
    void exportExcellaNonAjaxColumns(Page page) throws EncryptedDocumentException, IOException {
        var backingBean = new DynamicColumnsView();
        backingBean.init();
        List<String> rows = backingBean.getRows();

        CommandLink link = page.commandLinkNonAjaxColumns;
        link.getRoot().click();

        assertFileContent(rows, "dynamic-columns-non-ajax.xlsx");
    }

    private void assertFileContent(List<String> rows, String outputFileName) throws EncryptedDocumentException, IOException {
        Path localFile = Download.downloadFileToLocal(outputFileName);
        try (Workbook workbook = WorkbookFactory.create(localFile.toFile(), null, true)) {
            assertFileContent(rows, workbook);
        }
    }

    private void assertFileContent(List<String> rows, Workbook workbook) {
        Sheet sheet = workbook.getSheetAt(0);

        Row headerFirstRow = sheet.getRow(0);
        Row headerSecondRow = sheet.getRow(1);
        Row footerFirstRow = sheet.getRow(rows.size() + 2);
        Row footerSecondRow = sheet.getRow(rows.size() + 3);

        assertAll(
            // header
            () -> assertCell("header first row:0", headerFirstRow.getCell(0), CellType.STRING, ValueType.STRING, "hd parent 0", Cell::getStringCellValue), //
            () -> assertCell("header first row:1", headerFirstRow.getCell(1), CellType.STRING, ValueType.STRING, "hd parent 1", Cell::getStringCellValue), //
            () -> assertMergedRegion(sheet, 0, 1, 0, 2), //
            () -> assertCell("header first row:2", headerFirstRow.getCell(3), CellType.STRING, ValueType.STRING, "hd parent 2", Cell::getStringCellValue), //
            () -> assertMergedRegion(sheet, 0, 3, 0, 4), //
            () -> assertAll(IntStream.range(0, 5).mapToObj(i -> (() -> assertCell("header second row:" + i, headerSecondRow.getCell(i), CellType.STRING, ValueType.STRING, "hd child " + i, Cell::getStringCellValue)))), //

            // data
            () -> assertAll(IntStream.range(0, rows.size()).boxed().flatMap(r -> {
                int rownum = /*headers*/2 + r;
                return IntStream.range(0, 5).mapToObj(i -> (() -> assertCell("data row: " + r + " col:" +  i, sheet.getRow(rownum).getCell(i), CellType.STRING, ValueType.STRING, rows.get(r) + ":" + i, Cell::getStringCellValue)));
            })), //

            // footer
            () -> assertCell("footer first row:0", footerFirstRow.getCell(0), CellType.STRING, ValueType.STRING, "ft parent 0", Cell::getStringCellValue), //
            () -> assertCell("footer first row:1", footerFirstRow.getCell(1), CellType.STRING, ValueType.STRING, "ft parent 1", Cell::getStringCellValue), //
            () -> assertMergedRegion(sheet, rows.size() + 2, 1, rows.size() + 2, 2), //
            () -> assertCell("footer first row:2", footerFirstRow.getCell(3), CellType.STRING, ValueType.STRING, "ft parent 2", Cell::getStringCellValue), //
            () -> assertMergedRegion(sheet, rows.size() + 2, 3, rows.size() + 2, 4), //
            () -> assertAll(IntStream.range(0, 5).mapToObj(i -> (() -> assertCell("footer second row:" + i, footerSecondRow.getCell(i), CellType.STRING, ValueType.STRING, "ft child " + i, Cell::getStringCellValue)))), //

            () -> assertExportArea(sheet, 0, rows.size() + 3, 0, 4)
        );

    }

    @Test
    void exportExcellaAjaxNoGroup(Page page) throws EncryptedDocumentException, IOException {
        var backingBean = new DynamicColumnsView();
        backingBean.init();
        List<String> rows = backingBean.getRows();

        CommandLink link = page.commandLinkAjaxNoGroup;
        link.click();
        PrimeSelenium.wait(1000);

        assertFileContentNoGroup(rows, "dynamic-no-group-ajax.xlsx");
    }

    @Test
    void exportExcellaNonAjaxNoGroup(Page page) throws EncryptedDocumentException, IOException {
        var backingBean = new DynamicColumnsView();
        backingBean.init();
        List<String> rows = backingBean.getRows();

        CommandLink link = page.commandLinkNonAjaxNoGroup;
        link.getRoot().click();
        PrimeSelenium.wait(1000);

        assertFileContentNoGroup(rows, "dynamic-no-group-non-ajax.xlsx");
    }

    private void assertFileContentNoGroup(List<String> rows, String outputFileName) throws EncryptedDocumentException, IOException {
        Path localFile = Download.downloadFileToLocal(outputFileName);
        try (Workbook workbook = WorkbookFactory.create(localFile.toFile(), null, true)) {
            Sheet sheet = workbook.getSheetAt(0);

            Row headerRow = sheet.getRow(0);
            Row footerRow = sheet.getRow(rows.size() + 1);

            assertAll(
                // header
                () -> assertAll(IntStream.range(0, 5).mapToObj(i -> (() -> assertCell("header row:" + i, headerRow.getCell(i), CellType.STRING, ValueType.STRING, "hd " + i, Cell::getStringCellValue)))), //

                // data
                () -> assertAll(IntStream.range(0, rows.size()).boxed().flatMap(r -> {
                    int rownum = /*headers*/1 + r;
                    return IntStream.range(0, 5).mapToObj(i -> (() -> assertCell("data row: " + r + " col:" +  i, sheet.getRow(rownum).getCell(i), CellType.STRING, ValueType.STRING, rows.get(r) + ":" + i, Cell::getStringCellValue)));
                })), //

                // footer
                () -> assertAll(IntStream.range(0, 5).mapToObj(i -> (() -> assertCell("footer row:" + i, footerRow.getCell(i), CellType.STRING, ValueType.STRING, "ft " + i, Cell::getStringCellValue)))), //

                () -> assertExportArea(sheet, 0, rows.size() + 1, 0, 4)
            );
        }
    }

    public static class Page extends AbstractPrimePage {

        @FindBy(id = "form:excellaExportNonAjaxRepeat")
        CommandLink commandLinkNonAjaxRepeat;

        @FindBy(id = "form:excellaExportAjaxRepeat")
        CommandLink commandLinkAjaxRepeat;

        @FindBy(id = "form:excellaExportNonAjaxColumns")
        CommandLink commandLinkNonAjaxColumns;

        @FindBy(id = "form:excellaExportAjaxColumns")
        CommandLink commandLinkAjaxColumns;

        @FindBy(id = "form:excellaExportNonAjaxNoGroup")
        CommandLink commandLinkNonAjaxNoGroup;

        @FindBy(id = "form:excellaExportAjaxNoGroup")
        CommandLink commandLinkAjaxNoGroup;

        @Override
        public String getLocation() {
            return "ui/data/datatable/dynamicColumns.xhtml";
        }

    }
}
