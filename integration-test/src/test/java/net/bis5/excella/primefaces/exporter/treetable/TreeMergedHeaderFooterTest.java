package net.bis5.excella.primefaces.exporter.treetable;

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
import org.primefaces.model.TreeNode;
import org.primefaces.selenium.AbstractPrimePage;
import org.primefaces.selenium.AbstractPrimePageTest;
import org.primefaces.selenium.PrimeSelenium;
import org.primefaces.selenium.component.CommandLink;
import org.primefaces.showcase.view.data.treetable.BasicView.DataTypeCheck;
import org.primefaces.showcase.view.data.treetable.MergedHeaderFooterView;

import net.bis5.excella.primefaces.exporter.TakeScreenShotAfterFailure;
import net.bis5.excella.primefaces.exporter.ValueType;

@ExtendWith(TakeScreenShotAfterFailure.class)
class TreeMergedHeaderFooterTest extends AbstractPrimePageTest {

    private String getBaseDir() {
        return System.getProperty("basedir");
    }

    @Test
    void exportExcellaAjax(Page page) throws EncryptedDocumentException, IOException {
        var backingBean = new MergedHeaderFooterView();
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

        assertFileContent(parentRecord1, childRecord1, parentRecord2, childRecord2, "tt-merged-hf-ajax.xlsx");
    }

    @Test
    void exportExcellaNonAjax(Page page) throws EncryptedDocumentException, IOException {
        var backingBean = new MergedHeaderFooterView();
        backingBean.initialize();
        TreeNode<DataTypeCheck> parentNode = backingBean.getRoot().getChildren().get(0);
        TreeNode<DataTypeCheck> childNode = parentNode.getChildren().get(0);
        DataTypeCheck parentRecord1 = (DataTypeCheck) parentNode.getData();
        DataTypeCheck childRecord1 = (DataTypeCheck) childNode.getData();
        DataTypeCheck parentRecord2 = (DataTypeCheck) backingBean.getRoot().getChildren().get(1).getData();
        DataTypeCheck childRecord2 = (DataTypeCheck) backingBean.getRoot().getChildren().get(1).getChildren().get(0).getData();

        CommandLink link = page.commandLinkNonAjax;
        link.getRoot().click();

        assertFileContent(parentRecord1, childRecord1, parentRecord2, childRecord2, "tt-merged-hf-non-ajax.xlsx");
    }

    private static final int ROW_OFFSET = 2;
    private static final int COL_OFFSET = 1;
    private void assertFileContent(DataTypeCheck parentRecord1, DataTypeCheck childRecord1, DataTypeCheck parentRecord2, DataTypeCheck childRecord2, String outputFileName) throws EncryptedDocumentException, IOException {
        try (Workbook workbook = WorkbookFactory.create(new File(getBaseDir()+"/docker-compose/downloads/" + outputFileName), null, true)) {
            Sheet sheet = workbook.getSheetAt(0);
            List<String> detailHeaders = Arrays.asList("headerText B-1", "headerText B-2");

            Row groupedHeaderRow = sheet.getRow(ROW_OFFSET + 0);
            Row detailHeaderRow = sheet.getRow(ROW_OFFSET + 1);
            Row parentNode1Row = sheet.getRow(ROW_OFFSET + 2);
            Row childNode1Row = sheet.getRow(ROW_OFFSET + 3);
            Row parentNode2Row = sheet.getRow(ROW_OFFSET + 4);
            Row childNode2Row = sheet.getRow(ROW_OFFSET + 5);
            Row totalFooterRow = sheet.getRow(ROW_OFFSET + 6);
            Row eosRow = sheet.getRow(ROW_OFFSET + 7);
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
                () -> assertAll("Parent node1 row",
                    () -> assertEquals(0, parentNode1Row.getCell(COL_OFFSET).getCellStyle().getIndention(), "indention"),
                    () -> assertCell("P1-String cell", parentNode1Row.getCell(COL_OFFSET + 0), CellType.STRING, ValueType.STRING, parentRecord1.getStringProperty(), Cell::getStringCellValue),
                    () -> assertCell("YearMonth cell", parentNode1Row.getCell(COL_OFFSET + 1), CellType.NUMERIC, ValueType.YEAR_MONTH, parentRecord1.getYearMonthProperty().atDay(1).atStartOfDay(), Cell::getLocalDateTimeCellValue),
                    () -> assertCell("LocalDate cell", parentNode1Row.getCell(COL_OFFSET + 2), CellType.NUMERIC, ValueType.DATE, parentRecord1.getLocalDateProperty().atStartOfDay(), Cell::getLocalDateTimeCellValue)
                ),
                () -> assertAll("Child node1 row",
                    () -> assertEquals(1, childNode1Row.getCell(COL_OFFSET).getCellStyle().getIndention(), "indention"),
                    () -> assertCell("C1-String cell", childNode1Row.getCell(COL_OFFSET + 0), CellType.STRING, ValueType.STRING, childRecord1.getStringProperty(), Cell::getStringCellValue),
                    () -> assertCell("YearMonth cell", childNode1Row.getCell(COL_OFFSET + 1), CellType.NUMERIC, ValueType.YEAR_MONTH, childRecord1.getYearMonthProperty().atDay(1).atStartOfDay(), Cell::getLocalDateTimeCellValue),
                    () -> assertCell("LocalDate cell", childNode1Row.getCell(COL_OFFSET + 2), CellType.NUMERIC, ValueType.DATE, childRecord1.getLocalDateProperty().atStartOfDay(), Cell::getLocalDateTimeCellValue)
                ),
                () -> assertAll("Parent node2 row",
                    () -> assertEquals(0, parentNode2Row.getCell(COL_OFFSET).getCellStyle().getIndention(), "indention"),
                    () -> assertCell("P2-String cell", parentNode2Row.getCell(COL_OFFSET + 0), CellType.STRING, ValueType.STRING, parentRecord2.getStringProperty(), Cell::getStringCellValue),
                    () -> assertCell("YearMonth cell", parentNode2Row.getCell(COL_OFFSET + 1), CellType.NUMERIC, ValueType.YEAR_MONTH, parentRecord2.getYearMonthProperty().atDay(1).atStartOfDay(), Cell::getLocalDateTimeCellValue),
                    () -> assertCell("LocalDate cell", parentNode2Row.getCell(COL_OFFSET + 2), CellType.NUMERIC, ValueType.DATE, parentRecord2.getLocalDateProperty().atStartOfDay(), Cell::getLocalDateTimeCellValue)
                ),
                () -> assertAll("Child node2 row",
                    () -> assertEquals(1, childNode2Row.getCell(COL_OFFSET).getCellStyle().getIndention(), "indention"),
                    () -> assertCell("C2-String cell", childNode2Row.getCell(COL_OFFSET + 0), CellType.STRING, ValueType.STRING, childRecord2.getStringProperty(), Cell::getStringCellValue),
                    () -> assertCell("YearMonth cell", childNode2Row.getCell(COL_OFFSET + 1), CellType.NUMERIC, ValueType.YEAR_MONTH, childRecord2.getYearMonthProperty().atDay(1).atStartOfDay(), Cell::getLocalDateTimeCellValue),
                    () -> assertCell("LocalDate cell", childNode2Row.getCell(COL_OFFSET + 2), CellType.NUMERIC, ValueType.DATE, childRecord2.getLocalDateProperty().atStartOfDay(), Cell::getLocalDateTimeCellValue)
                ),
                () -> assertAll("Grouped Footer row",
                    () -> assertMergedRegion(sheet, ROW_OFFSET + 6, COL_OFFSET + 0, ROW_OFFSET + 6, COL_OFFSET + 1),
                    () -> assertEquals("Foot left", totalFooterRow.getCell(COL_OFFSET + 0).getStringCellValue()),
                    () -> assertEquals("Foot right", totalFooterRow.getCell(COL_OFFSET + 2).getStringCellValue())
                ),
                () -> assertAll("EOS row",
                    () -> assertMergedRegion(sheet, ROW_OFFSET + 7, COL_OFFSET + 0, ROW_OFFSET + 7, COL_OFFSET + 2),
                    () -> assertEquals("EOS", eosRow.getCell(COL_OFFSET + 0).getStringCellValue())
                )
            );
        }
    }

    @Test
    void exportExcellaAjaxSingleRow(Page page) throws EncryptedDocumentException, IOException {
        var backingBean = new MergedHeaderFooterView();
        backingBean.initialize();
        TreeNode<DataTypeCheck> parentNode = backingBean.getRoot().getChildren().get(0);
        TreeNode<DataTypeCheck> childNode = parentNode.getChildren().get(0);
        DataTypeCheck parentRecord1 = (DataTypeCheck) parentNode.getData();
        DataTypeCheck childRecord1 = (DataTypeCheck) childNode.getData();
        DataTypeCheck parentRecord2 = (DataTypeCheck) backingBean.getRoot().getChildren().get(1).getData();
        DataTypeCheck childRecord2 = (DataTypeCheck) backingBean.getRoot().getChildren().get(1).getChildren().get(0).getData();

        CommandLink link = page.commandLinkAjaxSingleRow;
        link.click();
        PrimeSelenium.wait(1000);

        assertFileContentSingleRow(parentRecord1, childRecord1, parentRecord2, childRecord2, "tt-merged-hf-ajax-sr.xlsx");
    }

    @Test
    void exportExcellaNonAjaxSingleRow(Page page) throws EncryptedDocumentException, IOException {
        var backingBean = new MergedHeaderFooterView();
        backingBean.initialize();
        TreeNode<DataTypeCheck> parentNode = backingBean.getRoot().getChildren().get(0);
        TreeNode<DataTypeCheck> childNode = parentNode.getChildren().get(0);
        DataTypeCheck parentRecord1 = (DataTypeCheck) parentNode.getData();
        DataTypeCheck childRecord1 = (DataTypeCheck) childNode.getData();
        DataTypeCheck parentRecord2 = (DataTypeCheck) backingBean.getRoot().getChildren().get(1).getData();
        DataTypeCheck childRecord2 = (DataTypeCheck) backingBean.getRoot().getChildren().get(1).getChildren().get(0).getData();

        CommandLink link = page.commandLinkNonAjaxSingleRow;
        link.getRoot().click();

        assertFileContentSingleRow(parentRecord1, childRecord1, parentRecord2, childRecord2, "tt-merged-hf-non-ajax-sr.xlsx");
    }

    private void assertFileContentSingleRow(DataTypeCheck parentRecord1, DataTypeCheck childRecord1, DataTypeCheck parentRecord2, DataTypeCheck childRecord2, String outputFileName) throws EncryptedDocumentException, IOException {
        try (Workbook workbook = WorkbookFactory.create(new File(getBaseDir()+"/docker-compose/downloads/" + outputFileName), null, true)) {
            Sheet sheet = workbook.getSheetAt(0);

            Row groupedHeaderRow = sheet.getRow(ROW_OFFSET + 0);
            Row parentNode1Row = sheet.getRow(ROW_OFFSET + 1);
            Row childNode1Row = sheet.getRow(ROW_OFFSET + 2);
            Row parentNode2Row = sheet.getRow(ROW_OFFSET + 3);
            Row childNode2Row = sheet.getRow(ROW_OFFSET + 4);
            Row groupedFooterRow = sheet.getRow(ROW_OFFSET + 5);
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
                () -> assertAll("Parent node1 row",
                    () -> assertEquals(0, parentNode1Row.getCell(COL_OFFSET).getCellStyle().getIndention(), "indention"),
                    () -> assertCell("P1-String cell", parentNode1Row.getCell(COL_OFFSET + 0), CellType.STRING, ValueType.STRING, parentRecord1.getStringProperty(), Cell::getStringCellValue),
                    () -> assertCell("YearMonth cell", parentNode1Row.getCell(COL_OFFSET + 1), CellType.NUMERIC, ValueType.YEAR_MONTH, parentRecord1.getYearMonthProperty().atDay(1).atStartOfDay(), Cell::getLocalDateTimeCellValue),
                    () -> assertCell("LocalDate cell", parentNode1Row.getCell(COL_OFFSET + 2), CellType.NUMERIC, ValueType.DATE, parentRecord1.getLocalDateProperty().atStartOfDay(), Cell::getLocalDateTimeCellValue)
                ),
                () -> assertAll("Child node1 row",
                    () -> assertEquals(1, childNode1Row.getCell(COL_OFFSET).getCellStyle().getIndention(), "indention"),
                    () -> assertCell("C1-String cell", childNode1Row.getCell(COL_OFFSET + 0), CellType.STRING, ValueType.STRING, childRecord1.getStringProperty(), Cell::getStringCellValue),
                    () -> assertCell("YearMonth cell", childNode1Row.getCell(COL_OFFSET + 1), CellType.NUMERIC, ValueType.YEAR_MONTH, childRecord1.getYearMonthProperty().atDay(1).atStartOfDay(), Cell::getLocalDateTimeCellValue),
                    () -> assertCell("LocalDate cell", childNode1Row.getCell(COL_OFFSET + 2), CellType.NUMERIC, ValueType.DATE, childRecord1.getLocalDateProperty().atStartOfDay(), Cell::getLocalDateTimeCellValue)
                ),
                () -> assertAll("Parent node2 row",
                    () -> assertEquals(0, parentNode2Row.getCell(COL_OFFSET).getCellStyle().getIndention(), "indention"),
                    () -> assertCell("P2-String cell", parentNode2Row.getCell(COL_OFFSET + 0), CellType.STRING, ValueType.STRING, parentRecord2.getStringProperty(), Cell::getStringCellValue),
                    () -> assertCell("YearMonth cell", parentNode2Row.getCell(COL_OFFSET + 1), CellType.NUMERIC, ValueType.YEAR_MONTH, parentRecord2.getYearMonthProperty().atDay(1).atStartOfDay(), Cell::getLocalDateTimeCellValue),
                    () -> assertCell("LocalDate cell", parentNode2Row.getCell(COL_OFFSET + 2), CellType.NUMERIC, ValueType.DATE, parentRecord2.getLocalDateProperty().atStartOfDay(), Cell::getLocalDateTimeCellValue)
                ),
                () -> assertAll("Child node2 row",
                    () -> assertEquals(1, childNode2Row.getCell(COL_OFFSET).getCellStyle().getIndention(), "indention"),
                    () -> assertCell("C2-String cell", childNode2Row.getCell(COL_OFFSET + 0), CellType.STRING, ValueType.STRING, childRecord2.getStringProperty(), Cell::getStringCellValue),
                    () -> assertCell("YearMonth cell", childNode2Row.getCell(COL_OFFSET + 1), CellType.NUMERIC, ValueType.YEAR_MONTH, childRecord2.getYearMonthProperty().atDay(1).atStartOfDay(), Cell::getLocalDateTimeCellValue),
                    () -> assertCell("LocalDate cell", childNode2Row.getCell(COL_OFFSET + 2), CellType.NUMERIC, ValueType.DATE, childRecord2.getLocalDateProperty().atStartOfDay(), Cell::getLocalDateTimeCellValue)
                ),
                () -> assertAll("Grouped Footer row",
                    () -> assertMergedRegion(sheet, ROW_OFFSET + 5, COL_OFFSET + 0, ROW_OFFSET + 5, COL_OFFSET + 2),
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
            return "ui/data/treetable/mergedHeaderFooter.xhtml";
        }

    }

}
