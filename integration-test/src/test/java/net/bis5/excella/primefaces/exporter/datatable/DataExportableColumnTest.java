package net.bis5.excella.primefaces.exporter.datatable;

import static net.bis5.excella.primefaces.exporter.Assertions.assertCell;
import static net.bis5.excella.primefaces.exporter.Assertions.assertHeaderCell;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

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
import org.primefaces.showcase.view.data.datatable.BasicView;
import org.primefaces.showcase.view.data.datatable.BasicView.DataTypeCheck;

import net.bis5.excella.primefaces.exporter.TakeScreenShotAfterFailure;
import net.bis5.excella.primefaces.exporter.ValueType;

@ExtendWith(TakeScreenShotAfterFailure.class)
class DataExportableColumnTest extends AbstractPrimePageTest {

    private String getBaseDir() {
        return System.getProperty("basedir");
    }

    @Test
    void exportExcellaAjax(Page page) throws EncryptedDocumentException, IOException {
        BasicView backingBean = new BasicView();
        DataTypeCheck record = backingBean.getDataTypes().get(0);

        CommandLink link = page.commandLinkAjax;
        link.click();
        PrimeSelenium.wait(1000);

        assertFileContent(record, "exportable-cars-ajax.xlsx", false);
    }

    @Test
    void exportExcellaNonAjax(Page page) throws EncryptedDocumentException, IOException {
        BasicView backingBean = new BasicView();
        DataTypeCheck record = backingBean.getDataTypes().get(0);

        CommandLink link = page.commandLinkNonAjax;
        link.getRoot().click();

        assertFileContent(record, "exportable-cars-non-ajax.xlsx", false);
    }

    @Test
    void exportExcellaAjaxVisibleOnly(Page page) throws EncryptedDocumentException, IOException {
        BasicView backingBean = new BasicView();
        DataTypeCheck record = backingBean.getDataTypes().get(0);

        CommandLink link = page.commandLinkAjaxVisibleOnly;
        link.click();
        PrimeSelenium.wait(1000);

        assertFileContent(record, "exportable-cars-ajax-visible.xlsx", true);
    }

    @Test
    void exportExcellaNonAjaxVisibleOnly(Page page) throws EncryptedDocumentException, IOException {
        BasicView backingBean = new BasicView();
        DataTypeCheck record = backingBean.getDataTypes().get(0);

        CommandLink link = page.commandLinkNonAjaxVisibleOnly;
        link.getRoot().click();

        assertFileContent(record, "exportable-cars-non-ajax-visible.xlsx", true);
    }

    private void assertFileContent(DataTypeCheck record, String outputFileName, boolean visibleOnly) throws EncryptedDocumentException, IOException {
        try (Workbook workbook = WorkbookFactory.create(new File(getBaseDir()+"/docker-compose/downloads/" + outputFileName), null, true)) {
            Sheet sheet = workbook.getSheetAt(0);
            List<String> headers = visibleOnly ? Arrays.asList("String", "j.u.Date (date)") : Arrays.asList("String", "invisible", "j.u.Date (date)");

            Row headerRow = sheet.getRow(0);
            Row dataRow = sheet.getRow(1);
            Row footerRow = sheet.getRow(2);

            assertAll(
                () -> {
                    List<Executable> assertions = new ArrayList<>();
                    for (int i = 0; i < headers.size(); i++) {
                        Cell cell = headerRow.getCell(i);
                        String expectedHeaderValue = headers.get(i);
                        var index = i;
                        assertions.add(() -> assertHeaderCell(index, cell, expectedHeaderValue));
                    }
                    assertAll("Header row", assertions.toArray(Executable[]::new));
                },
                () -> {
                    List<Executable> assertions = new ArrayList<>();
                    var idx = new AtomicInteger();
                    assertions.add(() -> assertCell("String cell", dataRow.getCell(idx.getAndIncrement()), CellType.STRING, ValueType.STRING, record.getStringProperty(), Cell::getStringCellValue));
                    if (!visibleOnly) {
                        assertions.add(() -> assertCell("invisible cell", dataRow.getCell(idx.getAndIncrement()), CellType.STRING, ValueType.STRING, "", Cell::getStringCellValue));
                    }
                    assertions.add(() -> assertCell("Date cell", dataRow.getCell(idx.getAndIncrement()), CellType.NUMERIC, ValueType.DATE, record.getDateProperty(), Cell::getDateCellValue));
                    assertAll("Data row", assertions.toArray(Executable[]::new));
                },
                () -> {
                    List<Executable> assertions = new ArrayList<>();
                    for (int i = 0; i < headers.size(); i++) {
                        Cell cell = footerRow.getCell(i);
                        String expectedFooterValue = headers.get(i);
                        var index = i;
                        assertions.add(() -> assertHeaderCell(index, cell, expectedFooterValue));
                    }
                    assertAll("Footer row", assertions.toArray(Executable[]::new));
                }
            );
        }
    }

    public static class Page extends AbstractPrimePage {

        @FindBy(id = "form:excellaExportNonAjax")
        CommandLink commandLinkNonAjax;

        @FindBy(id = "form:excellaExportAjax")
        CommandLink commandLinkAjax;

        @FindBy(id = "form:excellaExportNonAjaxVisibleOnly")
        CommandLink commandLinkNonAjaxVisibleOnly;

        @FindBy(id = "form:excellaExportAjaxVisibleOnly")
        CommandLink commandLinkAjaxVisibleOnly;

        @Override
        public String getLocation() {
            return "ui/data/datatable/exportable.xhtml";
        }

    }

}
