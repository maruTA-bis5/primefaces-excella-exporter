package net.bis5.excella.primefaces.exporter.treetable;

import static net.bis5.excella.primefaces.exporter.Assertions.assertBlankCell;
import static net.bis5.excella.primefaces.exporter.Assertions.assertCell;
import static net.bis5.excella.primefaces.exporter.Assertions.assertHeaderCell;
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
import org.openqa.selenium.WebElement;
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
class TreeNodeVarTest extends AbstractPrimePageTest {

    private String getBaseDir() {
        return System.getProperty("basedir");
    }

    @Test
    void exportExcellaAjax(Page page) throws EncryptedDocumentException, IOException {
        BasicView backingBean = new BasicView();
        backingBean.initialize();
        TreeNode<DataTypeCheck> parentNode = backingBean.getRoot().getChildren().get(0);
        TreeNode<DataTypeCheck> childNode = parentNode.getChildren().get(0);

        DataTypeCheck parentRecord1 = (DataTypeCheck) parentNode.getData();
        DataTypeCheck childRecord1 = (DataTypeCheck) childNode.getData();
        DataTypeCheck parentRecord2 = (DataTypeCheck) backingBean.getRoot().getChildren().get(1).getData();
        DataTypeCheck childRecord2 = (DataTypeCheck) backingBean.getRoot().getChildren().get(1).getChildren().get(0).getData();

        CommandLink link = page.commandLinkAjax;
        link.click();
        PrimeSelenium.wait(1000);

        assertFileContent(parentRecord1, childRecord1, parentRecord2, childRecord2, "tt-cars-ajax-nodevar.xlsx");
    }

    @Test
    void exportExcellaAjaxSelectionOnly(Page page) throws EncryptedDocumentException, IOException {
        BasicView backingBean = new BasicView();
        backingBean.initialize();
        TreeNode<DataTypeCheck> parentNode = backingBean.getRoot().getChildren().get(0);
        TreeNode<DataTypeCheck> childNode = parentNode.getChildren().get(0);
        DataTypeCheck parentRecord = (DataTypeCheck) parentNode.getData();
        DataTypeCheck childRecord = (DataTypeCheck) childNode.getData();

        page.firstParentRow.click();

        CommandLink link = page.commandLinkAjaxSelectionOnly;
        link.click();
        PrimeSelenium.wait(1000);

        assertFileContent(parentRecord, childRecord, "tt-cars-ajax-nodevar-selection.xlsx");
    }

    @Test
    void exportExcellaNonAjax(Page page) throws EncryptedDocumentException, IOException {
        BasicView backingBean = new BasicView();
        backingBean.initialize();
        TreeNode<DataTypeCheck> parentNode = backingBean.getRoot().getChildren().get(0);
        TreeNode<DataTypeCheck> childNode = parentNode.getChildren().get(0);

        DataTypeCheck parentRecord1 = (DataTypeCheck) parentNode.getData();
        DataTypeCheck childRecord1 = (DataTypeCheck) childNode.getData();
        DataTypeCheck parentRecord2 = (DataTypeCheck) backingBean.getRoot().getChildren().get(1).getData();
        DataTypeCheck childRecord2 = (DataTypeCheck) backingBean.getRoot().getChildren().get(1).getChildren().get(0).getData();

        CommandLink link = page.commandLinkNonAjax;
        link.getRoot().click();

        assertFileContent(parentRecord1, childRecord1, parentRecord2, childRecord2, "tt-cars-non-ajax-nodevar.xlsx");
    }

    @Test
    void exportExcellaNonAjaxSelectionOnly(Page page) throws EncryptedDocumentException, IOException {
        BasicView backingBean = new BasicView();
        backingBean.initialize();
        TreeNode<DataTypeCheck> parentNode = backingBean.getRoot().getChildren().get(0);
        TreeNode<DataTypeCheck> childNode = parentNode.getChildren().get(0);
        DataTypeCheck parentRecord = (DataTypeCheck) parentNode.getData();
        DataTypeCheck childRecord = (DataTypeCheck) childNode.getData();

        page.firstParentRow.click();

        CommandLink link = page.commandLinkNonAjaxSelectionOnly;
        link.getRoot().click();

        assertFileContent(parentRecord, childRecord, "tt-cars-non-ajax-nodevar-selection.xlsx");
    }

    private void assertFileContent(DataTypeCheck parentRecord1, DataTypeCheck childRecord1, DataTypeCheck parentRecord2, DataTypeCheck childRecord2, String fileName) throws EncryptedDocumentException, IOException {
        try (Workbook workbook = WorkbookFactory.create(new File(getBaseDir()+"/docker-compose/downloads/" + fileName), null, true)) {
            Sheet sheet = workbook.getSheetAt(0);
            List<String> headers = Arrays.asList("String", "YearMonth", "j.u.Date (date)", "j.u.Date (datetime)");

            Row headerRow = sheet.getRow(0);
            Row parentNodeRow1 = sheet.getRow(1);
            Row childNodeRow2 = sheet.getRow(2);
            Row parentNodeRow3 = sheet.getRow(3);
            Row childNodeRow4 = sheet.getRow(4);
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
                () -> assertAll("Parent node row 1 (odd)",
                    () -> assertEquals(0, parentNodeRow1.getCell(0).getCellStyle().getIndention(), "indention"),
                    () -> assertCell("String cell", parentNodeRow1.getCell(0), CellType.STRING, ValueType.STRING, parentRecord1.getStringProperty(), Cell::getStringCellValue),
                    () -> assertCell("Date cell", parentNodeRow1.getCell(2), CellType.NUMERIC, ValueType.DATE, parentRecord1.getDateProperty(), Cell::getDateCellValue)
                ),
                () -> assertAll("Child node row 2 (even)",
                    () -> assertEquals(1, childNodeRow2.getCell(0).getCellStyle().getIndention(), "indention"),
                    () -> assertBlankCell("String cell", childNodeRow2.getCell(0)),
                    () -> assertCell("YearMonth cell", childNodeRow2.getCell(1), CellType.NUMERIC, ValueType.YEAR_MONTH, childRecord1.getYearMonthProperty().atDay(1).atStartOfDay(), Cell::getLocalDateTimeCellValue),
                    () -> assertBlankCell("Date cell", childNodeRow2.getCell(2)),
                    () -> assertCell("Date time cell", childNodeRow2.getCell(3), CellType.NUMERIC, ValueType.DATE_TIME, childRecord1.getDateTimeProperty(), Cell::getDateCellValue)
                ),
                () -> assertAll("Parent node row 3 (odd)",
                    () -> assertEquals(0, parentNodeRow3.getCell(0).getCellStyle().getIndention(), "indention"),
                    () -> assertCell("String cell", parentNodeRow3.getCell(0), CellType.STRING, ValueType.STRING, parentRecord2.getStringProperty(), Cell::getStringCellValue),
                    () -> assertBlankCell("YearMonth cell", parentNodeRow3.getCell(1)),
                    () -> assertCell("Date cell", parentNodeRow3.getCell(2), CellType.NUMERIC, ValueType.DATE, parentRecord2.getDateProperty(), Cell::getDateCellValue),
                    () -> assertBlankCell("Date time cell", parentNodeRow3.getCell(3))
                ),
                () -> assertAll("Child node row 4 (even)",
                    () -> assertEquals(1, childNodeRow4.getCell(0).getCellStyle().getIndention(), "indention"),
                    () -> assertBlankCell("String cell", childNodeRow4.getCell(0)),
                    () -> assertCell("YearMonth cell", childNodeRow4.getCell(1), CellType.NUMERIC, ValueType.YEAR_MONTH, childRecord2.getYearMonthProperty().atDay(1).atStartOfDay(), Cell::getLocalDateTimeCellValue),
                    () -> assertBlankCell("Date cell", childNodeRow4.getCell(2)),
                    () -> assertCell("Date time cell", childNodeRow4.getCell(3), CellType.NUMERIC, ValueType.DATE_TIME, childRecord2.getDateTimeProperty(), Cell::getDateCellValue)
                )
            );
        }
    }

    private void assertFileContent(DataTypeCheck parentRecord, DataTypeCheck childRecord, String fileName) throws EncryptedDocumentException, IOException {
        try (Workbook workbook = WorkbookFactory.create(new File(getBaseDir()+"/docker-compose/downloads/" + fileName), null, true)) {
            Sheet sheet = workbook.getSheetAt(0);
            List<String> headers = Arrays.asList("String", "YearMonth", "j.u.Date (date)", "j.u.Date (datetime)");

            Row headerRow = sheet.getRow(0);
            Row parentNodeRow1 = sheet.getRow(1);
            Row childNodeRow2 = sheet.getRow(2);
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
                () -> assertAll("Parent node row 1 (odd)",
                    () -> assertEquals(0, parentNodeRow1.getCell(0).getCellStyle().getIndention(), "indention"),
                    () -> assertCell("String cell", parentNodeRow1.getCell(0), CellType.STRING, ValueType.STRING, parentRecord.getStringProperty(), Cell::getStringCellValue),
                    () -> assertBlankCell("YearMonth cell", parentNodeRow1.getCell(1)),
                    () -> assertCell("Date cell", parentNodeRow1.getCell(2), CellType.NUMERIC, ValueType.DATE, parentRecord.getDateProperty(), Cell::getDateCellValue),
                    () -> assertBlankCell("Date time cell", parentNodeRow1.getCell(3))
                ),
                () -> assertAll("Child node row 2 (even)",
                    () -> assertEquals(1, childNodeRow2.getCell(0).getCellStyle().getIndention(), "indention"),
                    () -> assertBlankCell("String cell", childNodeRow2.getCell(0)),
                    () -> assertCell("YearMonth cell", childNodeRow2.getCell(1), CellType.NUMERIC, ValueType.YEAR_MONTH, childRecord.getYearMonthProperty().atDay(1).atStartOfDay(), Cell::getLocalDateTimeCellValue),
                    () -> assertBlankCell("Date cell", childNodeRow2.getCell(2)),
                    () -> assertCell("Date time cell", childNodeRow2.getCell(3), CellType.NUMERIC, ValueType.DATE_TIME, childRecord.getDateTimeProperty(), Cell::getDateCellValue)
                )
            );
        }
    }
    public static class Page extends AbstractPrimePage {

        @FindBy(id = "form:tbl_node_0")
        WebElement firstParentRow;

        @FindBy(id = "form:excellaExportNonAjax")
        CommandLink commandLinkNonAjax;

        @FindBy(id = "form:excellaExportAjax")
        CommandLink commandLinkAjax;

        @FindBy(id = "form:excellaExportNonAjaxSelectionOnly")
        CommandLink commandLinkNonAjaxSelectionOnly;

        @FindBy(id = "form:excellaExportAjaxSelectionOnly")
        CommandLink commandLinkAjaxSelectionOnly;

        @Override
        public String getLocation() {
            return "ui/data/treetable/nodevar.xhtml";
        }

    }

}
