package net.bis5.excella.primefaces.exporter.treetable;

import static net.bis5.excella.primefaces.exporter.Assertions.assertCell;
import static net.bis5.excella.primefaces.exporter.Assertions.assertHeaderCell;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

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
import org.primefaces.model.TreeNode;
import org.primefaces.selenium.AbstractPrimePage;
import org.primefaces.selenium.AbstractPrimePageTest;
import org.primefaces.selenium.PrimeSelenium;
import org.primefaces.selenium.component.CommandLink;
import org.primefaces.showcase.view.data.treetable.BasicView;
import org.primefaces.showcase.view.data.treetable.BasicView.DataTypeCheck;

import net.bis5.excella.primefaces.exporter.TakeScreenShotAfterFailure;
import net.bis5.excella.primefaces.exporter.ValueType;

@ExtendWith(TakeScreenShotAfterFailure.class)
public class TreeExportableColumnTest extends AbstractPrimePageTest {

    private String getBaseDir() {
        return System.getProperty("basedir");
    }

    @Test
    void exportExcellaAjax(Page page) throws EncryptedDocumentException, IOException {
        BasicView backingBean = new BasicView();
        backingBean.initialize();
        TreeNode<DataTypeCheck> parentNode = backingBean.getRoot().getChildren().get(0);
        TreeNode<DataTypeCheck> childNode = parentNode.getChildren().get(0);

        DataTypeCheck parentRecord = (DataTypeCheck) parentNode.getData();
        DataTypeCheck childRecord = (DataTypeCheck) childNode.getData();

        CommandLink link = page.commandLinkAjax;
        link.click();
        PrimeSelenium.wait(1000);

        assertFileContent(parentRecord, childRecord, "exportable-tt-cars-ajax.xlsx", false);
    }

    @Test
    void exportExcellaNonAjax(Page page) throws EncryptedDocumentException, IOException {
        BasicView backingBean = new BasicView();
        backingBean.initialize();
        TreeNode<DataTypeCheck> parentNode = backingBean.getRoot().getChildren().get(0);
        TreeNode<DataTypeCheck> childNode = parentNode.getChildren().get(0);

        DataTypeCheck parentRecord = (DataTypeCheck) parentNode.getData();
        DataTypeCheck childRecord = (DataTypeCheck) childNode.getData();

        CommandLink link = page.commandLinkNonAjax;
        link.getRoot().click();

        assertFileContent(parentRecord, childRecord, "exportable-tt-cars-non-ajax.xlsx", false);
    }

    @Test
    void exportExcellaAjaxVisibleOnly(Page page) throws EncryptedDocumentException, IOException {
        BasicView backingBean = new BasicView();
        backingBean.initialize();
        TreeNode<DataTypeCheck> parentNode = backingBean.getRoot().getChildren().get(0);
        TreeNode<DataTypeCheck> childNode = parentNode.getChildren().get(0);

        DataTypeCheck parentRecord = (DataTypeCheck) parentNode.getData();
        DataTypeCheck childRecord = (DataTypeCheck) childNode.getData();

        CommandLink link = page.commandLinkAjaxVisibleOnly;
        link.click();
        PrimeSelenium.wait(1000);

        assertFileContent(parentRecord, childRecord, "exportable-tt-cars-ajax-visible.xlsx", true);
    }

    @Test
    void exportExcellaNonAjaxVisibleOnly(Page page) throws EncryptedDocumentException, IOException {
        BasicView backingBean = new BasicView();
        backingBean.initialize();
        TreeNode<DataTypeCheck> parentNode = backingBean.getRoot().getChildren().get(0);
        TreeNode<DataTypeCheck> childNode = parentNode.getChildren().get(0);

        DataTypeCheck parentRecord = (DataTypeCheck) parentNode.getData();
        DataTypeCheck childRecord = (DataTypeCheck) childNode.getData();

        CommandLink link = page.commandLinkNonAjaxVisibleOnly;
        link.getRoot().click();

        assertFileContent(parentRecord, childRecord, "exportable-tt-cars-non-ajax-visible.xlsx", true);
    }

    private void assertFileContent(DataTypeCheck parentRecord, DataTypeCheck childRecord, String outputFileName, boolean visibleOnly) throws EncryptedDocumentException, IOException {
        try (Workbook workbook = WorkbookFactory.create(new File(getBaseDir()+"/docker-compose/downloads/" + outputFileName), null, true)) {
            Sheet sheet = workbook.getSheetAt(0);
            List<String> headers = visibleOnly ? Arrays.asList("String", "j.u.Date (date)") : Arrays.asList("String", "invisible", "j.u.Date (date)");

            Row headerRow = sheet.getRow(0);
            Row parentNodeRow = sheet.getRow(1);
            Row childNodeRow = sheet.getRow(2);
            Row footerRow = sheet.getRow(3);

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
                    assertions.add(() -> assertEquals(0, parentNodeRow.getCell(0).getCellStyle().getIndention(), "indention"));
                    var idx = new AtomicInteger();
                    assertions.add(() -> assertCell("String cell", parentNodeRow.getCell(idx.getAndIncrement()), CellType.STRING, ValueType.STRING, parentRecord.getStringProperty(), Cell::getStringCellValue));
                    if (!visibleOnly) {
                        assertions.add(() -> assertCell("invisible cell", parentNodeRow.getCell(idx.getAndIncrement()), CellType.STRING, ValueType.STRING, "", Cell::getStringCellValue));
                    }
                    assertions.add(() -> assertCell("Date cell", parentNodeRow.getCell(idx.getAndIncrement()), CellType.NUMERIC, ValueType.DATE, parentRecord.getDateProperty(), Cell::getDateCellValue));
                    assertAll("Parent node row", assertions.toArray(Executable[]::new));
                },
                () -> {
                    List<Executable> assertions = new ArrayList<>();
                    assertions.add(() -> assertEquals(1, childNodeRow.getCell(0).getCellStyle().getIndention(), "indention"));
                    var idx = new AtomicInteger();
                    assertions.add(() -> assertCell("String cell", childNodeRow.getCell(idx.getAndIncrement()), CellType.STRING, ValueType.STRING, childRecord.getStringProperty(), Cell::getStringCellValue));
                    if (!visibleOnly) {
                        assertions.add(() -> assertCell("invisible cell", childNodeRow.getCell(idx.getAndIncrement()), CellType.STRING, ValueType.STRING, "", Cell::getStringCellValue));
                    }
                    assertions.add(() -> assertCell("Date cell", childNodeRow.getCell(idx.getAndIncrement()), CellType.NUMERIC, ValueType.DATE, childRecord.getDateProperty(), Cell::getDateCellValue));
                    assertAll("Child node row", assertions.toArray(Executable[]::new));
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
            return "ui/data/treetable/exportable.xhtml";
        }

    }

}
